package foundation.oned6.dicegrid.protocol;

import foundation.oned6.dicegrid.comms.EspNowCommunicator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;

import static foundation.oned6.dicegrid.protocol.FaultReason.*;
import static foundation.oned6.dicegrid.protocol.NodeType.LOAD;
import static foundation.oned6.dicegrid.protocol.NodeType.SOURCE;
import static foundation.oned6.dicegrid.protocol.protocol_h.*;
import static java.lang.System.Logger.Level.WARNING;

class WirelessDicegridConnection implements GridConnection {
	private static final System.Logger logger = System.getLogger("WirelessDicegridConnection");

	private final ReentrantLock lock = new ReentrantLock(true);
	private final EspNowCommunicator communicator;

	public WirelessDicegridConnection(EspNowCommunicator communicator) {
		this.communicator = communicator;
	}

	@Override
	public List<NodeInfo> scan() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			var message = request_msg.allocate(Arena.ofAuto());
			request_msg.type(message, REQ_SCAN());
			request_msg.scan.include_states(request_msg.scan(message), false);

			var responses = communicator.broadcast(message);

			return parseScanResponses(responses);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Map<NodeInfo, NodeState> scanWithState() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			var message = request_msg.allocate(Arena.ofAuto());
			request_msg.type(message, REQ_SCAN());
			request_msg.scan.include_states(request_msg.scan(message), true);

			var responses = communicator.broadcast(message);

			return parseScanStatesResponses(responses);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void flash(NodeAddress address, byte[] firmware) throws DeviceException, InterruptedException {
		lock.lockInterruptibly();
		try {
			if (firmware.length > 8192)
				throw new IllegalArgumentException("firmware too large");

			int chunkCount = switch (firmware.length % CHUNK_SIZE) {
				case 0 -> firmware.length / CHUNK_SIZE;
				default -> firmware.length / CHUNK_SIZE + 1;
			};

			var message = request_msg.allocate(Arena.ofAuto());

			request_msg.type(message, REQ_FLASH_BEGIN());
			var flashBegin = request_msg.flash_begin(message);
			request_msg.flash_begin.node_id(flashBegin, address.nodeId());
			request_msg.flash_begin.total_size(flashBegin, (short) firmware.length);
			request_msg.flash_begin.total_chunks(flashBegin, (byte) chunkCount);

			expectOneOK(communicator.send(address.deviceMAC(), message));

			int offset = 0;
			int chunkRetryCount = 0;
			while (offset < firmware.length) {
				byte chunkIndex = (byte) (offset / CHUNK_SIZE);
				byte chunkSize = (byte) Math.min(CHUNK_SIZE, firmware.length - offset);

				var currentChunk = MemorySegment.ofArray(firmware).asSlice(offset, chunkSize);

				request_msg.type(message, REQ_FLASH_DATA());

				var flashData = request_msg.flash_data(message);
				request_msg.flash_data.chunk_idx(flashData, chunkIndex);
				request_msg.flash_data.crc(flashData, crc8(currentChunk));
				request_msg.flash_data.data(flashData).copyFrom(currentChunk);

				try {
					expectOneOK(communicator.send(address.deviceMAC(), message));
				} catch (DeviceException.ActionFailure e) {
					logger.log(WARNING, "failed to flash chunk " + chunkIndex, e);

					if (++chunkRetryCount > 3)
						throw e;

					continue;
				}

				chunkRetryCount = 0;
				offset += chunkSize;
			}

			request_msg.type(message, REQ_FLASH_DATA_END());
			request_msg.flash_data_end.crc(request_msg.flash_data_end(message), crc32(firmware));

			expectOneOK(communicator.send(address.deviceMAC(), message));
		} finally {
			lock.unlock();
		}
	}

	private ResponseMessage expectOneOK(EspNowCommunicator.ReceivedMessage[] responses) throws DeviceException.ProtocolError, DeviceException.ActionFailure {
		if (responses.length != 1) {
			throw new DeviceException.ProtocolError(responses[0].mac(), "unexpected number of responses: " + responses.length);
		}

		return switch (ResponseMessage.parse(responses[0].mac(), responses[0].data())) {
			case ResponseMessage received when received.ok() -> received;
			case ResponseMessage _ -> throw new DeviceException.ActionFailure(responses[0].mac());
		};
	}

	private static List<NodeInfo> parseScanResponses(EspNowCommunicator.ReceivedMessage[] responses) {
		var result = new ArrayList<NodeInfo>();
		for (var response : responses) {
			try {
				switch (ResponseMessage.parse(response.mac(), response.data())) {
					case ResponseMessage.Scan(boolean ok, NodeInfo[] infos) when ok ->
						result.addAll(Arrays.asList(infos));

					case ResponseMessage.Scan _ ->
						throw new DeviceException.ActionFailure(response.mac());
					case ResponseMessage received ->
						throw new DeviceException.ProtocolError(response.mac(), "unexpected response type: " + received);
				}
			} catch (DeviceException e) {
				logger.log(WARNING, "bad scan response", e);
			}
		}

		return Collections.unmodifiableList(result);
	}

	private static Map<NodeInfo, NodeState> parseScanStatesResponses(EspNowCommunicator.ReceivedMessage[] responses) {
		var result = new LinkedHashMap<NodeInfo, NodeState>();
		for (var response : responses) {
			try {
				switch (ResponseMessage.parse(response.mac(), response.data())) {
					case ResponseMessage.GetState(boolean ok, NodeInfo info, NodeState state) when ok ->
						result.put(info, state);

					case ResponseMessage.GetState _ ->
						throw new DeviceException.ActionFailure(response.mac());
					case ResponseMessage received ->
						throw new DeviceException.ProtocolError(response.mac(), "unexpected response type: " + received);
				}
			} catch (DeviceException e) {
				logger.log(WARNING, "bad scan response", e);
			}
		}

		return result;
	}

	@Override
	public void setEngaged(NodeAddress address, boolean engaged) throws DeviceException, InterruptedException {
		lock.lockInterruptibly();
		try (var arena = Arena.ofConfined()) {
			var message = request_msg.allocate(arena);

			request_msg.type(message, REQ_CONFIGURE_ENGAGEMENT());
			request_msg.configure_engagement.node_id(request_msg.configure_engagement(message), address.nodeId());
			request_msg.configure_engagement.engaged(request_msg.configure_engagement(message), engaged);

			var responses = communicator.send(address.deviceMAC(), message);
			if (responses.length != 1) {
				throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected number of responses: " + responses.length);
			}

			switch (ResponseMessage.parse(address.deviceMAC(), responses[0].data())) {
				case ResponseMessage.ConfigureEngagement(boolean ok) when !ok ->
					throw new DeviceException.ActionFailure(address);
				case ResponseMessage received ->
					throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected response type: " + received);
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setShutdown(NodeAddress address, boolean shutdown) throws DeviceException, InterruptedException {
		lock.lockInterruptibly();
		try (var arena = Arena.ofConfined()) {
			var message = request_msg.allocate(arena);

			request_msg.type(message, REQ_CONFIGURE_SHUTDOWN());
			request_msg.configure_shutdown.node_id(request_msg.configure_shutdown(message), address.nodeId());
			request_msg.configure_shutdown.shutdown(request_msg.configure_shutdown(message), shutdown);

			var responses = communicator.send(address.deviceMAC(), message);
			if (responses.length != 1) {
				throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected number of responses: " + responses.length);
			}

			switch (ResponseMessage.parse(address.deviceMAC(), responses[0].data())) {
				case ResponseMessage.ConfigureShutdown(boolean ok) when !ok ->
					throw new DeviceException.ActionFailure(address);
				case ResponseMessage received ->
					throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected response type: " + received);
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public NodeState getState(NodeAddress address) throws DeviceException, InterruptedException {
		lock.lockInterruptibly();
		try (var arena = Arena.ofConfined()) {
			var message = request_msg.allocate(arena);

			request_msg.type(message, REQ_NODE_STATE());
			request_msg.node_state.node_id(request_msg.node_state(message), address.nodeId());

			var responses = communicator.send(address.deviceMAC(), message);
			if (responses.length != 1) {
				throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected number of responses: " + responses.length);
			}

			return switch (ResponseMessage.parse(address.deviceMAC(), responses[0].data())) {
				case ResponseMessage.GetState(boolean ok, NodeInfo info, NodeState state)
					when ok && info.address().equals(address) -> state;
				case ResponseMessage.GetState _ -> throw new DeviceException.ActionFailure(address);
				case ResponseMessage received ->
					throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected response type: " + received);
			};
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		communicator.close();
	}

	private static byte crc8(MemorySegment seg) {
		var crc = new CRC8();
		crc.update(seg.asByteBuffer());
		return crc.crc();
	}

	private static int crc32(byte[] data) {
		var crc = new CRC32();
		crc.update(data);
		return (int) crc.getValue();
	}

	public sealed interface ResponseMessage {
		boolean ok();

		record FlashBegin(boolean ok) implements ResponseMessage {
		}

		record FlashData(boolean ok) implements ResponseMessage {
		}

		record FlashEnd(boolean ok) implements ResponseMessage {
		}

		record ConfigureShutdown(boolean ok) implements ResponseMessage {
		}

		record ConfigureEngagement(boolean ok) implements ResponseMessage {
		}

		record Scan(boolean ok, NodeInfo[] deviceInfo) implements ResponseMessage {
		}

		record GetState(boolean ok, NodeInfo info, NodeState state) implements ResponseMessage {
		}

		static ResponseMessage parse(byte[] sender, MemorySegment data) throws DeviceException.ProtocolError {
			var type = response_msg.type(data);
			var ok = response_msg.ok(data);

			return switch (type) {
				case RES_FLASH_BEGIN -> new FlashBegin(ok);
				case RES_FLASH_DATA -> new FlashData(ok);
				case RES_FLASH_DATA_END -> new FlashEnd(ok);
				case RES_CONFIGURE_SHUTDOWN -> new ConfigureShutdown(ok);
				case RES_CONFIGURE_ENGAGEMENT -> new ConfigureEngagement(ok);
				case RES_SCAN -> {
					var inner = response_msg.scan(data);

					var nodeCount = device_info.node_count(inner);
					var nodeInfos = device_info.nodes(inner);

					var result = new NodeInfo[nodeCount];
					for (byte i = 0; i < nodeCount; i++) {
						var nodeInfo = nodeInfos.asSlice(i * node_info.sizeof(), node_info.layout());
						result[i] = parseNodeInfo(sender, nodeInfo);
					}

					yield new Scan(ok, result);
				}
				case RES_NODE_STATE -> {
					var inner = response_msg.info_and_state(data);
					var infoStruct = response_msg.info_and_state.info(inner);
					var stateStruct = response_msg.info_and_state.state(inner);

					var info = parseNodeInfo(sender, infoStruct);
					var state = parseNodeState(sender, stateStruct);

					yield new GetState(ok, info, state);
				}
				default ->
					throw new DeviceException.ProtocolError(sender, "invalid packet: don't understand message type " + type);
			};
		}
	}

	private static NodeState parseNodeState(byte[] sender, MemorySegment stateStruct) throws DeviceException.ProtocolError {
		var faultReason = convertFaultReason(sender, stateStruct);

		return new NodeState(node_state.shutdown(stateStruct), node_state.engaged(stateStruct), node_state.current_rms_inner(stateStruct), node_state.current_rms_outer(stateStruct),
			node_state.voltage_rms(stateStruct), node_state.current_freq_inner(stateStruct), node_state.current_freq_outer(stateStruct), node_state.voltage_freq(stateStruct),
			node_state.phase_angle(stateStruct), node_state.currents_angle(stateStruct), node_state.current_thd_inner(stateStruct), node_state.current_thd_outer(stateStruct),
			node_state.voltage_thd(stateStruct), Optional.ofNullable(faultReason)
		);
	}

	private static FaultReason convertFaultReason(byte[] sender, MemorySegment stateStruct) throws DeviceException.ProtocolError {
		return switch (node_state.trip_reason(stateStruct)) {
			case TRIP_REASON_NONE -> null;
			case TRIP_REASON_OVERCURRENT -> OVERCURRENT;
			case TRIP_REASON_OVERVOLTAGE -> OVERVOLTAGE;
			case TRIP_REASON_ANGLE -> ANGLE;
			case TRIP_REASON_THD -> THD;
			case TRIP_REASON_MANUAL -> MANUALLY_TRIPPED;
			default ->
				throw new DeviceException.ProtocolError(sender, "invalid packet: don't understand trip reason " + node_state.trip_reason(stateStruct));
		};
	}

	private static NodeInfo parseNodeInfo(byte[] deviceMAC, MemorySegment struct) throws DeviceException.ProtocolError {
		var id = node_info.node_id(struct);
		var ownerID = node_info.owner_id(struct);
		var nodeType = switch (node_info.type(struct)) {
			case NODE_TYPE_SOURCE -> NodeType.SOURCE;
			case NODE_TYPE_LOAD -> NodeType.LOAD;
			default ->
				throw new DeviceException.ProtocolError(deviceMAC, "invalid packet: expected load type to be SOURCE or LOAD");
		};

		return new NodeInfo(new NodeAddress(deviceMAC, id), nodeType, ownerID);
	}
}

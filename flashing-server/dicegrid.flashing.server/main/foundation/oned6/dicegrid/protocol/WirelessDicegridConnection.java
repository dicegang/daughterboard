package foundation.oned6.dicegrid.protocol;

import foundation.oned6.dicegrid.comms.EspNowCommunicator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static foundation.oned6.dicegrid.protocol.NodeInfo.NodeType.LOAD;
import static foundation.oned6.dicegrid.protocol.NodeInfo.NodeType.SOURCE;
import static foundation.oned6.dicegrid.protocol.protocol_h.*;
import static java.lang.System.Logger.Level.WARNING;

public class WirelessDicegridConnection {
	private static final System.Logger logger = System.getLogger("WirelessDicegridConnection");

	private final ReentrantLock lock = new ReentrantLock(true);
	private final EspNowCommunicator communicator;

	public WirelessDicegridConnection(EspNowCommunicator communicator) {
		this.communicator = communicator;
	}

	public List<NodeInfo> scan() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			var message = request_msg.allocate(Arena.ofAuto());
			request_msg.type(message, REQ_SCAN());

			var responses = communicator.broadcast(message);

			return parseScanResponses(responses);
		} finally {
			lock.unlock();
		}
	}

	private static List<NodeInfo> parseScanResponses(EspNowCommunicator.ReceivedMessage[] responses) {
		var result = new ArrayList<NodeInfo>();
		for (var response : responses) {
			try {
				switch (ResponseMessage.parse(response.mac(), response.data())) {
					case ResponseMessage.Scan(boolean ok, NodeInfo[] infos) when ok ->
							result.addAll(Arrays.asList(infos));

					case ResponseMessage.Scan _ -> throw new DeviceException.ActionFailure(response.mac());
					case ResponseMessage received ->
							throw new DeviceException.ProtocolError(response.mac(), "unexpected response type: " + received);
				}
			} catch (DeviceException e) {
				logger.log(WARNING, "bad scan response", e);
			}
		}

		return Collections.unmodifiableList(result);
	}

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
				case ResponseMessage.ConfigureEngagement(boolean ok) when !ok -> throw new DeviceException.ActionFailure(address);
				case ResponseMessage received -> throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected response type: " + received);
			}
		} finally {
			lock.unlock();
		}
	}

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
				case ResponseMessage.ConfigureShutdown(boolean ok) when !ok -> throw new DeviceException.ActionFailure(address);
				case ResponseMessage received -> throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected response type: " + received);
			}
		} finally {
			lock.unlock();
		}
	}

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
				case ResponseMessage.GetState(boolean ok, NodeState state) when ok -> state;

				case ResponseMessage.GetState _ -> throw new DeviceException.ActionFailure(address);
				case ResponseMessage received -> throw new DeviceException.ProtocolError(address.deviceMAC(), "unexpected response type: " + received);
			};
		} finally {
			lock.unlock();
		}
	}

	public sealed interface ResponseMessage {
		boolean ok();

		record FlashAttiny(boolean ok) implements ResponseMessage {
		}

		record ConfigureShutdown(boolean ok) implements ResponseMessage {
		}

		record ConfigureEngagement(boolean ok) implements ResponseMessage {
		}

		record Scan(boolean ok, NodeInfo[] deviceInfo) implements ResponseMessage {
		}

		record GetState(boolean ok, NodeState state) implements ResponseMessage {
		}

		static ResponseMessage parse(byte[] sender, MemorySegment data) throws DeviceException.ProtocolError {
			var type = response_msg.type(data);
			var ok = response_msg.ok(data);

			return switch (type) {
				case RES_FLASH_ATTINY -> new FlashAttiny(ok);
				case RES_CONFIGURE_SHUTDOWN -> new ConfigureShutdown(ok);
				case RES_CONFIGURE_ENGAGEMENT -> new ConfigureEngagement(ok);
				case RES_SCAN -> {
					var inner = response_msg.scan(data);

					var nodeCount = device_info.node_count(inner);
					var nodeInfos = device_info.nodes(inner);

					var result = new NodeInfo[nodeCount];
					for (byte i = 0; i < nodeCount; i++) {
						var nodeInfo = nodeInfos.asSlice(i * node_info.sizeof(), node_info.layout());
						result[i] = new NodeInfo(new NodeAddress(sender, i), switch (node_info.type(nodeInfo)) {
							case NODE_TYPE_LOAD -> LOAD;
							case NODE_TYPE_SOURCE -> SOURCE;
							default ->
									throw new DeviceException.ProtocolError(sender, "invalid packet: expected load type to be SOURCE or LOAD");
						}, node_info.owner_id(nodeInfo));
					}

					yield new Scan(ok, result);
				}
				case RES_NODE_STATE -> {
					var inner = response_msg.node_state(data);
					yield new GetState(ok, new NodeState(node_state.shutdown(inner), node_state.engaged(inner), node_state.current_rms_inner(inner), node_state.current_rms_outer(inner),
							node_state.voltage_rms(inner), node_state.current_freq_inner(inner), node_state.current_freq_outer(inner), node_state.voltage_freq(inner),
							node_state.phase_angle(inner), node_state.currents_angle(inner), node_state.current_thd_inner(inner), node_state.current_thd_outer(inner),
							node_state.voltage_thd(inner)
					));
				}
				default -> throw new DeviceException.ProtocolError(sender, "invalid packet: don't understand message type " + type);
			};
		}
	}
}

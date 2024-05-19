package foundation.oned6.dicegrid.comms;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static foundation.oned6.dicegrid.comms.SlaveConfig.CLOCK_SPEED;
import static foundation.oned6.dicegrid.comms.SlaveConfig.SLAVE_BUS;
import static foundation.oned6.dicegrid.comms.comms_h.COMMS_REQ_BROADCAST;
import static foundation.oned6.dicegrid.comms.comms_h.COMMS_REQ_UNICAST;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class EspNowCommunicator {
	private static final Context context = Pi4J.newAutoContext();

	private final Spi spi;

	public EspNowCommunicator() {
		var spiConfig = SpiConfigBuilder.newInstance(context)
				.baud(CLOCK_SPEED)
				.bus(SLAVE_BUS)
				.chipSelect(SpiChipSelect.CS_0).build();

		this.spi = context.spi().create(spiConfig);
		spi.open();
	}

	public ReceivedMessage[] send(byte[] deviceMAC, MemorySegment data) throws InterruptedException {
		return send(COMMS_REQ_UNICAST(), deviceMAC, data);
	}

	public ReceivedMessage[] broadcast(MemorySegment data) throws InterruptedException {
		return send(COMMS_REQ_BROADCAST(), new byte[6], data);
	}

	private ReceivedMessage[] send(int messageType, byte[] deviceMAC, MemorySegment data) throws InterruptedException {
		try (var arena = Arena.ofConfined()) {
			var txBuf = arena.allocate(comms_request.layout());

			comms_request.type(txBuf, messageType);
			comms_request.recipient(txBuf, MemorySegment.ofArray(deviceMAC));
			comms_request.message_size(txBuf, (int) data.byteSize());

			spi.write(txBuf.asByteBuffer(), data.asByteBuffer());
			if (Thread.interrupted())
				throw new InterruptedException();

			int responseTotalSize = MemorySegment.ofArray(spi.readNBytes(4)).get(JAVA_INT, 0);
			var buffer = arena.allocate(responseTotalSize);
			comms_response.total_size(buffer, responseTotalSize);

			int read = spi.read(buffer.asSlice(comms_response.total_size$offset()).asByteBuffer());
			if (Thread.interrupted())
				throw new InterruptedException();

			assert read == responseTotalSize - 4;

			return parseResponse(buffer);
		}
	}


	private ReceivedMessage[] parseResponse(MemorySegment response) {
		var result = new ReceivedMessage[comms_response.message_count(response)];
		var messages = response.asSlice(comms_response.messages$offset());

		int totalSize = comms_response.total_size(response);
		long offset = comms_response.messages$offset();
		for (int i = 0; i < result.length; i++) {
			var message = messages.asSlice(offset, comms_response.messages.layout());

			var mac = comms_response.messages.sender(message);
			var messageSize = comms_response.messages.message_size(message);
			var data = messages.asSlice(offset + comms_response.sizeof(), messageSize);

			result[i] = new ReceivedMessage(mac.toArray(JAVA_BYTE), data);
			offset += comms_response.sizeof() + messageSize;
		}

		assert offset == totalSize;

		return result;
	}

	public record ReceivedMessage(byte[] mac, MemorySegment data) {
	}
}

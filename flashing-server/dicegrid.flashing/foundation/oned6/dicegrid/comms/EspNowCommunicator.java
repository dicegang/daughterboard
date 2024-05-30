package foundation.oned6.dicegrid.comms;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent;
import com.pi4j.io.gpio.digital.DigitalStateChangeListener;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static foundation.oned6.dicegrid.comms.SlaveConfig.*;
import static foundation.oned6.dicegrid.comms.comms_h.*;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class EspNowCommunicator implements AutoCloseable {
	private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(300);
	private static final Context context = Pi4J.newAutoContext();

	private final System.Logger logger = System.getLogger(EspNowCommunicator.class.getSimpleName());
	private final Spi spi;

	private final DigitalInput handshakeMISO;

	private final ReentrantLock lock = new ReentrantLock();

	public EspNowCommunicator() {
		this.handshakeMISO = context.din().create(MISO_HANDSHAKE_GPIO);

		var spiConfig = SpiConfigBuilder.newInstance(context)
			.baud(CLOCK_SPEED)
			.bus(SLAVE_BUS)
			.chipSelect(SS).build();

		this.spi = context.spi().create(spiConfig);
		spi.open();
	}

	public ReceivedMessage[] send(byte[] deviceMAC, MemorySegment data) throws InterruptedException {
		return send(comms_h.COMMS_REQ_UNICAST(), deviceMAC, data);
	}

	public ReceivedMessage[] broadcast(MemorySegment data) throws InterruptedException {
		return send(comms_h.COMMS_REQ_BROADCAST(), new byte[6], data);
	}

	public void ping() throws InterruptedException {
		var result = send(comms_h.COMMS_REQ_HELLO(), new byte[6], MemorySegment.NULL);
		if (result.length != 0)
			logger.log(WARNING, "unexpected response to hello: " + Arrays.toString(result));
	}

	private ReceivedMessage[] send(int messageType, byte[] deviceMAC, MemorySegment data) throws InterruptedException {
		lock.lockInterruptibly();

		var handshakeMISOEvents = new ArrayBlockingQueue<DigitalStateChangeEvent<?>>(5);
		DigitalStateChangeListener listener = handshakeMISOEvents::add;
		handshakeMISO.addListener(listener);

		try (var arena = Arena.ofConfined()) {
			if (handshakeMISO.isOn()) {
				var event = handshakeMISOEvents.poll(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS); // give it a chance to go low
				if (event == null)
					throw new IllegalStateException("handshake MISO is high when it shouldnt be");
			}

			byte[] b;
			while ((b = spi.readNBytes(65535)).length != 0) {
				logger.log(WARNING, "Discarding " + b.length + " garbage bytes in SPI buffer");
			}

			var txBuf = arena.allocate(comms_request.layout());

			comms_request.type(txBuf, messageType);
			comms_request.recipient(txBuf, MemorySegment.ofArray(deviceMAC));
			comms_request.message_size(txBuf, (int) data.byteSize());

			int result = spi.write(txBuf.asByteBuffer(), data.asByteBuffer());
			assert result == txBuf.byteSize() + data.byteSize();

			if (Thread.interrupted())
				throw new InterruptedException();

			var response = readWithHandshake(handshakeMISOEvents, arena);
			return parseResponse(response);
		} finally {
			handshakeMISO.removeListener(listener);
			lock.unlock();
		}
	}

	private MemorySegment readWithHandshake(ArrayBlockingQueue<DigitalStateChangeEvent<?>> handshakeMISOEvents, Arena arena) throws InterruptedException {
		var highEvent = handshakeMISOEvents.poll(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		if (highEvent == null)
			throw new IllegalStateException("timeout waiting for handshake to go high");

		if (highEvent.state() != DigitalState.HIGH)
			throw new IllegalStateException("this should not happen?");


		var lowEvent = handshakeMISOEvents.poll(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
		if (lowEvent == null)
			throw new IllegalStateException("timeout waiting for handshake to go low");

		if (lowEvent.state() != DigitalState.LOW)
			throw new IllegalStateException("this should not happen?");

		Thread.sleep(5); // give some time to let linux get its shit together

		int responseTotalSize = MemorySegment.ofArray(spi.readNBytes(4)).get(JAVA_INT, 0);
		var buffer = arena.allocate(responseTotalSize);
		comms_response.total_size(buffer, responseTotalSize);

		int read = spi.read(buffer.asSlice(comms_response.total_size$layout().byteSize()).asByteBuffer());
		assert read == responseTotalSize - 4;

		if (Thread.interrupted())
			throw new InterruptedException();

		return buffer;
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

	@Override
	public void close() {
		spi.close();
	}

	public record ReceivedMessage(byte[] mac, MemorySegment data) {
	}
}

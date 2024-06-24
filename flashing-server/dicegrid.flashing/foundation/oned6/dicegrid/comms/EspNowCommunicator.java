package foundation.oned6.dicegrid.comms;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiChipSelect;
import com.pi4j.io.spi.SpiConfigBuilder;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static foundation.oned6.dicegrid.comms.SlaveConfig.*;
import static foundation.oned6.dicegrid.comms.comms_h.*;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class EspNowCommunicator implements AutoCloseable {
	private static final Duration RESPONSE_TIMEOUT = Duration.ofMillis(300);
	private final System.Logger logger = System.getLogger(EspNowCommunicator.class.getSimpleName());
	private final SerialPort serialPort = SerialPort.getCommPort("/dev/cu.usbmodemT14XYIYUGUMV53");


	private final ReentrantLock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	public EspNowCommunicator() {
		serialPort.openPort();
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 100, 0);

		serialPort.setBaudRate(115200);
		serialPort.addDataListener(new SerialPortDataListener() {
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
			}

			@Override
			public void serialEvent(SerialPortEvent event) {
				lock.lock();
				try {
					condition.signalAll();
				} finally {
					lock.unlock();
				}
			}
		});
	}

	public ReceivedMessage[] send(byte[] deviceMAC, MemorySegment data) throws InterruptedException {
		return send(comms_h.COMMS_REQ_UNICAST(), deviceMAC, data);
	}

	public ReceivedMessage[] broadcast(MemorySegment data) throws InterruptedException {
		return send(comms_h.COMMS_REQ_BROADCAST(), new byte[6], data);
	}

	public void ping() throws InterruptedException {
		var result = send(comms_h.COMMS_REQ_HELLO(), new byte[6], MemorySegment.NULL);
		if (result.length != 1 || result[0].data.byteSize() != 0 || !Arrays.equals(result[0].mac, new byte[6]))
			logger.log(WARNING, "unexpected response to hello: " + Arrays.toString(result));
	}

	private ReceivedMessage[] send(int messageType, byte[] deviceMAC, MemorySegment data) throws InterruptedException {
		lock.lockInterruptibly();

		try (var arena = Arena.ofConfined()) {
			var txBuf = arena.allocate(comms_request.layout());

			comms_request.type(txBuf, messageType);
			comms_request.recipient(txBuf, MemorySegment.ofArray(deviceMAC));
			comms_request.message_size(txBuf, (int) data.byteSize());

			serialPort.writeBytes(txBuf.toArray(JAVA_BYTE), (int) txBuf.byteSize());
			serialPort.writeBytes(data.toArray(JAVA_BYTE), (int) data.byteSize());

			if (Thread.interrupted())
				throw new InterruptedException();

			var result = new ArrayList<ReceivedMessage>();

			var end = Instant.now().plus(RESPONSE_TIMEOUT);
			while (Instant.now().isBefore(end)) {
				byte[] sender = new byte[6];
				byte[] size = new byte[4];

				if (serialPort.readBytes(sender, 6) != 6)
					continue;

				serialPort.readBytes(size, 4);

				var sizeInt = ByteBuffer.wrap(size).order(ByteOrder.LITTLE_ENDIAN).getInt();
				var msg = new byte[sizeInt];
				serialPort.readBytes(msg, sizeInt);

				result.add(new ReceivedMessage(sender, MemorySegment.ofArray(msg)));
			}

			return result.toArray(ReceivedMessage[]::new);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		serialPort.closePort();
	}

	public record ReceivedMessage(byte[] mac, MemorySegment data) {
	}
}

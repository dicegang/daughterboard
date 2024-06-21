package foundation.oned6.dicegrid.comms;

import java.lang.foreign.MemorySegment;

public class CommsTest {
	public static void main(String[] args) throws InterruptedException {
		try (var c = new EspNowCommunicator()){
			c.ping();
		}
	}
}

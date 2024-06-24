package foundation.oned6.dicegrid.comms;

import foundation.oned6.dicegrid.protocol.GridConnection;

public class CommsTest {
	public static void main(String[] args) throws InterruptedException {
		try (var c = GridConnection.create()) {
			System.out.println(c.scan());
		}
	}
}

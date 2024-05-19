package foundation.oned6.dicegrid.protocol;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static foundation.oned6.dicegrid.protocol.protocol_h.*;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public abstract sealed class DeviceException extends Exception {
	public final byte[] deviceMAC;

	protected DeviceException(byte[] deviceMAC, String msg, Throwable cause) {
		super(msg, cause);
		this.deviceMAC = deviceMAC;
	}

	public static final class ActionFailure extends DeviceException {
		public final NodeAddress nodeAddress;

		public ActionFailure(NodeAddress nodeAddress) {
			super(nodeAddress.deviceMAC(), "device reported failure", null);
			this.nodeAddress = nodeAddress;
		}

		public ActionFailure(byte[] deviceMAC) {
			super(deviceMAC, "device reported failure", null);
			this.nodeAddress = null;
		}
	}

	public static final class ProtocolError extends DeviceException {
		public ProtocolError(byte[] deviceMAC, String msg) {
			super(deviceMAC, msg, null);
		}
	}
}

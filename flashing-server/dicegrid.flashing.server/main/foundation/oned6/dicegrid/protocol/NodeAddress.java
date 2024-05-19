package foundation.oned6.dicegrid.protocol;

import java.util.Arrays;

public record NodeAddress(byte[] deviceMAC, byte nodeId) {
	public static NodeAddress fromBytes(byte[] bytes) {
		return new NodeAddress(Arrays.copyOfRange(bytes, 0, 6), bytes[6]);
	}

	public byte[] asBytes() {
		var result = new byte[7];
		System.arraycopy(deviceMAC, 0, result, 0, 6);
		result[6] = nodeId;
		return result;
	}
}

package foundation.oned6.dicegrid.protocol;

import java.util.Arrays;

public record NodeAddress(byte[] deviceMAC, byte nodeId) {
}

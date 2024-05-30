package foundation.oned6.dicegrid.protocol;

import java.lang.foreign.MemorySegment;

public record NodeInfo(NodeAddress address, NodeType nodeType, int ownerID) {

	@Override
	public String toString() {
		return "Team %d %s".formatted(ownerID, address);
	}
}

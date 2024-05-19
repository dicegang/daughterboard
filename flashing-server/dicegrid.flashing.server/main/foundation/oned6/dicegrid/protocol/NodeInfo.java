package foundation.oned6.dicegrid.protocol;

public record NodeInfo(NodeAddress address, NodeType nodeType, int ownerID) {

	public enum NodeType {
		SOURCE, LOAD
	}
}

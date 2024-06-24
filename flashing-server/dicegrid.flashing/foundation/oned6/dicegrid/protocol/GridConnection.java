package foundation.oned6.dicegrid.protocol;

import foundation.oned6.dicegrid.comms.EspNowCommunicator;

import java.util.List;
import java.util.Map;

public interface GridConnection extends AutoCloseable {
	static GridConnection create() {
		return new WirelessDicegridConnection(new EspNowCommunicator());
	}

	List<NodeInfo> scan() throws InterruptedException;
	Map<NodeInfo, NodeState> scanWithState() throws InterruptedException;
	void flash(NodeAddress address, byte[] firmware) throws DeviceException, InterruptedException;
	void setEngaged(NodeAddress address, boolean engaged) throws DeviceException, InterruptedException;
	void setShutdown(NodeAddress address, boolean shutdown) throws DeviceException, InterruptedException;
	NodeState getState(NodeAddress address) throws DeviceException, InterruptedException;

	@Override
	void close();
}

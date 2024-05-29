package foundation.oned6.dicegrid.protocol;

import java.util.List;

public interface GridConnection {
	List<NodeInfo> scan() throws InterruptedException;
	void flash(NodeAddress address, byte[] firmware) throws DeviceException, InterruptedException;
	void setEngaged(NodeAddress address, boolean engaged) throws DeviceException, InterruptedException;
	void setShutdown(NodeAddress address, boolean shutdown) throws DeviceException, InterruptedException;
	NodeState getState(NodeAddress address) throws DeviceException, InterruptedException;


}

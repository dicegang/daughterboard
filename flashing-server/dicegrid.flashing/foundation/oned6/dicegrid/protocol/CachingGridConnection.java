package foundation.oned6.dicegrid.protocol;

import java.util.List;
import java.util.Map;

public class CachingGridConnection implements GridConnection {
	private final GridConnection delegate;

	private final Thread cacheThread;

	private volatile ScanResult cache = new ScanResult(null, Map.of());

	private record ScanResult(RuntimeException e, Map<NodeInfo, NodeState> nodes) {
	}

	public CachingGridConnection(GridConnection delegate) {
		this.delegate = delegate;
		cacheThread = Thread.startVirtualThread(this::cacheUpdateLoop);
	}

	private void cacheUpdateLoop() {
		while (!Thread.interrupted()) {
			try {
				cache = new ScanResult(null, delegate.scanWithState());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			} catch (RuntimeException e) {
				cache = new ScanResult(e, Map.of());
			}
		}
	}

	@Override
	public List<NodeInfo> scan() {
		var result = cache;
		if (result.e != null) {
			throw result.e;
		}

		return List.copyOf(result.nodes.keySet());
	}

	@Override
	public Map<NodeInfo, NodeState> scanWithState() {
		var result = cache;
		if (result.e != null) {
			throw result.e;
		}

		return Map.copyOf(result.nodes);
	}

	@Override
	public void flash(NodeAddress address, byte[] firmware) throws DeviceException, InterruptedException {
		delegate.flash(address, firmware);
	}

	@Override
	public void setEngaged(NodeAddress address, boolean engaged) throws DeviceException, InterruptedException {
		delegate.setEngaged(address, engaged);
	}

	@Override
	public void setShutdown(NodeAddress address, boolean shutdown) throws DeviceException, InterruptedException {
		delegate.setShutdown(address, shutdown);
	}

	@Override
	public NodeState getState(NodeAddress address) throws DeviceException, InterruptedException {
		return delegate.getState(address);
	}

	@Override
	public void close() {
		cacheThread.interrupt();
		try {
			cacheThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		delegate.close();
	}
}

package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.protocol.DeviceException;
import foundation.oned6.dicegrid.protocol.GridConnection;
import foundation.oned6.dicegrid.protocol.NodeAddress;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Consumer;

public class FlashingQueue {
	private final GridConnection gridConnection;
	private final SynchronousQueue<FlashingJob> jobs = new SynchronousQueue<>();

	public FlashingQueue(GridConnection gridConnection) {
		this.gridConnection = gridConnection;
		Thread.startVirtualThread(this::flashLoop);
	}

	public Future<?> submit(NodeAddress address, byte[] hex) throws InterruptedException {
		CompletableFuture<Void> future = new CompletableFuture<>();
		FlashingJob job = new FlashingJob(address, hex, () -> future.complete(null), future::completeExceptionally);
		jobs.put(job);
		return future;
	}

	private void flashLoop() {
		while (!Thread.interrupted()) {
			FlashingJob job = null;
			try {
				job = jobs.take();
				gridConnection.flash(job.address(), job.hex());
				job.success().run();
			} catch (InterruptedException e) {
				return;
			} catch (DeviceException e) {
				job.failure().accept(e);
			}
		}
	}

	public record FlashingJob(NodeAddress address, byte[] hex, Runnable success, Consumer<DeviceException> failure) {
	}
}

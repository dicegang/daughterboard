package foundation.oned6.dicegrid.protocol;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static foundation.oned6.dicegrid.protocol.NodeType.LOAD;
import static foundation.oned6.dicegrid.protocol.NodeType.SOURCE;
import static java.lang.Math.PI;
import static java.lang.Math.signum;

public class FakeGridConnection implements GridConnection {
	private final List<NodeInfo> nodes = List.of(
		new NodeInfo(new NodeAddress(new byte[]{0, 0, 0, 0, 0, 0}, (byte) 1), SOURCE, 1),
		new NodeInfo(new NodeAddress(new byte[]{0, 0, 0, 0, 0, 0}, (byte) 2), LOAD, 1),
		new NodeInfo(new NodeAddress(new byte[]{0, 0, 0, 0, 0, 0}, (byte) 3), SOURCE, 2),
		new NodeInfo(new NodeAddress(new byte[]{0, 0, 0, 0, 0, 0}, (byte) 4), LOAD, 2)
	);

	private final boolean[] engaged = new boolean[nodes.size()];
	private final boolean[] shutdown = new boolean[nodes.size()];

	@Override
	public List<NodeInfo> scan() throws InterruptedException {
		return nodes;
	}

	@Override
	public void flash(NodeAddress address, byte[] firmware) throws DeviceException, InterruptedException {

	}

	@Override
	public void setEngaged(NodeAddress address, boolean engaged) throws DeviceException, InterruptedException {
		var index = indexOf(address);

		if (index == nodes.size()) {
			throw new DeviceException.ActionFailure(address);
		}

		this.engaged[index] = engaged;
	}

	@Override
	public void setShutdown(NodeAddress address, boolean shutdown) throws DeviceException, InterruptedException {
		var index = indexOf(address);

		if (index == nodes.size()) {
			throw new DeviceException.ActionFailure(address);
		}

		this.shutdown[index] = shutdown;
	}

	@Override
	public NodeState getState(NodeAddress address) throws DeviceException, InterruptedException {
		int index = indexOf(address);

		var r = ThreadLocalRandom.current();
		FaultReason faultReason = null;
		if (shutdown[index]) {
			faultReason = FaultReason.values()[r.nextInt(FaultReason.values().length)];
		}

		boolean engaged = this.engaged[index];
		boolean shutdown = this.shutdown[index];

		double currentOuter = r.nextGaussian(1, 0.05);
		double currentInner = shutdown ? 0 : engaged ? currentOuter : r.nextDouble();
		double voltage = shutdown ? 0 : signum(currentInner) * 40 * r.nextDouble() * (engaged ? 1. : 0.01);
		double currentFreqOuter =  r.nextGaussian(2 * PI * 50, 0.5);
		double currentFreqInner = shutdown ? 0 : engaged ? currentFreqOuter : r.nextGaussian(2 * PI * 50, 0.5);
		double voltageFreq = shutdown ? 0 : r.nextGaussian(2 * PI * 50, 0.5);
		double phaseAngle = shutdown ? 0 : r.nextGaussian(0, 0.1);
		double currentsAngle = engaged ? 0 : r.nextGaussian(0, 0.1);
		double currentsThdInner = shutdown ? 0 : r.nextDouble() * 0.1;
		double currentsThdOuter = engaged ? currentsThdInner : r.nextDouble() * 0.05;
		double voltageThd = shutdown ? 0 : r.nextDouble() * 0.05;


		return new NodeState(
			shutdown,
			engaged,
			currentInner,
			currentOuter,
			voltage,
			currentFreqInner,
			currentFreqOuter,
			voltageFreq,
			phaseAngle,
			currentsAngle,
			currentsThdInner,
			currentsThdOuter,
			voltageThd,
			Optional.ofNullable(faultReason)
		);
	}

	private int indexOf(NodeAddress address) {
		int index;
		for (index = 0; index < nodes.size(); index++) {
			if (nodes.get(index).address().equals(address)) {
				break;
			}
		}

		return index;
	}
}

package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.protocol.FaultReason;
import foundation.oned6.dicegrid.protocol.NodeState;
import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static foundation.oned6.dicegrid.server.Server.context;
import static java.lang.Math.*;

public record TestDataPage() implements View {
	@Override
	public String html() {
		var inner = new NodeDataTable(randomData()).html();

		if (context().queryParam("update").isPresent()) {
			return inner;
		}

		return """
			<div hx-get="%s" hx-trigger="every 1s">%s</div>
			""".formatted(context().contentRoot().resolve("test?update"), inner);
	}

	@Override
	public String title() {
		return "skibi";
	}

	private List<NodeDataEntry> randomData() {
		var result = new ArrayList<NodeDataEntry>();

		for (int i = 0; i < 10; i++) {
			var name = new TeamPrincipal("Team" + i, i);
			var r = ThreadLocalRandom.current();

			FaultReason faultReason = null;
			if (r.nextInt(4) == 0) {
				faultReason = FaultReason.values()[r.nextInt(FaultReason.values().length)];
			}

			boolean engaged = faultReason == null && r.nextBoolean();
			double currentInner = r.nextDouble();
			double currentOuter = engaged ? currentInner : r.nextDouble();
			double voltage = signum(currentInner) * 40 * r.nextDouble() * (engaged ? 1. : 0.01);
			double currentFreqInner = r.nextGaussian(2 * PI * 50, 0.5);
			double currentFreqOuter = engaged ? currentFreqInner : r.nextGaussian(2 * PI * 50, 0.5);
			double voltageFreq = r.nextGaussian(2 * PI * 50, 0.5);
			double phaseAngle = r.nextGaussian(0, 0.1);
			double currentsAngle = engaged ? 0 : r.nextGaussian(0, 0.1);
			double currentsThdInner = r.nextDouble() * 0.1;
			double currentsThdOuter = engaged ? currentsThdInner : r.nextDouble() * 0.05;
			double voltageThd = r.nextDouble() * 0.05;


			var state = new NodeState(
				faultReason != null,
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

			result.add( NodeDataEntry.of(name, NodeType.values()[i % 2], state));
		}

		return result;
	}
}

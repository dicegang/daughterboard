package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.protocol.NodeState;
import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.protocol.Phasor;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static foundation.oned6.dicegrid.server.view.Views.*;
import static java.lang.Math.cos;
import static java.util.Map.entry;

public record NodeDataEntry(TeamName teamName, NodeType type, NodeState state, boolean hasControlButtons, String formName) implements View {
	private static final String LEGEND_OVERLAY_STYLE = """
					<style>
						@scope {
							caption {
								display: none;
							}
					
							table {
								position: fixed;
								background-color: white;
								border: 1px solid black;
								text-align: left;
							}
						}
					</style>""";
	public static List<String> headers() {
		return Stream.of(
			"Team",
			"Type",
			"<div>%s %s</div>".formatted(LEGEND_OVERLAY_STYLE, Legend.of(
				"Status",
				entry(span("Engaged", "green").html(), "Current flowing through the node"),
				entry(span("Disengaged", "orange").html(), "Switched on, but isolated from the grid."),
				entry(strong(span("Fault", "red")).html(), "In series with the grid and actively producing/consuming power")
			).html())
			,
			"Power",
			"Power Factor",
			"Current",
			"Voltage",
			"Current THD",
			"Voltage THD"
		).map("<th>%s</th>"::formatted).toList();
	}

	@Override
	public String html() {
		var S = state.voltage().multiply(state.innerCurrent());
		var cells = new ArrayList<>(List.of(
			teamName.html(),
			switch (type) {
				case SOURCE -> "Source";
				case LOAD -> "Load";
			},
			status().html(),

			phasorInfo("W", S),
			"%.2f".formatted(cos(S.phase())),

			phasorInfo("A", state.innerCurrent()),
			phasorInfo("V", state.voltage()),

			"%3.1f%%".formatted(state.currentThdInner() * 100),
			"%4.1f%%".formatted(state.voltageThd() * 100)
		));

		if (hasControlButtons) {
			if (state.shutdown())
				cells.add("<button type=submit name=action value=Start form=%s>Start</button>".formatted(formName));
			else {
				cells.add("<button type=submit name=action value=Shutdown form=%s>Shutdown</button>".formatted(formName));
				if (state.engaged()) {
					cells.add("<button type=submit name=action value=Disengage form=%s>Disengage</button>".formatted(formName));
				} else
					cells.add("<button type=submit name=action value=Engage form=%s>Engage</button>".formatted(formName));
			}
		}


		return cells.stream()
			.map("<td>%s</td>"::formatted)
				.collect(Collectors.joining("\n", "<tr>", "</tr>"));
	}

	private static String phasorInfo(String unit, Phasor value) {
		var legend = Legend.of(
			"%4.1f %s RMS".formatted(value.magnitude(), unit),
			entry("Phase (relative to ∠I(grid))", "%4.1f rad".formatted(value.phase())),
			entry("Real", "%4.1f %s".formatted(value.real(), unit)),
			entry("Imaginary", "%4.1f %s".formatted(value.imaginary(), unit)
			)).html();

		return """
			<div class="phasor-info">
				%s
				%s
			</div>""".formatted(LEGEND_OVERLAY_STYLE, legend);
	}

	private View status() {
		if (state.shutdown())
			return strong(span("Shutdown", "red"));
		else if (!state.engaged())
			return span("Disengaged", "orange");
		else
			return span("Engaged", "green");
	}

	@Override
	public String title() {
		return teamName + " " + switch (type) {
			case SOURCE -> "Source";
			case LOAD -> "Load";
		};
	}

	public static NodeDataEntry of(TeamPrincipal team, NodeType type, NodeState state) {
		return new NodeDataEntry(TeamName.of(team), type, state, false, "");
	}

	public static NodeDataEntry withButtons(TeamPrincipal team, NodeType type, NodeState state, String formName) {
		return new NodeDataEntry(TeamName.of(team), type, state, true, formName);
	}
}

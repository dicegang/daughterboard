package foundation.oned6.dicegrid.server.cp;

import foundation.oned6.dicegrid.protocol.NodeState;
import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.flash.FlashingHistoryEntry;
import foundation.oned6.dicegrid.server.flash.FlashingHistoryView;
import foundation.oned6.dicegrid.server.monitoring.NodeDataEntry;
import foundation.oned6.dicegrid.server.monitoring.NodeDataTable;
import foundation.oned6.dicegrid.server.schematic.SchematicListEntry;
import foundation.oned6.dicegrid.server.schematic.SchematicListView;
import foundation.oned6.dicegrid.server.view.UpdateView;
import foundation.oned6.dicegrid.server.view.View;

import java.net.URI;
import java.util.List;

import static foundation.oned6.dicegrid.server.Server.context;

public record ControlPanelView(UpdateView updateSchematic, SchematicListView schematicList, UpdateView flash,
			       FlashingHistoryView programHistory, NodeDataTable table) implements View {
	@Override
	public String html() {
		if (context().queryParam("table").isPresent()) {
			return table.html();
		}

		return """
			<style>
				@scope {
					.displays-grid {
						display: grid;
						grid-template-columns: repeat(2, 1fr);
						gap: 1em;
					}
						
					.displays-grid > div {
						display: inline-flex;
						flex-direction: column;
						align-items: center;
						border: 1px solid black;
						padding: 1em;
					}
				
					.status dialog::backdrop {
						background-color: rgba(0, 0, 0, 0.5);
					}
				}
			</style>

			<div class="displays-grid">			
				<div title="Schematic upload form">
					<h2>Upload New Schematic</h2>
					%s
				</div>

				<div title="Schematic history">
					<h2>Team Schematic History</h2>
					%s
				</div>
				
				<div title="Attiny85 flashing form">
					<h2>Flash Attiny85</h2>
					%s
				</div>
				
				<div title="Program history">
					<h2>Program History</h2>
					%s
				</div>
				
				<div title="Download certificate" hx-ext="response-targets">
					<h2>Download Certificate</h2>
					<div class="status" style="justify-self: center;"></div>
					<form hx-get="/download-certificate" hx-target-*="previous .status">
						<label for="password">Password:</label>
						<input type="password" id="password" name="password" required>
						<input type="submit" value="Download Certificate">
					</form>
				</div>
				
				<div title="Control Panel" hx-ext="response-targets">
					<h2>Control Panel</h2>
					<div class="status" style="justify-self: center;"></div>
					
					<div hx-get=%s hx-trigger="every 1s">
						%s
					</div>
				</div>
			</div>
			""".formatted(updateSchematic.html(), schematicList.html(), flash.html(), programHistory.html(),
			context().contentRoot().resolve("control?table"),
			table.html());
	}

	@Override
	public String title() {
		return "Control Panel";
	}

	public static ControlPanelView of(TeamPrincipal team, NodeState source, NodeState load, List<GridRepository.Schematic> schematics, List<GridRepository.FlashEvent> flashEvents) {
		var teamID = team.teamID();
		var table = new NodeDataTable(List.of(
			NodeDataEntry.of(team, NodeType.SOURCE, source, true),
			NodeDataEntry.of(team, NodeType.LOAD, load, true)
		));
		return new ControlPanelView(
			new UpdateView("application/pdf", "Schematic PDF", "Update Schematic", URI.create("/update-schematic")),
			new SchematicListView(teamID, schematics.stream().map(SchematicListEntry::new).toList()),
			new UpdateView(".c", "C Program", "Flash Program", URI.create("/flash-program")),
			new FlashingHistoryView(teamID, flashEvents.stream().map(n -> new FlashingHistoryEntry(false, n)).toList()),
			table
		);
	}
}

package foundation.oned6.dicegrid.server.view;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.flash.FlashingHistoryEntry;
import foundation.oned6.dicegrid.server.flash.FlashingHistoryView;
import foundation.oned6.dicegrid.server.schematic.SchematicListEntry;
import foundation.oned6.dicegrid.server.schematic.SchematicListView;

import java.net.URI;
import java.util.List;

public record ControlPanelView(UpdateView updateSchematic, SchematicListView schematicList, UpdateView flash, FlashingHistoryView programHistory) implements View {
	@Override
	public String generateHTML() {
		return """
			<div class="status"></div>
			
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
			</div>
			""".formatted(updateSchematic.generateHTML(), schematicList.generateHTML(), flash.generateHTML(), programHistory.generateHTML());
	}

	@Override
	public String title() {
		return "Control Panel";
	}

	public static ControlPanelView of(int teamID, List<GridRepository.Schematic> schematics, List<GridRepository.FlashEvent> flashEvents) {
		return new ControlPanelView(
			new UpdateView("application/pdf", "Schematic PDF", "Update Schematic", URI.create("/update-schematic")),
			new SchematicListView(teamID, schematics.stream().map(SchematicListEntry::new).toList()),
			new UpdateView(".c", "C Program", "Flash Program", URI.create("/flash-program")),
			new FlashingHistoryView(teamID, flashEvents.stream().map(FlashingHistoryEntry::new).toList())
		);
	}
}

package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.server.view.View;

import java.util.List;

import static foundation.oned6.dicegrid.server.Server.context;

public record FlashingHistoryView(int teamID, List<FlashingHistoryEntry> flashingEvents) implements View {
	public String generateHTML() {
		var sb = new StringBuilder();
		for (var event : flashingEvents) {
			sb.append(event.generateHTML());
		}

		return """
			<div title="%s" hx-ext="sse" sse-connect="%s" sse-swap="update" hx-swap="afterbegin" hx-target="find tbody">
				<style>
					@scope {
						table {
							border: 1px solid black;
							border-collapse: collapse;
						}
			
						th, td {
							border: 1px solid black;
							padding: 5px;
						}
					}
				</style>
			
				<table>
					<thead><tr>
						<th>Submitted</th>
						<th>Started</th>
						<th>Finished</th>
						<th>Target Device</th>
						<th>Code</th>
						<th>Binary</th>
						<th>Build Log</th>
					</tr></thead>
			
					<tbody>
						%s
					</tbody>
				</table>
			</div>
			""".formatted(title(), context().contentRoot().resolve("program-list?team=" + teamID), sb);
	}

	@Override
	public String title() {
		return "Team schematic list";
	}
}

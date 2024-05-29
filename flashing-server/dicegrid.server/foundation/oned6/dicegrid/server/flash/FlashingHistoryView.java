package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.server.view.View;

import java.util.List;

import static foundation.oned6.dicegrid.server.Server.context;

public record FlashingHistoryView(int teamID, List<FlashingHistoryEntry> flashingEvents) implements View {
	public String html() {
		var sb = new StringBuilder();
		for (var event : flashingEvents) {
			sb.append(event.html());
		}

		return """
			<div title="%s" hx-ext="sse" sse-connect="%s">
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
						<th>Last update</th>
						<th>Status</th>
						<th>Target Device</th>
						<th>Code</th>
						<th>Build Log</th>
						<th>Binary</th>
					</tr></thead>
			
					<tbody sse-swap="new_entry" hx-swap="afterbegin">
						%s
					</tbody>
				</table>
			</div>
			""".formatted(title(), context().contentRoot().resolve("program-list?team=" + teamID), sb);
	}

	@Override
	public String title() {
		return "Team flashing historyx";
	}
}

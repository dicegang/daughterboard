package foundation.oned6.dicegrid.server.schematic;

import foundation.oned6.dicegrid.server.view.View;

import java.util.List;

import static foundation.oned6.dicegrid.server.Server.context;

public record SchematicListView(int teamID, List<SchematicListEntry> schematics) implements View {
	public String html() {
		var sb = new StringBuilder();
		for (var schematic : schematics) {
			sb.append(schematic.html());
		}

		return """
			<div title="%s" hx-ext="sse" sse-connect="%s" sse-swap="new_entry" hx-swap="afterbegin" hx-target="find tbody">
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
						<th>Time Uploaded</th>
						<th>Target Device</th>
						<th>Viewing Link</th>
						<th>Download Link</th>
					</tr></thead>
			
					<tbody>
						%s
					</tbody>
				</table>
			</div>
			""".formatted(title(), context().contentRoot().resolve("schematics?stream&team=" + teamID), sb);
	}

	@Override
	public String title() {
		return "Team schematic list";
	}
}

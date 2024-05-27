package foundation.oned6.dicegrid.server.schematic;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.view.View;

import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneId;

import static foundation.oned6.dicegrid.server.Server.CONTEXT;

public record SchematicListEntry(GridRepository.Schematic schematic) implements View {
	@Override
	public String generateHTML() {
		return STR."""
		<tr>
			<td>\{LocalTime.ofInstant(schematic.uploaded(), ZoneId.systemDefault())}</td>
			<td>\{schematic.target()}</td>
			<td><a href="\{schematicPDFLocation(schematic)}&view">View PDF</a></td>
			<td><a href="\{schematicPDFLocation(schematic)}&download">Download PDF</a></td>
		</tr>""";
	}

	@Override
	public String title() {
		return "Schematic";
	}

	private URI schematicPDFLocation(GridRepository.Schematic schematic) {
		return CONTEXT.get().contentRoot().resolve(STR."schematics?team=\{schematic.teamID()}&index=\{schematic.index()}");
	}
}

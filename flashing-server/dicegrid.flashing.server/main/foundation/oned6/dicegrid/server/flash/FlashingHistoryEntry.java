package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.view.View;

import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneId;

import static foundation.oned6.dicegrid.server.Server.CONTEXT;

public record FlashingHistoryEntry(GridRepository.FlashEvent event) implements View {
	@Override
	public String generateHTML() {
		var trAttributes = event.completed() != null ? "" : STR."""
			hx-ext="sse" sse-connect="\{status(event)}" sse-swap="update" hx-swap="outerHTML" hx-target="this"
			""";
		return STR."""
		<tr \{trAttributes}>
			<td>\{LocalTime.ofInstant(event.submitted(), ZoneId.systemDefault())}</td>
			<td>\{event.start() == null ? "N/A" : LocalTime.ofInstant(event.start(), ZoneId.systemDefault())}</td>
			<td>\{event.completed() == null ? "N/A" : LocalTime.ofInstant(event.completed(), ZoneId.systemDefault())}</td>
			<td>\{event.target()}</td>
			<td><a href="\{codeLocation(event)}">View C</a></td>
			<td><a href="\{hexLocation(event)}">Download HEX</a></td>
			<td><a href="\{compileLog(event)}">Compile Log</a></td>
		</tr>""";
	}

	@Override
	public String title() {
		return "Flashing event";
	}

	private URI codeLocation(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program?team=\{event.teamID()}&index=\{event.index()}");
	}

	private URI hexLocation(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program-hex?team=\{event.teamID()}&index=\{event.index()}");
	}

	private URI compileLog(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program-log?team=\{event.teamID()}&index=\{event.index()}");
	}

	private URI status(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program-status?id=\{event.id()}");
	}
}

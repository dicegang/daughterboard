package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.view.View;

import java.net.URI;
import java.time.LocalTime;
import java.time.ZoneId;

import static foundation.oned6.dicegrid.server.GridRepository.FlashEvent.Status.BUILDING;
import static foundation.oned6.dicegrid.server.GridRepository.FlashEvent.Status.QUEUED;
import static foundation.oned6.dicegrid.server.Server.CONTEXT;

public record FlashingHistoryEntry(boolean isInner, GridRepository.FlashEvent event) implements View {
	@Override
	public String html() {
		var sb = new StringBuilder();

		if (!isInner)
			sb.append("<tr sse-swap=update-%d hx-swap=innerHTML>".formatted(event.index()));

		sb.append("<td>").append(LocalTime.ofInstant(event.lastUpdate(), ZoneId.systemDefault())).append("</td>");
		sb.append("<td>").append(colouredStatus(event.status())).append("</td>");
		sb.append("<td>").append(event.target()).append("</td>");
		sb.append("<td><a target=\"_blank\" href=\"").append(codeLocation(event)).append("\">View C</a></td>");
		if (event.status().compareTo(BUILDING) > 0)
			sb.append("<td><a target=\"_blank\" href=\"").append(compileLog(event)).append("\">Compile Log</a></td>");
		if (event.status().compareTo(QUEUED) >= 0)
			sb.append("<td><a href=\"").append(hexLocation(event)).append("\">Download HEX</a></td>");
		if (!isInner)
			sb.append("</tr>");

		return sb.toString();
	}

	private static String colouredStatus(GridRepository.FlashEvent.Status status) {
		var colour = switch (status) {
			case QUEUED -> "blue";
			case BUILDING -> "orange";
			case FLASHING -> "orange";
			case SUCCESS -> "green";
			default -> "red";
		};

		return STR."""
			<strong style="color: \{colour}">\{status}</span>
			""";
	}

	@Override
	public String title() {
		return "Flashing event";
	}

	private URI codeLocation(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program?id=\{event.id()}");
	}

	private URI hexLocation(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program-hex?id=\{event.id()}");
	}

	private URI compileLog(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."build-log?id=\{event.id()}");
	}

	private URI status(GridRepository.FlashEvent event) {
		return CONTEXT.get().contentRoot().resolve(STR."program-status?stream&id=\{event.id()}");
	}
}

package foundation.oned6.dicegrid.server.flash;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.controller.Controller;

import java.io.IOException;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.NOT_FOUND;
import static foundation.oned6.dicegrid.server.HTTPUtils.handleHttpException;
import static foundation.oned6.dicegrid.server.HTTPUtils.tryParseLong;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ProgramHexController implements Controller {
	private final GridRepository repository;

	public ProgramHexController(GridRepository repository) {
		this.repository = repository;
	}

	@Override
	public String mimeType() {
		return "application/octet-stream";
	}

	@Override
	public void handleRequest(HttpsExchange exchange) {
		try (exchange) {
			var eventID = context().queryParam("id").flatMap(n -> tryParseLong(n))
				.orElseThrow(() -> HTTPException.of(BAD_REQUEST));
			var event = repository.getFlashEvent(eventID).orElseThrow(() -> HTTPException.of(NOT_FOUND));
			var logBytes = event.hex();

			exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
			var filename = switch (event.target()) {
				case SOURCE -> "source";
				case LOAD -> "load";
			} + "-" + repository.findTeamName(event.teamID()) + "-" + eventID + ".hex";
			exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"" );
			exchange.sendResponseHeaders(200, logBytes.length);
			exchange.getResponseBody().write(logBytes);

		} catch (HTTPException e) {
			handleHttpException(exchange, e);
		} catch (IOException _) {
		}
	}
}

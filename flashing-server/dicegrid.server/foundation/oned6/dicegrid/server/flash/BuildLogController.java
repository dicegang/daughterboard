package foundation.oned6.dicegrid.server.flash;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.controller.Controller;

import java.io.IOException;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.NOT_FOUND;
import static foundation.oned6.dicegrid.server.HTTPUtils.*;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.nio.charset.StandardCharsets.UTF_8;

public class BuildLogController implements Controller {
	private final GridRepository repository;

	public BuildLogController(GridRepository repository) {
		this.repository = repository;
	}

	@Override
	public String mimeType() {
		return "text/plain";
	}

	@Override
	public void handleRequest(HttpsExchange exchange) {
		try (exchange) {
			var eventID = context().queryParam("id").flatMap(n -> tryParseLong(n))
				.orElseThrow(() -> HTTPException.of(BAD_REQUEST));
			var event = repository.getFlashEvent(eventID).orElseThrow(() -> HTTPException.of(NOT_FOUND));
			var logBytes = event.compileLog().getBytes(UTF_8);

			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, logBytes.length);
			exchange.getResponseBody().write(logBytes);

		} catch (HTTPException e) {
			handleHttpException(exchange, e);
		} catch (IOException _) {
		}
	}
}

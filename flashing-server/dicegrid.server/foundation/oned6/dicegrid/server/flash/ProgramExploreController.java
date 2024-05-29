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

public class ProgramExploreController implements Controller {
	private final GridRepository repository;

	public ProgramExploreController(GridRepository repository) {
		this.repository = repository;
	}

	@Override
	public String mimeType() {
		return "text/plain";
	}

	@Override
	public void handleRequest(HttpsExchange exchange) {
		try {
			var eventID = context().queryParam("id").flatMap(n -> tryParseLong(n))
				.orElseThrow(() -> HTTPException.of(BAD_REQUEST));

			var program = repository.getFlashEvent(eventID)
				.orElseThrow(() -> HTTPException.of(NOT_FOUND));

			exchange.getResponseHeaders().add("Content-Disposition", "inline; filename=code.c");
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.getResponseHeaders().add("Last-Modified", HTTP_DATE.format(program.lastUpdate()));

			exchange.sendResponseHeaders(200, program.code().code().getBytes(UTF_8).length);
			exchange.getResponseBody().write(program.code().code().getBytes(UTF_8));
		} catch (HTTPException e) {
			handleHttpException(exchange, e);
			return;
		} catch (IOException _) {

		}
	}
}

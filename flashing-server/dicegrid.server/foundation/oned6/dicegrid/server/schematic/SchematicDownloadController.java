package foundation.oned6.dicegrid.server.schematic;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.controller.Controller;

import java.io.IOException;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.NOT_FOUND;
import static foundation.oned6.dicegrid.server.HTTPUtils.*;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class SchematicDownloadController implements Controller {
	private final GridRepository repository;

	public SchematicDownloadController(GridRepository repository) {
		this.repository = repository;
	}

	@Override
	public String mimeType() {
		return "application/pdf";
	}

	@Override
	public void handleRequest(HttpsExchange exchange) {
		try {
			var teamID = context().queryParam("team").flatMap(n -> tryParseInteger(n))
				.orElseThrow(() -> HTTPException.of(BAD_REQUEST));
			var schematicID = context().queryParam("index").flatMap(n -> tryParseInteger(n))
				.orElseThrow(() -> HTTPException.of( BAD_REQUEST));

			var schematic = repository.getSchematic(teamID, schematicID).orElseThrow(() -> HTTPException.of(NOT_FOUND));
			var filename = repository.findTeamName(teamID) + "-" + schematicID + ".pdf";

			if (context().queryParam("download").isPresent())
				exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename*=UTF-8''" + urlEncode(filename));
			else
				exchange.getResponseHeaders().add("Content-Disposition", "inline; filename*=UTF-8''" + urlEncode(filename));

			exchange.getResponseHeaders().add("Content-Type", "application/pdf");
			exchange.getResponseHeaders().add("Last-Modified", HTTP_DATE.format(schematic.uploaded()));

			exchange.sendResponseHeaders(200, schematic.pdfData().byteSize());
			if (schematic.pdfData().isNative()) {
				var bb = schematic.pdfData().asByteBuffer();
				exchange.getResponseBody().write(bb.array(), bb.arrayOffset(), bb.remaining());
			} else {
				exchange.getResponseBody().write(schematic.pdfData().toArray(JAVA_BYTE));
			}
		} catch (HTTPException e) {
			handleHttpException(exchange, e);
			return;
		} catch (Exception e) {
			handleUnexpectedException(exchange, e);
		}
	}
}

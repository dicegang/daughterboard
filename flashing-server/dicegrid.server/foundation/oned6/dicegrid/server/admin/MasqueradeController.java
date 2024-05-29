package foundation.oned6.dicegrid.server.admin;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.controller.Controller;

import java.io.IOException;
import java.util.List;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.FORBIDDEN;
import static foundation.oned6.dicegrid.server.HTTPUtils.*;
import static foundation.oned6.dicegrid.server.RequestContext.parseQuery;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MasqueradeController implements Controller {
	@Override
	public String mimeType() {
		return "text/plain";
	}

	@Override
	public void handleRequest(HttpsExchange exchange) {
		try {
			if (!(context().activeUser() instanceof AdminPrincipal))
				throw HTTPException.of(FORBIDDEN);

			var form = new String(context().requestBody().readAllBytes(), UTF_8);
			var teamID = parseQuery(form).get("teamID");
			if (teamID == null)
				throw HTTPException.of(BAD_REQUEST);

			exchange.getResponseHeaders().add("Set-Cookie", "masquerade=" + teamID);
			exchange.getResponseHeaders().add("Location", exchange.getRequestHeaders().getOrDefault("Referer", List.of("/")).getFirst());
				exchange.sendResponseHeaders(303, 0);
		} catch (HTTPException e) {
			handleHttpException(exchange, e);
		} catch (IOException _) {

		}
	}
}

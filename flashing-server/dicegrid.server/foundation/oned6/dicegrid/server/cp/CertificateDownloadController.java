package foundation.oned6.dicegrid.server.cp;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.auth.AuthManager;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.Controller;
import foundation.oned6.dicegrid.server.view.StatusMessageView;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.FORBIDDEN;
import static foundation.oned6.dicegrid.server.HTTPUtils.*;
import static foundation.oned6.dicegrid.server.Server.context;

public class CertificateDownloadController implements Controller {
	private final AuthManager authManager;

	public CertificateDownloadController( AuthManager authManager) {
		this.authManager = authManager;
	}

	@Override
	public String mimeType() {
		return "application/octet-stream";
	}

	@Override
	public void handleRequest(HttpsExchange exchange) {
		try {
			var me = switch (currentIdentity()) {
				case TeamPrincipal tp -> tp;
				case AdminPrincipal _ -> throw HTTPException.withMessage("admin certificates cannot be generated", FORBIDDEN);
				case null -> throw HTTPException.of(FORBIDDEN);
			};

			var pw = context().queryParam("password").orElseThrow(() -> HTTPException.of(BAD_REQUEST));
			if (pw.length() < 8)
				throw HTTPException.withMessage("Password must be at least 8 characters", BAD_REQUEST);

			if (exchange.getRequestHeaders().containsKey("HX-Request")) {
				exchange.getResponseHeaders().add("HX-Redirect", context().requestURI().toString());
				exchange.sendResponseHeaders(303, 0);
				return;
			}

			var result = authManager.createCertificate(pw, me);

			exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
			exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"dicegrid.p12\"" );

			exchange.sendResponseHeaders(200, result.length);
			exchange.getResponseBody().write(result);
		} catch (HTTPException e) {
			exchange.getResponseHeaders().add("Content-Type", "text/html");
			e = e.withViewWrapper(v -> new StatusMessageView("Create Certificate", v.html(), StatusMessageView.Status.FAILURE));
			handleHttpException(exchange, e);
		} catch (Exception e) {
			handleUnexpectedException(exchange, e);
		}
	}
}

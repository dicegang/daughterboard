package foundation.oned6.dicegrid.server.controller;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.admin.MasqueradeWrapperView;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.view.View;

import java.io.IOException;
import java.util.Optional;

import static foundation.oned6.dicegrid.server.HTTPUtils.handleHttpException;
import static foundation.oned6.dicegrid.server.HTTPUtils.handleUnexpectedException;
import static foundation.oned6.dicegrid.server.Server.*;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class ViewController implements Controller {
	private static final System.Logger logger = System.getLogger("ViewController");
	public abstract View constructPage() throws HTTPException;

	@Override
	public String mimeType() {
		return "text/html";
	}

	public final void handleRequest(HttpsExchange exchange) {
		byte[] page;

		try {
			var view = constructPage();
			if (context().requestHeader("HX-Request").isEmpty())
				view = wrapView(view);

			page = view.html().getBytes(UTF_8);

			exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
		} catch (HTTPException e) {
			e = e.withViewWrapper(this::wrapView);
			handleHttpException(exchange, e);
			return;
		} catch (Exception e) {
			logger.log(ERROR, "Unexpected exception", e);
			handleUnexpectedException(exchange, e);
			return;
		}

		try (exchange) {
			exchange.sendResponseHeaders(200, page.length);
			exchange.getResponseBody().write(page);
		} catch (IOException e) {
			logger.log(WARNING, "Failed to send response", e);
		}
	}

	private View wrapView(View view) {
		if (context().activeUser() instanceof AdminPrincipal)
			return new MasqueradeWrapperView(Optional.ofNullable(MASQUERADE.orElse(null)), REPOSITORY.get().getAllTeams(), view);
		else
			return view;
	}
}

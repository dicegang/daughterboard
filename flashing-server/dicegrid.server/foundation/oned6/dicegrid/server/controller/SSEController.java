package foundation.oned6.dicegrid.server.controller;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.view.View;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.TOO_MANY_REQUESTS;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.lang.System.Logger.Level.WARNING;

public abstract class SSEController implements Controller {
	private static final int TEAM_MAX_SSE_CONNECTIONS = 1000;
	private static final System.Logger logger = System.getLogger("SSEController");

	private final ScopedValue<HttpsExchange> exchange = ScopedValue.newInstance();
	private final Map<Principal, AtomicInteger> SSE_CONNECTION_COUNT = new ConcurrentHashMap<>();


	protected abstract void handlerLoopImpl() throws HTTPException, IOException, InterruptedException;

	protected final void sendEvent(String eventName, View event) throws IOException, InterruptedException {
		try {
			var body = exchange.get().getResponseBody();
			body.write("event: %s\n".formatted(eventName).getBytes(StandardCharsets.UTF_8));
			body.write("data: ".getBytes(StandardCharsets.UTF_8));
			body.write(event.html().replace('\n', ' ').getBytes(StandardCharsets.UTF_8));
			body.write("\n\n".getBytes(StandardCharsets.UTF_8));
			body.flush();
		} catch (InterruptedIOException ie) {
			throw new InterruptedException(ie.getMessage());
		}
	}

	public final void handlerLoop(HttpsExchange exchange) throws HTTPException, IOException, InterruptedException {
		try {
			ScopedValue.callWhere(this.exchange, exchange, () -> {
				handlerLoopImpl();
				return null;
			});
		} catch (HTTPException | IOException | InterruptedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final String mimeType() {
		return "text/event-stream";
	}

	public final void handleRequest(HttpsExchange exchange) {
		AtomicInteger activeCount = null;

		try (exchange) {
			activeCount = SSE_CONNECTION_COUNT.computeIfAbsent(context().activeUser(), _ -> new AtomicInteger());

			if (activeCount.getAndIncrement() > TEAM_MAX_SSE_CONNECTIONS)
				throw new HTTPException(TOO_MANY_REQUESTS);

			exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
			exchange.getResponseHeaders().add("Cache-Control", "no-cache");
			exchange.getResponseHeaders().add("Connection", "keep-alive");
			exchange.sendResponseHeaders(200, 0);

			handlerLoop(exchange);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			if (activeCount != null)
				activeCount.decrementAndGet();
		}
	}
}

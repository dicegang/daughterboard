package foundation.oned6.dicegrid.server;

import com.sun.net.httpserver.HttpsExchange;
import foundation.oned6.dicegrid.server.auth.GridPrincipal;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.*;
import static foundation.oned6.dicegrid.server.HTTPMethod.POST;
import static foundation.oned6.dicegrid.server.Server.MASQUERADE;
import static foundation.oned6.dicegrid.server.Server.context;
import static java.nio.charset.StandardCharsets.UTF_8;

public class HTTPUtils {
	public static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

	public static void requireMethod(HTTPMethod... allowed) throws HTTPException {
		var method = context().httpMethod();
		for (var m : allowed) {
			if (m == method)
				return;
		}

		throw new HTTPException(METHOD_NOT_ALLOWED);
	}

	public static GridPrincipal currentIdentity() {
		var me = context().activeUser();
		if (me == null)
			return null;

		if (MASQUERADE.isBound())
			return MASQUERADE.get();

		return me;
	}

	public static void handleHttpException(HttpsExchange exchange, HTTPException e) {
		try (exchange) {
			var response = e.view().html().getBytes(UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(e.code().code, response.length);
			exchange.getResponseBody().write(response);
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	public static void handleUnexpectedException(HttpsExchange exchange, Exception e) {
		try (exchange) {
			exchange.sendResponseHeaders(500, 0);
		} catch (IOException _) {
		}
	}

	public static Optional<Integer> tryParseInteger(String value) {
		try {
			return Optional.of(Integer.parseInt(value));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public static Optional<Long> tryParseLong(String value) {
		try {
			return Optional.of(Long.parseLong(value));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public static <T> T requirePresent(Optional<T> value, HTTPException.HTTPCode code) throws HTTPException {
		return value.orElseThrow(() -> new HTTPException(code));
	}

	public static Supplier<HTTPException> errorCode(HTTPException.HTTPCode code) {
		return () -> new HTTPException(code);
	}

	public static Supplier<HTTPException> errorCode(HTTPException.HTTPCode code, String message) {
		return () -> new HTTPException(message, code);
	}

	public static Supplier<HTTPException> errorCode(HTTPException.HTTPCode code, Throwable cause) {
		return () -> new HTTPException(cause, code);
	}


	public static String urlEncode(String value) {
		return URLEncoder.encode(value, UTF_8);
	}

	public static FormBody parsePOSTBody() throws HTTPException, IOException {
		requireMethod(context().httpMethod(), POST);

		var length = context().contentLength().orElseThrow(() -> HTTPException.withMessage("Content-Length header is required", BAD_REQUEST));
		var contentType = context().contentType().orElseThrow(() -> HTTPException.withMessage("Content-Type header is required", BAD_REQUEST));

		var content = context().requestBody().readNBytes(length);
		var body = FormBody.parse(context().charset(), contentType, MemorySegment.ofArray(content));
		return body;
	}
}

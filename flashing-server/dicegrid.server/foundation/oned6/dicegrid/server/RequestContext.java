package foundation.oned6.dicegrid.server;

import com.sun.net.httpserver.Headers;
import foundation.oned6.dicegrid.server.auth.GridPrincipal;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record RequestContext(GridPrincipal activeUser, URI contentRoot, URI requestURI, HTTPMethod httpMethod, Headers requestHeaders, InputStream requestBody, Charset charset) {
	public Optional<Integer> contentLength() {
		try {
			return Optional.ofNullable(requestHeaders.getFirst("Content-Length")).map(Integer::parseInt);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public Optional<String> contentType() {
		return Optional.ofNullable(requestHeaders.getFirst("Content-Type"));
	}

	public Optional<String> requestHeader(String name) {
		return Optional.ofNullable(requestHeaders.getFirst(name));
	}

	public Map<String, String> getQueryData() {
		var query = requestURI().getQuery();
		if (query == null) {
			return Collections.emptyMap();
		}

		return parseQuery(query);
	}

	public static Map<String, String> parseQuery(String query) {
		var parts = query.split("&");
		var map = new HashMap<String, String>();
		for (var part : parts) {
			var kv = part.split("=");
			map.put(kv[0], kv.length > 1 ? kv[1] : "");
		}
		return Collections.unmodifiableMap(map);
	}

	public Optional<String> queryParam(String key) {
		return Optional.ofNullable(getQueryData().get(key));
	}

	public Optional<String> cookie(String name) {
		return Optional.ofNullable(requestHeaders.getFirst("Cookie")).map(cookie -> {
			var parts = cookie.split(";");
			for (var part : parts) {
				var kv = part.split("=");
				if (kv[0].trim().equals(name))
					return kv[1].trim();
			}
			return null;
		});
	}
}

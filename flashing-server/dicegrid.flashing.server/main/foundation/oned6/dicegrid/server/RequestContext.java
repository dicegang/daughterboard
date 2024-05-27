package foundation.oned6.dicegrid.server;

import com.sun.net.httpserver.Headers;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static foundation.oned6.dicegrid.server.Server.context;

public record RequestContext(TeamPrincipal activeUser, URI contentRoot, URI requestURI, HTTPMethod httpMethod, Headers requestHeaders, InputStream requestBody, Charset charset) {
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

	public Map<String, String> parseQuery() {
		var query = context().requestURI().getQuery();
		var parts = query.split("&");
		var map = new HashMap<String, String>();
		for (var part : parts) {
			var kv = part.split("=");
			map.put(kv[0], kv.length > 1 ? kv[1] : "");
		}
		return Collections.unmodifiableMap(map);
	}

	public Optional<String> queryParam(String key) {
		return Optional.ofNullable(parseQuery().get(key));
	}
}

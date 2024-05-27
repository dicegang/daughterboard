package foundation.oned6.dicegrid.server.controller;

import com.sun.net.httpserver.HttpsExchange;

public interface Controller {
	String mimeType();
	void handleRequest(HttpsExchange exchange);
}

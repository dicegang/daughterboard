package foundation.oned6.dicegrid.server;

public enum HTTPMethod {
	GET,
	POST,
	PUT,
	DELETE;

	public static HTTPMethod fromString(String method) {
		return switch (method) {
			case "GET" -> GET;
			case "POST" -> POST;
			case "PUT" -> PUT;
			case "DELETE" -> DELETE;
			default -> null;
		};
	}
}

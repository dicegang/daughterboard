package foundation.oned6.dicegrid.server;

import foundation.oned6.dicegrid.server.view.HypertextFrameView;
import foundation.oned6.dicegrid.server.view.View;

import java.util.function.Function;

public class HTTPException extends Exception {
	private final HTTPCode code;
	private Function<View, View> viewWrapper = Function.identity();

	public HTTPException(HTTPCode code) {
		this.code = code;
	}

	public HTTPException(String message, HTTPCode code) {
		super(message);
		this.code = code;
	}

	public HTTPException(String message, Throwable cause, HTTPCode code) {
		super(message, cause);
		this.code = code;
	}

	public HTTPException(Throwable cause, HTTPCode code) {
		super(cause);
		this.code = code;
	}

	public void addViewWrapper(Function<View, View> wrapper) {
		viewWrapper = viewWrapper.andThen(wrapper);
	}

	public View view() {
		var message = getMessage();
		message = message == null ? "" : message.trim();

		return viewWrapper.apply(HypertextFrameView.of(getClass().getSimpleName(), message.isEmpty() ? "" : "<p>%s</p>".formatted(message)));
	}

	public HTTPCode code() {
		return code;
	}

	public enum HTTPCode {
		UNAUTHORISED(401, "Unauthorised"),
		FORBIDDEN(403, "Forbidden"),
		NOT_FOUND(404, "Not Found"),
		TOO_MANY_REQUESTS(429, "Too Many Requests"),
		METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
		BAD_REQUEST(400, "Bad Request"),
		INTERNAL_SERVER_ERROR(500, "Internal Server Error");

		public final int code;
		public final String message;

		HTTPCode(int code, String message) {
			this.code = code;
			this.message = message;
		}
	}

	public static HTTPException withMessage(String message, HTTPCode code) {
		return new HTTPException(message, code);
	}

	public static HTTPException of(HTTPCode code) {
		return new HTTPException(code);
	}
}

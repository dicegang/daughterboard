package foundation.oned6.dicegrid.server;

import foundation.oned6.dicegrid.server.view.StatusMessageView;
import foundation.oned6.dicegrid.server.view.View;

import static foundation.oned6.dicegrid.server.view.StatusMessageView.Status.FAILURE;

public class StatusMessageException extends HTTPException {
	private final String operationName;

	public StatusMessageException(HTTPException cause, String operationName) {
		super(cause, cause.code());
		this.operationName = operationName;
	}

	@Override
	public View view() {
		var causeMessage = getCause().getMessage();
		return new StatusMessageView(operationName, causeMessage == null ? "" : causeMessage, FAILURE);
	}

	public static StatusMessageException of(HTTPException cause, String operationName) {
		return new StatusMessageException(cause, operationName);
	}
}

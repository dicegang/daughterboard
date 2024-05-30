package foundation.oned6.dicegrid.server.view;

import foundation.oned6.dicegrid.server.HTTPException;

public record StatusMessageView(String operation, View inner, Status status) implements View {
	@Override
	public String html() {
		String colour = switch (status) {
			case FAILURE -> "red";
			case SUCCESS -> "green";
			case INFO -> "black";
		};

		return """
			<style onload="this.nextElementSibling.showModal()"></style>
			<dialog title="Status: %s" autofocus align="center">
				<strong style="color: %s">%s</strong>
				%s

				<form method="dialog">
					<button>Dismiss</button>
				</form>
			</dialog>
			""".formatted(operation, colour, title(), inner.html());
	}

	public StatusMessageView(String operation, String message, Status status) {
		this(operation, (message == null || message.isBlank()) ? View.blank() : View.hypertext("<p>" + message + "<p>"), status);
	}

	@Override
	public String title() {
		String statusPresentPerfect = switch (status) {
			case FAILURE -> "Failed";
			case SUCCESS -> "Succeeded";
			case INFO -> "Info";
		};

		if (operation.isEmpty())
			return statusPresentPerfect;
		else
			return statusPresentPerfect + ": " + operation;
	}

	public static HTTPException wrapException(String operation, HTTPException e) {
		return e.withViewWrapper(view -> new StatusMessageView(operation, view, Status.FAILURE));
	}

	public enum Status {
		FAILURE,
		SUCCESS,
		INFO;
	}
}

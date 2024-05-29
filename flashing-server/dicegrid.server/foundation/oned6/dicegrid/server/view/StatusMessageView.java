package foundation.oned6.dicegrid.server.view;

public record StatusMessageView(String operation, String message, Status status) implements View {
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
			""".formatted(operation, colour, title(), message.isEmpty() ? "" : "<p>" + message + "</p>");
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

	public enum Status {
		FAILURE,
		SUCCESS,
		INFO;
	}
}

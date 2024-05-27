package foundation.oned6.dicegrid.server.view;

public record StatusMessageView(String operation, String message, Status status) implements View {
	@Override
	public String generateHTML() {
		String colour = switch (status) {
			case FAILURE -> "red";
			case SUCCESS -> "green";
			case INFO -> "black";
		};

		return """
			<dialog open autofocus>
				<strong style="color: %s">%s</strong>
				<p>%s</p>
						
				<form method="dialog">
					<button>Dismiss</button>
				</form>
			</dialog>
			""".formatted(colour, title(), message);
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

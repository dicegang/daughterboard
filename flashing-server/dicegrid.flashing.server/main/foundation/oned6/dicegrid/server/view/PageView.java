package foundation.oned6.dicegrid.server.view;

public record PageView(View inner) implements View {
	@Override
	public String generateHTML() {
		return """
			<!DOCTYPE html>
			<html>
			<head>
				<script src="https://unpkg.com/htmx.org@1.9.12"></script>
				<script src="https://unpkg.com/htmx.org@1.9.12/dist/ext/sse.js"></script>
			
				<title>%s</title>
			</head>
			
			<body>
				%s
			</body>
			</html>
			""".formatted(inner.title(), inner.generateHTML());
	}

	@Override
	public String title() {
		return inner.title();
	}
}

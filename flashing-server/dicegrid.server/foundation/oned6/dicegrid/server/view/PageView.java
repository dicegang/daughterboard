package foundation.oned6.dicegrid.server.view;

import static foundation.oned6.dicegrid.server.Server.context;

public record PageView(View inner) implements View {
	@Override
	public String html() {
		if (context().requestHeader("HX-Request").isPresent())
			return inner.html();
		else
			return """
				<!DOCTYPE html>
				<html>
				<head>
					<script src="https://unpkg.com/htmx.org@1.9.12"></script>
					<script src="https://unpkg.com/htmx.org@1.9.12/dist/ext/sse.js"></script>
					<script src='https://unpkg.com/htmx.org/dist/ext/response-targets.js'></script>
							
					<title>%s</title>
				</head>
							
				<body>
					%s
				</body>
				</html>
				""".formatted(inner.title(), inner.html());
	}

	@Override
	public String title() {
		return inner.title();
	}
}

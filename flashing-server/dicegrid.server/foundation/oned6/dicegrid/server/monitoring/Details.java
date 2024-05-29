package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.server.view.View;

import static foundation.oned6.dicegrid.server.view.Views.text;

public record Details(View summary, View details) implements View {
	@Override
	public String html() {
		return """
			<details>
				<summary>%s</summary>
				%s
			</details>""".formatted(summary.html(), details.html());
	}

	@Override
	public String title() {
		return summary.title();
	}

	public static Details of(String summary, View details) {
		return new Details(text(summary), details);
	}
}

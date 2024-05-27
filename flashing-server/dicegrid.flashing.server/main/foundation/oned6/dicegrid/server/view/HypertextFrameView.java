package foundation.oned6.dicegrid.server.view;

public record HypertextFrameView(View contents) implements View {
	@Override
	public String generateHTML() {
		return """
			<div title="%s">
				%s
			</div>
			""".formatted(contents.title(), contents.generateHTML());
	}

	@Override
	public String title() {
		return contents.title();
	}

	public static HypertextFrameView of(String title, String innerHTML) {
		return new HypertextFrameView(new View() {
			@Override
			public String generateHTML() {
				return innerHTML;
			}

			@Override
			public String title() {
				return title;
			}
		});
	}
}

package foundation.oned6.dicegrid.server.view;

public record HypertextFrameView(View contents) implements View {
	@Override
	public String html() {
		return """
			%s""".formatted(contents.html());
	}

	@Override
	public String title() {
		return contents.title();
	}

	public static HypertextFrameView of(String title, String innerHTML) {
		return new HypertextFrameView(new View() {
			@Override
			public String html() {
				return innerHTML;
			}

			@Override
			public String title() {
				return title;
			}
		});
	}
}

package foundation.oned6.dicegrid.server.view;

public interface Views {
	static View text(String text) {
		return new View() {
			@Override
			public String html() {
				return text;
			}

			@Override
			public String title() {
				return "";
			}
		};
	}

	static View span(String text, String colour) {
		return new View() {
			@Override
			public String html() {
				return "<span style=\"color: %s;\">%s</span>".formatted(colour, text);
			}

			@Override
			public String title() {
				return text;
			}
		};
	}

	static View strong(View view) {
		return new View() {
			@Override
			public String html() {
				return "<strong>%s</strong>".formatted(view.html());
			}

			@Override
			public String title() {
				return view.title();
			}
		};
	}
}

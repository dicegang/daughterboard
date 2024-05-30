package foundation.oned6.dicegrid.server.view;

public interface View {
	String html();
	String title();

	static View blank() {
		return new View() {
			@Override
			public String html() {
				return "";
			}

			@Override
			public String title() {
				return "";
			}
		};
	}

	static View hypertext(String hypertext) {
		return new View() {
			@Override
			public String html() {
				return hypertext;
			}

			@Override
			public String title() {
				return "";
			}
		};
	}
}

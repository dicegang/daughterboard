package foundation.oned6.dicegrid.server.controller;

import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.view.PageView;
import foundation.oned6.dicegrid.server.view.View;

public class PageController extends ViewController {
	private final ViewController inner;

	private PageController(ViewController inner) {
		this.inner = inner;
	}

	@Override
	public View constructPage() throws HTTPException {
		var innerView = inner.constructPage();

		return new PageView(innerView);
	}

	public static PageController of(ViewController inner) {
		return new PageController(inner);
	}
}

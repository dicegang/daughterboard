package foundation.oned6.dicegrid.server.controller;

import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.view.PageView;
import foundation.oned6.dicegrid.server.view.View;

public class PageController extends ViewController {
	private final GridRepository repository;
	private final ViewController inner;

	private PageController(GridRepository repository, ViewController inner) {
		this.repository = repository;
		this.inner = inner;
	}

	@Override
	public View constructPage() throws HTTPException {
		try {
			var innerView = inner.constructPage();
			return wrapView(innerView);
		} catch (HTTPException e) {
			e.withViewWrapper(this::wrapView);
			throw e;
		}
	}

	private View wrapView(View innerView) {
		return new PageView(innerView);
	}

	public static PageController of(GridRepository repository, ViewController inner) {
		return new PageController(repository, inner);
	}
}

package foundation.oned6.dicegrid.server.controller;

import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.view.HypertextFrameView;
import foundation.oned6.dicegrid.server.view.View;

public abstract class HypertextResponseController extends ViewController {
	@Override
	public final View constructPage() throws HTTPException {
		return new HypertextFrameView(constructContents());
	}

	protected abstract View constructContents() throws HTTPException;
}

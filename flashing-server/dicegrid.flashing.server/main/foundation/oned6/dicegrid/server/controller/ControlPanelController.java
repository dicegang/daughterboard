package foundation.oned6.dicegrid.server.controller;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.view.ControlPanelView;
import foundation.oned6.dicegrid.server.view.View;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static foundation.oned6.dicegrid.server.HTTPUtils.requireAuthentication;

public class ControlPanelController extends ViewController {
	private final GridRepository repository;

	public ControlPanelController(GridRepository repository) {
		this.repository = repository;
	}

	public View constructPage() throws HTTPException {
		var me = requireAuthentication();

		var schematics = repository.schematicHistory(me.teamID());
		var flashing = repository.flashingHistory(me.teamID());

		Arrays.sort(schematics, Comparator.comparing(GridRepository.Schematic::uploaded).reversed());
		Arrays.sort(flashing, Comparator.comparing(GridRepository.FlashEvent::submitted).reversed());



		return ControlPanelView.of(me.teamID(), List.of(schematics), List.of(flashing));
	}

}

package foundation.oned6.dicegrid.server.schematic;

import foundation.oned6.dicegrid.server.*;
import foundation.oned6.dicegrid.server.controller.HypertextResponseController;
import foundation.oned6.dicegrid.server.view.StatusMessageView;
import foundation.oned6.dicegrid.server.view.View;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.INTERNAL_SERVER_ERROR;
import static foundation.oned6.dicegrid.server.HTTPUtils.requireAuthentication;
import static foundation.oned6.dicegrid.server.view.StatusMessageView.Status.SUCCESS;

public class UpdateSchematicController extends HypertextResponseController {
	private final ReentrantLock lock = new ReentrantLock(true);

	private final GridRepository repository;
	private final BiConsumer<TeamPrincipal, GridRepository.Schematic> onSchematicUpdate;

	public UpdateSchematicController(GridRepository repository, BiConsumer<TeamPrincipal, GridRepository.Schematic> onSchematicUpdate) {
		this.repository = repository;
		this.onSchematicUpdate = onSchematicUpdate;
	}

	@Override
	protected View constructContents() throws HTTPException {
		try {
			var body = HTTPUtils.parsePOSTBody();

			updateSchematic(body);

			return new StatusMessageView("Update schematic", "Schematic updated", SUCCESS);
		} catch (IOException | InterruptedException e) {
			throw new HTTPException(INTERNAL_SERVER_ERROR);
		} catch (HTTPException e) {
			throw StatusMessageException.of(e, "Update schematic");
		}
	}

	private void updateSchematic(FormBody body) throws InterruptedException, HTTPException {
		var me = requireAuthentication();

		var target = switch (body.getString("target").map(String::trim).orElse(null)) {
			case "Source" -> GridRepository.TargetDevice.SOURCE;
			case "Load" -> GridRepository.TargetDevice.LOAD;
			case null, default -> throw HTTPException.of(BAD_REQUEST);
		};
		var schematic = body.get("file").orElseThrow(() -> HTTPException.of(BAD_REQUEST));

		lock.lockInterruptibly();
		try {
			var updated = repository.updateSchematic(me.teamID(), target, schematic, Instant.now());
			onSchematicUpdate.accept(me, updated);
		} finally {
			lock.unlock();
		}
	}
}

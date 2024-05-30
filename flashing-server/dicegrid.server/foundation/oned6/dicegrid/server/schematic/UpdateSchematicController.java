package foundation.oned6.dicegrid.server.schematic;

import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.FormBody;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.HTTPUtils;
import foundation.oned6.dicegrid.server.StatusMessageException;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.HypertextResponseController;
import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.view.StatusMessageView;
import foundation.oned6.dicegrid.server.view.View;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.*;
import static foundation.oned6.dicegrid.server.HTTPUtils.currentIdentity;
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

			return new StatusMessageView("Update Schematic", "Schematic Updated", SUCCESS);
		} catch (IOException | InterruptedException e) {
			throw new HTTPException(INTERNAL_SERVER_ERROR);
		} catch (HTTPException e) {
			throw StatusMessageException.of(e, "Update Schematic");
		}
	}

	private void updateSchematic(FormBody body) throws InterruptedException, HTTPException {
		var me = switch (currentIdentity()) {
			case TeamPrincipal tp -> tp;
			case AdminPrincipal _ -> throw HTTPException.withMessage("admin doesn't have schematics (you need to masquerade)", NOT_FOUND);
			case null -> throw HTTPException.of(UNAUTHORISED);
		};

		var target = switch (body.getString("target").map(String::trim).orElse(null)) {
			case "Source" -> NodeType.SOURCE;
			case "Load" -> NodeType.LOAD;
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

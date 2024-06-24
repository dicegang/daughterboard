package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.compile.Compiler;
import foundation.oned6.dicegrid.protocol.GridConnection;
import foundation.oned6.dicegrid.protocol.NodeInfo;
import foundation.oned6.dicegrid.protocol.NodeType;
import foundation.oned6.dicegrid.server.*;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.HypertextResponseController;
import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.view.StatusMessageView;
import foundation.oned6.dicegrid.server.view.View;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.*;
import static foundation.oned6.dicegrid.server.HTTPUtils.currentIdentity;
import static foundation.oned6.dicegrid.server.repository.GridRepository.FlashEvent.Status.*;
import static foundation.oned6.dicegrid.server.view.StatusMessageView.Status.SUCCESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class UpdateProgramController extends HypertextResponseController {
	private final Compiler compiler;
	private final FlashingQueue queue;
	private final GridConnection gridConnection;
	private final GridRepository repository;

	private final Consumer<GridRepository.FlashEvent> onNewJob;
	private final Consumer<GridRepository.FlashEvent> onStatusUpdate;

	public UpdateProgramController(Compiler compiler, FlashingQueue queue, GridConnection gridConnection, GridRepository repository, Consumer<GridRepository.FlashEvent> onNewJob, Consumer<GridRepository.FlashEvent> onStatusUpdate) {
		this.compiler = compiler;
		this.queue = queue;
		this.gridConnection = gridConnection;
		this.repository = repository;
		this.onNewJob = onNewJob;
		this.onStatusUpdate = onStatusUpdate;
	}

	@Override
	protected View constructContents() throws HTTPException {
		try {
			var body = HTTPUtils.parsePOSTBody();

			updateProgram(body);

			return new StatusMessageView("Flash Device", "Device flashed successfully", SUCCESS);
		} catch (IOException | InterruptedException e) {
			throw new HTTPException(INTERNAL_SERVER_ERROR);
		} catch (HTTPException e) {
			throw StatusMessageException.of(e, "Flash Device");
		}
	}

	private void updateProgram(FormBody body) throws InterruptedException, HTTPException {
		var me = switch (currentIdentity()) {
			case TeamPrincipal tp -> tp;
			case AdminPrincipal _ -> throw HTTPException.withMessage("admin doesn't have devices to flash (you need to masquerade)", NOT_FOUND);
			case null -> throw HTTPException.of(UNAUTHORISED);
		};

		var target = switch (body.getString("target").map(String::trim).orElse(null)) {
			case "Source" -> NodeType.SOURCE;
			case "Load" -> NodeType.LOAD;
			case null, default -> throw HTTPException.of(BAD_REQUEST);
		};
		var code = body.get("file")
			.map(f -> new Code(new String(f.toArray(JAVA_BYTE), UTF_8)))
			.orElseThrow(() -> HTTPException.of(BAD_REQUEST));

		var job = repository.beginFlashing(me.teamID(), target, code);
		setStatus(job, BUILDING);

		var compiled = compiler.compile(code.code());
		repository.setFlashCompilerOutput(job, compiled.compileLog(), compiled.hex());
		if (compiled.hex() == null) {
			setStatus(job, BUILD_FAILED);
			throw new HTTPException("Compilation failed: " + compiled.compileLog(), BAD_REQUEST);
		}

		setStatus(job, QUEUED);
		try {
			var flashFuture = queue.submit(findNode(me.teamID(), target).address(), compiled.hex());
			setStatus(job, FLASHING);
			flashFuture.get();
		} catch (ExecutionException e) {
			setStatus(job, FLASH_FAILED);
			throw HTTPException.withMessage(e.getCause().getMessage(), INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			setStatus(job, FLASH_FAILED);
			throw e;
		}

		setStatus(job, GridRepository.FlashEvent.Status.SUCCESS);
	}

	private void setStatus(long job, GridRepository.FlashEvent.Status status) throws HTTPException {
		repository.updateFlashStatus(job, status);
		(status == BUILDING ? onNewJob : onStatusUpdate).accept(repository.getFlashEvent(job).orElseThrow());
	}

	private NodeInfo findNode(int teamID, NodeType target) throws HTTPException {
		try {
			return gridConnection.scan().stream()
				.filter(n -> n.ownerID() == teamID && n.nodeType() == target).findFirst()
				.orElseThrow(() -> HTTPException.withMessage("Couldn't find appropriate target device belonging to team.", NOT_FOUND));
		} catch (InterruptedException e) {
			throw HTTPException.withMessage(e.getMessage(), INTERNAL_SERVER_ERROR);
		}
	}
}

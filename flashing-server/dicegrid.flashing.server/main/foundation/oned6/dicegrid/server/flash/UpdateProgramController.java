package foundation.oned6.dicegrid.server.flash;

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
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class UpdateProgramController extends HypertextResponseController {
	private final ReentrantLock lock = new ReentrantLock(true);

	private final GridRepository repository;
	private final BiConsumer<TeamPrincipal, GridRepository.FlashEvent> onFlashingQueue;

	public UpdateProgramController(GridRepository repository, BiConsumer<TeamPrincipal, GridRepository.FlashEvent> onFlashingQueue) {
		this.repository = repository;
		this.onFlashingQueue = onFlashingQueue;
	}

	@Override
	protected View constructContents() throws HTTPException {
		try {
			var body = HTTPUtils.parsePOSTBody();

			updateProgram(body);

			return new StatusMessageView("Flash device", "Queued flashing job", SUCCESS);
		} catch (IOException | InterruptedException e) {
			throw new HTTPException(INTERNAL_SERVER_ERROR);
		} catch (HTTPException e) {
			throw StatusMessageException.of(e, "Flash device");
		}
	}

	private void updateProgram(FormBody body) throws InterruptedException, HTTPException {
		var me = requireAuthentication();

		var target = switch (body.getString("target").map(String::trim).orElse(null)) {
			case "Source" -> GridRepository.TargetDevice.SOURCE;
			case "Load" -> GridRepository.TargetDevice.LOAD;
			case null, default -> throw HTTPException.of(BAD_REQUEST);
		};
		var code = body.get("file")
			.map(f -> new Code(new String(f.toArray(JAVA_BYTE), UTF_8)))
			.orElseThrow(() -> HTTPException.of(BAD_REQUEST));

		lock.lockInterruptibly();
		try {
			var updated = repository.enqueueFlash(me.teamID(), target, code, Instant.now());
			Thread.startVirtualThread(() -> {
				try {
					Thread.sleep(1000);
					repository.postFlashingInProgress(updated, Instant.now());
					onFlashingQueue.accept(me, repository.getFlashEvent(updated));
					Thread.sleep(1000);
					repository.postFlashingCompletion(updated, Instant.now());
					onFlashingQueue.accept(me, repository.getFlashEvent(updated));
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
			onFlashingQueue.accept(me, repository.getFlashEvent(updated));
		} finally {
			lock.unlock();
		}
	}
}

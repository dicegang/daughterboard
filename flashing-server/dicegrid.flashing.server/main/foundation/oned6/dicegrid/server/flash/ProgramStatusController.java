package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.controller.SSEController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPUtils.tryParseLong;
import static foundation.oned6.dicegrid.server.Server.context;

public class ProgramStatusController extends SSEController {
	private final GridRepository repository;
	private final Map<Long, List<OpenConnection>> connections = new ConcurrentHashMap<>();

	public ProgramStatusController(GridRepository repository) {
		this.repository = repository;
	}

	public void refresh(GridRepository.FlashEvent event) {
		try {
			for (var connection : connections.getOrDefault(event.id(), List.of())) {
				connection.updateQueue.put(event);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected void handlerLoopImpl() throws HTTPException, IOException, InterruptedException {
		long id = context().queryParam("id").flatMap(n -> tryParseLong(n))
			.orElseThrow(() -> HTTPException.of(BAD_REQUEST));

		var curr = repository.getFlashEvent(id);

		var queue = new SynchronousQueue<GridRepository.FlashEvent>();
		var connection = new OpenConnection(queue);

		var list = connections.computeIfAbsent(id, k -> new ArrayList<>());
		synchronized (list) {
			list.add(connection);
		}

		try {
			while (curr.completed() == null) {
				curr = queue.take();
				sendEvent(new FlashingHistoryEntry(curr));
			}
		} catch (IOException _) {

		} finally {
			synchronized (list) {
				list.remove(connection);
			}
		}
	}

	private record OpenConnection(SynchronousQueue<GridRepository.FlashEvent> updateQueue) {
	}
}

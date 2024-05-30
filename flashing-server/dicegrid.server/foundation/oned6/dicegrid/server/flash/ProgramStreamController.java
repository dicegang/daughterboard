package foundation.oned6.dicegrid.server.flash;

import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.controller.SSEController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPUtils.tryParseInteger;
import static foundation.oned6.dicegrid.server.Server.context;

public class ProgramStreamController extends SSEController {
	private final Map<Integer, List<OpenConnection>> connections = new ConcurrentHashMap<>();

	public void refresh(GridRepository.FlashEvent event) {
		for (var connection : connections.getOrDefault(event.teamID(), List.of())) {
			connection.updateQueue.offer(event);
		}

	}

	@Override
	protected void handlerLoopImpl() throws HTTPException, IOException, InterruptedException {
		var team = context().queryParam("team").flatMap(n -> tryParseInteger(n))
			.orElseThrow(() -> HTTPException.of(BAD_REQUEST));

		var queue = new ArrayBlockingQueue<GridRepository.FlashEvent>(10);
		var connection = new OpenConnection(queue);

		var list = connections.computeIfAbsent(team, k -> new ArrayList<>());
		synchronized (list) {
			list.add(connection);
		}

		try {
			while (!Thread.interrupted()) {
				var event = queue.take();
				if (event.status() == GridRepository.FlashEvent.Status.values()[0])
					sendEvent("new_entry", new FlashingHistoryEntry(false, event));
				else
					sendEvent("update-" + event.index(), new FlashingHistoryEntry(true, event));
			}
		} catch (IOException e) {
			return;
		} finally {
			synchronized (list) {
				list.remove(connection);
			}
		}
	}

	private record OpenConnection(BlockingQueue<GridRepository.FlashEvent> updateQueue) {
	}
}

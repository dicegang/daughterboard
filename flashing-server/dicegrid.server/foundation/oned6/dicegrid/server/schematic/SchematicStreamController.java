package foundation.oned6.dicegrid.server.schematic;

import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.SSEController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPUtils.tryParseInteger;
import static foundation.oned6.dicegrid.server.Server.context;

public class SchematicStreamController extends SSEController {
	private final Map<Integer, List<OpenConnection>> connections = new ConcurrentHashMap<>();

	public void refresh(TeamPrincipal team, GridRepository.Schematic schematic) {
		try {
			for (var connection : connections.getOrDefault(team.teamID(), List.of())) {
				connection.updateQueue.put(schematic);
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected void handlerLoopImpl() throws HTTPException, IOException, InterruptedException {
		var team = context().queryParam("team").flatMap(n -> tryParseInteger(n))
			.orElseThrow(() -> HTTPException.withMessage("Team ID required", BAD_REQUEST));
		var queue = new SynchronousQueue<GridRepository.Schematic>();
		var connection = new OpenConnection(queue);

		var list = connections.computeIfAbsent(team, k -> new ArrayList<>());
		synchronized (list) {
			list.add(connection);
		}

		try {
			while (true) {
				sendEvent("new_entry", new SchematicListEntry(queue.take()));
			}
		} catch (IOException _) {

		} finally {
			synchronized (list) {
				list.remove(connection);
			}
		}
	}

	private record OpenConnection(SynchronousQueue<GridRepository.Schematic> updateQueue) {
	}
}

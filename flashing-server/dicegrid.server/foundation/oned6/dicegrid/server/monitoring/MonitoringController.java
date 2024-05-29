package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.protocol.DeviceException;
import foundation.oned6.dicegrid.protocol.GridConnection;
import foundation.oned6.dicegrid.protocol.NodeInfo;
import foundation.oned6.dicegrid.protocol.NodeState;
import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.ViewController;
import foundation.oned6.dicegrid.server.view.View;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static foundation.oned6.dicegrid.server.Server.context;

public class MonitoringController extends ViewController {
	private final GridRepository repository;
	private final GridConnection connection;

	private final Map<Integer, TeamPrincipal> teams = new ConcurrentHashMap<>();
	private volatile List<NodeInfo> knownNodes = List.of();
	private final Map<NodeInfo, NodeState> nodes = new ConcurrentHashMap<>();

	public MonitoringController(GridRepository repository, GridConnection connection) {
		this.repository = repository;
		this.connection = connection;

		Thread.startVirtualThread(this::scanLoop);
		Thread.startVirtualThread(this::updateLoop);
	}

	private void scanLoop() {
		try {
			while (!Thread.interrupted()) {
				knownNodes = connection.scan();
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	private void updateLoop() {
		try {
			while (!Thread.interrupted()) {
				for (var node : knownNodes) {
					try {
						nodes.put(node, connection.getState(node.address()));
					} catch (DeviceException e) {
						e.printStackTrace();
					}
				}

				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	@Override
	public View constructPage() throws HTTPException {
		var map = new HashMap<TeamPrincipal, Map<NodeInfo, NodeState>>();
		for (var entry : nodes.entrySet()) {
			var team = teams.computeIfAbsent(entry.getKey().ownerID(), n -> new TeamPrincipal(repository.findTeamName(n), n));
			map.computeIfAbsent(team, t -> new HashMap<>()).put(entry.getKey(), entry.getValue());
		}
		if (context().requestHeader("HX-Request").isPresent())
			return NodeDataTable.of(map);
		else {
			var table = NodeDataTable.of(map);
			return new View() {
				@Override
				public String html() {
					return """
						<div hx-get="%s" hx-trigger="every 1s">%s</div>
						""".formatted(context().contentRoot().resolve("monitoring"), table.html());
				}

				@Override
				public String title() {
					return "Monitoring";
				}
			};
		}
	 	}
}

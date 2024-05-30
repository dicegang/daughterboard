package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.protocol.GridConnection;
import foundation.oned6.dicegrid.protocol.NodeInfo;
import foundation.oned6.dicegrid.protocol.NodeState;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.ViewController;
import foundation.oned6.dicegrid.server.repository.GridRepository;
import foundation.oned6.dicegrid.server.view.View;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.INTERNAL_SERVER_ERROR;
import static foundation.oned6.dicegrid.server.HTTPUtils.currentIdentity;
import static foundation.oned6.dicegrid.server.HTTPUtils.requirePresent;
import static foundation.oned6.dicegrid.server.Server.context;

public class MonitoringController extends ViewController {
	private final GridRepository repository;
	private final GridConnection connection;

	public MonitoringController(GridRepository repository, GridConnection connection) {
		this.repository = repository;
		this.connection = connection;
	}

	@Override
	public View constructPage() throws HTTPException {
		Map<NodeInfo, NodeState> nodes;
		Map<NodeInfo, TeamPrincipal> nodeOwners = new HashMap<>();

		try {
			nodes = connection.scanWithState();

			for (var node : nodes.keySet()) {
				var owner = requirePresent(repository.getPrinciple(node.ownerID()), INTERNAL_SERVER_ERROR);
				nodeOwners.put(node, owner);
			}
		} catch (InterruptedException e) {
			throw new HTTPException(e, INTERNAL_SERVER_ERROR);
		}

		var knownNodes = nodes.keySet().stream();
		List<NodeInfo> controllableNodes = (switch (currentIdentity()) {
			case AdminPrincipal _ -> knownNodes;
			case TeamPrincipal tp -> knownNodes.filter(node -> nodeOwners.get(node).equals(tp));
			case null -> Stream.<NodeInfo>empty();
		}).toList();

		var table = NodeDataTable.of(controllableNodes, nodeOwners, nodes);

		if (context().requestHeader("HX-Request").isPresent())
			return table;
		else
			return new MonitoringView(table);
	}
}

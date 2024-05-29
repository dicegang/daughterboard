package foundation.oned6.dicegrid.server.cp;

import foundation.oned6.dicegrid.protocol.*;
import foundation.oned6.dicegrid.server.GridRepository;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.ViewController;
import foundation.oned6.dicegrid.server.view.StatusMessageView;
import foundation.oned6.dicegrid.server.view.View;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.BAD_REQUEST;
import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.INTERNAL_SERVER_ERROR;
import static foundation.oned6.dicegrid.server.HTTPMethod.POST;
import static foundation.oned6.dicegrid.server.HTTPUtils.requireAuthentication;
import static foundation.oned6.dicegrid.server.Server.context;

public class ControlPanelController extends ViewController {
	private final GridConnection connection;
	private final GridRepository repository;

	public ControlPanelController(GridConnection connection, GridRepository repository) {
		this.connection = connection;
		this.repository = repository;
	}

	public View constructPage() throws HTTPException {
		var me = requireAuthentication();

		if (context().httpMethod() == POST) {
			String action = "";
			NodeType target = null;
			try {
				var body = new String(context().requestBody().readAllBytes(), StandardCharsets.UTF_8);
				var bodyParts = body.split("=");
				if (bodyParts.length != 2)
					throw HTTPException.withMessage("Invalid body", BAD_REQUEST);
				if (!bodyParts[0].equals("action"))
					throw HTTPException.withMessage("Invalid body", BAD_REQUEST);

				target = switch (context().queryParam("target").orElseThrow(() -> HTTPException.withMessage("Need target", BAD_REQUEST))) {
					case "source" -> NodeType.SOURCE;
					case "load" -> NodeType.LOAD;
					default -> throw HTTPException.withMessage("Invalid target", BAD_REQUEST);
				};

				var nodes = getTeamNodes(me);
				var targetNode = switch (target) {
					case SOURCE -> nodes.source();
					case LOAD -> nodes.load();
				};

				 action = bodyParts[1];
				switch (action) {
					case "Engage" -> connection.setEngaged(targetNode.address(), true);
					case "Disengage" -> connection.setEngaged(targetNode.address(), false);
					case "Start" -> {
						connection.setEngaged(targetNode.address(), false);
						connection.setShutdown(targetNode.address(), false);
					}
					case "Shutdown" -> connection.setShutdown(targetNode.address(), true);
					default -> throw HTTPException.withMessage("Invalid action", BAD_REQUEST);
				}
			} catch (HTTPException e) {
				e.addViewWrapper(v -> new StatusMessageView("Perform Action", v.html(), StatusMessageView.Status.FAILURE));
				throw e;
			}
			catch (IOException | DeviceException | InterruptedException e) {
				var ex = HTTPException.withMessage(e.getMessage(), INTERNAL_SERVER_ERROR);
				String finalAction = action;
				NodeType finalTarget = target;
				ex.addViewWrapper(v -> new StatusMessageView(finalAction + " " + finalTarget, e.getMessage(), StatusMessageView.Status.FAILURE));
				throw ex;
			}

			return new StatusMessageView(action + " " + target, "", StatusMessageView.Status.SUCCESS);
		}

		var schematics = repository.schematicHistory(me.teamID());
		var flashing = repository.flashingHistory(me.teamID());

		Arrays.sort(schematics, Comparator.comparing(GridRepository.Schematic::uploaded).reversed());
		Arrays.sort(flashing, Comparator.comparing(GridRepository.FlashEvent::lastUpdate).reversed());

		var result = getTeamNodes(me);
		return ControlPanelView.of(me, result.sourceState(), result.loadState(), List.of(schematics), List.of(flashing));
	}

	private TeamNodes getTeamNodes(TeamPrincipal me) throws HTTPException {
		Map<NodeInfo, NodeState> map = null;
		try {
			map = connection.scan().stream().filter(n -> n.ownerID() == me.teamID())
				.collect(Collectors.toMap(n -> n, n -> {
					try {
						return connection.getState(n.address());
					} catch (DeviceException | InterruptedException e) {
						throw new RuntimeException(e);
					}
				}));
		} catch (InterruptedException e) {
			throw HTTPException.withMessage("Scan failed", INTERNAL_SERVER_ERROR);
		}
		if (map.size() != 2)
			throw HTTPException.withMessage("Wrong number of nodes for team " + me + ": " + map.size(), INTERNAL_SERVER_ERROR);

		var source = map.entrySet().stream().filter(n -> n.getKey().nodeType() == NodeType.SOURCE).findAny().
			orElseThrow(() -> HTTPException.withMessage("No sourceState node found", INTERNAL_SERVER_ERROR));
		var load = map.entrySet().stream().filter(n -> n.getKey().nodeType() == NodeType.LOAD).findAny()
			.orElseThrow(() -> HTTPException.withMessage("No loadState node found", INTERNAL_SERVER_ERROR));
		return new TeamNodes(source.getKey(), load.getKey(), source.getValue(), load.getValue());
	}

	private record TeamNodes(NodeInfo source, NodeInfo load, NodeState sourceState, NodeState loadState) {
	}

}

package foundation.oned6.dicegrid.server.cp;

import foundation.oned6.dicegrid.protocol.*;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.repository.GridRepository;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.*;
import static foundation.oned6.dicegrid.server.HTTPUtils.*;
import static foundation.oned6.dicegrid.server.RequestContext.parseQuery;
import static foundation.oned6.dicegrid.server.Server.context;

public class ControlPanelController extends ViewController {
	private final GridConnection connection;
	private final GridRepository repository;

	public ControlPanelController(GridConnection connection, GridRepository repository) {
		this.connection = connection;
		this.repository = repository;
	}

	public View constructPage() throws HTTPException {
		return switch (context().httpMethod()) {
			case GET -> handleGET();
			case POST -> handlePOST();
			default -> throw HTTPException.of(METHOD_NOT_ALLOWED);
		};
	}

	private ControlPanelView handleGET() throws HTTPException {
		var me = switch (currentIdentity()) {
			case TeamPrincipal tp -> tp;
			case AdminPrincipal _ -> throw HTTPException.withMessage("admin panel not done yet", NOT_IMPLEMENTED);
			case null -> throw HTTPException.of(UNAUTHORISED);
		};

		var schematics = repository.schematicHistory(me.teamID());
		var flashing = repository.flashingHistory(me.teamID());

		Arrays.sort(schematics, Comparator.comparing(GridRepository.Schematic::uploaded).reversed());
		Arrays.sort(flashing, Comparator.comparing(GridRepository.FlashEvent::lastUpdate).reversed());

		var result = getTeamNodes(me.teamID());
		return ControlPanelView.of(me, result.sourceState(), result.loadState(), List.of(schematics), List.of(flashing));
	}

	private StatusMessageView handlePOST() throws HTTPException {
		NodeInfo targetDevice = null;
		String action = "";

		try {
			var body = parsePOSTBody();
			checkControlPermissions(body.targetTeam());

			action = body.action();
			var targetTeamNodes = getTeamNodes(body.targetTeam());
			targetDevice = switch (body.nodeType()) {
				case SOURCE -> targetTeamNodes.source();
				case LOAD -> targetTeamNodes.load();
			};

			performAction(action, targetDevice);
			var message = (switch (action) {
				case "engage" ->  "Your <strong>%s</strong> now <strong>ENGAGED</strong>";
				case "disengage" -> "Your <strong>%s</strong> now <strong>DISENGAGED</strong>";
				case "start" -> "Your <strong>%s</strong> is now <strong>DISENGAGED</strong>";
				case "shutdown" -> "Your <strong>%s</strong> is now <strong>SHUTDOWN</strong>";
				default -> throw new AssertionError();
			}).formatted(targetDevice.nodeType().name());
			return new StatusMessageView(action, message, StatusMessageView.Status.SUCCESS);
		} catch (HTTPException e) {
			throw StatusMessageView.wrapException(action, e);
		}
	}

	private static void checkControlPermissions(int teamID) throws HTTPException {
		switch (currentIdentity()) {
			case AdminPrincipal _ -> {}
			case TeamPrincipal tp when tp.teamID() == teamID -> {}
			case null, default -> throw HTTPException.of(UNAUTHORISED);
		}
	}

	private static final Pattern ACTION = Pattern.compile("(shutdown|start|engage|disengage)-(\\d+)-(source|load)");

	private void performAction(String action, NodeInfo targetNode) throws HTTPException {
		try {
			switch (action) {
				case "engage" -> connection.setEngaged(targetNode.address(), true);
				case "disengage" -> connection.setEngaged(targetNode.address(), false);
				case "start" -> {
					connection.setEngaged(targetNode.address(), false);
					connection.setShutdown(targetNode.address(), false);
				}
				case "shutdown" -> connection.setShutdown(targetNode.address(), true);
				default -> throw HTTPException.withMessage("Invalid action", BAD_REQUEST);
			}
		} catch (DeviceException | InterruptedException e) {
			throw new HTTPException(e, INTERNAL_SERVER_ERROR);
		}
	}

	private ActionFormBody parsePOSTBody() throws HTTPException {
		try {
			var body = new String(context().requestBody().readAllBytes(), StandardCharsets.UTF_8);
			var formValue = parseQuery(body).get("action");
			if (formValue == null)
				throw HTTPException.of(BAD_REQUEST);

			var matcher = ACTION.matcher(formValue);
			if (!matcher.matches())
				throw HTTPException.of(BAD_REQUEST);

			var action = matcher.group(1);
			int targetTeam = requirePresent(tryParseInteger(matcher.group(2)), BAD_REQUEST);
			var targetDevice = switch (matcher.group(3)) {
				case "source" -> NodeType.SOURCE;
				case "load" -> NodeType.LOAD;
				default -> throw HTTPException.of(BAD_REQUEST);
			};

			return new ActionFormBody(targetTeam, action, targetDevice);
		} catch (IOException e) {
			throw HTTPException.withMessage("Failed to read request body", INTERNAL_SERVER_ERROR);
		}
	}

	private TeamNodes getTeamNodes(int teamID) throws HTTPException {
		Map<NodeInfo, NodeState> map = null;
		try {
			map = connection.scan().stream().filter(n -> n.ownerID() == teamID)
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
			throw HTTPException.withMessage("Wrong number of nodes for team " + repository.findTeamName(teamID) + ": " + map.size(), INTERNAL_SERVER_ERROR);

		var source = map.entrySet().stream().filter(n -> n.getKey().nodeType() == NodeType.SOURCE).findAny().
			orElseThrow(() -> HTTPException.withMessage("No sourceState node found", INTERNAL_SERVER_ERROR));
		var load = map.entrySet().stream().filter(n -> n.getKey().nodeType() == NodeType.LOAD).findAny()
			.orElseThrow(() -> HTTPException.withMessage("No loadState node found", INTERNAL_SERVER_ERROR));
		return new TeamNodes(source.getKey(), load.getKey(), source.getValue(), load.getValue());
	}

	private record ActionFormBody(int targetTeam, String action, NodeType nodeType) {
	}

	private record TeamNodes(NodeInfo source, NodeInfo load, NodeState sourceState, NodeState loadState) {
	}

}

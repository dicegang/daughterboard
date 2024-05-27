package foundation.oned6.dicegrid.server;

import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsServer;
import foundation.oned6.dicegrid.server.controller.*;
import foundation.oned6.dicegrid.server.flash.ProgramStatusController;
import foundation.oned6.dicegrid.server.flash.ProgramExploreController;
import foundation.oned6.dicegrid.server.flash.ProgramStreamController;
import foundation.oned6.dicegrid.server.flash.UpdateProgramController;
import foundation.oned6.dicegrid.server.schematic.SchematicDownloadController;
import foundation.oned6.dicegrid.server.schematic.SchematicStreamController;
import foundation.oned6.dicegrid.server.schematic.UpdateSchematicController;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.METHOD_NOT_ALLOWED;
import static foundation.oned6.dicegrid.server.HTTPUtils.handleHttpException;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server implements AutoCloseable {
	public static final ScopedValue<RequestContext> CONTEXT = ScopedValue.newInstance();

	private static final System.Logger logger = System.getLogger("Server");

	private final Map<String, List<ConditionalHandler>> contexts = new HashMap<>();

	private final GridRepository repository;
	private final HttpsServer server;

	private Server(HttpsServer server, GridRepository repository) {
		this.server = server;
		this.repository = repository;

		var schematicsStream  = new SchematicStreamController();
		var programStream = new ProgramStreamController();
		var programStatus = new ProgramStatusController(repository);

		addController("/schematics", ctx -> ctx.queryParam("stream").isPresent(), schematicsStream);
		addController("/schematics", ctx -> ctx.queryParam("download").isPresent() || ctx.queryParam("view").isPresent(), new SchematicDownloadController(repository));

		addController("/program", new ProgramExploreController(repository));
		addController("/program-list", programStream);
		addController("/program-status", programStatus);

		addController("/control", PageController.of(new ControlPanelController(repository)));

		addController("/update-schematic", new UpdateSchematicController(repository, schematicsStream::refresh));
		addController("/flash-program", new UpdateProgramController(repository, (_, e) -> {
			programStream.refresh(e);
			programStatus.refresh(e);
		}));
	}

	private synchronized void addController(String path, Controller controller) {
		contexts.computeIfAbsent(path, _ -> {
				registerHandledRoute(path);
				return new ArrayList<>();
			})
			.addFirst(ConditionalHandler.forMimeType(controller.mimeType(), controller));
	}

	private synchronized void addController(String path, Predicate<RequestContext> predicate, Controller controller) {
		contexts.computeIfAbsent(path, _ -> {
				registerHandledRoute(path);
				return new ArrayList<>();
			})
			.addFirst(new ConditionalHandler(predicate, controller));
	}

	private void registerHandledRoute(String path) {
		server.createContext(path, exchange -> {
			try (exchange) {
				var context = createContext((HttpsExchange) exchange);
				handleRequest(path, (HttpsExchange) exchange, context);
			} catch (HTTPException e) {
				handleHttpException((HttpsExchange) exchange, e);
			}
		});
	}

	private void handleRequest(String path, HttpsExchange exchange, RequestContext context) {
		ScopedValue.runWhere(CONTEXT, context, () -> {
			for (var handler : contexts.getOrDefault(path, List.of()))
				if (handler.predicate().test(context)) {
					handler.controller.handleRequest(exchange);
					return;
				}
		});
	}


	private TeamPrincipal authenticateTeam(HttpsExchange exchange) {
		try {
			var teamName = parseTeamName(exchange.getSSLSession().getPeerPrincipal().getName());
			return new TeamPrincipal(teamName, repository.findTeamID(teamName));
		} catch (SSLPeerUnverifiedException e) {
			return null;
		}
	}

	public static String parseTeamName(String distinguishedName) {
//		return ((X509Certificate) certificate).getSubjectX500Principal().getName();
		return "test";
	}

	private RequestContext createContext(HttpsExchange exchange) throws HTTPException {
		var method = HTTPMethod.fromString(exchange.getRequestMethod());
		if (method == null)
			throw new HTTPException(METHOD_NOT_ALLOWED);

		return new RequestContext(
			authenticateTeam(exchange),
			exchange.getRequestURI().resolve("/"),
			exchange.getRequestURI(),
			method,
			exchange.getRequestHeaders(),
			exchange.getRequestBody(),
			UTF_8
		);
	}

	private record ConditionalHandler(Predicate<RequestContext> predicate, Controller controller) {
		private static ConditionalHandler forMimeType(String mimeType, Controller controller) {
			return new ConditionalHandler(context ->
				Arrays.stream(context.requestHeader("Accept").orElse("*/*").split(","))
					.map(n -> n.split(";")[0].strip())
				.anyMatch(n -> mimeTypeMatch(mimeType, n)), controller);
		}

		private static boolean mimeTypeMatch(String mimeType, String... accepted) {
			var split = mimeType.toLowerCase().split("/");
			if (split.length != 2)
				return false;

			var type = split[0];
			var subtype = split[1];

			for (var accept : accepted) {
				var acceptedSplit = accept.toLowerCase().split("/");
				if (acceptedSplit.length != 2)
					continue;

				var acceptedType = acceptedSplit[0];
				var acceptedSubtype = acceptedSplit[1];

				if (acceptedType.equals("*") || acceptedType.equals(type)) {
					if (acceptedSubtype.equals("*") || acceptedSubtype.equals(subtype))
						return true;
				}
			}

			return false;
		}
	}

	@Override
	public void close() {
		server.stop(1);
	}

	public static RequestContext context() {
		return CONTEXT.orElseThrow(IllegalCallerException::new);
	}

	public static Server create(GridRepository repository, AuthManager authManager, InetSocketAddress address) throws IOException {
		var server = HttpsServer.create(address, 0);
		server.setHttpsConfigurator(authManager.configurator());
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		server.start();

		return new Server(server, repository);
	}
}

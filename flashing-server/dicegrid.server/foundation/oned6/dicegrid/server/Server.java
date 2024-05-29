package foundation.oned6.dicegrid.server;

import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsServer;
import foundation.oned6.dicegrid.compile.DockerCompiler;
import foundation.oned6.dicegrid.protocol.GridConnection;
import foundation.oned6.dicegrid.server.admin.MasqueradeController;
import foundation.oned6.dicegrid.server.auth.AdminPrincipal;
import foundation.oned6.dicegrid.server.auth.AuthManager;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.controller.*;
import foundation.oned6.dicegrid.server.cp.CertificateDownloadController;
import foundation.oned6.dicegrid.server.cp.ControlPanelController;
import foundation.oned6.dicegrid.server.flash.*;
import foundation.oned6.dicegrid.server.monitoring.MonitoringController;
import foundation.oned6.dicegrid.server.monitoring.TestDataPage;
import foundation.oned6.dicegrid.server.schematic.SchematicDownloadController;
import foundation.oned6.dicegrid.server.schematic.SchematicStreamController;
import foundation.oned6.dicegrid.server.schematic.UpdateSchematicController;
import foundation.oned6.dicegrid.server.view.View;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.METHOD_NOT_ALLOWED;
import static foundation.oned6.dicegrid.server.HTTPUtils.handleHttpException;
import static foundation.oned6.dicegrid.server.HTTPUtils.tryParseInteger;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server implements AutoCloseable {
	public static final ScopedValue<RequestContext> CONTEXT = ScopedValue.newInstance();
	public static final ScopedValue<TeamPrincipal> MASQUERADE = ScopedValue.newInstance();
	public static final ScopedValue<GridRepository> REPOSITORY = ScopedValue.newInstance();

	private static final System.Logger logger = System.getLogger("Server");

	private final Map<String, List<ConditionalHandler>> contexts = new HashMap<>();

	private final AuthManager au;

	private final GridRepository repository;
	private final HttpsServer server;

	private Server(HttpsServer server, GridRepository repository, GridConnection connection, AuthManager au) {
		this.server = server;
		this.repository = repository;
		this.au = au;

		var schematicsStream  = new SchematicStreamController();
		var programStream = new ProgramStreamController();

		addController("/test", PageController.of(repository, new ViewController() {
			@Override
			public View constructPage() throws HTTPException {
				return new TestDataPage();
			}
		}));

		addController("/build-log", new BuildLogController(repository));
		addController("/program-hex", new ProgramHexController(repository));

		addController("/schematics", ctx -> ctx.queryParam("stream").isPresent(), schematicsStream);
		addController("/schematics", ctx -> ctx.queryParam("download").isPresent() || ctx.queryParam("view").isPresent(), new SchematicDownloadController(repository));

		addController("/program", new ProgramExploreController(repository));
		addController("/program-list", programStream);

		addController("/monitoring", PageController.of(repository, new MonitoringController(repository, connection)));
		addController("/control", PageController.of(repository, new ControlPanelController(connection, repository)));
		addController("/download-certificate", new CertificateDownloadController(au));
		addController("/masquerade", new MasqueradeController());

		addController("/update-schematic", new UpdateSchematicController(repository, schematicsStream::refresh));
		addController("/flash-program", new UpdateProgramController(new DockerCompiler(), new FlashingQueue(connection), connection, repository, programStream::refresh, programStream::refresh));
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
		var scope = ScopedValue.where(CONTEXT, context)
			.where(REPOSITORY, repository);
		if (context.activeUser() instanceof AdminPrincipal) {
			var cookie = context.cookie("masquerade");
			var id = cookie.flatMap(HTTPUtils::tryParseInteger);
			if (id.isPresent()) {
				var name = repository.findTeamName(id.get());
				if (name != null)
					scope = scope.where(MASQUERADE, new TeamPrincipal(name, id.get()));
			}
		}

		scope.run(() -> {
			for (var handler : contexts.getOrDefault(path, List.of()))
				if (handler.predicate().test(context)) {
					handler.controller.handleRequest(exchange);
					return;
				}
		});
	}


	private Principal authenticateTeam(HttpsExchange exchange) {
		try {
			var teamName = parseTeamName(exchange.getSSLSession().getPeerPrincipal().getName());
			if (teamName.equals("Administrator"))
				return new AdminPrincipal();

			return new TeamPrincipal(teamName, repository.findTeamID(teamName));
		} catch (SSLPeerUnverifiedException e) {
			return null;
		}
	}

	public static String parseTeamName(String distinguishedName) {
		var parts = distinguishedName.split(",");
		for (var part : parts) {
			var split = part.split("=");
			if (split.length == 2 && split[0].trim().equals("CN"))
				return split[1].trim();
		}

		return null;
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

	public static Server create(GridRepository repository, GridConnection connection, AuthManager authManager, InetSocketAddress address) throws IOException {
		var server = HttpsServer.create(address, 0);
		server.setHttpsConfigurator(authManager.configurator());
		server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		server.start();

		return new Server(server, repository, connection, authManager);
	}
}

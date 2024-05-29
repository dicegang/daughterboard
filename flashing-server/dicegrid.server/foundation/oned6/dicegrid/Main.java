package foundation.oned6.dicegrid;

import com.sun.net.httpserver.*;
import foundation.oned6.dicegrid.protocol.FakeGridConnection;
import foundation.oned6.dicegrid.server.auth.AuthManager;
import foundation.oned6.dicegrid.server.Server;

import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class Main {
	final static String SERVER_PWD = "j*E[OJ92)EUQG1`<DMo-LgC)l";
	final static String KST_SERVER = "localhost.p12";
	final static String TST_SERVER = "root.p12";

	public static HttpsServer server;

	public static void main(String[] args) throws Exception {
		var repo = new TestRepository();
		repo.addTeam("Test Team 1");
		repo.addTeam("Test Team 2");
		repo.addTeam("Test Team 3");
		try (var server = Server.create(repo, new FakeGridConnection(),
			new AuthManager(
				loadKeyStore("ca.p12", SERVER_PWD), SERVER_PWD, "ca",
				loadKeyStore(KST_SERVER, SERVER_PWD),
				loadKeyStore(TST_SERVER, SERVER_PWD),
				SERVER_PWD
			), new InetSocketAddress(8443))) {
			System.out.println("Server started on port 8443");
			System.in.read();
		}

	}

	private static KeyStore loadKeyStore(String name, String password) throws Exception {
		var keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream(name), password.toCharArray());
		return keyStore;
	}
}


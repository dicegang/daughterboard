package foundation.oned6.dicegrid;

import com.sun.net.httpserver.*;
import foundation.oned6.dicegrid.server.AuthManager;
import foundation.oned6.dicegrid.server.Server;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class Main {
	final static String SERVER_PWD = "aoopgod1286";
	final static String KST_SERVER = "server.jks";
	final static String TST_SERVER = "ca.jks";

	public static HttpsServer server;

	public static void main(String[] args) throws Exception {
		try (var server = Server.create(new TestRepository(), new AuthManager(loadKeyStore(KST_SERVER, SERVER_PWD), loadKeyStore(TST_SERVER, SERVER_PWD), SERVER_PWD), new InetSocketAddress(8443))) {
			System.out.println("Server started on port 8443");
			System.in.read();
		}

	}

	private static KeyStore loadKeyStore(String name, String password) throws Exception {
		var keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(name), password.toCharArray());
		return keyStore;
	}
}


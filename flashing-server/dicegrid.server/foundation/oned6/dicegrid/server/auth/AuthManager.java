package foundation.oned6.dicegrid.server.auth;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import foundation.oned6.dicegrid.server.HTTPException;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.security.cert.Certificate;
import java.util.stream.StreamSupport;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.INTERNAL_SERVER_ERROR;

public class AuthManager {
	private final KeyStore signingStore;
	private final String signingStorePassword;
	private final String signingAlias;

	private final KeyManagerFactory kmf;
	private final TrustManagerFactory tmf;

	public AuthManager(KeyStore signingStore, String signingStorePassword, String signingAlias, KeyStore keyStore, KeyStore trustStore, String keyStorePassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		this.kmf = KeyManagerFactory.getInstance("SunX509");
		this.tmf = TrustManagerFactory.getInstance("SunX509");

		kmf.init(keyStore, keyStorePassword.toCharArray());
		tmf.init(trustStore);

		this.signingStore = signingStore;
		this.signingStorePassword = signingStorePassword;
		this.signingAlias = signingAlias;
	}

	public byte[] createCertificate(String password, TeamPrincipal team) throws HTTPException {
		try {
			var m = DicegridCertificateManager.create();
			var store = m.deriveLeaf(new X500Principal("CN=" + team.getName()), signingStore, signingStorePassword, signingAlias, password, team.getName() + "'s Certificate");

			return m.asLegacyP12(store, password, team.getName() + "'s Certificate", password);
		} catch (GeneralSecurityException e) {
			throw HTTPException.withMessage( "Failed to create certificate: " + e.getMessage(), INTERNAL_SERVER_ERROR);
		}
	}

	public HttpsConfigurator configurator() {
		return new HttpsConfigurator(sslContext()) {
			@Override
			public void configure(HttpsParameters params) {
				var sslParams = getSSLContext().getDefaultSSLParameters();
				sslParams.setNeedClientAuth(true);

				params.setNeedClientAuth(true);
				params.setSSLParameters(sslParams);
			}
		};
	}

	private SSLContext sslContext() {
		try {
			var sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

			return sslContext;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
}

package foundation.oned6.dicegrid.server;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import javax.net.ssl.*;
import java.security.*;

public class AuthManager {

	private final KeyManagerFactory kmf;
	private final TrustManagerFactory tmf;

	public AuthManager(KeyStore keyStore, KeyStore trustStore, String keyStorePassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		this.kmf = KeyManagerFactory.getInstance("SunX509");
		this.tmf = TrustManagerFactory.getInstance("SunX509");

		kmf.init(keyStore, keyStorePassword.toCharArray());
		tmf.init(trustStore);
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

package foundation.oned6.dicegrid.server.auth;

import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public interface DicegridCertificateManager {
	KeyStore createRoot(X500Principal details, String storePassword) throws GeneralSecurityException;
	KeyStore deriveCA(X500Principal details, KeyStore root, String rootStorePassword, String caStorePassword) throws GeneralSecurityException;
	KeyStore deriveLeaf(X500Principal details, KeyStore ca, String caStorePassword, String caAlias, String leafPassword, String leafAlias) throws GeneralSecurityException;
	byte[] asLegacyP12(KeyStore store, String storePassword, String alias, String p12Password) throws GeneralSecurityException;

	static DicegridCertificateManager create() {
		return new KeyToolManager();
	}
}

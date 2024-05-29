package foundation.oned6.dicegrid.server.admin;

import foundation.oned6.dicegrid.protocol.GridConnection;
import foundation.oned6.dicegrid.server.HTTPException;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.auth.DicegridCertificateManager;
import foundation.oned6.dicegrid.server.controller.ViewController;
import foundation.oned6.dicegrid.server.view.View;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.util.Set;

import static foundation.oned6.dicegrid.server.HTTPException.HTTPCode.FORBIDDEN;
import static foundation.oned6.dicegrid.server.Server.context;
import static foundation.oned6.dicegrid.server.auth.AdminPrincipal.ADMIN;

public class AdminController extends ViewController {
	private final GridConnection connection;

	private final KeyStore caStore;
	private final String caPassword;

	public AdminController(GridConnection connection, KeyStore caStore, String caPassword) {
		this.connection = connection;
		this.caStore = caStore;
		this.caPassword = caPassword;
	}

	@Override
	public View constructPage() throws HTTPException {
		if (!context().activeUser().implies(ADMIN))
			throw HTTPException.of(FORBIDDEN);


		return null;
	}

//	private KeyStore createPrincipalStore(TeamPrincipal principal) throws GeneralSecurityException {
//		try {
//			var mgr = DicegridCertificateManager.create();
//
//			var password = randomPassword();
//			return mgr.deriveLeaf(new X500Principal("CN=" + principal.getName()), caStore, caPassword, password);
//		} catch (Exception e) {
//			throw new IOException("Failed to create principal store", e);
//		}
//	}

	private static String randomPassword() {
		try {
			return SecureRandom.getInstanceStrong()
				.ints(10, 33, 126)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}

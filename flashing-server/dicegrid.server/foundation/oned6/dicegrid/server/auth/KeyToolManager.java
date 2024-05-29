package foundation.oned6.dicegrid.server.auth;

import foundation.oned6.dicegrid.TempFile;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

class KeyToolManager implements DicegridCertificateManager {

	@Override
	public KeyStore createRoot(X500Principal details, String storePassword) throws GeneralSecurityException {
		try {
			var rootKey = generateKeyPair(details.getName(), "bc:c");

			var store = KeyStore.getInstance("PKCS12");
			store.load(null, storePassword.toCharArray());
			store.setKeyEntry("root", rootKey.privateKey(), storePassword.toCharArray(), new Certificate[]{rootKey.selfSignedCertificate()});

			return store;
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public KeyStore deriveCA(X500Principal details, KeyStore root, String rootStorePassword, String caStorePassword) throws GeneralSecurityException {
		try {
			var ca = generateKeyPair(details.getName(), "bc:c");
			var store = ca.store("ca", caStorePassword);

			var csr = csr(store, caStorePassword, "ca");
			var cert = generateCert(root, rootStorePassword, "root", csr, "BC=0");

			store.deleteEntry("ca");
			store.setKeyEntry("ca", ca.privateKey(), caStorePassword.toCharArray(), new Certificate[]{ cert, root.getCertificate("root")});

			return store;
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public KeyStore deriveLeaf(X500Principal details, KeyStore ca, String caStorePassword, String caAlias, String leafPassword, String leafAlias) throws GeneralSecurityException {
		try {
			var leaf = generateKeyPair(details.getName(), "ku:c=dig,keyEncipherment");
			var leafStore = leaf.store(leafAlias, leafPassword);

			var csr = csr(leafStore, leafPassword, leafAlias);
			var cert = generateCert(ca, caStorePassword, caAlias, csr, "ku:c=dig,keyEncipherment");

			var caChain = ca.getCertificateChain(caAlias);
			var newChain = new Certificate[caChain.length + 1];
			System.arraycopy(caChain, 0, newChain, 1, caChain.length);
			newChain[0] = cert;

			leafStore.setKeyEntry(leafAlias, leaf.privateKey(), leafPassword.toCharArray(), newChain);

			return leafStore;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] asLegacyP12(KeyStore store, String storePassword, String alias, String p12Password) throws GeneralSecurityException {
		try (
			var leafPKCS12 = TempFile.create("leaf", ".p12");
			var p12File = TempFile.create("store", ".p12");
			var privKeyPem = TempFile.create("privkey", ".pem");
			var chainPem = TempFile.create("chain", ".pem")) {
			writeKeyStore(store, leafPKCS12.path(), storePassword);

			convertToPkcs12(leafPKCS12.path(), p12File.path(), storePassword);
			extractPrivateKey(p12File.path(), privKeyPem.path(), p12Password);

			var chain = Arrays.stream(store.getCertificateChain(alias))
				.map(this::toPem)
				.collect(Collectors.joining());
			System.out.println("c");

			System.out.println(chain);
			Files.writeString(chainPem.path(), chain);
			createLegacyP12(alias, chainPem.path(), privKeyPem.path(), p12File.path(), p12Password);

			return Files.readAllBytes(p12File.path());
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private KeyPairCertificate generateKeyPair(String dname, String ext) throws IOException, InterruptedException, GeneralSecurityException {
		try (var keystore = TempFile.create("keystore", ".p12")) {
			var pass = randomPassword();
			new ProcessBuilder("keytool", "-genkeypair", "-keyalg", "EC",
				"-keystore", keystore.toString(), "-alias", "kp",
				"-ext", ext,
				"-dname", dname, "-storepass", pass)
				.inheritIO().start().waitFor();

			var store = loadKeyStore(keystore.path(), pass);
			var privateKey = (PrivateKey) store.getKey("kp", pass.toCharArray());
			var cert = store.getCertificate("kp");

			return new KeyPairCertificate(new KeyPair(cert.getPublicKey(), privateKey), store.getCertificate("kp"));
		}
	}

	private void exportCert(Path keystore, String alias, String storepass, Path outfile) throws IOException, InterruptedException {
		new ProcessBuilder("keytool", "-exportcert", "-keystore", keystore.toString(),
			"-alias", alias, "-rfc", "-storepass", storepass,
			"-file", outfile.toString())
			.inheritIO().start().waitFor();
	}

	private String csr(KeyStore signeeStore, String signeePass, String signeeAlias) throws IOException, GeneralSecurityException, InterruptedException {
		try (var signeePKCS12 = TempFile.create("signee", ".p12");
		     var csr = TempFile.create("csr", ".csr")) {
			writeKeyStore(signeeStore, signeePKCS12.path(), signeePass);
			new ProcessBuilder("keytool", "-storepass", signeePass, "-keystore", signeePKCS12.path().toString(),
				"-certreq", "-keyalg", "EC", "-alias", signeeAlias).redirectOutput(csr.path().toFile()).start().waitFor();
			return Files.readString(csr.path());
		}
	}

	private Certificate generateCert(KeyStore signerStore, String signerPass, String signerAlias, String csr, String ext) throws IOException, InterruptedException, GeneralSecurityException {
		// ${KEYTOOL} -storepass ${CA_PASS} -keystore ${CA_STORE} -certreq  -keyalg EC -alias ${CA_ALIAS} | ${KEYTOOL} -storepass ${SIGNER_PASSWORD} -keystore ${SIGNER_STORE} -gencert -keyalg EC  -alias ${SIGNER_ALIAS} -ext ${EXT} -rfc > ca.pem
		try (var csrFile = TempFile.create("csr", ".csr");
		     var cert = TempFile.create("cert", ".pem");
		     var signerPKCS12 = TempFile.create("signer", ".p12")
		) {
			Files.writeString(csrFile.path(), csr);
			writeKeyStore(signerStore, signerPKCS12.path(), signerPass);

			var p = new ProcessBuilder("keytool", "-storepass", signerPass, "-keystore", signerPKCS12.path().toString(),
				"-gencert", "-keyalg", "EC", "-alias", signerAlias, "-ext", ext, "-rfc")
				.start();
			p.getOutputStream().write(csr.getBytes(StandardCharsets.UTF_8));
			p.getOutputStream().close();
			p.getInputStream().transferTo(Files.newOutputStream(cert.path()));
			if (p.waitFor() != 0)
				throw new IOException("Failed to generate certificate");

			return CertificateFactory.getInstance("X.509").generateCertificate(Files.newInputStream(cert.path()));
		}
	}

	private void importCert(Path keystore, String alias, Path infile, String storepass) throws IOException, InterruptedException {
		new ProcessBuilder("keytool", "-importcert", "-keystore", keystore.toString(),
			"-alias", alias, "-file", infile.toString(),
			"-storepass", storepass)
			.inheritIO().start().waitFor();
	}

	private void convertToPkcs12(Path srcKeystore, Path destKeystore, String storepass) throws IOException, InterruptedException {
		new ProcessBuilder("keytool", "-importkeystore", "-srckeystore", srcKeystore.toString(),
			"-destkeystore", destKeystore.toString(), "-deststoretype", "PKCS12",
			"-deststorepass", storepass, "-destkeypass", storepass, "-srcstorepass", storepass)
			.inheritIO().start().waitFor();
	}

	private void extractPrivateKey(Path p12file, Path outfile, String storepass) throws IOException, InterruptedException {
		new ProcessBuilder("openssl", "pkcs12", "-in", p12file.toString(), "-nodes",
			"-nocerts", "-out", outfile.toString(),
			"-passin", "pass:" + storepass, "-passout", "pass:" + storepass)
			.inheritIO().start().waitFor();
	}

	private void createLegacyP12(String alias, Path infile, Path privkey, Path outfile, String storepass) throws IOException, InterruptedException {
		new ProcessBuilder("openssl", "pkcs12", "-export", "-in", infile.toString(),
			"-inkey", privkey.toString(), "-out", outfile.toString(), "-name", alias,
			"-legacy", "-passin", "pass:" + storepass, "-passout", "pass:" + storepass)
			.inheritIO().start().waitFor();
	}

	private KeyStore loadKeyStore(Path keystorePath, String storePassword) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
		try (var fis = Files.newInputStream(keystorePath)) {
			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(fis, storePassword.toCharArray());
			return ks;
		}
	}

	private void writeKeyStore(KeyStore keyStore, Path keystorePath, String storePassword) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
		try (var fos = Files.newOutputStream(keystorePath)) {
			keyStore.store(fos, storePassword.toCharArray());
		}
	}

	private String randomPassword() {
		try {
			return SecureRandom.getInstanceStrong().ints(32, 33, 126)
				.mapToObj(i -> String.valueOf((char) i)).collect(Collectors.joining());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private String toPem(Certificate cert) {
		try {
			return "-----BEGIN CERTIFICATE-----\n" +
				Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(cert.getEncoded()) +
				"\n-----END CERTIFICATE-----\n";
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
	}

	private record KeyPairCertificate(KeyPair keyPair, Certificate selfSignedCertificate) {
		PrivateKey privateKey() {
			return keyPair.getPrivate();
		}

		PublicKey publicKey() {
			return keyPair.getPublic();
		}

		KeyStore store(String alias, String password) {
			try {
				var store = KeyStore.getInstance("PKCS12");
				store.load(null, password.toCharArray());
				store.setKeyEntry(alias, privateKey(), password.toCharArray(), new Certificate[]{selfSignedCertificate});
				return store;
			} catch (GeneralSecurityException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

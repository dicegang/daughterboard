package scripts;

import foundation.oned6.dicegrid.server.auth.DicegridCertificateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import static scripts.Util.parseArgs;
import static scripts.Util.parseDN;

public class createLeaf {
	public static void main(String[] argv) {
		var manager = DicegridCertificateManager.create();
		var allowedArgs = Set.of("common-name", "organization", "organizational-unit", "locality", "state", "country", "ca-password", "leaf-password");
		var shorthand = Map.of(
			'n', "common-name",
			'o', "organization",
			'u', "organizational-unit",
			'l', "locality",
			's', "state",
			'c', "country",
			'C', "ca-password",
			'L', "leaf-password"
		);

		try {
			var args = parseArgs(2, allowedArgs, shorthand, argv);

			var unnamedArgs = args.getKey();
			var namedArgs = args.getValue();

			var details = parseDN(namedArgs);
			var caPassword = namedArgs.get("ca-password");
			var leafPassword = namedArgs.get("leaf-password");

			if (caPassword == null || leafPassword == null || unnamedArgs.length < 2)
				throw new IllegalArgumentException("Missing required arguments");

			var caPath = Path.of(unnamedArgs[0]);
			var leafPath = Path.of(unnamedArgs[1]);

			KeyStore ca;
			try (var fis = Files.newInputStream(caPath)) {
				ca = KeyStore.getInstance("PKCS12");
				ca.load(fis, caPassword.toCharArray());
			}

			var keyStore = manager.deriveLeaf(details, ca, caPassword, "ca", leafPassword, "leaf");
			Files.write(leafPath, manager.asLegacyP12(keyStore, leafPassword, "leaf", leafPassword));
		} catch (IOException | IllegalArgumentException e) {
			System.err.println(e.getMessage());
			usage(ProcessHandle.current().info().commandLine().orElse("CreateLeaf"));
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
	}


	static void usage(String command) {
		System.err.printf("""
			Create a leaf certificate from a CA certificate
			Usage: %s [options] -C <ca password> -L <leaf password> <ca.p12> <leaf.p12>
			Options:
				-n, --common-name=<name>
				-o, --organization=<name
				-u, --organizational-unit=<name>
				-l, --locality=<name>
				-s, --state=<name>
				-c, --country=<name>
				-C, --ca-password=<password>
				-L, --leaf-password=<password>
			""", command);
	}


}
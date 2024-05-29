package scripts;

import foundation.oned6.dicegrid.server.auth.DicegridCertificateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;

import static scripts.Util.parseArgs;

public class createCA {
	public static void main(String[] argv) {
		var manager = DicegridCertificateManager.create();
		var allowedArgs = Set.of("common-name", "organization", "organizational-unit", "locality", "state", "country", "root-password", "ca-password");
		var shorthand = Map.of(
			'n', "common-name",
			'o', "organization",
			'u', "organizational-unit",
			'l', "locality",
			's', "state",
			'c', "country",
			'R', "root-password",
			'C', "ca-password"
		);

		try {
			var args = parseArgs(2, allowedArgs, shorthand, argv);

			var unnamedArgs = args.getKey();
			var namedArgs = args.getValue();

			var details = Util.parseDN(namedArgs);
			var rootPassword = namedArgs.get("root-password");
			var caPassword = namedArgs.get("ca-password");

			if (rootPassword == null || caPassword == null || unnamedArgs.length < 2)
				throw new IllegalArgumentException("Missing required arguments");

			var rootPath = Path.of(unnamedArgs[0]);
			var caPath = Path.of(unnamedArgs[1]);

			KeyStore root;
			try (var fis = Files.newInputStream(rootPath)) {
				root = KeyStore.getInstance("PKCS12");
				root.load(fis, rootPassword.toCharArray());
			}

			var keyStore = manager.deriveCA(details, root, rootPassword, caPassword);

			Files.write(caPath, manager.asLegacyP12(keyStore, caPassword, "ca", caPassword));
		} catch (IOException | IllegalArgumentException e) {
			System.err.println(e.getMessage());
			usage(ProcessHandle.current().info().commandLine().orElse("CreateCA"));
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	// options:
// [--common-name=<name>]
// [--organization=<name>]
// [--organizational-unit=<name>]
// [--locality=<name>]
// [--state=<name>]
// [--country=<name>]
// --root-password=<password>
// --ca-password=<password>
// [file.p12]

	static void usage(String command) {
		System.err.printf("""
			Create a CA and sign it with a root certificate authority.
			Usage: %s [options] -R <root-password> -C <ca-password> <root.p12> <ca.p12>
			Options:
				-n, --common-name=<name>
				-o, --organization=<name
				-u, --organizational-unit=<name>
				-l, --locality=<name>
				-s, --state=<name>
				-c, --country=<name>
				-R, --root-password=<password>
				-C, --ca-password=<password>
			""", command);
	}


}
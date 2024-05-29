package scripts;

import foundation.oned6.dicegrid.server.auth.DicegridCertificateManager;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static scripts.Util.parseArgs;
import static scripts.Util.parseDN;

public class createRoot {
	public static void main(String[] argv) {
		var manager = DicegridCertificateManager.create();
		var allowedArgs = Set.of("common-name", "organization", "organizational-unit", "locality", "state", "country", "root-password");
		var shorthand = Map.of(
			'n', "common-name",
			'o', "organization",
			'u', "organizational-unit",
			'l', "locality",
			's', "state",
			'c', "country",
			'R', "root-password"
		);

		try {
			var args = parseArgs(1, allowedArgs, shorthand, argv);

			var unnamedArgs = args.getKey();
			var namedArgs = args.getValue();

			var details = parseDN(namedArgs);
			var storePassword = namedArgs.get("root-password");
			if (storePassword == null || unnamedArgs.length == 0)
				throw new IllegalArgumentException("Missing required arguments");

			var keyStore = manager.createRoot(details, storePassword);
			var keyStorePath = Path.of(unnamedArgs[0]);

			Files.write(keyStorePath, manager.asLegacyP12(keyStore, storePassword, "root", storePassword));
		} catch (IOException | IllegalArgumentException e) {
			System.err.println(e.getMessage());
			usage(ProcessHandle.current().info().commandLine().orElse("CreateRoot"));
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
// [file.jks]

	static void usage(String command) {
		System.err.printf("""
			Create a root certificate authority.
			Usage: %s [options] -R <password> <file.p12>
			Options:
				-n, --common-name=<name>
				-o, --organization=<name
				-u, --organizational-unit=<name>
				-l, --locality=<name>
				-s, --state=<name>
				-c, --country=<name>
				-R, --root-password=<password>%n""", command);
	}


}
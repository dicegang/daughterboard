package scripts;

import javax.security.auth.x500.X500Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {
	public static Map.Entry<String[], Map<String, String>> parseArgs(int numUnnamedArgs, Set<String> allowedArgs, Map<Character, String> shorthand, String[] argv) throws IllegalArgumentException {
		var args = new HashMap<String, String>();
		var unnamedArgs = 0;
		var unnamedArgValues = new String[numUnnamedArgs];

		for (var i = 0; i < argv.length; i++) {
			var arg = argv[i];
			if (arg.startsWith("--")) {
				var parts = arg.split("=", 2);
				if (parts.length != 2) {
					throw new IllegalArgumentException("Invalid argument: " + arg);
				}
				var key = parts[0].substring(2);
				if (!allowedArgs.contains(key)) {
					throw new IllegalArgumentException("Unknown argument: " + key);
				}
				args.put(key, parts[1]);
			} else if (arg.startsWith("-")) {
				var key = shorthand.get(arg.charAt(1));
				if (key == null) {
					throw new IllegalArgumentException("Unknown argument: " + arg);
				}
				if (!allowedArgs.contains(key)) {
					throw new IllegalArgumentException("Unknown argument: " + key);
				}
				if (i + 1 >= argv.length) {
					throw new IllegalArgumentException("Missing argument: " + arg);
				}

				args.put(key, argv[++i]);
			} else if (unnamedArgs < numUnnamedArgs) {
				unnamedArgValues[unnamedArgs++] = arg;
			} else {
				throw new IllegalArgumentException("Too many arguments");
			}
		}

		return Map.entry(unnamedArgValues, args);
	}

	static X500Principal parseDN(Map<String, String> namedArgs) {
		Map<String, String> fieldArgs = new LinkedHashMap<>();
		fieldArgs.put("common-name", "CN");
		fieldArgs.put("organization", "O");
		fieldArgs.put("organizational-unit", "OU");
		fieldArgs.put("locality", "L");
		fieldArgs.put("state", "ST");
		fieldArgs.put("country", "C");

		var dn = fieldArgs.entrySet().stream()
			.filter(e -> namedArgs.containsKey(e.getKey()))
			.map(e -> e.getValue() + "=" + namedArgs.get(e.getKey()))
			.collect(Collectors.joining(", "));
		var details = new X500Principal(dn);
		return details;
	}
}

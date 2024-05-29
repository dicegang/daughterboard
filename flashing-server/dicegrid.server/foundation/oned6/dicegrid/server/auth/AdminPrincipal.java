package foundation.oned6.dicegrid.server.auth;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;

public record AdminPrincipal() implements Principal {
	public static final Subject ADMIN = new Subject(true, Set.of(new AdminPrincipal()), Set.of(), Set.of());

	@Override
	public String getName() {
		return "Administrator";
	}
}

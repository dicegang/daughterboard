package foundation.oned6.dicegrid.server.auth;

import java.security.Principal;

public record TeamPrincipal(String teamName, int teamID) implements GridPrincipal {
	@Override
	public String getName() {
		return teamName;
	}
}

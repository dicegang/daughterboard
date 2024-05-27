package foundation.oned6.dicegrid.server;

import java.security.Principal;

public record TeamPrincipal(String teamName, int teamID) implements Principal {
	@Override
	public String getName() {
		return teamName;
	}
}

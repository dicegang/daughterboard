package foundation.oned6.dicegrid.server.admin;

import foundation.oned6.dicegrid.server.auth.TeamPrincipal;

import java.util.List;

public record AdminView(List<TeamPrincipal> teams) {
}

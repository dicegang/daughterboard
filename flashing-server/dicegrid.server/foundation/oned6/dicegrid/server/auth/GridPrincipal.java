package foundation.oned6.dicegrid.server.auth;

import java.security.Principal;

public sealed interface GridPrincipal extends Principal permits AdminPrincipal, TeamPrincipal {
}

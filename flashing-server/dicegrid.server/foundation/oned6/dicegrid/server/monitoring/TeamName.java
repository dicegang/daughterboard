package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.view.View;

import static foundation.oned6.dicegrid.server.Server.context;

public record TeamName(TeamPrincipal team) implements View {
	@Override
	public String html() {
		var url = context().contentRoot().resolve("team?team=" + team.teamID());
		return "<a href=\"%s\">%s</a>".formatted(url, team.teamName());
	}

	@Override
	public String title() {
		return team.teamName();
	}

	public static TeamName of(TeamPrincipal team) {
		return new TeamName(team);
	}
}

package foundation.oned6.dicegrid.server.admin;

import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.view.View;

import java.util.List;
import java.util.Optional;

import static foundation.oned6.dicegrid.server.Server.context;

public record MasqueradeWrapperView(Optional<TeamPrincipal> currentMasqueradingTeam, List<TeamPrincipal> availableTeams, View inner) implements View {
	@Override
	public String html() {
		if (context().requestHeader("HX-Request").isPresent())
			return inner.html();

		var options = availableTeams.stream()
				.map(team -> """
						<option value="%d" %s>%s</option>
						""".formatted(team.teamID(), currentMasqueradingTeam.map(n -> n.equals(team)).orElse(false) ? "selected" : "", team.teamName()))
				.reduce("", String::concat);

		var masqueradingText = currentMasqueradingTeam.map(team -> STR."<strong>Masquerading as \{team.getName()}</strong>").orElse("No active masquerade");
		return """
			<form method="post" action="/masquerade">
				<label for="teamID">Masquerade as:</label>
				<select name="teamID">
					<option value="-1">None</option>
					%s
				</select>
				<button type="submit">Configure</button>
			</form>
			%s
			<div style="border: 1px solid black; padding: 1em;">
				%s
			</div>
			""".formatted(options, masqueradingText, inner.html());
	}

	@Override
	public String title() {
		return inner.title();
	}
}

package foundation.oned6.dicegrid.server.view;

import java.net.URI;

import static foundation.oned6.dicegrid.server.Server.context;

public record UpdateView(String accept, String uploadLabel, String actionVerb, URI postURI) implements View {
	@Override
	public String generateHTML() {
		return """
			<div>
				<style>
					@scope {
						form {
							display: grid;
							grid-template-columns: repeat(2, 1fr);
						}
		
						button {
							margin-top: 1em;
							grid-column: span 2;
							justify-self: center;
							width: fit-content;
						}
					}
				</style>
			
				<form hx-post="%s" hx-target="previous .status" enctype="multipart/form-data">
					<label for="target">Target Device</label>
					<select name="target" required>
						<option value="Source">Source</option>
						<option value="Load">Load</option>
					</select>
		
					<label for="file">%s</label>
					<input type="file" name="file" accept="%s" required>
					<button type="submit">%s</button>
				</form>
			</div>
			""".formatted(context().contentRoot().resolve(postURI), uploadLabel, accept, actionVerb);
	}

	@Override
	public String title() {
		return "Update schematic";
	}
}

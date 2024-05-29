package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.server.view.View;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static foundation.oned6.dicegrid.server.view.Views.text;

public record Legend(View caption, List<Map.Entry<View, View>> items) implements View {
	@Override
	public String html() {
		var rows = items.stream()
			.map(e -> row(e.getKey(), e.getValue()))
			.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
			.toString();
		return """
			<div>
				<style>
					@scope {
						details:first-of-type > summary {
							list-style-type: 'ⓘ ';
						}
					
						details:first-of-type > table {
							display: none;
						}
			
						details:first-of-type:has(> summary:hover) > table {
							display: revert;
						}
					}
				</style>
				<details class="legend" open>
					<summary>%s</summary>
					<table>
						<tbody>
							%s
						</tbody>
					</table>
				</details>
			</div>
			""".formatted(caption.html(), rows);
	}

	private static String row(View key, View value) {
		return "<tr><th scope=\"row\">%s</th><td>%s</td></tr>".formatted(key.html(), value.html());
	}

	@Override
	public String title() {
		return "Legend";
	}

	public static Legend of(String caption, Map.Entry<String, String>... items) {
		return new Legend(text(caption), Arrays.stream(items)
			.map(e -> Map.entry(text(e.getKey()), text(e.getValue())))
			.toList());
	}
}

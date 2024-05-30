package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.server.view.View;

import static foundation.oned6.dicegrid.server.Server.context;

public record MonitoringView(NodeDataTable table) implements View {
	@Override
	public String html() {
		return """
			<div class="status" style="justify-self: center;"></div>
			<div hx-get="%s" hx-trigger="every 1s">%s</div>
			""".formatted(context().contentRoot().resolve("monitoring"), table.html());
	}

	@Override
	public String title() {
		return "Monitoring";
	}
}

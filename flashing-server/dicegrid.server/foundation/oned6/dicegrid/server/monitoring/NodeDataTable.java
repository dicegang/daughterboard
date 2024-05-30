package foundation.oned6.dicegrid.server.monitoring;

import foundation.oned6.dicegrid.protocol.NodeInfo;
import foundation.oned6.dicegrid.protocol.NodeState;
import foundation.oned6.dicegrid.server.auth.TeamPrincipal;
import foundation.oned6.dicegrid.server.view.View;

import java.util.*;
import java.util.stream.Collectors;

public record NodeDataTable(List<NodeDataEntry> entries) implements View {
	@Override
	public String html() {
		var header = NodeDataEntry.headers().stream()
			.collect(Collectors.joining("\n", "<thead><tr>", "</tr></thead>"));
		var body = entries.stream().sorted(Comparator.comparing(n -> n.teamName().title()))
			.map(NodeDataEntry::html)
			.collect(Collectors.joining("\n", "<tbody>", "</tbody>"));

		return """
			<div>
				<style>
					@scope  {
						.node-data-table > tbody > tr > td, .node-data-table > thead > tr > th {
							border: 1px solid black;
							padding: 5px;
						}
						
						.node-data-table > thead {
							background-color: lightblue;
						}
			
						div.phasor-info .legend > table > tbody {
							tr:nth-child(2) {
								background-color: lightcoral;
							}
				
							tr:nth-child(3) {
								background-color: lightgray;	
							}
						}
					}
				</style>
				<form style="display: none" id="node-controls" hx-post="/control" hx-target="previous .status" hx-target-*="previous .status"></form>
				
				<table class="node-data-table" style="table-layout: auto; border: 1px solid black; border-collapse: collapse;">
					<caption>Source/load info and measurements</caption>
					%s
					%s
				</table>
			</div>""".formatted(header, body);
	}

	@Override
	public String title() {
		return "Node Data";
	}

	public static NodeDataTable of(Collection<NodeInfo> controllableNodes, Map<NodeInfo, TeamPrincipal> nodeOwners, Map<NodeInfo, NodeState> nodeStates) {
		assert nodeOwners.keySet().equals(nodeStates.keySet());

		var entries = new ArrayList<NodeDataEntry>();
		for (var node : nodeStates.keySet()) {
			var state = nodeStates.get(node);
			entries.add(NodeDataEntry.of(nodeOwners.get(node), node.nodeType(), state, controllableNodes.contains(node)));
		}

		return new NodeDataTable(entries);
	}
}

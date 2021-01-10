package de.tum.ei.lkn.eces.webgraphgui.color;

import de.tum.ei.lkn.eces.graph.Edge;

/**
 * All edges are black.
 *
 * @author Florian Kreft
 * @author Amaury Van Bemten
 */
public class BlackScheme implements ColoringScheme {
	@Override
	public String getColor(Edge e) {
		return RGBColor.gray();
	}

	public boolean isPolling(Edge e) {
		return true;
	}
}

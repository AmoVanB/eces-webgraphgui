package de.tum.ei.lkn.eces.routing.pathlist;

import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Class coloring the last embedded flow.
 *
 * @author Jochen Guck
 * @author Amaury Van Bemten
 */
public class LastEmbeddingColoring implements ColoringScheme {
	private PathListSystem pathListSystem;

	public LastEmbeddingColoring(PathListSystem pathListSystem) {
		this.pathListSystem = pathListSystem;
	}

	public String getColor(Edge edge) {
		if(pathListSystem.getLastEmbeddedEdges().contains(edge))
			return RGBColor.percentToColor(1);
		else
			return RGBColor.gray();
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

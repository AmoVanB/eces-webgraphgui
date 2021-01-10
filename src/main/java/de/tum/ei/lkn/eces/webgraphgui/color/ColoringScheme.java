package de.tum.ei.lkn.eces.webgraphgui.color;

import de.tum.ei.lkn.eces.graph.Edge;

/**
 * A coloring scheme for edges.
 *
 * @author Florian Kreft
 */
public interface ColoringScheme {
	/**
	 * Gets the color of an edge.
	 * @param edge Given Edge.
	 * @return Color of the form "rgb(R, G, B)" until 255
	 */
	String getColor(Edge edge);

	/**
	 * Whether the server should poll for updates
	 * @param edge Given Edge.
	 * @return boolean
	 */
	boolean isPolling(Edge edge);
}

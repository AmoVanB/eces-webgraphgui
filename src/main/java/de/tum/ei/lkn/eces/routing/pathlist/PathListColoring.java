package de.tum.ei.lkn.eces.routing.pathlist;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.routing.mappers.PathListMapper;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Class coloring an Edge based on the number of Paths (i.e. flows) going
 * through this Edge.
 *
 * @author Jochen Guck
 * @author Amaury Van Bemten
 */
public class PathListColoring implements ColoringScheme {
	/**
	 * Mapper handling PathList Components.
	 */
	private PathListMapper pathListMapper;

	/**
	 * The color of the Edge will depend on the ratio: current number of paths
	 * to 'maxValue'.
	 */
	private int maxValue;

	/**
	 * Creates a new PathListColoring.
	 * @param controller Controller responsible for handling this scheme.
	 * @param maxValue maxValue to be used by the coloring class.
	 */
	public PathListColoring (Controller controller, int maxValue) {
		this.pathListMapper = new PathListMapper(controller);
		this.maxValue = maxValue;
	}

	/**
	 * Creates a new PathListColoring. The default maxValue (1000) will be
	 * used.
	 * @param controller Controller responsible for handling this scheme.
	 */
	public PathListColoring (Controller controller) {
		this(controller, 0);
	}

	@Override
	public String getColor(Edge edge) {
		if(pathListMapper.isIn(edge.getEntity())) {
			int size = pathListMapper.get(edge.getEntity()).getPathList().size();
			updateMaxValue(size);
			return RGBColor.percentToColor(((double)size)/(double) maxValue);
		}
		return RGBColor.gray();
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}

	private void updateMaxValue(int value) {
		maxValue = (value > maxValue) ? value : maxValue;
	}
}

package de.tum.ei.lkn.eces.network.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.Mapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.network.Delay;
import de.tum.ei.lkn.eces.network.Link;
import de.tum.ei.lkn.eces.network.ToNetwork;
import de.tum.ei.lkn.eces.network.mappers.DelayMapper;
import de.tum.ei.lkn.eces.network.mappers.LinkMapper;
import de.tum.ei.lkn.eces.network.mappers.ToNetworkMapper;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Class coloring an Edge based on the Delay of the Edge.
 *
 * @author Florian Kreft
 * @author Jochen Guck
 * @author Amaury Van Bemten
 */
public class DelayColoring implements ColoringScheme {
	/**
	 * Mapper handling the Delay Components.
	 */
	private Mapper<Delay> delayMapper;
	private Mapper<ToNetwork> toNetworkMapper;
	private Mapper<Link> linkMapper;
	private double maxDelay;

	public DelayColoring(Controller controller) {
		delayMapper = new DelayMapper(controller);
		toNetworkMapper = new ToNetworkMapper(controller);
		linkMapper = new LinkMapper(controller);
		maxDelay = 0;
	}

	public String getColor(Edge edge) {
		Delay delay = delayMapper.get(edge.getEntity());
		if(delay != null) {
			updateMaxDelay(delay.getDelay());
			return RGBColor.percentToColor(delay.getDelay()/maxDelay);
		}

		return RGBColor.gray();
	}

	private void updateMaxDelay(double delay) {
		maxDelay = (delay > maxDelay) ? delay : maxDelay;
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

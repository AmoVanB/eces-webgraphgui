package de.tum.ei.lkn.eces.network.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.Mapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.network.Link;
import de.tum.ei.lkn.eces.network.Rate;
import de.tum.ei.lkn.eces.network.ToNetwork;
import de.tum.ei.lkn.eces.network.mappers.LinkMapper;
import de.tum.ei.lkn.eces.network.mappers.RateMapper;
import de.tum.ei.lkn.eces.network.mappers.ToNetworkMapper;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Class coloring an Edge based on the Rate of the Edge.
 *
 * @author Florian Kreft
 * @author Jochen Guck
 * @author Amaury Van Bemten
 */
public class RateColoring implements ColoringScheme {
	/**
	 * Mapper handling the Rate Components.
	 */
	private Mapper<Rate> rateMapper;
	private Mapper<ToNetwork> toNetworkMapper;
	private Mapper<Link> linkMapper;
	private double maxRate;

	public RateColoring(Controller controller) {
		rateMapper = new RateMapper(controller);
		toNetworkMapper = new ToNetworkMapper(controller);
		linkMapper = new LinkMapper(controller);
		maxRate = 0;
	}

	public String getColor(Edge edge) {
		Rate rate = rateMapper.get(edge.getEntity());
		if(rate != null) {
			updateMaxRate(rate.getRate());
			return RGBColor.percentToColor(1 - rate.getRate()/maxRate);
		}
		else {
			// If edge is queue-link, get rate of corresponding link.
			if(toNetworkMapper.isIn(edge.getEntity())) {
				Entity networkEntity = toNetworkMapper.get(edge.getEntity()).getNetworkEntity();
				Link link = linkMapper.get(networkEntity);
				if(link != null) {
					rate = rateMapper.get(link.getLinkEdge().getEntity());
					if(rate != null) {
						updateMaxRate(rate.getRate());
						return RGBColor.percentToColor(1 - rate.getRate()/maxRate);
					}
				}
			}
		}

		return RGBColor.gray();
	}

	private void updateMaxRate(double rate) {
		maxRate = (rate > maxRate) ? rate : maxRate;
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

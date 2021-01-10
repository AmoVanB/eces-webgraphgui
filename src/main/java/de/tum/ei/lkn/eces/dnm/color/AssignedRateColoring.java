package de.tum.ei.lkn.eces.dnm.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.mappers.MHMQueueModelMapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Color an edge based on its assigned rate (MHM).
 *
 * @author Amaury Van Bemten
 */
public class AssignedRateColoring implements ColoringScheme {
	private Controller controller;
	private double maxRate;

	public AssignedRateColoring(Controller controller) {
		this.controller = controller;
		maxRate = 0;
	}

	public String getColor(Edge edge) {
		double value;
		try {
			value = getAvailableRate(edge);
		} catch (RuntimeException e) {
			return RGBColor.gray();
		}

		updateMaxAvailableRate(value);
		return RGBColor.percentToColor(1 - value/maxRate);
	}

	public double getAvailableRate(Edge edge) {
		if(new MHMQueueModelMapper(controller).isIn(edge.getEntity()))
			return new MHMQueueModelMapper(controller).get(edge.getEntity()).getMaximumTokenBucket().getUltAffineRate().doubleValue();

		throw new RuntimeException("Not implemented");
	}

	private void updateMaxAvailableRate(double rate) {
		maxRate = (rate > maxRate) ? rate : maxRate;
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

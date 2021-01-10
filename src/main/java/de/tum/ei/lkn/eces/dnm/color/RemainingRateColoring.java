package de.tum.ei.lkn.eces.dnm.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.mappers.TokenBucketUtilizationMapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Color an edge based on its remaining rate (MHM).
 *
 * @author Amaury Van Bemten
 */
public class RemainingRateColoring implements ColoringScheme {
	private Controller controller;
	private AssignedRateColoring assignedRateColoring;

	public RemainingRateColoring(Controller controller) {
		this.controller = controller;
		assignedRateColoring = new AssignedRateColoring(controller);
	}

	public String getColor(Edge edge) {
		try {
			return RGBColor.percentToColor(getUsedRate(edge) / assignedRateColoring.getAvailableRate(edge));
		} catch (RuntimeException e) {
			return RGBColor.gray();
		}
	}

	private double getUsedRate(Edge edge) {
		if(new TokenBucketUtilizationMapper(controller).isIn(edge.getEntity()))
			return new TokenBucketUtilizationMapper(controller).get(edge.getEntity()).getRate().doubleValue();

		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

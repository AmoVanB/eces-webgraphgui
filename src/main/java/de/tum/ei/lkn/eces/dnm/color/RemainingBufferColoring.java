package de.tum.ei.lkn.eces.dnm.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.mappers.TokenBucketUtilizationMapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Color an edge based on its remaining buffer space (MHM).
 *
 * @author Amaury Van Bemten
 */
public class RemainingBufferColoring implements ColoringScheme {
	private Controller controller;
	private AssignedBufferColoring assignedBufferColoring;

	public RemainingBufferColoring(Controller controller) {
		this.controller = controller;
		assignedBufferColoring = new AssignedBufferColoring(controller);
	}

	public String getColor(Edge edge) {
		try {
			return RGBColor.percentToColor(getUsedBurst(edge) / assignedBufferColoring.getAvailableBuffer(edge));
		} catch (RuntimeException e) {
			return RGBColor.gray();
		}
	}

	private double getUsedBurst(Edge edge) {
		if(new TokenBucketUtilizationMapper(controller).isIn(edge.getEntity()))
			return new TokenBucketUtilizationMapper(controller).get(edge.getEntity()).getBurst().doubleValue();

		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

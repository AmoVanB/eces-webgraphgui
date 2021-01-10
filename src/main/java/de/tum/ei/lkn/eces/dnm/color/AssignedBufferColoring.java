package de.tum.ei.lkn.eces.dnm.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.mappers.MHMQueueModelMapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Color an edge based on its assigned buffer (MHM).
 *
 * @author Amaury Van Bemten
 */
public class AssignedBufferColoring implements ColoringScheme {
	private double maxBurst;
	private MHMQueueModelMapper mhmQueueModelMapper;

	public AssignedBufferColoring(Controller controller) {
		maxBurst = 0;
		mhmQueueModelMapper = new MHMQueueModelMapper(controller);
	}

	public String getColor(Edge edge) {
		double value;
		try {
			value = getAvailableBuffer(edge);
		} catch (RuntimeException e) {
			return RGBColor.gray();
		}

		updateMaxAvailableBuffer(value);
		return RGBColor.percentToColor(1 - value/maxBurst);
	}

	public double getAvailableBuffer(Edge edge) {
		if(mhmQueueModelMapper.isIn(edge.getEntity()))
			return mhmQueueModelMapper.get(edge.getEntity()).getMaximumTokenBucket().getBurst().doubleValue();

		throw new RuntimeException("Not implemented");
	}

	private void updateMaxAvailableBuffer(double burst) {
		maxBurst = (burst > maxBurst) ? burst : maxBurst;
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

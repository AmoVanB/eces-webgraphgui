package de.tum.ei.lkn.eces.network.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.Mapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.network.Queue;
import de.tum.ei.lkn.eces.network.mappers.QueueMapper;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;

/**
 * Class coloring an Edge based on the queue size.
 *
 * @author Florian Kreft
 * @author Jochen Guck
 * @author Amaury Van Bemten
 */
public class QueueColoring implements ColoringScheme {
	/**
	 * Mapper handling the Queue Components.
	 */
	private Mapper<Queue> queueMapper;
	private double maxSize;

	public QueueColoring(Controller controller) {
		queueMapper = new QueueMapper(controller);
		maxSize = 0;
	}

	public String getColor(Edge edge) {
		Queue queue = queueMapper.get(edge.getEntity());
		if(queue != null) {
			updateMaxSize(queue.getSize());
			return RGBColor.percentToColor(1 - queue.getSize()/maxSize);
		}

		return RGBColor.gray();
	}

	private void updateMaxSize(double size) {
		maxSize = (size > maxSize) ? size : maxSize;
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

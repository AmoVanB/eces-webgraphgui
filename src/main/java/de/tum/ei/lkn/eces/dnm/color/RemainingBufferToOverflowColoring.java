package de.tum.ei.lkn.eces.dnm.color;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.mappers.QueueModelMapper;
import de.tum.ei.lkn.eces.dnm.mappers.TokenBucketUtilizationMapper;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.network.mappers.QueueMapper;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.RGBColor;
import de.uni_kl.cs.discodnc.curves.ArrivalCurve;
import de.uni_kl.cs.discodnc.curves.CurvePwAffine;
import de.uni_kl.cs.discodnc.curves.ServiceCurve;
import de.uni_kl.cs.discodnc.nc.bounds.Bound;

/**
 * Color an edge based on its remaining buffer space until overflow (MHM and TBM).
 *
 * For MHM, it is more than normal remaining buffer: it assumes the assigned buffer is the buffer capacity.
 *
 * @author Amaury Van Bemten
 */
public class RemainingBufferToOverflowColoring implements ColoringScheme {
	private Controller controller;

	public RemainingBufferToOverflowColoring(Controller controller) {
		this.controller = controller;
	}

	public String getColor(Edge edge) {
		double queueSize = new QueueMapper(controller).get(edge.getEntity()).getSize();
		if(new TokenBucketUtilizationMapper(controller).isIn(edge.getEntity()) && new QueueModelMapper(controller).isIn(edge.getEntity())) {
			ArrivalCurve tb = CurvePwAffine.getFactory().createTokenBucket(new TokenBucketUtilizationMapper(controller).get(edge.getEntity()).getRate(),
					new TokenBucketUtilizationMapper(controller).get(edge.getEntity()).getBurst());
			ServiceCurve curve = new QueueModelMapper(controller).get(edge.getEntity()).getServiceCurve();
			return RGBColor.percentToColor(Bound.backlog(tb, curve).doubleValue()/queueSize);
		}
		else {
			return RGBColor.gray();
		}
	}

	@Override
	public boolean isPolling(Edge edge) {
		return true;
	}
}

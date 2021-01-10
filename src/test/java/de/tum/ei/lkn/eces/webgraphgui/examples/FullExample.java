package de.tum.ei.lkn.eces.webgraphgui.examples;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.MapperSpace;
import de.tum.ei.lkn.eces.dnm.ResidualMode;
import de.tum.ei.lkn.eces.dnm.proxies.DetServProxy;
import de.tum.ei.lkn.eces.dnm.DNMSystem;
import de.tum.ei.lkn.eces.dnm.color.*;
import de.tum.ei.lkn.eces.dnm.config.DetServConfig;
import de.tum.ei.lkn.eces.dnm.NCRequestData;
import de.tum.ei.lkn.eces.dnm.mappers.DetServConfigMapper;
import de.tum.ei.lkn.eces.dnm.mappers.NCRequestDataMapper;
import de.tum.ei.lkn.eces.dnm.config.ACModel;
import de.tum.ei.lkn.eces.dnm.config.BurstIncreaseModel;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Division;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.LowerLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Summation;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.UpperLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.Constant;
import de.tum.ei.lkn.eces.dnm.resourcemanagement.resourceallocation.TBM.TBMDelayRatiosAllocation;
import de.tum.ei.lkn.eces.network.Network;
import de.tum.ei.lkn.eces.network.NetworkNode;
import de.tum.ei.lkn.eces.network.NetworkingSystem;
import de.tum.ei.lkn.eces.network.color.DelayColoring;
import de.tum.ei.lkn.eces.network.color.QueueColoring;
import de.tum.ei.lkn.eces.network.color.RateColoring;
import de.tum.ei.lkn.eces.routing.RoutingSystem;
import de.tum.ei.lkn.eces.routing.algorithms.csp.unicast.larac.LARACAlgorithm;
import de.tum.ei.lkn.eces.routing.pathlist.LastEmbeddingColoring;
import de.tum.ei.lkn.eces.routing.pathlist.PathListColoring;
import de.tum.ei.lkn.eces.routing.pathlist.PathListSystem;
import de.tum.ei.lkn.eces.webgraphgui.WebGraphGuiSystem;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringSystem;

public class FullExample {
	public static void main(String[] args) throws InterruptedException {
		// ECES, Network and DNM inits
		Controller controller = new Controller();
		NetworkingSystem myNetworkingSystem = new NetworkingSystem(controller);
		RoutingSystem routingSystem = new RoutingSystem(controller);
		DetServConfig cfg = new DetServConfig(ACModel.TBM,
				ResidualMode.HIGHEST_SLOPE,
				BurstIncreaseModel.WORST_CASE_BURST_REAL_RESERVATION,
				false,
				new LowerLimit(new UpperLimit(
						new Summation(
								new Constant(),
								new Division(
										new Constant(), new Constant(2))),
						2), 1),
				(cont, sched) -> new TBMDelayRatiosAllocation(cont));
		new DNMSystem(controller);
		cfg.initCostModel(controller);
		LARACAlgorithm dclcAlgorithm = new LARACAlgorithm(controller);
		dclcAlgorithm.setProxy(new DetServProxy(controller));

		// GUI
		ColoringSystem myColoringSys = new ColoringSystem(controller);
		myColoringSys.addColoringScheme(new DelayColoring(controller), "Delay");
		myColoringSys.addColoringScheme(new QueueColoring(controller), "Queue sizes");
		myColoringSys.addColoringScheme(new RateColoring(controller), "Link rate");
		myColoringSys.addColoringScheme(new RemainingRateColoring(controller), "Remaining rate");
		myColoringSys.addColoringScheme(new AssignedRateColoring(controller), "Assigned rate");
		myColoringSys.addColoringScheme(new RemainingBufferColoring(controller), "Remaining buffer space");
		myColoringSys.addColoringScheme(new RemainingDelayColoring(controller), "Remaining delay");
		myColoringSys.addColoringScheme(new AssignedBufferColoring(controller), "Assigned buffer space");
		myColoringSys.addColoringScheme(new LastEmbeddingColoring(new PathListSystem(controller)), "Last embedded flow");
		myColoringSys.addColoringScheme(new PathListColoring(controller), "Amount of paths");
		new WebGraphGuiSystem(controller, myColoringSys, 8080);

		// Create network
		Network network = myNetworkingSystem.createNetwork();
		new DetServConfigMapper(controller).attachComponent(network.getQueueGraph(), cfg);
		NetworkNode node1 = myNetworkingSystem.createNode(network, "Node1");
		NetworkNode node2 = myNetworkingSystem.createNode(network, "Node2");
		NetworkNode node3 = myNetworkingSystem.createNode(network, "Node3");
		myNetworkingSystem.createLinkWithPriorityScheduling(node1, node2, 12500000, 0, new double[]{4000, 8000, 3587, 8554});
		myNetworkingSystem.createLinkWithPriorityScheduling(node2, node1, 12500000, 0, new double[]{1258, 754, 3587, 8554});
		myNetworkingSystem.createLinkWithPriorityScheduling(node2, node3, 12500000, 2, new double[]{4000, 8000, 4000, 754});
		myNetworkingSystem.createLinkWithPriorityScheduling(node3, node1, 12500000, 3, new double[]{2547, 424, 750, 1258});

		while (true)
			Thread.sleep(5000);
	}
}






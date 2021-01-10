package de.tum.ei.lkn.eces.webgraphgui.examples;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.dnm.DNMSystem;
import de.tum.ei.lkn.eces.dnm.ResidualMode;
import de.tum.ei.lkn.eces.dnm.config.ACModel;
import de.tum.ei.lkn.eces.dnm.config.BurstIncreaseModel;
import de.tum.ei.lkn.eces.dnm.config.DetServConfig;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Division;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.LowerLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.Summation;
import de.tum.ei.lkn.eces.dnm.config.costmodels.functions.UpperLimit;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.Constant;
import de.tum.ei.lkn.eces.dnm.config.costmodels.values.QueuePriority;
import de.tum.ei.lkn.eces.dnm.mappers.DetServConfigMapper;
import de.tum.ei.lkn.eces.dnm.resourcemanagement.resourceallocation.MHM.MHMRateRatiosAllocation;
import de.tum.ei.lkn.eces.graph.GraphSystem;
import de.tum.ei.lkn.eces.network.Host;
import de.tum.ei.lkn.eces.network.Network;
import de.tum.ei.lkn.eces.network.NetworkNode;
import de.tum.ei.lkn.eces.network.NetworkingSystem;
import de.tum.ei.lkn.eces.network.util.NetworkInterface;
import de.tum.ei.lkn.eces.webgraphgui.WebGraphGuiSystem;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class WithNetworkingExample {
	static NetworkingSystem myNetworkingSystem ;
	static Controller controller;
	static GraphSystem myGraphSys;
	static WebGraphGuiSystem myGUISystem;

	public static void main(String[] args) throws InterruptedException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);

		controller = new Controller();
		DetServConfigMapper modelingConfigMapper = new DetServConfigMapper(controller);
		myGraphSys = new GraphSystem(controller);
		myNetworkingSystem = new NetworkingSystem(controller, myGraphSys);
		myGUISystem = new WebGraphGuiSystem(controller, 8080);

		DetServConfig cfg = new DetServConfig(ACModel.MHM,
				ResidualMode.HIGHEST_SLOPE,
				BurstIncreaseModel.WORST_CASE_BURST_REAL_RESERVATION,
				false,
				new LowerLimit(new UpperLimit(
						new Summation(
								new Constant(),
								new Division(
										new Constant(),
										new QueuePriority())),
						2), 1),
				(cont, sched) -> new MHMRateRatiosAllocation(cont, new double[]{1.0, 0.0, 0.0}));
		new DNMSystem(controller);
		cfg.initCostModel(controller);

		Network net1 = myNetworkingSystem.createNetwork();
		modelingConfigMapper.attachComponent(net1.getQueueGraph(), cfg);

		Host host = myNetworkingSystem.createHost(net1, "hazard");
		NetworkNode node1 = myNetworkingSystem.addInterface(host, new NetworkInterface("eth0", "00:00:00:00:00:01", "125.2.2.1"));
		NetworkNode node2 = myNetworkingSystem.createNode(net1, "lukaku");
		NetworkNode node3 = myNetworkingSystem.createNode(net1, "kompany");

		myNetworkingSystem.createLink(node1, node2, 1250000, 0, 40000);
		myNetworkingSystem.createLinkWithPriorityScheduling(node2, node3, 1250000, 0, new double[]{40000, 40000, 40000});
		myNetworkingSystem.createLinkWithPriorityScheduling(node3, node2, 1250000, 0, new double[]{40000, 40000, 40000});
		myNetworkingSystem.createLink(node3, node1, 1250000, 0, 40000);


		while (true) {
			Thread.sleep(5000);
		}
	}

}






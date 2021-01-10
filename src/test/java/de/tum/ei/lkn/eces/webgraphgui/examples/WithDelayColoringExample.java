package de.tum.ei.lkn.eces.webgraphgui.examples;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.graph.GraphSystem;
import de.tum.ei.lkn.eces.network.Network;
import de.tum.ei.lkn.eces.network.NetworkNode;
import de.tum.ei.lkn.eces.network.NetworkingSystem;
import de.tum.ei.lkn.eces.network.color.DelayColoring;
import de.tum.ei.lkn.eces.webgraphgui.WebGraphGuiSystem;
import de.tum.ei.lkn.eces.webgraphgui.color.BlackScheme;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringSystem;

public class WithDelayColoringExample {
	static NetworkingSystem myNetworkingSystem ;
	static Controller controller;
	static GraphSystem myGraphSys;
	static WebGraphGuiSystem myGUISystem;
	static ColoringSystem myColoringSys;

	public static void main(String[] args) throws InterruptedException {
		controller = new Controller();
		myGraphSys = new GraphSystem(controller);
		myNetworkingSystem = new NetworkingSystem(controller, myGraphSys);
		myColoringSys= new ColoringSystem(controller);
		myGUISystem = new WebGraphGuiSystem(controller, myColoringSys, 8080);
		BlackScheme blackScheme = new BlackScheme();
		DelayColoring delayScheme = new DelayColoring(controller);
		
		myColoringSys.addColoringScheme(blackScheme, "Black");
		myColoringSys.addColoringScheme(delayScheme, "Delay");
		
		Network net1 = myNetworkingSystem.createNetwork();
		NetworkNode node1 = myNetworkingSystem.createNode(net1, "Node1");
		NetworkNode node2 = myNetworkingSystem.createNode(net1, "Node2");
		NetworkNode node3 = myNetworkingSystem.createNode(net1, "Node3");
		myNetworkingSystem.createLinkWithPriorityScheduling(node1, node2, 1250000, 0, new double[]{4, 4, 4, 4});
		myNetworkingSystem.createLinkWithPriorityScheduling(node2, node3, 1250000, 100, new double[]{4, 4, 4, 4});
		myNetworkingSystem.createLinkWithPriorityScheduling(node3, node1, 1250000, 200, new double[]{4, 4, 4, 4});
		

		while (true) {
			Thread.sleep(5000);
		}
	}
	
}






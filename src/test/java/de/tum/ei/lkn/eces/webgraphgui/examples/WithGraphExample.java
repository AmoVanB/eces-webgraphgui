package de.tum.ei.lkn.eces.webgraphgui.examples;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.graph.Graph;
import de.tum.ei.lkn.eces.graph.GraphSystem;
import de.tum.ei.lkn.eces.graph.Node;
import de.tum.ei.lkn.eces.webgraphgui.WebGraphGuiSystem;

public class WithGraphExample {

	static GraphSystem myGraphSys;
	
	static Controller controller;
	static WebGraphGuiSystem myGUISystem;

	
	public static void main(String[] args) throws InterruptedException{
		controller = new Controller();
		myGraphSys = new GraphSystem(controller);
		myGUISystem = new WebGraphGuiSystem(controller, 8080);
		
		Graph g = myGraphSys.createGraph();
		Node n1 = myGraphSys.createNode(g);
		Node n2 = myGraphSys.createNode(g);
		Node n3 = myGraphSys.createNode(g);
		Node n4 = myGraphSys.createNode(g);
		myGraphSys.createEdge(n1, n2);
		myGraphSys.createEdge(n2, n3);
		myGraphSys.createEdge(n3, n4);
		myGraphSys.createEdge(n1, n4);
		myGraphSys.createEdge(n1, n3);
		
		Graph g2 = myGraphSys.createGraph();
		Node n21 = myGraphSys.createNode(g2);
		Node n22 = myGraphSys.createNode(g2);
		myGraphSys.createEdge(n21, n22);
		Graph g3 = myGraphSys.createGraph();
		Node n31 = myGraphSys.createNode(g3);
		Node n32 = myGraphSys.createNode(g3);
		myGraphSys.createEdge(n31, n32);

		while (true) {
			Thread.sleep(2000);
		}
	}
}






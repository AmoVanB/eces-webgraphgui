package de.tum.ei.lkn.eces.webgraphgui.examples;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.graph.GraphSystem;
import de.tum.ei.lkn.eces.network.NetworkingSystem;
import de.tum.ei.lkn.eces.webgraphgui.WebGraphGuiSystem;

import java.io.File;

public class Test {
    static NetworkingSystem myNetworkingSystem ;
    static Controller controller;
    static GraphSystem myGraphSys;
    static WebGraphGuiSystem myGUISystem;

    public static void main(String[] args) throws InterruptedException {
        controller = new Controller();
        myGraphSys = new GraphSystem(controller);
        myNetworkingSystem = new NetworkingSystem(controller, myGraphSys);
        myGUISystem = new WebGraphGuiSystem(controller, 8080);
        Object o = new Object();
        File file = new File(o.getClass().getClassLoader().getResource("index.html").getFile());
        System.out.println(file.exists());
        System.out.println(file.isHidden());
        System.out.println(file.isDirectory());
    }
}

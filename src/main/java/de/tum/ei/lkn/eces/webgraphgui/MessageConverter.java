//package org.tum.ei.eces.webgraphgui;
package de.tum.ei.lkn.eces.webgraphgui;

import org.json.JSONObject;

import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.graph.Node;
/**
 * Class which provides methods for encoding and decoding messages for use with websocket.
 *
 * Currently incredibly ugly. We'll clean if we have time, but not very important as it is just the GUI.
 *
 * @author Florian Kreft
 */
public class MessageConverter {
	public static String getNodeMessage(Node realNode, long graphID, String type) {
		JSONObject obj = new JSONObject();
		obj.put("type", "Node");
		//obj.put("id", realNode.getIdentifier());
		obj.put("entityId", realNode.getWaitEntity().getId());
		obj.put("graph", graphID);
		obj.put("status", "default");
        obj.put("device", type);
        return  obj.toString();
	}
	
	public static String getNodeMessage(Node realNode, long graphID, String newStatus, String type) {
		JSONObject obj = new JSONObject();
		obj.put("type", "Node");
		//obj.put("id", realNode.getIdentifier());
		obj.put("entityId", realNode.getWaitEntity().getId());
		obj.put("graph", graphID);
		obj.put("status", newStatus);
		obj.put("device", type);
		return  obj.toString();
	}
	
	public static String getEdgeMessage(Edge realEdge, long graphID) {
		JSONObject obj = new JSONObject();
		obj.put("type", "Edge");
		obj.put("sourceEntityId", realEdge.getSource().getWaitEntity().getId());
		obj.put("destEntityId", realEdge.getDestination().getWaitEntity().getId());
		obj.put("entityId", realEdge.getWaitEntity().getId());
		obj.put("graph", graphID);
		obj.put("status", "default");
		return  obj.toString();
	}

	public static String getEdgeMessage(Edge realEdge, long graphID, String newStatus) {
		JSONObject obj = new JSONObject();
		obj.put("type", "Edge");
		obj.put("sourceEntityId", realEdge.getSource().getWaitEntity().getId());
		obj.put("destEntityId", realEdge.getDestination().getWaitEntity().getId());
		obj.put("entityId", realEdge.getWaitEntity().getId());
		obj.put("graph", graphID);
		obj.put("status", newStatus);
		return  obj.toString();
	}
	
	public static String getColorMessage(Edge realEdge, String newColor) {
		JSONObject obj = new JSONObject();
		obj.put("type", "Color");
		obj.put("entityId", realEdge.getWaitEntity().getId());
		obj.put("color", newColor);
		return  obj.toString();
	}
	
	public static String getColorSchemeMessage(String schemeName) {
		JSONObject obj = new JSONObject();
		obj.put("type", "ColoringScheme");
		obj.put("name", schemeName);
		return  obj.toString();
	}
	
	public static String getErrorMessage(String errorTypeString, String errorString) {
		JSONObject obj = new JSONObject();
		obj.put("type", "Error");
		obj.put("errorMsg", errorString);
		obj.put("errorType", errorTypeString);
		return  obj.toString();
	}
	
}
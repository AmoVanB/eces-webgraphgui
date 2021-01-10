package de.tum.ei.lkn.eces.webgraphgui.color;

import java.util.HashMap;
import java.util.Map;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.RootSystem;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.graph.Edge;

/**
 * Coloring system.
 *
 * Currently incredibly ugly. We'll clean if we have time, but not very important as it is just the GUI.
 *
 * @author Florian Kreft
 * @author Jochen Guck
 */
@ComponentBelongsTo(system = ColoringSystem.class)
public class ColoringSystem extends RootSystem {
	private HashMap<String, ColoringScheme> regColoringMap = new HashMap<String, ColoringScheme>();
	
	public ColoringSystem(Controller controller) {
		super(controller);
	}
	
    public boolean addColoringScheme(ColoringScheme newSch, String name) {
    	if (!regColoringMap.containsValue(newSch) && !regColoringMap.containsKey(name)) {
    		regColoringMap.put(name, newSch);
    		return false;
    	}
    	return true;
    }
    
    public String[] getColoringSchemeList() {
    	//TODO: create directly usable JSON-Object, most likely in MessageConverter
    	String[] returnStringList = new String[regColoringMap.size()];
    	int i = 0;
    	for (String name : regColoringMap.keySet()) {
    		returnStringList[i] = name;
    		i++;
    	}
    	return returnStringList;
    }
    
    public String getColor(String schemeName, Edge edge) {
    	for (Map.Entry<String, ColoringScheme> entry: regColoringMap.entrySet()) {
    		String entryName = entry.getKey();
    		ColoringScheme entryScheme = entry.getValue();
    		if (entryName.equals(schemeName)) {
    			return entryScheme.getColor(edge);
    		}
    	}
    	return null;
    }

	public boolean schemeIsPolling(String schemeName, Edge edge) {
		for (Map.Entry<String, ColoringScheme> entry: regColoringMap.entrySet()) {
			String entryName = entry.getKey();
			ColoringScheme entryScheme = entry.getValue();
			if (entryName.equals(schemeName)) {
				return entryScheme.isPolling(edge);
			}
		}
		return false;
	}
}
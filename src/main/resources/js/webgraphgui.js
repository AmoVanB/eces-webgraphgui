/**
 * Javascript for the GUI.
 *
 * Currently incredibly ugly. We'll clean if we have time, but not very important as it is just the GUI.
 *
 * @author Florian Kreft
 * @author Amaury Van Bemten
 * @author Jochen Guck
 */
var output;
var entityIdsToSvg = {};
var graph;
var renderer;
var graphics;
var nodes = {};
var edges = {};
var currentEntityId = -1;
var currentGraph = -1;
var paused = 0;
var debugVisible = 0;
var currentColorScheme = "no color scheme loaded";
var debugMode = false;
var weAreGoingThroughComponentsOfTheGraph = false;

var MHMbufferUsage = 0;
var MHMrateUsage = 0;
var MHMbufferMax = 0;
var MHMrateMax = 0;
var MHMmaxSet = false; // determines if all MHM variables are set
var MHMusageSet = false;

var TBMbufferUsage = 0;
var TBMdelayUsage = 0;
var TBMbufferMax = 0;
var TBMdelayMax = 0;
var TBMmaxDelaySet = false; // determines if all TBM variables are set
var TBMmaxBuferSet = false; // determines if all TBM variables are set
var TBMusageSet = false;

var lastGraphFlowsSort = null;
var lastEdgeFlowsSort = null;
var lastGraphFlowsSortDir = null;
var lastEdgeFlowsSortDir = null;

var maxXForPlots = 0;
var maxYForPlots = 0;

var clickedNode = null;

var edgeDelay = 0;

function init() {
    output = document.getElementById("output");
    initWebSocket();
    initGraph();
    $( "#toggleDebug").click(function() {
        if (debugVisible == 0) {
            debugVisible = 1;
            $( "#toggleDebug").text("Hide Communication Debug");
            $( "#debugColumn").css("display", "");
        }
        else {
            debugVisible = 0;
            $( "#toggleDebug").text("Display Communication Debug");
            $( "#debugColumn").css("display", "none");
        }
    });

    $( "#toggleDebugMode").click(function() {
        if (!debugMode) {
            debugMode = true;
            $( "#toggleDebugMode").text("Turn Off Visualization Debug");
        }
        else {
            debugMode = false;
            $( "#toggleDebugMode").text("Turn On Visualization Debug");
        }
    });
}

function initGraph() {
    // create a graph object.
    graph = Viva.Graph.graph();
    graphics = Viva.Graph.View.svgGraphics();
    var nodeSize = 14;

    var onElementClicked = function(obj) {
        try {
            //objjson = JSON.parse(obj.data);
            //writeToScreen('<span style="color: red;">executing onClicked, obj.data='+JSON.stringify(obj.data)+'</span>');
            currentEntityId = obj.data.entityId;
            document.getElementById('clickedType').innerHTML = " " + obj.data.type + " Info";
            var tmpString = "";
            if (obj.data.id != undefined)   {
                tmpString += "ID: " + obj.data.id + "<br>";
            }
            tmpString += "GraphID: " + obj.data.graph +
                "<br>EntityID: " + obj.data.entityId + "</p>";
            document.getElementById('infoHeader').innerHTML = tmpString;
        } catch (e) {
            currentEntityId = -1;
            document.getElementById('infoHeader').innerHTML = "<p>ERROR, obj.data in wrong format: " + JSON.stringify(obj.data) + "</p>";
        }
        doSend('{"type":"elementClicked", "data":'+currentEntityId+'}');
    }

    graphics.node(function(node) {
        var fillColor = "rgb(11, 109, 149)";
        if(node.data.device === "interface")
            fillColor = "rgb(203, 105, 0)";

        var edgeColor = "rgb(50, 130, 190)";
        if(node.data.device === "interface")
            edgeColor = "rgb(246, 127, 0)";

        var node_svg = Viva.Graph.svg('g'),
            node_text = Viva.Graph.svg('text').attr('stroke-width', '1px').attr("stroke", "white").attr('y', '5px').attr("fill", "white").text(node.data.entityId),
            node_rect = Viva.Graph.svg('circle')
             .attr('r', nodeSize)
             .attr('style','fill:' + fillColor)
                .attr('stroke', edgeColor)
                .attr('stroke-width', '2px')
                .attr('stroke-unclicked', edgeColor);

        if(node.data.entityId > 99)
            node_text.attr('x', '-12px');
        else if(node.data.entityId > 9)
            node_text.attr('x', '-8px');
        else
            node_text.attr('x', '-3px');
        node_svg.append(node_rect);
        node_svg.append(node_text);

        $(node_svg).click(function() { // click on node
            highlightEdges([]);

            if(clickedNode !== null) {
                clickedNode.attr('stroke', clickedNode.attr('stroke-unclicked')).attr('stroke-width', '2px');
            }

            clickedNode = node_rect;
            onElementClicked(node);
            node_rect.attr('stroke', 'red')
                     .attr('stroke-width', '3px');
        });

        return node_svg;
    }).placeNode(function(nodeUI, pos) {
        nodeUI.attr('transform', 'translate(' + (pos.x - 0/4) + ',' + (pos.y - 0/4) + ')');
    });

    graphics.link(function(link) {
        //create link itself
        //writeToScreen('<span style="color: green;">link.data.connectionNumber: '+link.data.connectionNumber+'</span>');
        var currentColor;
        if (!link.data.hasOwnProperty('color')) {
            currentColor = 'gray';
        }
        else {
            currentColor = link.data.color;
        }
        var link_svg = Viva.Graph.svg('path')
            .attr('stroke', currentColor)
            .attr('stroke-width', '2')
            .attr('fill', 'none');
        link_svg.connectionNumber = link.data.connectionNumber;

        entityIdsToSvg[link.data.entityId] = link_svg;

        //add function on click on link
        $(link_svg).click(function() { // click on link
            if(clickedNode !== null) {
                clickedNode.attr('stroke', clickedNode.attr('stroke-unclicked')).attr('stroke-width', '2px');
            }

            onElementClicked(link);
            highlightEdges([link.data.entityId]);
        });
        return link_svg;
        }).placeLink(function(linkUI, fromPos, toPos) {
            //place link
            var data;
            /*
            if (linkUI.connectionNumber == 0) {
                data = 'M' + fromPos.x + ',' + fromPos.y +
                'L' + toPos.x + ',' + toPos.y;
            }
            else {
              */
                var beta;
                if (Math.abs((toPos.x-fromPos.x)) < 3) {
                    if (toPos.y-fromPos.y < 0) {
                        beta = 0.5 * 3.14
                    }
                    else {
                        beta = 1.5 * 3.14
                    }
                }
                else if ((fromPos.x-toPos.x) > 0) {
                    beta = Math.atan((toPos.y-fromPos.y) / (toPos.x-fromPos.x))
                }
                else {
                    beta = Math.atan((toPos.y-fromPos.y) / (toPos.x-fromPos.x)) + 3.14
                }

                var alpha = 0.00 * 3.14;
                var distSupport = 10;
                for (var counter = 1; counter <= linkUI.connectionNumber; counter++) {
                    //alpha += 0.01 * 3.14;
                    distSupport += 7;
                    //writeToScreen('<span style="color: green;">rx: '+rx+'</span>');
                }
                // currently all links on the right: if on both sides wanted, add this again
                /*
                if (linkUI.connectionNumber % 2 === 1) {
                    alpha = -alpha;
                    distSupport = -distSupport;
                }
                */
                var x1 = fromPos.x + distSupport * Math.sin(-alpha + beta) ;
                var y1 = fromPos.y - distSupport * Math.cos(-alpha + beta);
                var x2 = toPos.x + distSupport * Math.sin(alpha + beta);
                var y2 = toPos.y - distSupport * Math.cos(alpha + beta);
                data = 'M' + fromPos.x + ',' + fromPos.y +
                       ' C '+x1+','+y1+','+x2+','+y2+',' + toPos.x + ',' + toPos.y;
            //}
            linkUI.attr("d", data);
        });

    // Render the graph.
    renderer = Viva.Graph.View.renderer(graph,
        {
            graphics: graphics,
            container  : document.getElementById('graphContainer')
        });
    renderer.run();

    $( "#toggleRenderer").click(function() {
        if (paused == 0) {
            renderer.pause();
            paused = 1;
            $( "#toggleRenderer").text("Resume Renderer");
        }
        else {
            renderer.resume();
            paused = 0;
            $( "#toggleRenderer").text("Pause Renderer");
        }
    });
}

function addGraph(graphid) {
    nodes[graphid] = [];
    edges[graphid] = [];
    //add Entry to dropdown
    document.getElementById('graphDropdown').innerHTML += '<li class="GraphEntry" graphnumber="'+graphid+'"><a href="#">GraphID: '+graphid+'</a></li>';
    //add functionality to dropdown entry
    $( ".GraphEntry").click(function() {
        //var clickedId = $(this).attr('id');
        //var graphNumber = $(this).attr('graphnumber');
        graph.clear();
        currentGraph = $(this).attr('graphnumber');
        sendActiveGraph();
        writeToScreen('<span style="color: green;">currentGraph='+currentGraph+'...</span>');
        /* evtl dashier wenn nicht funzt
        g.forEachNode(function(node){
            console.log(node.id, node.data);
        });
        */
        writeToScreen('<span style="color: red;">nodes='+JSON.stringify(nodes)+'</span>');
        for (var nodePos in nodes[currentGraph]) {
            nodeEntry = nodes[currentGraph][nodePos];
            writeToScreen('<span style="color: red;">TRYING TO ADD NODE, nodeEntry='+JSON.stringify(nodeEntry)+' nodeEntityId='+JSON.stringify(nodeEntry.entityId)+'</span>');
            graph.addNode(nodeEntry.entityId, nodeEntry);
            //writeToScreen('<span style="color: red;">NODE ADDED TO GRAPH! </span>');
        }
        writeToScreen('<span style="color: red;">edges[graphNumber]='+JSON.stringify(edges[currentGraph])+'</span>');
        for (var edgePos in edges[currentGraph]) {
            edgeEntry = edges[currentGraph][edgePos];
            //writeToScreen('<span style="color: red;">TRYING TO ADD EDGE, edgeEntry='+JSON.stringify(edgeEntry)+'</span>');
            graph.addLink(edgeEntry.sourceEntityId, edgeEntry.destEntityId, edgeEntry);
            //writeToScreen('<span style="color: red;">EDGE ADDED TO GRAPH! </span>');
        }

        $( "#toggleRenderer").text("Pause Renderer");
        if (paused == 1) {
            renderer.resume();
            paused = 0;
        }
        /*
        var jsonobj2;
        jsonobj2.type = "Node";
        jsonobj2.id = "54213";
        graph.addNode(1, jsonobj2);
        jsonobj2.id = "54214";
        graph.addNode(2, jsonobj2);
        var jsonobj1;
        jsonobj1.type = "Link";
        jsonobj1.status = "default";
        graph.addLink(1,2,jsonobj1);
        setTimeout(graph.addLink(1,2,jsonobj1),3000);
        */
        //writeToScreen('<span style="color: green;">Graph ready!</span>');
    });
}

function addColor(colorSchemeName) {
    document.getElementById('colorDropdown').innerHTML += '<li class="ColorEntry" colorSchemeName="'+colorSchemeName+'"><a href="#">'+colorSchemeName+'</a></li>';
    //add functionality to dropdown entry
    $( ".ColorEntry").click(function() {
        currentColorScheme = $(this).attr('colorSchemeName');
        getColors(currentColorScheme);
    });
}

function getColors(colorSchemeName) {
    doSend('{"type":"getColors", "schemeName":"'+colorSchemeName+'", "activeGraphEntityID":"'+currentGraph+'"}');
    document.getElementById('coloringScheme').innerHTML = "<strong>Active coloring scheme:</strong> " + colorSchemeName;
}

function sendActiveGraph() {
    doSend('{"type":"activeGraph", "currentGraph":"'+currentGraph+'"}');
    document.getElementById('coloringScheme').innerHTML = "<strong>Active coloring scheme:</strong> None";
}

function initWebSocket() {
    websocket = new WebSocket("ws://"+location.host+"/websocket");
    websocket.onopen = function (evt) {
        onOpen(evt)
    };
    websocket.onclose = function (evt) {
        onClose(evt)
    };
    websocket.onmessage = function (evt) {
        onMessage(evt)
    };
    websocket.onerror = function (evt) {
        onError(evt)
    };
}

function onOpen(evt) {
    writeToScreen("CONNECTED");
    doSend('{"type":"connected"}');
}

function onClose(evt) {
    writeToScreen("DISCONNECTED");
}

//executed whenever websocketframe recieved from server, evt.data in most cases in JSON format
function onMessage(evt) {
    weAreGoingThroughComponentsOfTheGraph = false;

    maxXForPlots = 0;
    maxYForPlots = 0;
    curvesToPlot = [];

    try {
        var jsonobj = JSON.parse(evt.data);
    } catch (e) {
        writeToScreen('<span style="color: red;">RECEIVED DATA NOT JSON</span>');
        return false;
    }

    if (jsonobj.type == "Node") {
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        var graphid = jsonobj.graph;
        if (jsonobj.status == "Destroyed") {
            for (var nodePos in nodes[graphid]) {
                if (nodes[graphid][nodePos].entityId == jsonobj.entityId) {
                    //writeToScreen('<span style="color: red;">BEFORE SPLICE:'+JSON.stringify(nodes[graphid])+'</span>');
                    //removes node from array
                    nodes[graphid].splice(nodePos, 1);

                    //writeToScreen('<span style="color: red;">AFTER SPLICE:'+JSON.stringify(nodes[graphid])+'</span>');
                }
            }
            if (graphid == currentGraph) {
                graph.removeNode(jsonobj.entityId);
            }
            return;
        }
        if (jsonobj.status == "Updated") {
            var nodeExists = false;
            for (var nodePos in nodes[graphid]) {
                if (nodes[graphid][nodePos].entityId == jsonobj.entityId) {
                    //writeToScreen('<span style="color: red;">BEFORE SPLICE:'+JSON.stringify(nodes[graphid])+'</span>');
                    //removes node from array
                    nodeExists = true;
                    nodes[graphid].splice(nodePos, 1);

                    //writeToScreen('<span style="color: red;">AFTER SPLICE:'+JSON.stringify(nodes[graphid])+'</span>');
                }
            }
            if (!nodeExists) {
                return;
            }
        }
        if (nodes[graphid] == null) {
            addGraph(graphid);
        }
        nodes[graphid].push(jsonobj);
        if (graphid == currentGraph) {
            graph.addNode(jsonobj.entityId, jsonobj);
        }
    }
    else if (jsonobj.type == "Edge") {
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        var graphid = jsonobj.graph;
        if (edges[graphid] == null) {
            addGraph(graphid);
        }

        if (jsonobj.status == "Destroyed") {
            for (var edgePos in edges[graphid]) {
                edgeEntry = edges[graphid][edgePos];
                //needed to update connection numbers of edges with same src and dest
                if (edgeEntry.connectionNumber > jsonobj.connectionNumber) {
                    edgeEntry.connectionNumber--;
                }
                //removes edge from array
                if (edgeEntry.entityId == jsonobj.entityId) {
                    edges[graphid].splice(edgePos, 1);
                }
            }
            graph.forEachLink(function (link) {
                if (link.data.entityId == jsonobj.entityId) {
                    graph.removeLink(link);
                }
            });

            return;
        }
        if (jsonobj.status == "Updated") {
            var edgeExists = false;
            for (var edgePos in edges[graphid]) {
                edgeEntry = edges[graphid][edgePos];
                //removes edge from array
                if (edgeEntry.entityId == jsonobj.entityId) {
                    edgeExists = true;
                    jsonobj.connectionNumber = edgeEntry.connectionNumber;
                    edges[graphid].splice(edgePos, 1);
                }
            }
            if (!edgeExists) {
                return;
            }
        }
        else {
            jsonobj.connectionNumber = 0;
            for (var edgePos in edges[graphid]) {
                edgeEntry = edges[graphid][edgePos];
                if ((edgeEntry.sourceEntityId == jsonobj.sourceEntityId) && (edgeEntry.destEntityId == jsonobj.destEntityId)) {
                    if (edgeEntry.connectionNumber == jsonobj.connectionNumber) {
                        jsonobj.connectionNumber = edgeEntry.connectionNumber + 1;
                    }
                }
            }
        }

        edges[graphid].push(jsonobj);

        if (graphid == currentGraph) {
            graph.addLink(jsonobj.sourceEntityId, jsonobj.destEntityId, jsonobj);
        }
        //writeToScreen('<span style="color: green;">New Edge added, edges='+JSON.stringify(edges)+'</span>');
        //graphs[id].addLink(jsonobj.sourceEntityId, jsonobj.destEntityId, evt.data);
    }
    else if (jsonobj.type == "Entity" && jsonobj.entityId == currentEntityId) {
        $("#plot-placeholder").hide();
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        var innerHtmlString = "<div>";
        //innerHtmlString += "EntityID: " + jsonobj.entityId + "<br>";

        //deconstruct data in recieved jsonobject
        //iterate through all Systems
        for (var sys in jsonobj.data) {
            var componentList = jsonobj.data[sys];
            var systemString = getSystemString(sys, componentList);
            innerHtmlString += systemString;
        }
        innerHtmlString += "</div>";
        document.getElementById('entityInfo').innerHTML = innerHtmlString;
    }
    else if (jsonobj.type == "Color") {
        graph.forEachLink(function (link) {
            if (link.data.entityId == jsonobj.entityId) {
                var linkUI = graphics.getLinkUI(link.id);
                if (linkUI) {
                    // linkUI is a UI object created by graphics below
                    linkUI.attr('stroke', jsonobj.color);
                }
            }
        });
    }
    else if (jsonobj.type == "ColoringScheme") {
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        addColor(jsonobj.name);
    }
    else if (jsonobj.type == "Error") {
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        writeToScreen('<span style="color: red;">ERROR: ' + jsonobj.errorMsg + '</span>');
        if (jsonobj.errorType == "onNodeClicked") {
            document.getElementById('entityInfo').innerHTML = "<div>Error onNodeClicked!";
        }
    }
    else if (jsonobj.type == "Entity" && jsonobj.entityId == currentGraph) {
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        var innerHtmlString = "<div>";
        //iterate through all Systems
        for (var sys in jsonobj.data) {
            var componentList = jsonobj.data[sys];
            weAreGoingThroughComponentsOfTheGraph = true;
            var systemString = getSystemString(sys, componentList);
            innerHtmlString += systemString;
        }
        innerHtmlString += "</div>";
        document.getElementById('graphEntityInfo').innerHTML = innerHtmlString;
    }
    else {
        writeToScreen('<span style="color: blue;">RESPONSE: ' + evt.data + '</span>');
        writeToScreen('<span style="color: red;">JSON WITHOUT TYPE / WRONG TYPE: ' + JSON.stringify(jsonobj) + '</span>');
    }

    if (curvesToPlot.length > 0 && !debugMode) {
        document.getElementById("plot-placeholder").style.display = 'block';
        $.plot("#plot-placeholder", curvesToPlot, {
            yaxes: [{
                axisLabel: '<small><em>KB</em></small>',
                axisLabelPadding: 5,
                min: 0,
                max: 2 * maxYForPlots
            }],
            xaxes: [{
                axisLabel: '<small><em>ms</em></small>',
                axisLabelPadding: 5,
                min: 0,
                max: maxXForPlots
            }],
            zoom: {
                interactive: true
            },
            pan: {
                interactive: true
            },
            legend: {
                position: "nw"
            }
        });
    }

    if (debugMode) {
        document.getElementById("plot-placeholder").style.display = 'none';
        document.getElementById("flow-table-placeholder").style.display = 'none';
        document.getElementById("graph-flow-table-placeholder").style.display = 'none';
    }
}

function highlightEdges(edgesToHighlight) {
    for (var entityId in entityIdsToSvg) {
        entityIdsToSvg[entityId].attr('stroke-width', '2');

        for (var i = 0; i < edgesToHighlight.length; i++) {
            if(parseInt(edgesToHighlight[i]) === parseInt(entityId))
                entityIdsToSvg[entityId].attr('stroke-width', '7');
        }
    }

}

function getSystemString(sysId, componentList) {

    //iterate through all Components in respective System
    var actStringClass;
    var componentStringList = [];
    for (var component in componentList) {
        if(!debugMode && component === "boguscomponent") {
            if(componentList[component]["class"] === "Measures") {
                TBMdelayUsage = componentList[component]["horizontal_deviation"];
                TBMbufferUsage = componentList[component]["vertical_deviation"];
                if(TBMdelayUsage > maxXForPlots)
                    maxXForPlots = TBMdelayUsage;
                TBMusageSet = true;
            }
            continue; // Hide bogus component if not debug
        }

        //in this case the value of "component" is actually the classname of the system
        if (component == "sysClass") {
            var actSystemClass = componentList[component];
        }
        else {
            //get all datasets in respective component to display
            var dataList = componentList[component];
            var componentString = getComponentString(component, dataList);
            componentStringList.push(componentString);
        }
    }
    var systemString = "<h5>#" + sysId + " " + actSystemClass + "</h5>";
    systemString += "<ul class='components-list'>"
    componentStringList.sort();
    for (index = 0; index < componentStringList.length; index++) {
        systemString += componentStringList[index];
    }
    systemString += "</ul>"

    return systemString;
}

//get all datasets in respective component to display
//dataList should be a JSONObject
function getComponentString(componentId, dataList) {
    var currentClass = dataList["class"];
    var infoList = [];

    for (var dataName in dataList) {
        var dataEntry = dataList[dataName];
        if (dataName === "class") {
            continue;
        }
        else if (typeof dataEntry === 'object') {
            infoList.push(dataName + getListString(dataEntry))
        }
        else {
            infoList.push(dataName + ": " + dataEntry);
        }
    }

    var componentString = "";
    var flowsTableString = "";

    if(!debugMode) {
        if(currentClass === "PathList") {
            // Here, we fill the flowsTableString (table showing all the flows) and not the normal bullet list string
            if (!weAreGoingThroughComponentsOfTheGraph)
                flowsTableString = "<table id='edge-flow-table' class='table table-condensed table-bordered table-striped table-hover table-responsive text-center'>";
            else
                flowsTableString = "<table id='graph-flow-table' class='table table-condensed table-bordered table-striped table-hover table-responsive text-center'>";

            if (!weAreGoingThroughComponentsOfTheGraph) {
                if(MHMmaxSet) {
                    flowsTableString += "<tr>" +
                        "<th class='text-center'>View</th>" +
                        "<th class='text-center'>Name</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(2, \"edge-flow-table\")'>Src</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(3, \"edge-flow-table\")'>Dst</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(4, \"edge-flow-table\")'>%B</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(5, \"edge-flow-table\")'>%R</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(6, \"edge-flow-table\")'>%d</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(7, \"edge-flow-table\")'>%D</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(8, \"edge-flow-table\")'>Cost</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(9, \"edge-flow-table\")'>Hops</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(10, \"edge-flow-table\")'>Time</th>" +
                        "<th class='text-center'>Delete</th>" +
                        "</tr>";
                }
                else {
                    flowsTableString += "<tr>" +
                        "<th class='text-center'>View</th>" +
                        "<th class='text-center'>Name</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(2, \"edge-flow-table\")'>Src</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(3, \"edge-flow-table\")'>Dst</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(4, \"edge-flow-table\")'>%d</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(5, \"edge-flow-table\")'>%D</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(6, \"edge-flow-table\")'>Cost</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(7, \"edge-flow-table\")'>Hops</th>" +
                        "<th class='hand-cursor text-center' onclick='sortFlowTable(8, \"edge-flow-table\")'>Time</th>" +
                        "<th class='text-center'>Delete</th>" +
                        "</tr>";
                }
            }
            else {
                flowsTableString += "<tr>" +
                    "<th class='text-center'>View</th>" +
                    "<th class='text-center'>Name</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(2, \"graph-flow-table\")'>Src</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(3, \"graph-flow-table\")'>Dst</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(4, \"graph-flow-table\")'>Burst (KB)</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(5, \"graph-flow-table\")'>Rate (Kbps)</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(6, \"graph-flow-table\")'>Deadline (ms)</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(7, \"graph-flow-table\")'>%D</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(8, \"graph-flow-table\")'>Cost</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(9, \"graph-flow-table\")'>Hops</th>" +
                    "<th class='hand-cursor text-center' onclick='sortFlowTable(10, \"graph-flow-table\")'>Time</th>" +
                    "<th class='text-center'>Delete</th>" +
                    "</tr>";

                flowsTableString += "<tr>" +
                    "<th class='text-center'></th>" +
                    "<th class='text-center'><input size='6' id=\"new-flow-name\" type=\"text\" value=\"\"></th>" +
                    "<th class='hand-cursor text-center'><input size='4' id=\"new-flow-source\" type=\"text\" value=\"\"></th>" +
                    "<th class='hand-cursor text-center'><input size='4' id=\"new-flow-destination\" type=\"text\" value=\"\"></th>" +
                    "<th class='hand-cursor text-center'><input size='8' id=\"new-flow-burst\" type=\"text\" value=\"\"></th>" +
                    "<th class='hand-cursor text-center'><input size='8' id=\"new-flow-rate\" type=\"text\" value=\"\"></th>" +
                    "<th class='hand-cursor text-center'><input size='8' id=\"new-flow-deadline\" type=\"text\" value=\"\"></th>" +
                    "<th class='hand-cursor text-center'></th>" +
                    "<th class='hand-cursor text-center'></th>" +
                    "<th class='hand-cursor text-center'></th>" +
                    "<th class='hand-cursor text-center'></th>" +
                    "<th class='text-center'><button class='btn btn-xs btn-success' onclick='addFlow()'><span class='glyphicon glyphicon-plus'></span></button></th>" +
                    "</tr>";
            }
            componentString += "<li><span class='badge'>" + dataList["size"] + "</span> flows</li>";
            for (var flow in dataList["flows"]) {
                var name = "";
                var source = "";
                var sourceForSort = 0;
                var destination = "";
                var destinationForSort = 0;
                var percentBurst = "";
                var percentBurstForSort = 0;
                var percentRate = "";
                var percentRateForSort = 0;
                var percentDelay = "";
                var percentDelayForSort = 0;
                var flowBurstValue = "";
                var flowBurstValueForSort = 0;
                var flowRateValue = "";
                var flowRateValueForSort = 0;
                var percentTotalDelay = "";
                var percentTotalDelayForSort = 0;
                var flowDeadlineValue = "";
                var flowDeadlineValueForSort = 0;
                var cost = "";
                var costForSort = "";
                var hops = "";
                var hopsForSort = "";
                var time = "";
                var timeForSort = "";
                var view = "";
                var del = "";

                var m = new Date(Number(flow));
                timeForSort = Number(flow);
                time =
                    //m.getDate() + "/" +
                    //("0" + (m.getMonth() + 1)).slice(-2) + "/" +
                    //("0" + m.getYear()).slice(-2) + " " +
                    ("0" + m.getHours()).slice(-2) + ":" +
                    ("0" + m.getMinutes()).slice(-2) + ":" +
                    ("0" + m.getSeconds()).slice(-2) + "." +
                    ("0" + m.getMilliseconds()).slice(-2);

                var flowEntry = dataList["flows"][flow];
                if (flowEntry.hasOwnProperty("RequestName")) {
                    name = flowEntry["RequestName"]["name"];
                }

                if (flowEntry.hasOwnProperty("UnicastRequest")) {
                    source = flowEntry["UnicastRequest"]["source"];
                    sourceForSort = source;
                    destination = flowEntry["UnicastRequest"]["destination"];
                    destinationForSort = destination;
                    del = "<button class='btn btn-xs btn-danger' onclick='sendDeleteFlow(" + flowEntry["UnicastRequest"]["entity"] + ")'><span class='glyphicon glyphicon-remove'></span></button>";

                    view = "<button class='btn btn-xs btn-primary' onclick='highlightEdges([" + flowEntry["Path"]["edges"] + "])'><span class='glyphicon glyphicon-search'></span></button>";
                }
                else if (flowEntry.hasOwnProperty("ResilientRequest")) {
                    source = flowEntry["ResilientRequest"]["source"];
                    sourceForSort = source;
                    destination = flowEntry["ResilientRequest"]["destination"];
                    destinationForSort = destination;
                    del = "<button class='btn btn-xs btn-danger' onclick='sendDeleteFlow(" + flowEntry["ResilientRequest"]["entity"] + ")'><span class='glyphicon glyphicon-remove'></span></button>";

                    var edgesString = "";
                    for (var j = 0; j < Object.keys(flowEntry["ResilientPath"]).length; j++) {
                        for (var k = 0; k < flowEntry["ResilientPath"][j]["Path"]["edges"].length; k++)
                            edgesString += flowEntry["ResilientPath"][j]["Path"]["edges"][k] + ","
                    }

                    view = "<button class='btn btn-xs btn-primary' onclick='highlightEdges([" + edgesString + "])'><span class='glyphicon glyphicon-search'></span></button>";
                }
                else if (flowEntry.hasOwnProperty("DisjointRequest")) {
                    var destinationsString = "";
                    for (var i = 0; i < flowEntry["DisjointRequest"]["destinations"].length; i++) {
                        destinationsString += flowEntry["DisjointRequest"]["destinations"][i] + ", ";
                    }
                    destinationForSort = flowEntry["DisjointRequest"]["destinations"][0];

                    source = flowEntry["DisjointRequest"]["source"];
                    sourceForSort = source;
                    del = "<button class='btn btn-xs btn-danger' onclick='sendDeleteFlow(" + flowEntry["DisjointRequest"]["entity"] + ")'><span class='glyphicon glyphicon-remove'></span></button>";
                    destination = destinationsString.slice(0, -2);

                    var edgesString = "";
                    for (var j = 0; j < Object.keys(flowEntry["DisjointPaths"]).length; j++) {
                        for (var k = 0; k < flowEntry["DisjointPaths"][j]["Path"]["edges"].length; k++)
                            edgesString += flowEntry["DisjointPaths"][j]["Path"]["edges"][k] + ","
                    }

                    view = "<button class='btn btn-xs btn-primary' onclick='highlightEdges([" + edgesString + "])'><span class='glyphicon glyphicon-search'></span></button>";
                }
                else {
                    source = "UNKNOWN TO GUI";
                    destination = "UNKNOWN TO GUI";
                    continue;
                }

                if (flowEntry.hasOwnProperty("NCRequestData")) {
                    if (flowEntry.hasOwnProperty("Path")) {
                        cost = (Math.round(flowEntry["Path"]["cost"] * 10) / 10);
                        costForSort = cost;
                        hops = flowEntry["Path"]["edges"].length;
                        hopsForSort = flowEntry["Path"]["edges"].length;
                    }
                    else if (flowEntry.hasOwnProperty("ResilientPath")) {
                        cost = (Math.round((flowEntry["ResilientPath"][0]["Path"]["cost"] + flowEntry["ResilientPath"][1]["Path"]["cost"]) * 10) / 10);
                        costForSort = parseFloat(flowEntry["ResilientPath"][0]["Path"]["cost"]) + parseFloat(flowEntry["ResilientPath"][1]["Path"]["cost"]);
                        hops = flowEntry["ResilientPath"][0]["Path"]["edges"].length + " and " + flowEntry["ResilientPath"][1]["Path"]["edges"].length;
                        hopsForSort = flowEntry["ResilientPath"][0]["Path"]["edges"].length + flowEntry["ResilientPath"][1]["Path"]["edges"].length;
                    }

                    flowBurstValue = flowEntry["NCRequestData"]["burst"];
                    flowBurstValueForSort = flowBurstValue;
                    flowRateValue = flowEntry["NCRequestData"]["rate"];
                    flowRateValueForSort = flowRateValue;
                    flowDeadlineValue = flowEntry["NCRequestData"]["deadline"];
                    flowDeadlineValueForSort = flowDeadlineValue;

                    // Buffer consumption bar
                    if (!weAreGoingThroughComponentsOfTheGraph) {
                        if(MHMmaxSet) {
                            var bufferUsage;
                            if (flowEntry.hasOwnProperty("ResilientRequest")) {
                                var pathOfThisEdge = -1;
                                if ($.inArray(currentEntityId, flowEntry["ResilientPath"][0]["Path"]["edges"]) !== -1)
                                    pathOfThisEdge = 0;
                                else if ($.inArray(currentEntityId, flowEntry["ResilientPath"][1]["Path"]["edges"]) !== -1)
                                    pathOfThisEdge = 1;

                                var edgeId = $.inArray(currentEntityId, flowEntry["ResilientPath"][pathOfThisEdge]["Path"]["edges"]);
                                var delaySoFar;
                                if (flowEntry["ResilientPath"][pathOfThisEdge].hasOwnProperty("BurstIncreaseList"))
                                    delaySoFar = flowEntry["ResilientPath"][pathOfThisEdge]["BurstIncreaseList"]["bursts"][edgeId];
                                else
                                    delaySoFar = 0
                                bufferUsage = flowEntry["NCRequestData"]["burst"] + delaySoFar * flowEntry["NCRequestData"]["rate"] / 8;
                            }
                            else if (flowEntry.hasOwnProperty("UnicastRequest")) {
                                var edgeId = $.inArray(currentEntityId, flowEntry["Path"]["edges"]);
                                var delaySoFar;
                                if (flowEntry.hasOwnProperty("BurstIncreaseList"))
                                    delaySoFar = flowEntry["BurstIncreaseList"]["bursts"][edgeId];
                                else
                                    delaySoFar = 0
                                bufferUsage = flowEntry["NCRequestData"]["burst"] + delaySoFar * flowEntry["NCRequestData"]["rate"] / 8;
                            }
                            var bufferPercent = Math.round(bufferUsage / MHMbufferMax * 100 * 10) / 10;

                            var barcolor = "green";
                            if (bufferPercent > 85)
                                barcolor = "red";
                            else if (bufferPercent > 65)
                                barcolor = "orange";

                            percentBurst = "<span style=\"color:" + barcolor + ";\" title='Queue buffer consumption: " + Math.round(bufferUsage * 100) / 100 + "KB/" + Math.round(MHMbufferMax * 100) / 100 + "KB&nbsp;(" + bufferPercent + "%)'>" + bufferPercent + "%</span>\n";
                            percentBurstForSort = bufferPercent;

                            // Rate consumption bar
                            var rateUsage = flowEntry["NCRequestData"]["rate"];
                            var ratePercent = Math.round(rateUsage / MHMrateMax * 100 * 10) / 10;

                            barcolor = "green";
                            if (ratePercent > 85)
                                barcolor = "red";
                            else if (ratePercent > 65)
                                barcolor = "orange";

                            percentRate = "<span style=\"color:" + barcolor + ";\" title='Queue rate consumption: " + Math.round(rateUsage / 1000 * 10) / 10 + "Mbps/" + Math.round(MHMrateMax / 1000 * 10) / 10 + "Mbps&nbsp;(" + ratePercent + "%)'>" + ratePercent + "%</span>\n";
                            percentRateForSort = ratePercent;
                        }

                        // Delay contribution bar
                        var delay = 0;
                        var delayPercent = 0;
                        if (flowEntry.hasOwnProperty("Path")) {
                            delay = flowEntry["Path"]["constraints"][0] * 1000;
                            delayPercent = Math.round(edgeDelay / delay * 100 * 10) / 10;
                        }
                        else if (flowEntry.hasOwnProperty("ResilientPath")) {
                            var pathOfThisEdge = -1;
                            if ($.inArray(currentEntityId, flowEntry["ResilientPath"][0]["Path"]["edges"]) !== -1)
                                pathOfThisEdge = 0;
                            else if ($.inArray(currentEntityId, flowEntry["ResilientPath"][1]["Path"]["edges"]) !== -1)
                                pathOfThisEdge = 1;

                            delay = flowEntry["ResilientPath"][pathOfThisEdge]["Path"]["constraints"][0] * 1000;
                            delayPercent = Math.round(edgeDelay / delay * 100 * 10) / 10;
                        }

                        barcolor = "green";
                        if (delayPercent > 85)
                            barcolor = "red";
                        else if (delayPercent > 65)
                            barcolor = "orange";

                        percentDelay = "<span style=\"color:" + barcolor + ";\" title='Queue delay contribution: " + Math.round(edgeDelay * 1000) / 1000 + "ms/" + Math.round(delay * 1000) / 1000 + "ms&nbsp;(" + delayPercent + "%)'>" + delayPercent + "%</span>\n";
                        percentDelayForSort = delayPercent;
                    }

                    // Total delay bar
                    var totalDelay = 0;
                    var limitDelay = 0;
                    var delayPercent = 0;
                    if (flowEntry.hasOwnProperty("Path")) {
                        totalDelay = flowEntry["Path"]["constraints"][0] * 1000;
                        limitDelay = flowEntry["NCRequestData"]["deadline"];
                        delayPercent = Math.round(totalDelay / limitDelay * 100 * 10) / 10;

                    }

                    barcolor = "green";
                    if (delayPercent > 85)
                        barcolor = "red";
                    else if (delayPercent > 65)
                        barcolor = "orange";

                    percentTotalDelay = "<span style=\"color:" + barcolor + ";\" title='Total delay: " + Math.round(totalDelay * 1000) / 1000 + "ms/" + Math.round(limitDelay * 1000) / 1000 + "ms&nbsp;(" + delayPercent + "%)'>" + delayPercent + "%</span>\n";
                    percentTotalDelayForSort = delayPercent;
                }

                if (!weAreGoingThroughComponentsOfTheGraph) {
                    if(MHMmaxSet) {
                        flowsTableString += "<tr>" +
                            "<td>" + view + "</td>" +
                            "<td><i>" + name + "</i></td>" +
                            "<td sort='" + sourceForSort + "'>" + source + "</td>" +
                            "<td sort='" + destinationForSort + "'>" + destination + "</td>" +
                            "<td sort='" + percentBurstForSort + "'>" + percentBurst + "</td>" +
                            "<td sort='" + percentRateForSort + "'>" + percentRate + "</td>" +
                            "<td sort='" + percentDelayForSort + "'>" + percentDelay + "</td>" +
                            "<td sort='" + percentTotalDelayForSort + "'>" + percentTotalDelay + "</td>" +
                            "<td sort='" + costForSort + "'>" + cost + "</td>" +
                            "<td sort='" + hopsForSort + "'>" + hops + "</td>" +
                            "<td sort='" + timeForSort + "'>" + time + "</td>" +
                            "<td>" + del + "</td>" +
                            "</tr>";
                    }
                    else {
                        flowsTableString += "<tr>" +
                            "<td>" + view + "</td>" +
                            "<td><i>" + name + "</i></td>" +
                            "<td sort='" + sourceForSort + "'>" + source + "</td>" +
                            "<td sort='" + destinationForSort + "'>" + destination + "</td>" +
                            "<td sort='" + percentDelayForSort + "'>" + percentDelay + "</td>" +
                            "<td sort='" + percentTotalDelayForSort + "'>" + percentTotalDelay + "</td>" +
                            "<td sort='" + costForSort + "'>" + cost + "</td>" +
                            "<td sort='" + hopsForSort + "'>" + hops + "</td>" +
                            "<td sort='" + timeForSort + "'>" + time + "</td>" +
                            "<td>" + del + "</td>" +
                            "</tr>";
                    }
                }
                else {
                    flowsTableString += "<tr>" +
                        "<td>" + view + "</td>" +
                        "<td><i>" + name + "</i></td>" +
                        "<td sort='" + sourceForSort + "'>" + source + "</td>" +
                        "<td sort='" + destinationForSort + "'>" + destination + "</td>" +
                        "<td sort='" + flowBurstValueForSort + "'>" + flowBurstValue + " KB</td>" +
                        "<td sort='" + flowRateValueForSort + "'>" + flowRateValue + " Kbps</td>" +
                        "<td sort='" + flowDeadlineValueForSort + "'>" + Math.round(flowDeadlineValue * 1000) / 1000 + " ms</td>" +
                        "<td sort='" + percentTotalDelayForSort + "'>" + percentTotalDelay + "</td>" +
                        "<td sort='" + costForSort + "'>" + cost + "</td>" +
                        "<td sort='" + hopsForSort + "'>" + hops + "</td>" +
                        "<td sort='" + timeForSort + "'>" + time + "</td>" +
                        "<td>" + del + "</td>" +
                        "</tr>";
                }

            }
            componentString += "</ul>";

            flowsTableString += "</table>";
            if (!weAreGoingThroughComponentsOfTheGraph) {
                if(!debugMode)
                    document.getElementById("flow-table-placeholder").style.display = 'block';
                document.getElementById("flow-table-placeholder").innerHTML = flowsTableString;
                if(lastEdgeFlowsSort !== null) {
                    if(lastEdgeFlowsSortDir === "desc")
                        sortFlowTable(lastEdgeFlowsSort, "edge-flow-table");
                    sortFlowTable(lastEdgeFlowsSort, "edge-flow-table");
                }
            }
            else {
                if(!debugMode)
                    document.getElementById("graph-flow-table-placeholder").style.display = 'block';
                document.getElementById("graph-flow-table-placeholder").innerHTML = flowsTableString;
                if(lastGraphFlowsSort !== null) {
                    if(lastGraphFlowsSortDir === "desc")
                        sortFlowTable(lastGraphFlowsSort, "graph-flow-table");
                    sortFlowTable(lastGraphFlowsSort, "graph-flow-table");
                }
            }
        }
        else if (currentClass == "MHMQueueModel") {
            if(typeof dataList["Token Bucket"] !== "undefined") {
                MHMrateMax = dataList["Token Bucket"]["rate"];
                MHMbufferMax = dataList["Token Bucket"]["burst"];
                MHMmaxSet = true;
            }
        }
        else if (currentClass == "SingleTokenBucket") {
            MHMrateUsage = dataList["Token Bucket"]["rate"];
            MHMbufferUsage = dataList["Token Bucket"]["burst"];
            MHMusageSet = true;
        }
        else if (currentClass == "Edge") {
            if(dataList["name"] !== "")
                componentString = "<li><span class='badge'>" + dataList["name"] + "</span></li>"
            componentString += "<li><span class='badge'>" + dataList["source"] + "</span><strong>&nbsp;&rarr;&nbsp;</strong><span class='badge'>" + dataList["destination"] + "</span></li>"
        }
        else if (currentClass == "Node") {
            if(dataList["name"] !== "")
                componentString = "<li><span class='badge'>" + dataList["name"] + "</span></li>"
        }
        else if (currentClass == "Delay") {
            componentString = "<li>Delay: " + Math.round(dataList["delay"] * 1000) / 1000 + "&nbsp;ms</li>"
            edgeDelay = dataList["delay"];
            TBMdelayMax = edgeDelay;
            if(TBMdelayMax > maxXForPlots)
                maxXForPlots = TBMdelayMax;
            TBMmaxDelaySet = true;
        }
        else if (currentClass == "Queue") {
            componentString = "<li>Queue size: " + Math.round(dataList["size"] / 1000) + "&nbsp;KB</li>"
            TBMbufferMax = dataList["size"] / 1000;
            if(TBMbufferMax > maxYForPlots)
                maxYForPlots = TBMbufferMax;
            TBMmaxBuferSet = true;
        }
        else if (currentClass == "Rate") {
            componentString = "<li>Rate: " + Math.round(dataList["rate"] / 1000 * 10) / 10 + "&nbsp;Mbps (" + Math.round(dataList["rate"] / 1000 / 8 * 10) / 10 + "&nbsp;MB/s)</li>"
        }

        if (TBMmaxBuferSet && TBMmaxDelaySet && TBMusageSet) {
            var delayPercent = Math.round(TBMdelayUsage / TBMdelayMax * 100 * 10) / 10;
            var bufferPercent = Math.round(TBMbufferUsage / TBMbufferMax * 100 * 10) / 10;

            var barcolor = "success";
            if (bufferPercent > 85)
                barcolor = "danger";
            else if (bufferPercent > 65)
                barcolor = "warning";

            componentString += "<li>Buffer capacity usage: <div class=\"progress\">" +
                "  <div class=\"progress-bar progress-bar-" + barcolor + "\" role=\"progressbar\" aria-valuenow=\"" + bufferPercent + "\"" +
                "  aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width:" + bufferPercent + "%\">" +
                Math.round(TBMbufferUsage * 100) / 100 + "KB/" + Math.round(TBMbufferMax * 100) / 100 + "KB&nbsp;(" + bufferPercent + "%)" +
                "</div>" +
                "</div></li>"

            barcolor = "success";
            if (delayPercent > 85)
                barcolor = "danger";
            else if (delayPercent > 65)
                barcolor = "warning";

            componentString += "<li>Delay capacity usage: <div class=\"progress\">" +
                "  <div class=\"progress-bar progress-bar-" + barcolor + "\" role=\"progressbar\" aria-valuenow=\"" + delayPercent + "\"" +
                "  aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width:" + delayPercent + "%\">" +
                Math.round(TBMdelayUsage * 100) / 100 + "ms/" + Math.round(TBMdelayMax * 100) / 100 + "ms&nbsp;(" + delayPercent + "%)" +
                "</div>" +
                "</div></li>"
            TBMmaxBuferSet = false;
            TBMmaxDelaySet = false;
            TBMusageSet = false;
        }

        if (MHMusageSet && MHMmaxSet) {
            var ratePercent = Math.round(MHMrateUsage / MHMrateMax * 100 * 10) / 10;
            var bufferPercent = Math.round(MHMbufferUsage / MHMbufferMax * 100 * 10) / 10;

            var barcolor = "success";
            if (bufferPercent > 85)
                barcolor = "danger";
            else if (bufferPercent > 65)
                barcolor = "warning";

            componentString += "<li>MHM buffer usage: <div class=\"progress\">" +
                "  <div class=\"progress-bar progress-bar-" + barcolor + "\" role=\"progressbar\" aria-valuenow=\"" + bufferPercent + "\"" +
                "  aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width:" + bufferPercent + "%\">" +
                Math.round(MHMbufferUsage * 100) / 100 + "KB/" + Math.round(MHMbufferMax * 100) / 100 + "KB&nbsp;(" + bufferPercent + "%)" +
                "</div>" +
                "</div></li>"

            barcolor = "success";
            if (ratePercent > 85)
                barcolor = "danger";
            else if (ratePercent > 65)
                barcolor = "warning";

            componentString += "<li>MHM rate usage: <div class=\"progress\">" +
                "  <div class=\"progress-bar progress-bar-" + barcolor + "\" role=\"progressbar\" aria-valuenow=\"" + ratePercent + "\"" +
                "  aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width:" + ratePercent + "%\">" +
                Math.round(MHMrateUsage / 1000 * 10) / 10 + "Mbps/" + Math.round(MHMrateMax / 1000 * 10) / 10 + "Mbps&nbsp;(" + ratePercent + "%)" +
                "</div>" +
                "</div></li>"
            MHMusageSet = false;
            MHMmaxSet = false;
        }
    }

    if(debugMode || (currentClass != "PathList" && currentClass != "TokenBucketUtilization" && currentClass != "PerInEdgeTokenBucketUtilization" && currentClass != "QueueModel"  && currentClass != "MHMQueueModel" && currentClass != "Edge" && currentClass != "Node" && currentClass != "Delay" && currentClass != "ToNetwork" && currentClass != "Queue"  && currentClass != "Rate")) {
        infoList.sort();
        componentString += '<li class="component-element"><em>' + currentClass + ' (#' + componentId + ')</em>';
        componentString += '<ul class="data-list">'
        for (index = 0; index < infoList.length; index++) {
            componentString += '<li>' + infoList[index] + '</li>';
        }
        componentString += '</ul>'
        componentString += '</li>'
    }

    return componentString;
}

function getListString(list) {
    var infoList = [];
    var color = "rgb(0, 0, 0)" // default black
    var name = "";

    for (var dataName in list) {
        if(dataName == 'plotting') {
            $("#plot-placeholder").show();
            var dataList = list[dataName];
            var data = [];
            var lastX = 0;
            var lastY = 0;
            var finalSlope = 0;
            for (var key in dataList) {
                if(key == "finalSlope") {
                    finalSlope = parseFloat(dataList[key]);
                    finalSlope = finalSlope/1000/1000; // KB/ms
                }
                else if(key == "color") {
                    color = dataList[key];
                }
                else if(key == "name") {
                    name = dataList[key];
                }
                else {
                    var value = parseFloat(dataList[key])/1000; // KB
                    key = parseFloat(key)*1000; // ms
                    data.push([key, value]);

                    // Just keeping track of the last point
                    if(key > lastX) {
                        lastX = key;
                        lastY = value;
                    }
                    else if(key === lastX) {
                        if(lastY < value)
                            lastY = value;
                    }
                }
            }

            console.log(name);


            // Sort data points per x value
            data.sort(function(a, b) { return (parseFloat(a[0]) < parseFloat(b[0])) ? -1 : 1;});
            data.unshift([0, 0], data);
            data.push([lastX + 100.5, lastY + 100.5 * finalSlope]);
            curvesToPlot.push({data: data, color: color, label: name});
        }

        if(debugMode || dataName != 'plotting') {
            var dataEntry = list[dataName];

            if(dataName == "NCRequestData" && !debugMode) {
                delete dataEntry["plotting"]; // No plotting of requests
            }

            if (typeof dataEntry == 'object') {
                infoList.push(dataName + getListString(dataEntry));
            }
            else if (list instanceof Array) {
                infoList.push(dataEntry);
            }
            else {
                infoList.push(dataName + ": " + dataEntry);
            }
        }
    }
    infoList.sort();

    var listString = '<ul class="data-list">';
    for (index = 0; index < infoList.length; index++) {
        listString += "<li>" + infoList[index] + "</li>";
    }
    listString += "</ul>";
    return listString;
}

function onError(evt) {
    writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message) {
    websocket.send(message);
    writeToScreen("SENT: " + message);
}

function writeToScreen(message) {
    var pre = document.createElement("p");
    pre.style.wordWrap = "break-word";
    pre.innerHTML = message;
    output.appendChild(pre);
}

function sendDeleteFlow(flowId) {
    doSend('{"type":"removeFlow", "entityId":"'+flowId+'"}');
}

function addFlow() {
    var source = document.getElementById('new-flow-source').value;
    var destination = document.getElementById('new-flow-destination').value;
    var burst = document.getElementById('new-flow-burst').value;
    var rate = document.getElementById('new-flow-rate').value;
    var deadline = document.getElementById('new-flow-deadline').value;
    var name = document.getElementById('new-flow-name').value;

    doSend('{"type":"newFlow", "name":"'+ name + '", "source":"'+ source + '", "destination":"'+destination+'", "burst":"'+burst+'", "rate":"'+rate+'", "deadline":"'+deadline+'"}');
}

function sortFlowTable(n, table_id) {
    var table, rows, switching, i, x, y, shouldSwitch, dir, switchcount = 0;
    table = document.getElementById(table_id);
    switching = true;
    // Set the sorting direction to ascending:
    dir = "asc";
    /* Make a loop that will continue until
    no switching has been done: */
    while (switching) {
        // Start by saying: no switching is done:
        switching = false;
        rows = table.rows;
        /* Loop through all table rows (except the
        first, which contains table headers): */
        for (i = 1; i < (rows.length - 1); i++) {
            // For this table, we also have the form that shouldn't be sorted.
            if(table_id === "graph-flow-table" && i === 1)
                continue;
            // Start by saying there should be no switching:
            shouldSwitch = false;
            /* Get the two elements you want to compare,
            one from current row and one from the next: */
            x = rows[i].getElementsByTagName("TD")[n];
            y = rows[i + 1].getElementsByTagName("TD")[n];
            /* Check if the two rows should switch place,
            based on the direction, asc or desc: */
            if (dir == "asc") {
                if (parseFloat(x.getAttribute('sort')) > parseFloat(y.getAttribute('sort'))) {
                    // If so, mark as a switch and break the loop:
                    shouldSwitch = true;
                    break;
                }
            } else if (dir == "desc") {
                if (parseFloat(x.getAttribute('sort')) < parseFloat(y.getAttribute('sort'))) {
                    // If so, mark as a switch and break the loop:
                    shouldSwitch = true;
                    break;
                }
            }
        }
        if (shouldSwitch) {
            /* If a switch has been marked, make the switch
            and mark that a switch has been done: */
            rows[i].parentNode.insertBefore(rows[i + 1], rows[i]);
            switching = true;
            // Each time a switch is done, increase this count by 1:
            switchcount ++;
        } else {
            /* If no switching has been done AND the direction is "asc",
            set the direction to "desc" and run the while loop again. */
            if (switchcount == 0 && dir == "asc") {
                dir = "desc";
                switching = true;
            }
        }
    }

    if(table_id === "graph-flow-table") {
        lastGraphFlowsSort = n;
        lastGraphFlowsSortDir = dir;
    }

    if(table_id === "edge-flow-table") {
        lastEdgeFlowsSort = n;
        lastEdgeFlowsSortDir = dir;
    }
}

window.addEventListener("load", init, false);

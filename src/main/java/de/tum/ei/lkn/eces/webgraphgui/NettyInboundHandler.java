package de.tum.ei.lkn.eces.webgraphgui;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.Mapper;
import de.tum.ei.lkn.eces.core.MapperSpace;
import de.tum.ei.lkn.eces.dnm.proxies.DetServProxy;
import de.tum.ei.lkn.eces.dnm.NCRequestData;
import de.tum.ei.lkn.eces.dnm.mappers.NCRequestDataMapper;
import de.tum.ei.lkn.eces.routing.algorithms.mcsp.astarprune.AStarPruneAlgorithm;
import de.uni_kl.cs.discodnc.curves.CurvePwAffine;
import de.tum.ei.lkn.eces.graph.mappers.EdgeMapper;
import de.tum.ei.lkn.eces.graph.mappers.NodeMapper;
import de.tum.ei.lkn.eces.network.Host;
import de.tum.ei.lkn.eces.network.NetworkNode;
import de.tum.ei.lkn.eces.network.ToNetwork;
import de.tum.ei.lkn.eces.network.mappers.NetworkNodeMapper;
import de.tum.ei.lkn.eces.network.mappers.ToNetworkMapper;
import de.tum.ei.lkn.eces.network.util.NetworkInterface;
import de.tum.ei.lkn.eces.routing.DeleteRequest;
import de.tum.ei.lkn.eces.routing.SelectedRoutingAlgorithm;
import de.tum.ei.lkn.eces.routing.algorithms.RoutingAlgorithm;
import de.tum.ei.lkn.eces.routing.mappers.*;
import de.tum.ei.lkn.eces.routing.requests.RequestName;
import de.tum.ei.lkn.eces.routing.requests.UnicastRequest;
import de.uni_kl.cs.discodnc.numbers.Num;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.graph.Graph;
import de.tum.ei.lkn.eces.graph.Node;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringSystem;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

/**
 *
 * Currently incredibly ugly. We'll clean if we have time, but not very important as it is just the GUI.
 *
 * @author Florian Kreft
 * @author Amaury Van Bemten
 */
@Sharable
public class NettyInboundHandler extends ChannelInboundHandlerAdapter {
	/**
	 * Logger.
	 */
	private final static Logger logger = Logger.getLogger(NettyInboundHandler.class);
	protected NettyHttpFileHandler httpFileHandler = new NettyHttpFileHandler();
	protected WebSocketServerHandshaker handshaker;
	private   StringBuilder frameBuffer = null;

	//keeps track of all channels for easy broadcasting and of witch node-info to update
	private Set<ChannelStateful> allChannels = new CopyOnWriteArraySet<>();

	private ColoringSystem coloringSys;
	private Controller mController;
	private RequestMapper requestMapper;
	Mapper<Node> nodeMapper;
	Mapper<Edge> edgeMapper;
	Mapper<DeleteRequest> deleteRequestMapper;
	protected NCRequestDataMapper ncRequestDataMapper;
	protected SelectedRoutingAlgorithmMapper selectedRoutingAlgorithmMapper;
	protected UnicastRequestMapper unicastRequestMapper;
	protected RequestNameMapper requestNameMapper;

	//keeps track of all graphs created after webgraphguisystem was creates
	Set<Graph> registeredGraphsList = new CopyOnWriteArraySet<>();

	public NettyInboundHandler(Controller controller) {
		this(controller, null);
	}

	public NettyInboundHandler(Controller controller, ColoringSystem passedColSys) {
		coloringSys = passedColSys;
		mController = controller;
		nodeMapper = new NodeMapper(mController);
		edgeMapper = new EdgeMapper(mController);
		requestMapper = new RequestMapper(mController);
		deleteRequestMapper = new DeleteRequestMapper(mController);
		unicastRequestMapper = new UnicastRequestMapper(mController);
		ncRequestDataMapper = new NCRequestDataMapper(mController);
		requestNameMapper = new RequestNameMapper(mController);
		selectedRoutingAlgorithmMapper = new SelectedRoutingAlgorithmMapper(mController);
	}
	//executed on established connection
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		//allChannels.add(ctx.channel());
		ChannelStateful newChannel = new ChannelStateful(ctx.channel());
		allChannels.add(newChannel);
	}

	//executed on received message, splits based on type
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			this.handleHttpRequest(ctx, (FullHttpRequest)msg);
		}
		else if (msg instanceof WebSocketFrame) {
			this.handleWebSocketFrame(ctx, (WebSocketFrame)msg);
		}
	}

	protected boolean handleREST(ChannelHandlerContext ctx, FullHttpRequest req) {
		// check request path here and process any HTTP REST calls
		// return true if message has been processed
		return false;
	}

	protected void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
	  // Handle a bad request.
	  if (!req.getDecoderResult().isSuccess()) {
	     httpFileHandler.sendError(ctx, HttpResponseStatus.BAD_REQUEST);
	     return;
	  }

	  // If you're going to do normal HTTP POST authentication before upgrading the
	  // WebSocket, the recommendation is to handle it right here
	  if (req.getMethod() == HttpMethod.POST) {
	     httpFileHandler.sendError(ctx, HttpResponseStatus.FORBIDDEN);
	     return;
	  }

	  // Allow only GET methods.
	  if (req.getMethod() != HttpMethod.GET) {
	     httpFileHandler.sendError(ctx, HttpResponseStatus.FORBIDDEN);
	     return;
	  }

	  // Send the demo page and favicon.ico
	  if ("/".equals(req.getUri())) {
	 httpFileHandler.sendRedirect(ctx, "/index.html");
	     return;
	  }

	  // check for websocket upgrade request
	  String upgradeHeader = req.headers().get("Upgrade");
	  if (upgradeHeader != null && "websocket".equalsIgnoreCase(upgradeHeader)) {
	 // Handshake. Ideally you'd want to configure your websocket uri
	 String url = "ws://" + req.headers().get("Host") + "/websocket";
	     WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(url, null, false);
	     handshaker = wsFactory.newHandshaker(req);
	     if (handshaker == null) {
	        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
	     } else {
	        handshaker.handshake(ctx.channel(), req);
	     }
	  } else {
	     boolean handled = handleREST(ctx, req);
	     if (!handled) {
	        httpFileHandler.sendFile(ctx, req);
	     }
	  }
	}

	/**
	 * Executed when websocket message completely received
	 * If the WEbGraphGui frontend sends other websocket messages, process them here.
	 * @param ctx ChannelHandlerContext that recieved message, used for sending answers
	 * @param frameText should be message in JSON Format with type and possible additional Information
	 */
	protected void handleMessageCompleted(ChannelHandlerContext ctx, String frameText) {
		//String response ="ERROR: last recieved String could not be processed";
		JSONObject messageObject = new JSONObject(frameText);
		logger.debug("Message Recieved! frameText: "+frameText);
		if (messageObject.getString("type").equals("connected")) {
			sendInitial(ctx);
		}
		else if (messageObject.getString("type").equals("elementClicked")) {
			for (ChannelStateful channelStateful : allChannels) {
				if (channelStateful.getChannel() == ctx.channel()) {
					channelStateful.setActiveEntity(messageObject.getLong("data"));
					sendCurrentEntityObject(channelStateful);
				}
			}
		}
		else if (messageObject.getString("type").equals("activeGraph")) {
			for (ChannelStateful channelStateful : allChannels) {
				if (channelStateful.getChannel() == ctx.channel()) {
					Long graphEntityId = messageObject.getLong("currentGraph");
					channelStateful.setActiveGraph(graphEntityId);
					for(Graph graph : registeredGraphsList) {
						Entity graphEntity = graph.getEntity();
						if (graphEntity.getId() == graphEntityId) {
							String graphString = nodeMapper.createJSONObject(graphEntity).toString();
							channelStateful.getChannel().writeAndFlush(new TextWebSocketFrame(graphString));
							logger.debug("Sent GraphEntity: " + graphString);
							return;
						}
					}
				}
			}
		}
		else if (messageObject.getString("type").equals("getColors")) {
			for (ChannelStateful channelStateful : allChannels) {
				if (channelStateful.getChannel() == ctx.channel()) {
					channelStateful.setPolling(false);
					String schemeName = messageObject.getString("schemeName");
					Long activeGraphEntityID = messageObject.getLong("activeGraphEntityID");
					channelStateful.setActiveColoring(schemeName);
					sendAllColors(channelStateful, activeGraphEntityID);
					Graph graph = null;
					for(Graph value :registeredGraphsList)
						if(value.getId() == channelStateful.getActiveGraph())
							graph = value;
					if (coloringSys.schemeIsPolling(schemeName, graph.getEdges().iterator().next())) {
						channelStateful.setPolling(true);
						startColorDaemon(channelStateful, activeGraphEntityID);
					}
				}
			}
		}
		else if(messageObject.get("type").equals("removeFlow")) {
			Long entityId = Long.valueOf(messageObject.get("entityId").toString());
			deleteRequestMapper.attachComponent(mController.createEntity(), new DeleteRequest(entityId));
		}
		else if(messageObject.get("type").equals("newFlow")) {
			try {
				Long source = Long.valueOf(messageObject.get("source").toString());
				Long destination = Long.valueOf(messageObject.get("destination").toString());
				Double burst = Double.valueOf(messageObject.get("burst").toString());
				Double rate = Double.valueOf(messageObject.get("rate").toString());
				Double deadline = Double.valueOf(messageObject.get("deadline").toString());
				String name = String.valueOf(messageObject.get("name").toString());

				logger.info(String.format("Direct request to add: name=%s source=%d destination=%d burst=%.2fKB rate=%.2fKbps deadline=%.2fms", name, source, destination, burst, rate, deadline));

				Entity entity = mController.createEntity();

				// Getting nodes.
				Node sourceNode = null;
				Node destNode = null;
				for(Graph graph : this.registeredGraphsList) {
					for(Node node : graph.getNodes()) {
						if(node.getEntity().getId() == source)
							sourceNode = node;
						if(node.getEntity().getId() == destination)
							destNode = node;
					}
				}

				if(sourceNode == null) {
					logger.error("Impossible to find a node with entity ID " + source + ". Not creating request!");
					return;
				}

				if(destNode == null) {
					logger.error("Impossible to find a node with entity ID " + destination + ". Not creating request!");
					return;
				}

				if(destNode.getGraph().getEntity().getId() != sourceNode.getGraph().getEntity().getId()) {
					logger.error("The two nodes do not belong to the same graph. Not creating request!");
					return;
				}

				RoutingAlgorithm algorithm = new AStarPruneAlgorithm(mController, true);
				algorithm.setProxy(new DetServProxy(mController));
				try(MapperSpace mapperSpace = mController.startMapperSpace()) {
					this.unicastRequestMapper.attachComponent(entity, new UnicastRequest(sourceNode, destNode));
					this.ncRequestDataMapper.attachComponent(entity, new NCRequestData(CurvePwAffine.getFactory().createTokenBucket(rate * 1000.0 / 8.0,burst * 1000.0), Num.getFactory().create(deadline / 1000.0)));
					this.selectedRoutingAlgorithmMapper.attachComponent(entity, new SelectedRoutingAlgorithm(algorithm));
					this.requestNameMapper.attachComponent(entity, new RequestName(name));
				}

			}
			catch(NumberFormatException e) {
				logger.error("Impossible to add the manually entered flow: requested value is not a number: " + e.getMessage());
			}
		}
		else {
			ctx.channel().writeAndFlush(new TextWebSocketFrame("ERROR: following Message could not be processed: "+frameText));
		}
	}

	/**
	 * Executed on established websocket connection: go through all registered graphs and send nodes + edges;
	 * Send all existing ColorScheme Names
	 * @param ctx ChannelHandlerContext of new connection
	 */
   	protected void sendInitial(ChannelHandlerContext ctx) {
		logger.debug("sendingInitial...");
	   	//send edges and nodes of all graphs
 	  	for (Graph graph : registeredGraphsList) {
			long graphid = graph.getWaitEntity().getId();
			for (Node node : graph.getNodes()) {
				MessageConverter.getNodeMessage(node, graphid, getNodeType(node));
				String nodeString = MessageConverter.getNodeMessage(node, graphid, getNodeType(node));
				ctx.channel().writeAndFlush(new TextWebSocketFrame(nodeString));
			    for (Edge edge : node.getOutgoingConnections()) {
			    	String edgestring = MessageConverter.getEdgeMessage(edge, graphid);
					ctx.channel().writeAndFlush(new TextWebSocketFrame(edgestring));
				}
			}
 	  	}
 	  	//send colorSchemes
 	  	if (coloringSys != null) {
 	 	  	String[] coloringSchemes = coloringSys.getColoringSchemeList();
 	 	  	for (String coloringScheme : coloringSchemes) {
 	 	  		String schemeString = MessageConverter.getColorSchemeMessage(coloringScheme);
				logger.debug(schemeString);
 	 	  		ctx.channel().writeAndFlush(new TextWebSocketFrame(schemeString));
 	 	  	}
 	  	}
 	  	else {
 	 	  	String schemeString = MessageConverter.getColorSchemeMessage("No Coloring System Loaded!");
 	 	  	ctx.channel().writeAndFlush(new TextWebSocketFrame(schemeString));
 	  	}
   	}

	/**
	 * Executed when message of type "nodeClicked" received
	 * @param channelStateful Needed because several instances of the frontend can be open with different activeEntitys
	 */
   	protected void sendCurrentEntityObject(ChannelStateful channelStateful) {

   		Channel channel = channelStateful.getChannel();
   		long currentEntityId = channelStateful.getActiveEntity();
	   for (Graph graph : registeredGraphsList) {
		   String graphString = nodeMapper.createJSONObject(graph.getEntity()).toString();
		   channel.writeAndFlush(new TextWebSocketFrame(graphString));
		   logger.debug("Sent GraphEntity: "+graphString);

			for (Node node : graph.getNodes()) {
			   Entity nodeEntity = node.getWaitEntity();
			   if (nodeEntity.getId() == currentEntityId) {
				   String nodestring = nodeMapper.createJSONObject(nodeEntity).toString();
				   channel.writeAndFlush(new TextWebSocketFrame(nodestring));
				   logger.debug("Sent NodeEntity: "+nodestring);
				   return;
			   }
			   else {
				   for (Edge edge : node.getOutgoingConnections()) {
					   Entity edgeEntity = edge.getWaitEntity();
					   if (edgeEntity.getId() == currentEntityId) {
						   String edgestring = edgeMapper.createJSONObject(edgeEntity).toString();
						   channel.writeAndFlush(new TextWebSocketFrame(edgestring));
						   logger.debug("Sent EdgeEntity: "+edgestring);
						   return;
					   }
				   }
			   }
		   }
	   }
   	}

   	/**
	 * Can be used to send arbitrary error Message
	 * @param channel channel to which to send the error
	 * @param errorTypeString Name of Error
	 * @param errorMsg additional Information
	 */
	protected void sendError(Channel channel, String errorTypeString, String errorMsg) {
		   String response = MessageConverter.getErrorMessage(errorTypeString, errorMsg);
		   channel.writeAndFlush(new TextWebSocketFrame(response));
	}

	/**
	 * Send Color Information of all edges in one graph to one specific frontend
	 * Used if a Color Scheme is clicked in the dropdown of the frontend
	 * @param channelStateful channel to which to send the color values
	 * @param activeGraphId limits messages to only the currently active graph of that specific frontend
	 */
  	protected void sendAllColors(ChannelStateful channelStateful, long activeGraphId) {
    	if (coloringSys == null) {
    		return;
    	}
	   	logger.trace("sendingAllColors...");
 	  	for (Graph graph : registeredGraphsList) {
			long graphid = graph.getWaitEntity().getId();
			if (graphid == activeGraphId) {
				for (Node node : graph.getNodes()) {
				    for (Edge edge : node.getOutgoingConnections()) {
				    	String colSchemeName = channelStateful.getActiveColoring();
				    	String color = coloringSys.getColor(colSchemeName, edge);
				    	if (color != null) {
				    		String colorMessage = MessageConverter.getColorMessage(edge, color);
				    		channelStateful.getChannel().writeAndFlush(new TextWebSocketFrame(colorMessage));
				    	}
					}
				}
			}
 	  	}
   	}

	/**
	 * Send Color of one specific edge to all active frontends, taking into account the respective active ColoringScheme
	 * Used if the Entity of an edge is updated, possibly changing the color value
	 * @param edge edge that should be colored
	 */
	public void sendColorOfEdge(Edge edge) {
		if (coloringSys == null) {
			return;
		}
		logger.debug("sendColorOfEdge()..." + " EdgeId="+edge.getEntity().getId());
		for (ChannelStateful channelStateful : allChannels) {
			String colSchemeName = channelStateful.getActiveColoring();
			String color = coloringSys.getColor(colSchemeName, edge);
			if (color != null) {
				String colorMessage = MessageConverter.getColorMessage(edge, color);
				channelStateful.getChannel().writeAndFlush(new TextWebSocketFrame(colorMessage));
			}
		}
	}

	/**
	 * Send a arbitrary text message as Websocket frame to all established websocket connections (connected frontends)
	 * Mainly used if all frontends need to be updated (e.g. update of edge/node)
	 * @param frameText complete Message text, should be JSON (mostly)
	 */
	public void sendWebsocketFrameToAll(String frameText) {
		if (frameText != null) {
			for (ChannelStateful channelWithActiveEntity : allChannels) {
				channelWithActiveEntity.getChannel().writeAndFlush(new TextWebSocketFrame(frameText));
			}
			if (!allChannels.isEmpty()) {
				logger.debug("Broadcasted Webssocketframe: "+frameText);
			}
		}
	}

   protected void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
	  // Check for closing frame
	  if (frame instanceof CloseWebSocketFrame) {
	     if (frameBuffer != null) {
	         handleMessageCompleted(ctx, frameBuffer.toString());
	     }
	     handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
	     return;
	  }

	  if (frame instanceof PingWebSocketFrame) {
	     ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
	     return;
	  }

	  if (frame instanceof PongWebSocketFrame) {
	     return;
	  }

	  if (frame instanceof TextWebSocketFrame) {
	     frameBuffer = new StringBuilder();
	     frameBuffer.append(((TextWebSocketFrame)frame).text());
	  } else if (frame instanceof ContinuationWebSocketFrame) {
	     if (frameBuffer != null) {
	        frameBuffer.append(((ContinuationWebSocketFrame)frame).text());
	     }
	  } else {
	     throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
	  }

	  // Check if Text or Continuation Frame is final fragment and handle if needed.
      if (frame.isFinalFragment()) {
         handleMessageCompleted(ctx, frameBuffer.toString());
         frameBuffer = null;
      }
   }
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	    // Close the connection when an exception is raised.
	    cause.printStackTrace();
	    ctx.close();
	}

	//following functions called by websocketguisystem whenever new Elements in applications created
	public void componentStateNode(Node newNode, String newStatus) {
		Graph tmpGraph = newNode.getGraph();
		if (!registeredGraphsList.contains(tmpGraph)) {
			registeredGraphsList.add(tmpGraph);
			//logger.debug("Added new Graph, graphid: "+tmpGraph.getWaitEntity().getId());
		}
		long graphid = tmpGraph.getWaitEntity().getId();

		String nodeString = MessageConverter.getNodeMessage(newNode, graphid, newStatus, getNodeType(newNode));
		sendWebsocketFrameToAll(nodeString);
	}

	public void componentStateEdge(Edge newEdge, String newStatus) {
		Graph tmpGraph;
		try {
			tmpGraph = newEdge.getSource().getGraph();
		}
		catch (NullPointerException e){
			logger.warn("ERROR: " +e.getMessage());
			return;
		}

		if (!registeredGraphsList.contains(tmpGraph)) {
			registeredGraphsList.add(tmpGraph);
			//logger.debug("Added new Graph, graphid: "+tmpGraph.getWaitEntity().getId());
		}

		long graphid = tmpGraph.getWaitEntity().getId();
		String edgestring = MessageConverter.getEdgeMessage(newEdge, graphid, newStatus);
		sendWebsocketFrameToAll(edgestring);
	}

	public void addGraph(Graph newGraph) {
		if (!registeredGraphsList.contains(newGraph)) {
			registeredGraphsList.add(newGraph);
			//logger.debug("Added new Graph, graphid: "+newGraph.getWaitEntity().getId());
		}
	}

	public void updatedActiveEntity(long updatedEntityId) {
		for (ChannelStateful channelStateful : allChannels) {
			if (channelStateful.getActiveEntity() == updatedEntityId || channelStateful.getActiveGraph() == updatedEntityId) {
				//logger.debug("Updated ACTIVE Entity! updatedEntityId: " + recievedEntityId+"; currentEntityId: "+channelWithActiveEntity.getActiveEntity());
				sendCurrentEntityObject(channelStateful);
			}
		}
	}


	private class ColorDaemon extends Thread {
		ChannelStateful mChannelStateful;
		long mActiveGraph = 0;
		ColorDaemon (ChannelStateful channelStateful, long activeGraphId) {
			mChannelStateful = channelStateful;
			mActiveGraph = activeGraphId;
			this.setDaemon(true);
		}
		public void run() {
			while (true) {
				if ( (!mChannelStateful.isPolling()) || (mChannelStateful.getActiveGraph() != mActiveGraph) )  {
					return;
				}
				sendAllColors(mChannelStateful, mActiveGraph);
				try {
					sleep(100);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
	protected void startColorDaemon(ChannelStateful channelStateful, long activeGraphId) {
		ColorDaemon colorDaemon = new ColorDaemon(channelStateful, activeGraphId);
		colorDaemon.start();
	}

	private String getNodeType(Node node) {
		String type = "switch";
		NetworkNode networkNode = null;
		NetworkNodeMapper networkNodeMapper = new NetworkNodeMapper(mController);
		ToNetworkMapper toNetworkMapper = new ToNetworkMapper(mController);
		ToNetwork toNetwork = toNetworkMapper.get(node.getEntity());
		if(toNetwork != null) {
			networkNode = networkNodeMapper.get(toNetwork.getNetworkEntity());
		}

		if(networkNode != null) {
			Collection<Host> hosts = networkNode.getNetwork().getHosts();
			for(Host host : hosts)
				for(NetworkInterface ifc : host.getInterfaces())
					if(host.getNetworkNode(ifc) == networkNode)
						type = "interface";
		}

		return type;
	}
}

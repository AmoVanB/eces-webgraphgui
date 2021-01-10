package de.tum.ei.lkn.eces.webgraphgui;


import de.tum.ei.lkn.eces.core.Entity;
import de.tum.ei.lkn.eces.core.Component;
import de.tum.ei.lkn.eces.core.ComponentStatus;
import de.tum.ei.lkn.eces.core.Mapper;
import de.tum.ei.lkn.eces.core.Controller;
import de.tum.ei.lkn.eces.core.RootSystem;
import de.tum.ei.lkn.eces.core.annotations.ComponentBelongsTo;
import de.tum.ei.lkn.eces.core.annotations.ComponentStateIs;
import de.tum.ei.lkn.eces.core.annotations.HasComponent;
import de.tum.ei.lkn.eces.graph.Edge;
import de.tum.ei.lkn.eces.graph.Graph;
import de.tum.ei.lkn.eces.graph.Node;
import de.tum.ei.lkn.eces.graph.mappers.EdgeMapper;
import de.tum.ei.lkn.eces.graph.mappers.GraphMapper;
import de.tum.ei.lkn.eces.webgraphgui.color.ColoringSystem;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * System for the Web Graph GUI that automatically creates edges, etc. when they appear.
 *
 * Currently incredibly ugly. We'll clean if we have time, but not very important as it is just the GUI.
 *
 * @author Florian Kreft
 * @author Jochen Guck
 * @author Amaury Van Bemten
 */
@ComponentBelongsTo(system = WebGraphGuiSystem.class)
public class WebGraphGuiSystem extends RootSystem {

    private int port;

    private EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ServerBootstrap bootstr = new ServerBootstrap(); // (2)
    private NettyInboundHandler myInboundHandler;

	private Mapper<Edge> edgeMapper;
	private Mapper<Graph> graphMapper;
	private ColoringSystem myColSys;

	/**
	 * Creates a new WebGraphGui System and starts webserver + Websocket-interface on standard port 80
	 * @param controller the controller of all running systems.
	 */
	public WebGraphGuiSystem(Controller controller, ColoringSystem colSys) {
		this(controller, colSys, 80);
	}
	/**
	 * Creates a new WebGraphGui System and starts webserver + Websocket-interface
	 * @param controller the controller of all running systems.
	 * @param port port to access webpage from
	 */
	public WebGraphGuiSystem(Controller controller, int port) {
		super(controller);
        this.port = port;
        myColSys = null;
        myInboundHandler = new NettyInboundHandler(controller);
		edgeMapper = new EdgeMapper(controller);
		graphMapper = new GraphMapper(controller);
		try {
			this.run();
		}
		catch (Exception e) {
			logger.error("Exception caught while calling run() in WebGraphGuiSystem, shutting webserver down. ERROR: "+e);
			shutdownServerGracefully();
		}
	}

	public WebGraphGuiSystem(Controller controller, ColoringSystem colSys, int port) {
		super(controller);
        this.port = port;
        myColSys = colSys;
        myInboundHandler = new NettyInboundHandler(controller, colSys);
		edgeMapper = new EdgeMapper(controller);
		try {
			this.run();
		}
		catch (Exception e) {
			logger.error("Exception caught while calling run() in WebGraphGuiSystem, shutting webserver down. ERROR: "+e);
			shutdownServerGracefully();
		}
	}
	/**
	 * Starts the NettyServer
	 */
    public void run() throws Exception {
        bootstr.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class) // (3)
         .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
             @Override
             public void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline p = ch.pipeline();
                 p.addLast("encoder", new HttpResponseEncoder());
	             p.addLast("decoder", new HttpRequestDecoder());
	             p.addLast("aggregator", new HttpObjectAggregator(65536));
	             p.addLast("handler", myInboundHandler);
              }
         })
         .option(ChannelOption.SO_BACKLOG, 128)
         .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        bootstr.bind(port).sync();

        ///The following isnt used here because it waits until the server socket is closed, locking the server!
        //f.channel().closeFuture().sync();
    }
	/**
	 * Use this function if NettyServer is to be shut down
	 */
	public void shutdownServerGracefully() {
    	workerGroup.shutdownGracefully();
    	bossGroup.shutdownGracefully();
	}

	// executed on state event NODE
	@ComponentStateIs(State = ComponentStatus.New)
	public void addNewNode(Node newNode){
		logger.debug("addNewNode; ID: "+newNode.getEntity().getId());
		myInboundHandler.componentStateNode(newNode, "New");
	}
	@ComponentStateIs(State = ComponentStatus.Updated)
	public void updatedNode(Node newNode){
		logger.debug("updateNode; ID: "+newNode.getEntity().getId());
		myInboundHandler.componentStateNode(newNode, "Updated");
	}
	@ComponentStateIs(State = ComponentStatus.Destroyed)
	public void nodeDestroyed(Node newNode){
		logger.debug("nodeDestroyed; ID: "+newNode.getEntity().getId());
		myInboundHandler.componentStateNode(newNode, "Destroyed");
	}

	//executed on state event of EDGE
	@ComponentStateIs(State = ComponentStatus.New)
	public void addNewEdge(Edge newEdge){
		logger.debug("addNewEdge; ID: "+newEdge.getEntity().getId());
		myInboundHandler.componentStateEdge(newEdge, "New");
		myInboundHandler.sendColorOfEdge(newEdge);
	}
	@ComponentStateIs(State = ComponentStatus.Updated)
	public void updatedEdge(Edge newEdge){
		logger.debug("updatedEdge; ID: "+newEdge.getEntity().getId());
		myInboundHandler.componentStateEdge(newEdge, "Updated");
	}
	@ComponentStateIs(State = ComponentStatus.Destroyed)
	public void edgeDestroyed(Edge newEdge){
		logger.debug("edgeDestroyed; ID: "+newEdge.getEntity().getId());
		myInboundHandler.componentStateEdge(newEdge, "Destroyed");
	}

	//executed on state event of GRAPH
	@ComponentStateIs(State = ComponentStatus.New)
	public void addNewGraph(Graph newGraph){
		myInboundHandler.addGraph(newGraph);
	}
	/*
	@ComponentStateIs(State = ComponentStatus.Updated)
	public void updatedGraph(Graph updatedGraph){
		//myInboundHandler.addGraph(updatedGraph);
		logger.debug("Graph UPDATED; EntityID: "+updatedGraph.getEntity().getId());
	}
	*/
	/*
	@ComponentStateIs(State = ComponentStatus.Destroyed)
	public void GraphDestroyed(Graph destroyedGraph){
		//TODO: implement function in inbound handler to send this event and remove graph from graph set
	}
	*/

	// executed on update of any COMPONENT
	@ComponentStateIs(State = ComponentStatus.Updated)
	public void updatedComponent(Component updatedComponent){
		logger.debug("updatedActiveEntity: "+updatedComponent.getClass().getSimpleName());
		Entity updatedEntity = updatedComponent.getEntity();
		long updatedEntityId = updatedEntity.getId();
		Edge edgeOfEntity = edgeMapper.get(updatedEntity);
		myInboundHandler.updatedActiveEntity(updatedEntityId);
		if (edgeOfEntity != null) {
			myInboundHandler.sendColorOfEdge(edgeOfEntity);
		}
	}
}

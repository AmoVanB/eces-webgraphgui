package de.tum.ei.lkn.eces.webgraphgui;

import io.netty.channel.Channel;

/**
 * A channel between the server and a client.
 *
 * @author Florian Kreft
 */
public class ChannelStateful  {
	private Channel channel;
	private long activeEntity = -1; // currently clicked entity
	private long activeGraph = -1; // currently displayed graph
	private String activeColoringScheme = "Colorless"; // current color scheme
	private boolean polling = false;
	
	public ChannelStateful(Channel newChannel) {
		this.channel = newChannel;
	}

	public Channel getChannel() {
		return this.channel;
	}

	public long getActiveEntity() {
		return this.activeEntity;
	}

	public void setActiveEntity(long newActiveEntity) {
		this.activeEntity = newActiveEntity;
	}

	public String getActiveColoring() {
		return this.activeColoringScheme;
	}

	public void setActiveColoring(String newActiveColoring) {
		this.activeColoringScheme = newActiveColoring;
	}
	public long getActiveGraph() {
		return this.activeGraph;
	}

	public void setActiveGraph(long newActiveGraph) {
		this.activeGraph = newActiveGraph;
	}

	public boolean isPolling() {
		return this.polling;
	}

	public void setPolling(boolean newPolling) {
		this.polling = newPolling;
	}
}

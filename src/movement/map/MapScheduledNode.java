/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement.map;

import core.Coord;
import core.SettingsError;

import java.util.List;
import java.util.Vector;

/**
 * A node which works as stop point in the {@link movement.MapScheduledMovement} model.
 * It contains the corresponding node in a SimMap and the scheduled time.
 * Time can be arrival or departure time.
 */
public class MapScheduledNode {
	/** The time the nodes is mentioned in the schedule */
	private double time;
	private MapNode node;

	/**
	 * Constructor. Creates a MapScheduledNode from a MapNode.
	 * @param time The time when the node occurs in the schedule.
	 * @param node The existing MapNode.
	 */
	public MapScheduledNode(double time, MapNode node) {
		this.time = time;
		this.node = node;
	}

	public double getTime() {
		return time;
	}

	public MapNode getNode() {
		return node;
	}

	public MapScheduledNode updateTime(double time) {
		return new MapScheduledNode(time, node);
	}


}

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
 * TODO adapt this class
 * A node in a SimMap. Node has a location, 0-n neighbors that it is
 * connected to and possibly a type identifier.
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

	public void setNode(MapNode node) {
		this.node = node;
	}

	public void setTime(double time) {
		this.time = time;
	}


}

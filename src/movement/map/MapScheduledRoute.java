/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement.map;

import core.Coord;
import core.SettingsError;
import input.ScheduleReader;
import util.Tuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A route that consists of map nodes and timestamps when a host should a arrive at or leave the given point.
 * See {@link ScheduleReader}.
 */
public class MapScheduledRoute {

	private List<MapScheduledNode> stops;
	private int index; // index of the previous returned map node
	private boolean arrived = false;

	/**
	 * Creates a new map route
	 * @param stops The stops of this route in a list
	 */
	public MapScheduledRoute(List<MapScheduledNode> stops) {
		assert stops.size() > 0 : "Route needs stops";
		this.stops = stops;
		this.index = 0;
	}

	/**
	 * Sets the next index for this route
	 * @param index The index to set
	 */
	public void setNextIndex(int index) {
		if (index > stops.size()) {
			index = stops.size();
		}

		this.index = index;
	}

	/**
	 * Returns the number of stops on this route
	 * @return the number of stops on this route
	 */
	public int getNrofStops() {
		return stops.size();
	}

	public List<MapScheduledNode> getStops() {
		return this.stops;
	}

	/**
	 * Returns the next stop on the route
	 * @return the next stop on the route
	 */
	public MapScheduledNode nextStop() {
		if (arrived) { return null; }
		MapScheduledNode next = stops.get(index);
		index++;
		if (index >= stops.size()) { // reached last stop
			arrived = true;
		}

		return next;
	}

	/**
	 * Returns a new route with the same settings
	 * @return a replicate of this route
	 */
	public MapScheduledRoute replicate() {
		return new MapScheduledRoute(stops);
	}

	public String toString() {
		return ("Route with "+ getNrofStops() + " stops starting at " + stops.get(0).getNode().getLocation() +
				"@" +  stops.get(0).getTime() + "s");
	}

	/**
	 * Reads routes from files defined in Settings
	 * @param fileName name of the file where to read routes
	 * @param map SimMap where corresponding map nodes are found
	 * @return A list of MapRoutes that were read
	 */
	public static List<MapScheduledRoute> readRoutes(String fileName,
                                                     SimMap map) {
		List<MapScheduledRoute> routes = new ArrayList<MapScheduledRoute>();
		ScheduleReader reader = new ScheduleReader();
		List<List<Tuple<Double, Coord>>> coords;
		File routeFile = null;
		boolean mirror = map.isMirrored();
		double xOffset = map.getOffset().getX();
		double yOffset = map.getOffset().getY();

		try {
			routeFile = new File(fileName);
			coords = reader.readRoutes(routeFile);
		}
		catch (IOException ioe){
			throw new SettingsError("Couldn't read MapRoute-data file " +
					fileName + 	" (cause: " + ioe.getMessage() + ")");
		}

		for (List<Tuple<Double, Coord>> l : coords) {
			List<MapScheduledNode> nodes = new ArrayList<MapScheduledNode>();
			for (Tuple<Double, Coord> c : l) {
				// make coordinates match sim map data
				if (mirror) {
					c.getValue().setLocation(c.getValue().getX(), -c.getValue().getY());
				}
				c.getValue().translate(xOffset, yOffset);

				MapNode node = map.getNodeByCoord(c.getValue());
				if (node == null) {
					Coord orig = c.getValue().clone();
					orig.translate(-xOffset, -yOffset);
					orig.setLocation(orig.getX(), -orig.getY());

					throw new SettingsError("MapRoute in file " + routeFile +
							" contained invalid coordinate " + c.getValue() + " orig: " +
							orig);
				}
				MapScheduledNode scheduledNode = new MapScheduledNode(c.getKey(), node);
				nodes.add(scheduledNode);
			}

			routes.add(new MapScheduledRoute(nodes));
		}

		return routes;
	}
}

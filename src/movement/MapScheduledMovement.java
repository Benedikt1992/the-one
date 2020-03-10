/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import movement.map.*;
import movement.map.delay.DelayModel;

import java.util.List;

/**
 * Map based movement model that uses predetermined scheduled paths within the map area.
 * Nodes using this model will follow the predetermined schedule and stop moving once the schedule ends.
 * See {@link input.ScheduleReader} for details.
 * The path between 2 stops in the schedule is calculated using {@link DijkstraPathFinder}.
 */
public class MapScheduledMovement extends MapBasedMovement implements
	SwitchableMovement {

	/** Per node group setting used for selecting a route file ({@value}) */
	public static final String ROUTE_FILE_S = "routeFile";

	/** Per node group setting used for selecting the {@link movement.map.delay.DelayModel} to use */
	public static final String DELAY_MODEL_S = "delayModel";
	public static final String DM_PACKAGE = "movement.map.delay.";

	/** node where the last path ended or node next to initial placement */
	protected MapScheduledNode lastMapNode;

	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Prototype's reference to all routes read for the group */
	private List<MapScheduledRoute> allRoutes = null;
	/** next route's index to give by prototype. Index of used route by node if not prototype. */
	private Integer nextRouteIndex = null;
	/** The DelayModel used to calculate delays on the route */
	private DelayModel delayModel;

	/** Route of the movement model's instance */
	private MapScheduledRoute route;

	/** activeTimes tha need to be updated in the ActivenessHandler of the host */
	private double[] updatedActiveTimes = null;

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MapScheduledMovement(Settings settings) {
		super(settings);
		String fileName = settings.getSetting(ROUTE_FILE_S);
		allRoutes = MapScheduledRoute.readRoutes(fileName, getMap());
		nextRouteIndex = 0;
		pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
		delayModel = (DelayModel)settings.createIntializedObject(DM_PACKAGE +
				settings.getSetting(DELAY_MODEL_S));
		this.route = this.allRoutes.get(this.nextRouteIndex).replicate();
		if (this.nextRouteIndex >= this.allRoutes.size()) {
			this.nextRouteIndex = 0;
		}
	}

	/**
	 * Copyconstructor. Gives a route to the new movement model from the
	 * list of routes.
	 * @param proto The MapRouteMovement prototype
	 */
	protected MapScheduledMovement(MapScheduledMovement proto) {
		super(proto);
		MapScheduledRoute route = proto.allRoutes.get(proto.nextRouteIndex).replicate();
		this.route = proto.delayModel.calculateDelay(route);
		this.nextRouteIndex = proto.nextRouteIndex;
		this.allRoutes = proto.allRoutes;

        List<MapScheduledNode> stops = this.route.getStops();
        this.updatedActiveTimes = new double[2];
        this.updatedActiveTimes[0] = stops.get(0).getTime();
		this.updatedActiveTimes[1] = stops.get(stops.size() - 1).getTime();

		/* use the first stop as starting point */
		this.route.setNextIndex(0);

		this.pathFinder = proto.pathFinder;

		proto.nextRouteIndex++; // give routes in order
		if (proto.nextRouteIndex >= proto.allRoutes.size()) {
			proto.nextRouteIndex = 0;
		}
	}

	@Override
	public Path getPath() {
		Path p = new Path(generateSpeed());
		MapScheduledNode to = route.nextStop();
		if (to == null) {
			return null;
		}
		if (to.getNode() == lastMapNode.getNode()) {
			lastMapNode = to;
			return null;
		}
		if (to.getTime() <= lastMapNode.getTime()) {
			return null;
		}

		List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode.getNode(), to.getNode());

		// this assertion should never fire if the map is checked in read phase
		assert nodePath.size() > 0 : "No path from " + lastMapNode.getNode() + " to " +
			to.getNode() + ". The simulation map isn't fully connected";

		double distance = 0;
		for (int i = 0; i < nodePath.size() - 1; i++) {
			MapNode n1 = nodePath.get(i);
			MapNode n2 = nodePath.get(i + 1);
			distance += n1.getLocation().distance(n2.getLocation());
		}
		double duration = to.getTime() - SimClock.getTime();

		for (MapNode node : nodePath) { // create a Path from the shortest path
			p.addWaypoint(node.getLocation());
		}
		p.setSpeed(distance / duration);

		lastMapNode = to;

		return p;
	}

	@Override
	public double nextPathAvailable() {
		return lastMapNode.getTime();
	}

	/**
	 * Returns the first stop on the route
	 */
	@Override
	public Coord getInitialLocation() {
        if (updatedActiveTimes != null) {
        	// TODO update activeTimes with delay model
            this.host.updateActiveness(updatedActiveTimes);
            updatedActiveTimes = null;
        }
		if (lastMapNode == null) {
			lastMapNode = route.nextStop();
		}

		return lastMapNode.getNode().getLocation().clone();
	}

	@Override
	public Coord getLastLocation() {
		if (lastMapNode != null) {
			return lastMapNode.getNode().getLocation().clone();
		} else {
			return null;
		}
	}

	public MapScheduledRoute getSchedule() {
		return allRoutes.get(nextRouteIndex).replicate();
	}


	@Override
	public MapScheduledMovement replicate() {
		return new MapScheduledMovement(this);
	}

	/**
	 * Returns the list of stops on the route
	 * @return The list of stops
	 */
	public List<MapScheduledNode> getStops() {
		return route.getStops();
	}
}

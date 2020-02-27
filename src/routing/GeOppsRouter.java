/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import movement.MapScheduledMovement;
import movement.MovementModel;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;
import util.Tuple;

import java.util.*;

/**
 * GeOpps message router from
 */
public class GeOppsRouter extends ActiveRouter {
	/** GeOpps router's settings name space ({@value})*/
	public static final String GEOPPS_NS = "GeOppsRouter";
	/** identifier for the message keeping setting ({@value})*/
	public static final String KEEP_MSG = "keepMessages";
	public static final String DIRECT_DISTANCE = "directDistance";
	public static final String STOPS_ONLY = "stopsOnly";

	protected boolean keepMessages;
	protected boolean directDistance;
	protected boolean stopsOnly;

	protected Map<String, Double> estimatedDeliveryTimes;
	protected DijkstraPathFinder pathFinder;


	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public GeOppsRouter(Settings s) {
		super(s);
		//TODO establish settings names space (see spray and wait router)
		Settings geoppsSettings = new Settings(GEOPPS_NS);

		keepMessages = geoppsSettings.getBoolean(KEEP_MSG);
		directDistance = geoppsSettings.getBoolean(DIRECT_DISTANCE);
		stopsOnly = geoppsSettings.getBoolean(STOPS_ONLY);
		estimatedDeliveryTimes = new HashMap<>();
		pathFinder = new DijkstraPathFinder(null);
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GeOppsRouter(GeOppsRouter r) {
		super(r);
		this.keepMessages = r.keepMessages;
		this.directDistance = r.directDistance;
		this.stopsOnly = r.stopsOnly;
		this.estimatedDeliveryTimes = new HashMap<>();
		this.pathFinder = r.pathFinder;
		//TODO: is there something we need to copy (global stuff)
	}

	@Override
	public boolean createNewMessage(Message m) {
		boolean succeeded = super.createNewMessage(m);
		if (succeeded) {
			Double deliveryTime = findDeliveryEstimation(m.getTo(), getHost());
			estimatedDeliveryTimes.put(m.getId(), deliveryTime);
		}
		return succeeded;
	}

	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		List<Tuple<Message, Connection>> sendableMessages = sortByQueueMode(getSendableMessages());
		this.tryMessagesForConnected(sendableMessages);

	}

	private List<Tuple<Message, Connection>> getSendableMessages() {
		Collection<Message> messages = getMessageCollection();
		List<Connection> connections = getConnections();
		HashMap<Message, Tuple<Double, Connection>> deliveryTimes = new HashMap<>();

		/* Find shortest possible delivery time for each message */
		for (Message m : messages) {
			for (Connection c : connections) {
				Double deliveryTime = findDeliveryEstimation(m.getTo(), c.getOtherNode(getHost()));
				if (deliveryTime < deliveryTimes.getOrDefault(m, new Tuple<>(Double.MAX_VALUE, null)).getKey()) {
					deliveryTimes.put(m, new Tuple<>(deliveryTime, c));
				}
			}
		}

		/* check if shortest possible delivery times are shorter than the own estimation */
		List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
		for (HashMap.Entry<Message, Tuple<Double, Connection>> entry :
				deliveryTimes.entrySet()) {
			if (entry.getValue().getKey() < estimatedDeliveryTimes.get(entry.getKey().getId())) {
				sendableMessages.add(new Tuple<>(entry.getKey(), entry.getValue().getValue()));
			}
		}

		return sendableMessages;
	}

	private Double findDeliveryEstimation(DTNHost destination, DTNHost transportNode) {
		Double estimatedTime = Double.MAX_VALUE;
		MovementModel mmodel = transportNode.getMovement();
		if (mmodel instanceof MapScheduledMovement) {
			MapNode dstNode = ((MapScheduledMovement)mmodel).getMap().getNodeByCoord(destination.getLocation());
			if (dstNode == null) {
				throw new SimError("Host " + destination.toString() + " is not located within the simulation map.");
			}
			MapScheduledRoute schedule = ((MapScheduledMovement)mmodel).getSchedule();
			List<MapScheduledNode> stops = schedule.getStops();
			MapScheduledNode first = schedule.getStop(0);
			MapScheduledNode second = schedule.getStop(1);
			double distance = mapDistance(second.getNode(), first.getNode());
			double duration = second.getTime() - first.getTime();
			double speed = distance / duration;
			double currentTime = SimClock.getTime();
			for (MapScheduledNode stop : stops) {
				if (stop.getTime() > currentTime) {
					if (directDistance) {
						distance = directDistance(dstNode, stop.getNode());
					} else {
						distance = mapDistance(dstNode, stop.getNode());
					}
					double possibleTime = stop.getTime() + distance / speed;
					if (possibleTime < estimatedTime) { estimatedTime = possibleTime; }
				}
			}

		} else {
			/* TODO implement DeliveryEstimaton for other MovementModels using the Path object of the host. */
		}


		return estimatedTime;
	}

	private double mapDistance(MapNode dst, MapNode from) {
		double distance = 0;
		List<MapNode> nodePath = pathFinder.getShortestPath(from, dst);
		for (int i = 0; i < nodePath.size() - 1; i++) {
			MapNode n1 = nodePath.get(i);
			MapNode n2 = nodePath.get(i + 1);
			distance += n1.getLocation().distance(n2.getLocation());
		}
		return distance;
	}

	private double directDistance(MapNode dst, MapNode from) {
		double distance;
		Coord dst_loc = dst.getLocation();
		double x = dst_loc.getX() - from.getLocation().getX();
		double y = dst_loc.getY() - from.getLocation().getY();
		distance = Math.sqrt(x*x + y*y);
		return distance;
	}

	@Override
	protected void transferDone(Connection con) {
		// TODO remove delivered messages
		  if (!keepMessages) {
			List<Message> messages = con.getMessage();
			for (Message m : messages) {
				deleteMessage(m.getId(), false);
				estimatedDeliveryTimes.remove(m.getId());
			}
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m =  super.messageTransferred(id, from);

		Double deliveryTime = findDeliveryEstimation(m.getTo(), getHost());
		estimatedDeliveryTimes.put(m.getId(), deliveryTime);
		return m;
	}

	@Override
	public GeOppsRouter replicate() {
		return new GeOppsRouter(this);
	}

}

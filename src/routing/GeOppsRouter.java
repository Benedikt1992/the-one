/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import movement.MapScheduledMovement;
import movement.MovementModel;
import movement.StationaryListMovement;
import movement.StationaryMovement;
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
    public static final String STOP_BUFER = "stopBuffer";

	protected boolean keepMessages;
	protected boolean directDistance;
	protected boolean stopsOnly;
	protected boolean stopBuffer;

	protected Map<String, Double> estimatedDeliveryTimes;
	protected DijkstraPathFinder pathFinder;
	protected Map<String, Double> distanceCache;
	protected Map<String, Double> messageDeadlines;
	protected Set<String> keepMessage;


	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public GeOppsRouter(Settings s) {
		super(s);
		Settings geoppsSettings = new Settings(GEOPPS_NS);

		keepMessages = geoppsSettings.getBoolean(KEEP_MSG);
		directDistance = geoppsSettings.getBoolean(DIRECT_DISTANCE);
		stopsOnly = geoppsSettings.getBoolean(STOPS_ONLY);
        stopBuffer = geoppsSettings.getBoolean(STOP_BUFER);
		estimatedDeliveryTimes = new HashMap<>();
		pathFinder = new DijkstraPathFinder(null);
		distanceCache = new HashMap<>();
        messageDeadlines = new HashMap<>();
        keepMessage = new HashSet<>();
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
        this.stopBuffer = r.stopBuffer;
		this.estimatedDeliveryTimes = new HashMap<>();
		this.pathFinder = r.pathFinder;
		this.distanceCache = r.distanceCache;
        this.messageDeadlines = new HashMap<>();
        this.keepMessage = new HashSet<>();
	}

	@Override
	public boolean createNewMessage(Message m) {
		boolean succeeded = super.createNewMessage(m);
		if (succeeded) {
            Tuple<Double, Double> deliveryTime = findDeliveryEstimation(m);
			estimatedDeliveryTimes.put(m.getTo().toString(), deliveryTime.getValue());
			messageDeadlines.put(m.getTo().toString(), deliveryTime.getKey());
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
		if (this.tryMessagesForConnected(sendableMessages) != null) {
		    return; // started a transfer, don't try others (yet)
        }

		if (stopBuffer) {
		    // Look if some messages exceeded their deadline (not TTL)
            List<Message> messages = getExceededMessages();
            if( messages.size() == 0) {
                return;
            }

			for (Message m : messages) {
				keepMessage.add(m.getId());
			}

            List<Connection> connections = getStationaryNodeConnections();
            Connection con = tryMessagesToConnections(messages, connections);
            if (con!= null) {
                List<Message> started = con.getMessage();
            }
        }

	}

    private List<Connection> getStationaryNodeConnections() {
        List<Connection> connections = new ArrayList<>();
        for (Connection con : getConnections()) {
            MovementModel mm = con.getOtherNode(getHost()).getMovement();
            if (mm instanceof StationaryListMovement ||
                mm instanceof StationaryMovement) {
                connections.add(con);
            }
        }
        return connections;
    }

    private List<Message> getExceededMessages() {
        List<Message> messages = new ArrayList<>();
        double cTime = SimClock.getTime();
        for (Message m : getMessageCollection()) {
            if (messageDeadlines.get(m.getTo().toString()) < cTime) {
                messages.add(m);
            }
        }
        return messages;
    }

    private List<Tuple<Message, Connection>> getSendableMessages() {
		Collection<Message> messages = getMessageCollection();
		List<Connection> connections = getConnections();
		HashMap<Message, Tuple<Double, Connection>> deliveryTimes = new HashMap<>();
		SortedSet<Message> deliveryKeys = new TreeSet<>();

		/* Find shortest possible delivery time for each message */
		for (Message m : messages) {
			for (Connection c : connections) {
				DTNHost otherNode = c.getOtherNode(getHost());
				MessageRouter router = otherNode.getRouter();
				Tuple<Double, Double> deliveryTime;
				if (router instanceof GeOppsRouter) {
					deliveryTime = ((GeOppsRouter) router).findDeliveryEstimation(m);
				} else {
					deliveryTime = new Tuple<>(Double.MAX_VALUE, Double.MAX_VALUE);
				}
				if (deliveryTime.getValue() < deliveryTimes.getOrDefault(m, new Tuple<>(Double.MAX_VALUE, null)).getKey()) {
					deliveryTimes.put(m, new Tuple<>(deliveryTime.getValue(), c));
					deliveryKeys.add(m);
				}
			}
		}

		/* check if shortest possible delivery times are shorter than the own estimation */
		List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
		for (Message m: deliveryKeys) {
			if (deliveryTimes.get(m).getKey() < estimatedDeliveryTimes.get(m.getTo().toString())) {
				sendableMessages.add(new Tuple<>(m, deliveryTimes.get(m).getValue()));
			}
		}

		return sendableMessages;
	}

	private Tuple<Double, Double> findDeliveryEstimation(Message message) {
		Double deadline = messageDeadlines.getOrDefault(message.getTo().toString(), null);
		double cTime = SimClock.getTime();
		// TODO use location instead of message as key! (messages with same location as destination...)
		if (deadline != null && deadline >= cTime) {
			Double deliveryTime = estimatedDeliveryTimes.get(message.getTo().toString());
			return new Tuple<>(deadline, deliveryTime);
		}

		DTNHost destination = message.getTo();
		DTNHost transportNode = getHost();
		Tuple<Double,Double> estimatedTime = new Tuple<>(Double.MAX_VALUE, Double.MAX_VALUE);
		MovementModel mmodel = transportNode.getMovement();
		if (mmodel instanceof MapScheduledMovement) {
			MapNode dstNode;
			if (destination.getMovement() instanceof StationaryListMovement) {
				dstNode = ((StationaryListMovement) destination.getMovement()).getMapLocation();
			} else {
				// Search for the MapNode. This takes a long time!
				dstNode = ((MapScheduledMovement)mmodel).getMap().getNodeByCoord(destination.getLocation());
				if (dstNode == null) {
					throw new SimError("Host " + destination.toString() + " is not located within the simulation map.");
				}
			}

			MapScheduledRoute schedule = ((MapScheduledMovement)mmodel).getSchedule();
			if (stopsOnly) {
				estimatedTime = stationDeliveryEstimation(dstNode, schedule);
			} else {
				estimatedTime = routeDeliveryEstimation(dstNode, schedule);
			}

		} else {
			/* TODO implement DeliveryEstimation for other MovementModels using the Path object of the host. */
		}

		messageDeadlines.put(message.getTo().toString(), estimatedTime.getKey());
		estimatedDeliveryTimes.put(message.getTo().toString(), estimatedTime.getValue());
		return estimatedTime;
	}

	private Tuple<Double, Double> routeDeliveryEstimation(MapNode dstNode, MapScheduledRoute schedule) {
        Tuple<Double, Double> estimatedTime = new Tuple<>(Double.MAX_VALUE, Double.MAX_VALUE);
		List<MapScheduledNode> stops = schedule.getStops();
		double currentTime = SimClock.getTime();
		for (int i = 0; i < stops.size() - 1; i++) {
			if (stops.get(i).getTime() > currentTime) {
				List<MapNode> path = pathFinder.getShortestPath(stops.get(i).getNode(), stops.get(i+1).getNode());
				double distance = mapDistance(stops.get(i+1).getNode(), stops.get(i).getNode());
				double duration = stops.get(i+1).getTime() - stops.get(i).getTime();
				double speed = distance / duration;
				double navigatedDistance = 0.0;
				for (int j = 0; j < path.size(); j++) {
					if (j>0) {
						double x = path.get(j).getLocation().getX() - path.get(j-1).getLocation().getX();
						double y = path.get(j).getLocation().getY() - path.get(j-1).getLocation().getY();
						navigatedDistance += Math.sqrt(x*x + y*y);
					}
					double possibleTime;
					if (directDistance) {
						distance = directDistance(dstNode, path.get(j));
					} else {
						distance = mapDistance(dstNode, path.get(j));
					}

					possibleTime = stops.get(i).getTime() + navigatedDistance/speed + distance / speed;
					if (possibleTime < estimatedTime.getValue()) {
					    estimatedTime = new Tuple<>(stops.get(i).getTime(), possibleTime);
					}
				}
			}
		}
		return estimatedTime;
	}

	private Tuple<Double, Double> stationDeliveryEstimation(MapNode dstNode, MapScheduledRoute schedule) {
        Tuple<Double, Double> estimatedTime = new Tuple<>(Double.MAX_VALUE, Double.MAX_VALUE);
		List<MapScheduledNode> stops = schedule.getStops();
		MapScheduledNode first = schedule.getStop(0);
		MapScheduledNode second = schedule.getStop(1);
		double distance = mapDistance(second.getNode(), first.getNode());
		double duration = second.getTime() - first.getTime();
		double speed = distance / duration;
		double currentTime = SimClock.getTime();
		for (MapScheduledNode stop : stops) {
			if (stop.getTime() > currentTime) {
				double possibleTime;
				if (directDistance) {
					distance = directDistance(dstNode, stop.getNode());
				} else {
					distance = mapDistance(dstNode, stop.getNode());
				}
				possibleTime = stop.getTime() + distance / speed;
				if (possibleTime < estimatedTime.getValue()) {
				    estimatedTime = new Tuple<>(stop.getTime(), possibleTime);
				}
			}
		}
		return estimatedTime;
	}

	private double mapDistance(MapNode dst, MapNode from) {
		String key = dst.getLocation().toString() + from.getLocation().toString();
		double distance = distanceCache.getOrDefault(key, 0.0);

		if (distance > 0) {
			return distance;
		}

		List<MapNode> nodePath = pathFinder.getShortestPath(from, dst);
		for (int i = 0; i < nodePath.size() - 1; i++) {
			MapNode n1 = nodePath.get(i);
			MapNode n2 = nodePath.get(i + 1);
			distance += n1.getLocation().distance(n2.getLocation());
		}
		distanceCache.put(key,distance);
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
			    if (!keepMessage.contains(m.getId())) {
                    deleteMessage(m.getId(), false);
                } else {
                    Tuple<Double, Double> deliveryTime = findDeliveryEstimation(m);
                    estimatedDeliveryTimes.put(m.getTo().toString(), deliveryTime.getValue());
                    messageDeadlines.put(m.getTo().toString(), deliveryTime.getKey());
                }
			}
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m =  super.messageTransferred(id, from);

//        Tuple<Double, Double> deliveryTime = findDeliveryEstimation(m);
//		estimatedDeliveryTimes.put(m.getId(), deliveryTime.getValue());
//		messageDeadlines.put(m.getId(), deliveryTime.getKey());
		return m;
	}

	@Override
	public GeOppsRouter replicate() {
		return new GeOppsRouter(this);
	}

}

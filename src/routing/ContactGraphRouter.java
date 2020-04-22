/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import movement.MapScheduledMovement;
import movement.MovementModel;
import movement.StationaryListMovement;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;
import routing.contactgraph.ContactGraph;
import routing.contactgraph.ContactGraphEdge;
import routing.contactgraph.ContactGraphNode;
import routing.mobyspace.ScheduledMapMobySpace;
import util.Range;
import util.Tuple;

import java.util.*;

/**
 * MobySpace message router.
 * This implementation of MobySpace is designed to run with the {@link MapScheduledMovement}
 * and {@link StationaryListMovement} model.
 */
public class ContactGraphRouter extends ActiveRouter {
	/** GeOpps router's settings name space ({@value})*/
	public static final String CONTACT_GRAPH_NS = "ContactGraphRouter";

	/** Message property key */
	public static final String MSG_ROUTE_PROPERTY = CONTACT_GRAPH_NS + "." + "route";
	public static final String MSG_ROUTE_INDEX_PROPERTY = CONTACT_GRAPH_NS + "." + "routeIndex";


	protected ContactGraph graph;
	private boolean isStationary;
	private Set<Message> unroutedMessages;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ContactGraphRouter(Settings s) {
		super(s);
		Settings contactSettings = new Settings(CONTACT_GRAPH_NS);
		this.graph = ContactGraph.getInstance();
		this.unroutedMessages = new HashSet<>();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ContactGraphRouter(ContactGraphRouter r) {
		super(r);
		this.graph = r.graph;
		this.unroutedMessages = new HashSet<>();
	}

	@Override
	public void init(DTNHost host, List<MessageListener> mListeners) {
		super.init(host, mListeners);
		MovementModel mModel = this.getHost().getMovement();
		if (mModel instanceof MapScheduledMovement) {
			this.isStationary = false;
			List<MapScheduledNode> schedule = ((MapScheduledMovement) mModel).getSchedule().getStops();
			ContactGraphEdge previousEdge = null;
			for (int i = 1; i < schedule.size(); i++) {
				MapScheduledNode prevEntry = schedule.get(i-1);
				MapScheduledNode cEntry = schedule.get(i);
				if (!prevEntry.getNode().equals(cEntry.getNode())) {
					ContactGraphEdge newEdge = new ContactGraphEdge(prevEntry.getNode(), prevEntry.getTime(),
							cEntry.getNode(), cEntry.getTime(), host.getAddress(), previousEdge);
					this.graph.addEdge(newEdge);
					previousEdge = newEdge;
				}
			}
		} else if (mModel instanceof StationaryListMovement) {
			this.isStationary = true;
			ContactGraphNode newNode = new ContactGraphNode(host.getAddress(), ((StationaryListMovement) mModel).getMapLocation());
			this.graph.addNode(newNode);
		} else {
			throw new RuntimeException("The simulation scenario contains unsupported movement models for ContactGraphRouting.");
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		Integer routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);
		if (!isStationary) {
			if (routeIndex != null) {
				m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, ++routeIndex);
			} else {
				unroutedMessages.add(m);
			}
		}
		return m;
	}

	private List<Tuple<Double, Integer>> findRoute(Message m, Integer from) {
		LinkedList<ContactGraphEdge> route = this.graph.getNearestRoute(from, m.getTo().getAddress(), SimClock.getTime());

		if (route == null) {
			return null;
		}
		List<Tuple<Double,Integer>> simpleRoute = new ArrayList<>();
		Integer previousAddress = null;
		for (ContactGraphEdge hop : route) {
			if (previousAddress == null || previousAddress != hop.getAddress()) {
				simpleRoute.add(new Tuple<>(hop.getDeparture(), hop.getAddress()));
			}
			previousAddress = hop.getAddress();
		}
		return simpleRoute;
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

	}

	@Override
	public boolean createNewMessage(Message m) {
		this.graph.calculateRoutesTo(m.getTo().getAddress());
		this.unroutedMessages.add(m);
		return super.createNewMessage(m);
	}

	private List<Tuple<Message, Connection>> getSendableMessages() {
		double cTime = SimClock.getTime();
		if (isStationary) {
			List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
			for (Connection c : getConnections()) {
				MessageRouter otherRouter = c.getOtherNode(getHost()).getRouter();
				if (otherRouter instanceof ContactGraphRouter) {
					// TODO delete set of unrouted Messages?
					for (Message m : getMessageCollection()) {
						List<Tuple<Double,Integer>> route = (List<Tuple<Double,Integer>>) m.getProperty(MSG_ROUTE_PROPERTY);
						Integer routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);
						if (route == null) {
							sendableMessages.add(new Tuple<>(m, c));
						}
						if (routeIndex != null && routeIndex < route.size()) {
							if (route != null && route.get(routeIndex).getValue() == c.getOtherNode(getHost()).getAddress()) {
								// TODO check if the first hop is already missed??
								sendableMessages.add(new Tuple<>(m, c));
							}
						}
					}
				}
			}
			return sendableMessages;
		} else {
			List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
			for (Connection c: getConnections()) {
				MessageRouter otherRouter = c.getOtherNode(getHost()).getRouter();
				if (otherRouter instanceof ContactGraphRouter) {
					for (Message m: getMessageCollection()) {
						List<Tuple<Double,Integer>> route = (List<Tuple<Double,Integer>>) m.getProperty(MSG_ROUTE_PROPERTY);
						Integer routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);
						if (route == null || routeIndex < route.size()) {
							if ((route == null || route.get(routeIndex).getKey() < cTime) && ((ContactGraphRouter) otherRouter).isStationary) {
								List<Tuple<Double,Integer>> newRoute = findRoute(m, c.getOtherNode(getHost()).getAddress());
								Integer newIndex = 0;
								if (newRoute != null) {
									m.updateProperty(MSG_ROUTE_PROPERTY, newRoute);
									m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, newIndex);
									if (newRoute.get(0).getValue() != getHost().getAddress()) {
										sendableMessages.add(new Tuple<>(m, c));
									} else {
										m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, ++newIndex);
									}
								}
							} else if (route != null && route.get(routeIndex).getValue() == c.getOtherNode(getHost()).getAddress()) {
								sendableMessages.add(new Tuple<>(m, c));
							}
						}
					}
				}
			}
			return  sendableMessages;
		}
	}

	public boolean isStationary() {
		return isStationary;
	}

	@Override
	protected void transferDone(Connection con) {
		List<Message> messages = con.getMessage();
		for (Message m : messages) {
			deleteMessage(m.getId(), false);
			this.unroutedMessages.remove(m);
		}
	}

	@Override
	public ContactGraphRouter replicate() {
		return new ContactGraphRouter(this);
	}

}

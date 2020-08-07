/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import movement.MapScheduledMovement;
import movement.MovementModel;
import movement.StationaryListMovement;
import routing.contactgraph.ContactGraph;
import util.Tuple;

import java.util.*;

/**
 * MobySpace message router.
 * This implementation of MobySpace is designed to run with the {@link MapScheduledMovement}
 * and {@link StationaryListMovement} model.
 */
public class GraphRouter extends ActiveRouter {
	/** CGR router's settings name space ({@value})*/
	public static final String CONTACT_GRAPH_NS = "GraphRouter";
	/** which graph type should be used for calculating routes */
	public static final String CONTACT_GRAPH_TYPE = "graph";

	/** Message property key */
	public static final String MSG_ROUTE_PROPERTY = CONTACT_GRAPH_NS + "." + "route";
	public static final String MSG_ROUTE_INDEX_PROPERTY = CONTACT_GRAPH_NS + "." + "routeIndex";



	protected ContactGraph graph;
	private boolean isStationary;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public GraphRouter(Settings s) {
		super(s);
		Settings contactSettings = new Settings(CONTACT_GRAPH_NS);
		String graphType = contactSettings.getSetting(CONTACT_GRAPH_TYPE);
		if (graphType.equals("ScheduleGraph")) {
			this.graph = ContactGraph.initializeScheduleGraph(contactSettings);
		} else if (graphType.equals("ContactPlanGraph")) {
			this.graph = ContactGraph.initializeContactPlanGraph(contactSettings);
		} else {
			throw new SettingsError(graphType + " is not implemented.");
		}
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GraphRouter(GraphRouter r) {
		super(r);
		this.graph = r.graph;
	}

	@Override
	public void init(DTNHost host, List<MessageListener> mListeners) {
		super.init(host, mListeners);
		MovementModel mModel = this.getHost().getMovement();
		if (mModel instanceof MapScheduledMovement) {
			this.isStationary = false;
			this.graph.addNode(host.getAddress());
		} else if (mModel instanceof StationaryListMovement) {
			this.isStationary = true;
			this.graph.addNode(host.getAddress(), ((StationaryListMovement) mModel).getMapLocation());
		} else {
			throw new RuntimeException("The simulation scenario contains unsupported movement models for ContactGraphRouting.");
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);
		List<Tuple<Double,Integer>> route = (List<Tuple<Double,Integer>>) m.getProperty(MSG_ROUTE_PROPERTY);
		Integer routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);
		if (routeIndex != null && routeIndex < route.size() && route.get(routeIndex).getValue() == getHost().getAddress()) {
			m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, ++routeIndex);
		} else if (routeIndex == null) {
			replaceAssociatedRoute(m, getHost().getAddress());
		}

		return m;
	}

	private List<Tuple<Double, Integer>> findRoute(Message m, Integer from) {
		LinkedList<Tuple<Double, Integer>> route = this.graph.getNearestRoute(from, m.getTo().getAddress(), SimClock.getTime());

		if (route == null) {
			return null;
		}
		List<Tuple<Double,Integer>> simpleRoute = new ArrayList<>();
		Integer previousAddress = null;
		for (Tuple<Double, Integer> hop : route) {
			if (previousAddress == null || !previousAddress.equals(hop.getValue())) {
				simpleRoute.add(hop);
			}
			previousAddress = hop.getValue();
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
		replaceAssociatedRoute(m, getHost().getAddress());
		return super.createNewMessage(m);
	}

	private boolean replaceAssociatedRoute(Message m, Integer startHost) {
		List<Tuple<Double,Integer>> newRoute = findRoute(m, startHost);
		Integer newIndex = 0;
		if (newRoute != null) {
			m.updateProperty(MSG_ROUTE_PROPERTY, newRoute);
			m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, newIndex);
			return true;
		}
		return false;
	}

	private List<Tuple<Message, Connection>> getSendableMessages() {
		double cTime = SimClock.getTime();
		List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
		for (Connection c : getConnections()) {
			MessageRouter otherRouter = c.getOtherNode(getHost()).getRouter();
			if (otherRouter instanceof GraphRouter) {
				for (Message m : getMessageCollection()) {
					List<Tuple<Double,Integer>> route = (List<Tuple<Double,Integer>>) m.getProperty(MSG_ROUTE_PROPERTY);
					Integer routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);
					if (route == null) {
						if (replaceAssociatedRoute(m, c.getOtherNode(getHost()).getAddress())) {
							route = (List<Tuple<Double,Integer>>) m.getProperty(MSG_ROUTE_PROPERTY);
							routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);

							if (route.get(routeIndex).getValue() != getHost().getAddress()) {
								sendableMessages.add(new Tuple<>(m, c));
							} else {
								m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, ++routeIndex);
							}
						} else if (isStationary && !((GraphRouter) otherRouter).isStationary) {
							sendableMessages.add(new Tuple<>(m, c));
						}
					} else if (routeIndex < route.size()) {
						if (route.get(routeIndex).getValue() == c.getOtherNode(getHost()).getAddress()) {
							sendableMessages.add(new Tuple<>(m, c));
						} else if (route.get(routeIndex).getKey() < cTime) {
							if (replaceAssociatedRoute(m, c.getOtherNode(getHost()).getAddress())) {
								route = (List<Tuple<Double,Integer>>) m.getProperty(MSG_ROUTE_PROPERTY);
								routeIndex = (Integer) m.getProperty(MSG_ROUTE_INDEX_PROPERTY);

								if (route.get(routeIndex).getValue() != getHost().getAddress()) {
									sendableMessages.add(new Tuple<>(m, c));
								} else {
									m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, ++routeIndex);
								}
							} else if (isStationary && !((GraphRouter) otherRouter).isStationary) {
								sendableMessages.add(new Tuple<>(m, c));
							}
						}
					}
				}
			}
		}
		return  sendableMessages;
	}

	public boolean isStationary() {
		return isStationary;
	}

	@Override
	protected void transferDone(Connection con) {
		List<Message> messages = con.getMessage();
		for (Message m : messages) {
			deleteMessage(m.getId(), false);
		}
	}

	@Override
	public GraphRouter replicate() {
		return new GraphRouter(this);
	}

}

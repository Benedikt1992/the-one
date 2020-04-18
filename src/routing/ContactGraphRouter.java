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

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ContactGraphRouter(Settings s) {
		super(s);
		Settings contactSettings = new Settings(CONTACT_GRAPH_NS);
		this.graph = ContactGraph.getInstance();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ContactGraphRouter(ContactGraphRouter r) {
		super(r);
		this.graph = r.graph;
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
		if (isStationary) {
			List<Tuple<Double,Integer>> newRoute = findRoute(m);
			Integer newIndex = 0;
			if (newRoute == null) {
				newIndex = null;
			}
			m.updateProperty(MSG_ROUTE_PROPERTY, newRoute);
			m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, newIndex);
		} else {
			if (routeIndex != null) {
				m.updateProperty(MSG_ROUTE_INDEX_PROPERTY, ++routeIndex);
			}
		}
		return m;
	}

	private List<Tuple<Double, Integer>> findRoute(Message m) {
		LinkedList<ContactGraphEdge> route = this.graph.getNearestRoute(getHost().getAddress(), m.getTo().getAddress(), SimClock.getTime());

		if (route == null) {
			return null;
		}
		List<Tuple<Double,Integer>> simpleRoute = new ArrayList<>();
		for (ContactGraphEdge hop : route) {
			simpleRoute.add(new Tuple<>(hop.getDeparture(), hop.getAddress()));
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
		return super.createNewMessage(m);
	}

	private List<Tuple<Message, Connection>> getSendableMessages() {
		// TODO find messages which can be transferredto the next hop
//		Collection<Message> messages = getMessageCollection();
//		List<Connection> connections = getConnections();
//		HashMap<Message, Tuple<Double, Connection>> distances = new HashMap<>();
//
//		/* Find shortest possible delivery time for each message */
//		for (Message m : messages) {
//			double minDistance = Double.MAX_VALUE;
//			if(keepMessage || (int)m.getProperty(MSG_FORWARD_PROPERTY) > 0) {
//				Connection minConnection = null;
//				for (Connection c : connections) {
//					DTNHost otherNode = c.getOtherNode(getHost());
//					Double distance;
//					distance = this.space.distance(otherNode.getAddress(),
//							m.getTo().getAddress());
//					if (distance < minDistance) {
//						minConnection = c;
//						minDistance = distance;
//					}
//				}
//				distances.put(m, new Tuple<>(minDistance, minConnection));
//			}
//		}

		/* check if shortest possible delivery times are shorter than the own estimation */
		List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
//		for (HashMap.Entry<Message, Tuple<Double, Connection>> entry :
//				distances.entrySet()) {
//			double distance = this.space.distance(getHost().getAddress(), entry.getKey().getTo().getAddress());
//			if (entry.getValue().getKey() < distance) {
//				Message m = entry.getKey();
//				DTNHost h = entry.getValue().getValue().getOtherNode(getHost());
//				if ( 	(int)m.getProperty(MSG_FORWARD_PROPERTY) > 0 ||
//						(keepMessage && space.getDeliveryTime(h.getAddress(), m.getTo().getAddress()) < (double)m.getProperty(MSG_DELIVERY_PROPERTY))
//				) {
//					sendableMessages.add(new Tuple<>(entry.getKey(), entry.getValue().getValue()));
//				}
//			}
//		}

		return sendableMessages;
	}


	@Override
	protected void transferDone(Connection con) {
		//TODO on the sending note. probabyl just remove the message
//		List<Message> messages = con.getMessage();
//		DTNHost h = con.getOtherNode(getHost());
//		for (Message m : messages) {
//			int nrofCopies = (int)m.getProperty(MSG_FORWARD_PROPERTY);
//			if(keepMessage) {
//				double deliveryTime = space.getDeliveryTime(h.getAddress(), m.getTo().getAddress());
//				m.updateProperty(MSG_DELIVERY_PROPERTY, deliveryTime);
//			}
//			if (--nrofCopies <= 0 && !keepMessage) {
//				deleteMessage(m.getId(), false);
//			} else {
//				m.updateProperty(MSG_FORWARD_PROPERTY, nrofCopies);
//			}
//		}
	}

	@Override
	public ContactGraphRouter replicate() {
		return new ContactGraphRouter(this);
	}

}

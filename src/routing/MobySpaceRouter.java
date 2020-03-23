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
import routing.mobyspace.ScheduledMapMobySpace;
import util.Range;
import util.Tuple;

import java.util.*;

/**
 * MobySpace message router.
 * This implementation of MobySpace is designed to run with the {@link MapScheduledMovement}
 * and {@link StationaryListMovement} model.
 */
public class MobySpaceRouter extends ActiveRouter {
	/** GeOpps router's settings name space ({@value})*/
	public static final String MobySpace_NS = "MobySpaceRouter";
	/** identifier which nodes should work as dimension in the MobySpace */
	public static final String DIMENSIONS = "dimensions";
	// TODO setting dimensions (defining which nodes are a dimension via ranges)


	protected ScheduledMapMobySpace space;
//	protected DijkstraPathFinder pathFinder;
//	protected Map<String, Double> distanceCache;
//	protected Map<String, Double> messageDeadlines;
//	protected Set<String> keepMessage;


	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public MobySpaceRouter(Settings s) {
		super(s);
		Settings mobySettings = new Settings(MobySpace_NS);
		Range[] ranges = mobySettings.getCsvRanges(DIMENSIONS);
		List<Integer> dimensions = new ArrayList<>();
		for (Range range : ranges) {
			for (int NodeId = (int) range.getStart(); NodeId <= (int) range.getEnd(); NodeId++) {
				dimensions.add(NodeId);
			}
		}
		space = ScheduledMapMobySpace.getInstance(); // TODO doesn't need to be singleton
		space.setDimensions(dimensions);
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected MobySpaceRouter(MobySpaceRouter r) {
		super(r);
		this.space = r.space;
		MovementModel mModel = this.getHost().getMovement();
		if (mModel instanceof MapScheduledMovement) {
			this.space.addPoint(this.getHost().getAddress(),
					((MapScheduledMovement) mModel).getSchedule());
		} else if (mModel instanceof StationaryListMovement) {
			this.space.addPoint(this.getHost().getAddress(),
					((StationaryListMovement) mModel).getMapLocation());
		} else {
			throw new RuntimeException("The simulation scenario contains unsupported movement models for MobySpace.");
		}
	}

	@Override
	public void update() {
		// TODO
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

//    private List<Connection> getStationaryNodeConnections() {
//        List<Connection> connections = new ArrayList<>();
//        for (Connection con : getConnections()) {
//            MovementModel mm = con.getOtherNode(getHost()).getMovement();
//            if (mm instanceof StationaryListMovement ||
//                mm instanceof StationaryMovement) {
//                connections.add(con);
//            }
//        }
//        return connections;
//    }

//    private List<Message> getExceededMessages() {
//        List<Message> messages = new ArrayList<>();
//        double cTime = SimClock.getTime();
//        for (Message m : getMessageCollection()) {
//            if (messageDeadlines.get(m.getTo().toString()) < cTime) {
//                messages.add(m);
//            }
//        }
//        return messages;
//    }

    private List<Tuple<Message, Connection>> getSendableMessages() {
		Collection<Message> messages = getMessageCollection();
		List<Connection> connections = getConnections();
		HashMap<Message, Tuple<Double, Connection>> distances = new HashMap<>();

		/* Find shortest possible delivery time for each message */
		for (Message m : messages) {
			double minDistance = Double.MAX_VALUE;
			Connection minConnection = null;
			for (Connection c : connections) {
				DTNHost otherNode = c.getOtherNode(getHost());
				Double distance;
				distance = this.space.euclideanDistance(otherNode.getAddress(),
						m.getTo().getAddress());
				if (distance < minDistance) {
					minConnection = c;
					minDistance = distance;
				}
			}
			distances.put(m, new Tuple<>(minDistance, minConnection));
		}

		/* check if shortest possible delivery times are shorter than the own estimation */
		List<Tuple<Message, Connection>> sendableMessages = new ArrayList<>();
		for (HashMap.Entry<Message, Tuple<Double, Connection>> entry :
				distances.entrySet()) {
			double distance = this.space.euclideanDistance(getHost().getAddress(), entry.getKey().getTo().getAddress());
			if (entry.getValue().getKey() < distance) {
				sendableMessages.add(new Tuple<>(entry.getKey(), entry.getValue().getValue()));
			}
		}

		return sendableMessages;
	}


	@Override
	protected void transferDone(Connection con) {
		List<Message> messages = con.getMessage();
		for (Message m : messages) {
			deleteMessage(m.getId(), false);
		}
	}

	@Override
	public MobySpaceRouter replicate() {
		return new MobySpaceRouter(this);
	}

}

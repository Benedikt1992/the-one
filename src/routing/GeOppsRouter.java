/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
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

	protected boolean keepMessages;

	protected Map<String, Double> estimatedDeliveryTimes;


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
		estimatedDeliveryTimes = new HashMap<>();
	}

	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected GeOppsRouter(GeOppsRouter r) {
		super(r);
		this.keepMessages = r.keepMessages;
		this.estimatedDeliveryTimes = new HashMap<>();
		//TODO: is there something we need to copy (global stuff)
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
				if (deliveryTime != null) {
					if (deliveryTime < deliveryTimes.getOrDefault(m, new Tuple<>(Double.MAX_VALUE, null)).getKey()) {
						deliveryTimes.put(m, new Tuple<>(deliveryTime, c));
					}
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

		if (sendableMessages.size() > 0) {
			return sendableMessages;
		} else {
			return null;
		}
	}

	private Double findDeliveryEstimation(DTNHost to, DTNHost otherNode) {
		//TODO calculate...
		return null;
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

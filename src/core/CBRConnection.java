/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package core;

import routing.MessageRouter;

import java.util.*;

/**
 * A constant bit-rate connection between two DTN nodes.
 */
public class CBRConnection extends Connection {
	private int speed;
	private List<Double> transferDoneTimes;
	private double intervalCapacity;
	private double queuedCapacity;
	protected List<Message> msgsOnFly;


	/**
	 * Creates a new connection between nodes and sets the connection
	 * state to "up".
	 * @param fromNode The node that initiated the connection
	 * @param fromInterface The interface that initiated the connection
	 * @param toNode The node in the other side of the connection
	 * @param toInterface The interface in the other side of the connection
	 * @param connectionSpeed Transfer speed of the connection (Bps) when
	 *  the connection is initiated
	 */
	public CBRConnection(DTNHost fromNode, NetworkInterface fromInterface,
			DTNHost toNode,	NetworkInterface toInterface, int connectionSpeed) {
		super(fromNode, fromInterface, toNode, toInterface);
		this.speed = connectionSpeed;
		this.transferDoneTimes = new ArrayList<Double>();
		this.intervalCapacity = this.speed * SimClock.getUpdateInterval();
		this.queuedCapacity = 0;
		this.msgsOnFly = new ArrayList<Message>();
	}

	/**
	 * Sets a message that this connection is currently transferring. If message
	 * passing is controlled by external events, this method is not needed
	 * (but then e.g. {@link #finalizeTransfer()} and
	 * {@link #isMessageTransferred()} will not work either). Only a one message
	 * at a time can be transferred using one connection.
	 * @param from The host sending the message
	 * @param m The message
	 * @return The value returned by
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int startTransfer(DTNHost from, Message m) {

		assert this.queuedCapacity < this.intervalCapacity: "Already transferring maximum capacity of data per " +
				"simulation updateInterval. Can't start transfer of " + m + " from " + from;

		this.msgFromNode = from;
		Message newMessage = m.replicate();
		int retVal = getOtherNode(from).receiveMessage(newMessage, from);

		if (retVal == MessageRouter.RCV_OK) {
			this.msgsOnFly.add(newMessage);
			if (this.transferDoneTimes.isEmpty()) {
				this.transferDoneTimes.add(SimClock.getTime() +
						(1.0*m.getSize()) / this.speed);
			} else {
				double maxTime = getMaxTime();
				this.transferDoneTimes.add( maxTime + (1.0*m.getSize()) / this.speed);
			}
			this.queuedCapacity += newMessage.getSize();
		}

		return retVal;
	}

	private double getMaxTime() {
		double maxTime = 0;
		for (Double time: this.transferDoneTimes) {
			if (time > maxTime) {
				maxTime = time;
			}
		}
		return maxTime;
	}

	/**
	 * Aborts the transfer of the currently transferred messages.
	 */
	public void abortTransfer() {
		assert !msgsOnFly.isEmpty() : "No messages to abort.";
		for (Message m :
				this.msgsOnFly) {
			getOtherNode(msgFromNode).messageAborted(m.getId(),
					msgFromNode,getRemainingByteCount());
		}
		clearMsgOnFly();
	}

	public void abortTransfer(String id) {
		assert !msgsOnFly.isEmpty() : "No messages to abort.";
		List<Integer> removals = new ArrayList<>();
		for (int i = 0; i < msgsOnFly.size(); i++) {
			if (msgsOnFly.get(i).getId().equals(id)) {
				removals.add(i);
			}
		}
		Collections.reverse(removals);
		for (int i :
				removals) {
			getOtherNode(msgFromNode).messageAborted(id, msgFromNode, getRemainingByteCount());
			this.queuedCapacity -= this.msgsOnFly.get(i).getSize();
			this.msgsOnFly.remove(i);
			this.transferDoneTimes.remove(i);
		}
		if(msgsOnFly.isEmpty()) {
			clearMsgOnFly();
		}
	}

	public void finalizeTransfer() {
		double time = SimClock.getTime();
		ArrayList<Integer> removals = new ArrayList<>();
		for (int i = 0; i < msgsOnFly.size(); i++) {
			if (transferDoneTimes.get(i) <= time) {
				removals.add(i);
				this.bytesTransferred += msgsOnFly.get(i).getSize();
				queuedCapacity -= msgsOnFly.get(i).getSize();
				getOtherNode(msgFromNode).messageTransferred(msgsOnFly.get(i).getId(),
						msgFromNode);
			}
		}
		Collections.reverse(removals);
		for (int i :
				removals) {
			transferDoneTimes.remove(i);
			msgsOnFly.remove(i);
		}
	}

	public int getTotalBytesTransferred() {
		if (this.msgsOnFly.isEmpty()) {
			return this.bytesTransferred;
		}
		else {
			return this.bytesTransferred +
					((int)queuedCapacity - getRemainingByteCount());
		}
	}

	public boolean isReadyForTransfer() {
		return super.isReadyForTransfer() && queuedCapacity < intervalCapacity;
	}

	protected void clearMsgOnFly() {
		super.clearMsgOnFly();
		this.msgOnFly = null;
		this.msgsOnFly.clear();
		this.msgFromNode = null;
		this.queuedCapacity = 0;
		this.transferDoneTimes.clear();
	}

	/**
	 * Gets the transferdonetime
	 */
	public double getTransferDoneTime() {
		return getMaxTime();
	}

	public List<Message> getMessage() {
		List<Message> messages = new ArrayList<>();
		if (!msgsOnFly.isEmpty()) {
			double currenTime = SimClock.getTime();
			for (int i = 0; i < msgsOnFly.size(); i++) {
				if (transferDoneTimes.get(i) <= currenTime) {
					messages.add(msgsOnFly.get(i));
				}
			}
		}
		if (!messages.isEmpty()) {
			return messages;
		}
		return super.getMessage();
	}

	/**
	 * Returns true if the current message transfer is done.
	 * @return True if the transfer is done, false if not
	 */
	public boolean isMessageTransferred() {
		double currentTime = SimClock.getTime();
		for (double transferTime: transferDoneTimes
			 ) {
			if (transferTime < currentTime) {
				return true;
			}
		}
		return false;
	}

	/**
	 * returns the current speed of the connection
	 */
	public double getSpeed() {
		return this.speed;
	}

	public boolean isTransferring() {
		return !this.msgsOnFly.isEmpty();
	}

	/**
	 * Returns the amount of bytes to be transferred before ongoing transfer
	 * is ready or 0 if there's no ongoing transfer or it has finished
	 * already
	 * @return the amount of bytes to be transferred
	 */
	public int getRemainingByteCount() {
		int remaining;

		if (msgsOnFly.isEmpty()) {
			return 0;
		}

		remaining = (int)((getMaxTime() - SimClock.getTime())
				* this.speed);

		return (remaining > 0 ? remaining : 0);
	}

	/**
	 * Returns a String presentation of the connection.
	 */
	public String toString() {
		return super.toString() + (isTransferring() ?
				" until " + String.format("%.2f", getMaxTime()) : "");
	}

}

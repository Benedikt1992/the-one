/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.*;
import util.Range;

import java.util.*;

/**
 * Report to show how many Messages were processed by each node.
 * Processed means the node had to decide if the Message should be transferred.
 * (e.g. if e a node tries to send a Message which is already in the buffer of the node it is counted)
 * This report can have a big impact on the simulation speed.
 */
public class MessageProcessingReport extends Report implements ConnectionListener, MessageListener {
	/** Optional reported node ranges (comma separated list of ranges, e.g. 3-6,34-56 */
	public static final String REPORTED_NODE_RANGES = "nodeRanges";

	public static final String STARTED_ONLY_S = "startedOnly";

	protected HashSet<Integer> reportedNodes;
	protected boolean startedOnly;

	private Map<String, Integer> outgoingCounts;
	private Map<String, Integer> incomingCounts;
	private Map<String, HashSet<Transfer>> connections;
	private Set<String> hosts;

	/**
	 * Constructor.
	 */
	public MessageProcessingReport() {
		super();
		Settings settings = getSettings();
		if (settings.contains(REPORTED_NODE_RANGES)) {
			if (this.reportedNodes == null) { this.reportedNodes = new HashSet<Integer>(); }

			Range[] ranges = settings.getCsvRanges(REPORTED_NODE_RANGES);
			for (Range range : ranges) {
				for (int NodeId = (int) range.getStart(); NodeId <= (int) range.getEnd(); NodeId++) {
					this.reportedNodes.add(NodeId);
				}
			}
		}
		startedOnly = settings.getBoolean(STARTED_ONLY_S, false);
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.outgoingCounts = new HashMap<String, Integer>();
		this.incomingCounts = new HashMap<String, Integer>();
		this.connections = new HashMap<String, HashSet<Transfer>>();
		this.hosts = new HashSet<String>();
	}

	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		String key = getConnectionKey(host1, host2);
		HashSet<Transfer> transfers = connections.remove(key);

		boolean allNodes = reportedNodes == null;
		if (transfers != null && (
				allNodes ||
				reportedNodes.contains(host1.getAddress()) ||
				reportedNodes.contains(host2.getAddress())
		)) {
			for (Transfer transfer : transfers) {
				if (allNodes || reportedNodes.contains(transfer.getFromAddress())) {
					Integer oldValue = outgoingCounts.getOrDefault(transfer.getFrom(), 0);
					outgoingCounts.put(transfer.getFrom(), ++oldValue);
					hosts.add(transfer.getFrom());
				}

				if (allNodes || reportedNodes.contains(transfer.getToAddress())) {
					Integer oldValue = incomingCounts.getOrDefault(transfer.getTo(), 0);
					incomingCounts.put(transfer.getTo(), ++oldValue);
					hosts.add(transfer.getTo());
				}
			}
		}
	}

	private String getConnectionKey(DTNHost host1, DTNHost host2) {
		String[] components = new String[2];
		components[0] = host1.toString();
		components[1] = host2.toString();
		Arrays.sort(components);
		return String.join("", components);
	}

	@Override
	public void messageTransferRequested(Message m, DTNHost from, DTNHost to) {
		if (!startedOnly) {
			String conKey = getConnectionKey(from, to);

			HashSet<Transfer> transfers = connections.getOrDefault(conKey, new HashSet<Transfer>());
			transfers.add(new Transfer(from, to, m));

			// todo is this necessary?
			connections.put(conKey, transfers);
		}
	}

	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if( startedOnly ) {
			boolean allNodes = reportedNodes == null;
			if (allNodes || reportedNodes.contains(from.getAddress())) {
				Integer oldValue = outgoingCounts.getOrDefault(from.toString(), 0);
				outgoingCounts.put(from.toString(), ++oldValue);
				hosts.add(from.toString());
			}

			if (allNodes || reportedNodes.contains(to.getAddress())) {
				Integer oldValue = incomingCounts.getOrDefault(to.toString(), 0);
				incomingCounts.put(to.toString(), ++oldValue);
				hosts.add(to.toString());
			}
		}
	}

	/* Nothing to do for this report */
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {}
	public void newMessage(Message m) {}

	@Override
	public void done() {
		write("host,outgoing,incoming");
		for (String hostKey :
				hosts) {
			Integer outgoing = outgoingCounts.getOrDefault(hostKey, 0);
			Integer incoming = incomingCounts.getOrDefault(hostKey, 0);
			write(hostKey + "," + outgoing.toString() + "," + incoming.toString());
		}
		super.done();
	}

	private class Transfer {
		private DTNHost from;
		private DTNHost to;
		private Message message;

		public Transfer(DTNHost from, DTNHost to, Message message) {
			this.from = from;
			this.to = to;
			this.message = message;
		}

		public String getFrom() {
			return from.toString();
		}

		public Integer getFromAddress() {
			return from.getAddress();
		}

		public String getTo() {
			return to.toString();
		}

		public Integer getToAddress() {
			return to.getAddress();
		}

		public String getMessage() {
			return message.getId();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Transfer transfer = (Transfer) o;
			return getFrom().equals(transfer.getFrom()) &&
					getTo().equals(transfer.getTo()) &&
					getMessage().equals(transfer.getMessage());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getFrom(), getTo(), getMessage());
		}
	}
}

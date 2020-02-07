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
 */
public class MessageProcessingReport extends Report implements ConnectionListener, MessageListener {
	/** Optional reported node ranges (comma separated list of ranges, e.g. 3-6,34-56 */
	public static final String REPORTED_NODE_RANGES = "nodeRanges";

	protected HashSet<Integer> reportedNodes;

	private Map<String, Integer> outgoingCounts;
	private Map<String, Integer> incomingCounts;
	private Map<String, HashSet<String>> connections;
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
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.outgoingCounts = new HashMap<String, Integer>();
		this.incomingCounts = new HashMap<String, Integer>();
		this.connections = new HashMap<String, HashSet<String>>();
		this.hosts = new HashSet<String>();
	}

	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		String key = getConnectionKey(host1, host2);
		connections.put(key, new HashSet<String>());
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		String key = getConnectionKey(host1, host2);
		connections.remove(key);
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
		String conKey = getConnectionKey(from, to);
		String transferKey = m.getId() + from.toString() + toString();
		if (!connections.get(conKey).contains(transferKey)) {
			connections.get(conKey).add(transferKey);

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
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}


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
}

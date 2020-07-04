/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.*;
import util.Range;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Report to show how many Messages were processed by each node.
 * Processed means the node had to decide if the Message should be transferred.
 * (e.g. if e a node tries to send a Message which is already in the buffer of the node it is counted)
 * This report can have a big impact on the simulation speed.
 */
public class MessageProcessingReport extends Report implements MessageListener {
	/** Optional reported node ranges (comma separated list of ranges, e.g. 3-6,34-56 */
	public static final String REPORTED_NODE_RANGES = "nodeRanges";

	public static final String STARTED_ONLY_S = "startedOnly";

	protected HashSet<Integer> reportedNodes;
	protected boolean startedOnly;

	private Map<String, Integer> outgoingCounts;
	private Map<String, Integer> incomingCounts;
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
		this.hosts = new HashSet<String>();
	}

	@Override
	public void messageTransferRequested(Message m, DTNHost from, DTNHost to) {
		if (!startedOnly) {
			process(from, to);
		}
	}

	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if( startedOnly ) {
			process(from, to);
		}
	}

	private void process(DTNHost from, DTNHost to) {
		Integer oldValue = outgoingCounts.getOrDefault(from.toString(), 0);
		outgoingCounts.put(from.toString(), ++oldValue);
		hosts.add(from.toString());

		oldValue = incomingCounts.getOrDefault(to.toString(), 0);
		incomingCounts.put(to.toString(), ++oldValue);
		hosts.add(to.toString());
	}

	/* Nothing to do for this report */
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {}
	public void newMessage(Message m) {}

	@Override
	public void done() {
		write("host,outgoing,incoming");
		boolean allNodes = reportedNodes == null;
		for (String hostKey :
				hosts) {
			if (allNodes || reportedNodes.contains(hostAddress(hostKey))) {
				Integer outgoing = outgoingCounts.getOrDefault(hostKey, 0);
				Integer incoming = incomingCounts.getOrDefault(hostKey, 0);
				write(hostKey + "," + outgoing.toString() + "," + incoming.toString());
			}
		}
		super.done();
	}

	private Integer hostAddress(String hostKey) {
		final Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");
		Matcher matcher = lastIntPattern.matcher(hostKey);
		if (matcher.find()) {
			String someNumberStr = matcher.group(1);
			return Integer.parseInt(someNumberStr);
		}
		throw new RuntimeException("Host " + hostKey + " does not contain it's address.");
	}
}

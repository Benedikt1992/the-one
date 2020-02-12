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
public class MessageDuplicatesReport extends Report implements MessageListener {
	/** Optional reported node ranges (comma separated list of ranges, e.g. 3-6,34-56 */
	public static final String REPORTED_NODE_RANGES = "nodeRanges";

	protected HashSet<Integer> reportedNodes;

	private Map<String, HashMap<String, HashSet<String>>> duplicates;

	/**
	 * Constructor.
	 */
	public MessageDuplicatesReport() {
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
		this.duplicates = new HashMap<String, HashMap<String, HashSet<String>>>();
	}

	@Override
	public void messageTransferRequested(Message m, DTNHost from, DTNHost to) {
		boolean allNodes = reportedNodes == null;
		if (allNodes || reportedNodes.contains(to.getAddress())) {
            HashMap<String, HashSet<String>> messages = duplicates.getOrDefault(to.toString(),
                    new HashMap<String, HashSet<String>>());
            HashSet<String> received = messages.getOrDefault(m.getId(), new HashSet<String>());
            // TODO use the message itself. Currently it doesn't count if the same host send the same message on 2 occasions.
            received.add(from.toString());
            messages.put(m.getId(), received);
            duplicates.put(to.toString(), messages);
		}
	}

	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

	/* Nothing to do for this report */
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {}
	public void newMessage(Message m) {}

	@Override
	public void done() {
		write("host,message,duplicates");

		for (HashMap.Entry<String,  HashMap<String, HashSet<String>>> messages :
				duplicates.entrySet()) {
			for (HashMap.Entry<String, HashSet<String>> received:
				messages.getValue().entrySet()){
				write(messages.getKey() + "," + received.getKey() + "," + received.getValue().size());
			}
		}
		super.done();
	}
}

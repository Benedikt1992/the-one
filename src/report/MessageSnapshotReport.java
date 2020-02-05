/*
 * Copyright 2016 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */

package report;

import core.Coord;
import core.DTNHost;
import core.Settings;
import core.SettingsError;
import util.Range;

import java.util.HashSet;

/**
 * Node message snapshot report. Reports the amount of messages of all
 * (or only some, see {@link SnapshotReport#REPORTED_NODES}) nodes every 
 * configurable-amount-of seconds (see {@link SnapshotReport#GRANULARITY}).
 * Additionally to the options provided by {@link SnapshotReport} it provides
 * the option nodeRanges to define ranges of nodes (both options can be used simultaneously).
 */
public class MessageSnapshotReport extends SnapshotReport {
	/** Optional reported node ranges (comma separated list of ranges, e.g. 3-6,34-56 */
	public static final String REPORTED_NODE_RANGES = "nodeRanges";

	public static String HEADER = "time,nodeId,messages,bufferOccupancy";

	public MessageSnapshotReport() {
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
	}

	@Override
	public void init() {
		super.init();
		write(HEADER);
	}

	@Override
	protected void writeSnapshot(DTNHost h) {
		int messages = h.getNrofMessages();
		double buffer = h.getBufferOccupancy();
		write((int)getSimTime() + "," + h.toString() + "," + String.valueOf(messages) + "," + format(buffer));
	}
}

package routing.contactgraph;

import core.Settings;
import movement.map.MapNode;
import util.Tuple;

import java.util.*;

public abstract class ContactGraph {
    private static ScheduleGraph scheduleGraph;
    private static ContactPlanGraph contactPlanGraph;

    public static ScheduleGraph getScheduleGraph() {
        if (ContactGraph.scheduleGraph == null) {
            throw new RuntimeException("ScheduleGraph has not yet been instantiated.");
        }
        return ContactGraph.scheduleGraph;
    }

    public static ContactGraph initializeScheduleGraph(Settings s) {
        if (ContactGraph.scheduleGraph != null) {
            return ContactGraph.scheduleGraph;
        }
        ContactGraph.scheduleGraph = new ScheduleGraph(s);
        return ContactGraph.scheduleGraph;
    }

    public static ContactPlanGraph getContactPlanGraph() {
        if (ContactGraph.contactPlanGraph == null) {
            throw new RuntimeException("ContactPlanGraph has not yet been instantiated.");
        }
        return ContactGraph.contactPlanGraph;
    }

    public static ContactPlanGraph initializeContactPlanGraph(Settings s) {
        if (ContactGraph.contactPlanGraph != null) {
            return ContactGraph.contactPlanGraph;
        }
        ContactGraph.contactPlanGraph = new ContactPlanGraph(s);
        return ContactGraph.contactPlanGraph;
    }

    protected Set<Integer> availableRoutes;

    protected ContactGraph() {
        this.availableRoutes = new HashSet<>();
    }

    public abstract void addNode(Integer address);
    public abstract void addNode(Integer address, MapNode location);
    public abstract void calculateRoutesTo(Integer address);
    protected abstract ContactGraphNode getNode(Integer address);

    public LinkedList<Tuple<Double, Integer>> getNearestRoute(int from, int to, double startTime) {
        ContactGraphNode fromNode = getNode(from);
        if (fromNode == null) {
            return null;
        }
        return fromNode.getNearestRoute(to, startTime);
    }
}

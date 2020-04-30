package routing.contactgraph;

import core.Settings;
import core.SimScenario;
import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;
import movement.map.SimMap;
import util.Tuple;

import java.util.*;

public abstract class ContactGraph {
    private static ScheduleGraph scheduleGraph;

    public static ScheduleGraph getScheduleGraph() {
        if (ContactGraph.scheduleGraph == null) {
            throw new RuntimeException("ContactGraph has not yet been instantiated.");
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


    protected Map<MapNode, ContactGraphNode> nodesByLocation;
    protected Map<Integer, ContactGraphNode> nodesByAddress;
    protected Set<Integer> availableRoutes;

    protected ContactGraph() {
        this.nodesByLocation = new HashMap<>();
        this.nodesByAddress = new HashMap<>();
        this.availableRoutes = new HashSet<>();
    }

    public ContactGraphNode addNode(ContactGraphNode node) {
        MapNode location = node.getLocation();
        Integer address = node.getAddress();
        ContactGraphNode addressNode=null, locationNode=null;

        if (address != null) {
            addressNode = this.nodesByAddress.getOrDefault(address, null);
        }
        if (location != null) {
            locationNode = this.nodesByLocation.getOrDefault(location, null);
        }

        if (addressNode == null && locationNode != null) {
            if(address != null) {
                locationNode.setAddress(address);
                this.nodesByAddress.put(address,locationNode);
            }
            node = locationNode;
        } else if(addressNode != null && locationNode == null) {
            if (location != null) {
                addressNode.setLocation(location);
                this.nodesByLocation.put(location, addressNode);
            }
            node = addressNode;
        } else if (addressNode == null && locationNode == null) {
            if (location != null) { this.nodesByLocation.put(location, node);}
            if (address != null) { this.nodesByAddress.put(address, node);}
        } else if (addressNode != null && locationNode != null) {
            node = addressNode;
        }
        return node;
    }

    public abstract void calculateRoutesTo(Integer address);

    public LinkedList<Tuple<Double, Integer>> getNearestRoute(int from, int to, double startTime) {
        ContactGraphNode fromNode = this.nodesByAddress.getOrDefault(from, null);
        if (fromNode == null) {
            return null;
        }
        return fromNode.getNearestRoute(to, startTime);
    }
}

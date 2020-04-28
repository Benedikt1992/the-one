package routing.contactgraph;

import core.Settings;
import core.SimScenario;
import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;
import movement.map.SimMap;
import util.Tuple;

import java.util.*;

public class ContactGraph {
    private static ContactGraph instance;

    public static final String CONTACT_GRAPH_SCHEDULE = "schedule";
    public static final String CONTACT_GRAPH_START = "scheduleStartId";

    private Map<MapNode, ContactGraphNode> nodesByLocation;
    private Map<Integer, ContactGraphNode> nodesByAddress;
    private Set<Integer> availableRoutes;
    private Set<ContactGraphEdge> visitedEdges;
    private String schedulePath;
    private Integer scheduleStartId;
    private boolean initialized;

    private ContactGraph(Settings contactSettings) {
        this.schedulePath = contactSettings.getSetting(CONTACT_GRAPH_SCHEDULE);
        this.scheduleStartId = contactSettings.getInt(CONTACT_GRAPH_START);
        this.initialized = false;

        this.nodesByLocation = new HashMap<>();
        this.nodesByAddress = new HashMap<>();
        this.availableRoutes = new HashSet<>();
        this.visitedEdges = new HashSet<>();
    }

    private void initializeGraph() {
        SimMap map = SimScenario.getInstance().getMap();
        List<MapScheduledRoute> schedule = MapScheduledRoute.readRoutes(this.schedulePath, map);

        Integer currentAddress = this.scheduleStartId;
        for (MapScheduledRoute route : schedule) {
            List<MapScheduledNode> stops = route.getStops();
            ContactGraphEdge previousEdge = null;
            for (int i = 1; i < stops.size(); i++) {
                MapScheduledNode prevEntry = stops.get(i-1);
                MapScheduledNode cEntry = stops.get(i);
                if (!prevEntry.getNode().equals(cEntry.getNode())) {
                    ContactGraphEdge newEdge = new ContactGraphEdge(prevEntry.getNode(), prevEntry.getTime(),
                            cEntry.getNode(), cEntry.getTime(), currentAddress, previousEdge);
                    addEdge(newEdge);
                    previousEdge = newEdge;
                }
            }
            currentAddress++;
        }


    }

    public static ContactGraph getInstance() {
        if (ContactGraph.instance == null) {
            throw new RuntimeException("ContactGraph has not yet been instantiated.");
        }
        return ContactGraph.instance;
    }

    public static ContactGraph instantiate(Settings s) {
        if (ContactGraph.instance != null) {
            return ContactGraph.instance;
        }
        ContactGraph.instance = new ContactGraph(s);
        return ContactGraph.instance;
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

    private void addEdge(ContactGraphEdge edge) {
        MapNode from = edge.getFrom();
        ContactGraphNode fromNode = this.nodesByLocation.getOrDefault(from, null);
        if (fromNode == null) {
            fromNode = new ContactGraphNode(from);
            this.nodesByLocation.put(from, fromNode);
        }
        fromNode.addOutgoingEdge(edge);

        MapNode to = edge.getTo();
        ContactGraphNode toNode = this.nodesByLocation.getOrDefault(to, null);
        if (toNode == null) {
            toNode = new ContactGraphNode(to);
            this.nodesByLocation.put(to, toNode);
        }
        toNode.addIncomingEdge(edge);
    }

    public void calculateRoutesTo(Integer address) {

        if (availableRoutes.contains(address)) {
            return;
        }
        if (!initialized) {
            initializeGraph();
        }
        ContactGraphNode destination = this.nodesByAddress.getOrDefault(address, null);
        if (destination == null) {
            throw new RuntimeException("Requested destination for routes is not part of the contact graph.");
        }

        for (Iterator<ContactGraphEdge> it = destination.incomingEdges(false); it.hasNext(); ) {
            ContactGraphEdge edge = it.next();
            deepSearch(edge, new LinkedList<>());
            finalizeRoutes(address);
        }

        availableRoutes.add(address);
    }

    private void finalizeRoutes(Integer destination) {
        for (Map.Entry<Integer, ContactGraphNode> entry :
                nodesByAddress.entrySet()) {
            entry.getValue().persistRouteCandidate(destination);
        }
        this.visitedEdges = new HashSet<>();
    }

    private void deepSearch(ContactGraphEdge edge, LinkedList<Tuple<Double, Integer>> routeState) {
        if (visitedEdges.contains(edge)) { return; }
        routeState.push(new Tuple<>(edge.getDeparture(), edge.getAddress()));
        ContactGraphNode node = nodesByLocation.get(edge.getFrom());
        LinkedList<Tuple<Double, Integer>> clone = ( LinkedList<Tuple<Double, Integer>>) routeState.clone();
        node.setRouteCandidate(clone);
        visitedEdges.add(edge);
        Set<ContactGraphEdge> contacts = node.getContacts(edge);
        for (ContactGraphEdge contact : contacts) {
            deepSearch(contact, routeState);
        }
        routeState.pop();
    }

    public LinkedList<Tuple<Double, Integer>> getNearestRoute(int from, int to, double startTime) {
        ContactGraphNode fromNode = this.nodesByAddress.getOrDefault(from, null);
        if (fromNode == null) {
            return null;
        }
        return fromNode.getNearestRoute(to, startTime);
    }
}

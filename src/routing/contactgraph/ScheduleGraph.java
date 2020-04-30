package routing.contactgraph;

import core.Settings;
import core.SimScenario;
import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;
import movement.map.SimMap;
import util.Tuple;

import java.util.*;

public class ScheduleGraph extends ContactGraph{

    public static final String CONTACT_GRAPH_SCHEDULE = "schedule";
    public static final String CONTACT_GRAPH_START = "scheduleStartId";

    private Set<ContactGraphEdge> visitedEdges;
    private String schedulePath;
    private Integer scheduleStartId;
    private boolean initialized;

    protected ScheduleGraph(Settings contactSettings) {
        super();
        this.schedulePath = contactSettings.getSetting(CONTACT_GRAPH_SCHEDULE);
        this.scheduleStartId = contactSettings.getInt(CONTACT_GRAPH_START);
        this.initialized = false;
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
}

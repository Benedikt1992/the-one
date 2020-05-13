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

    private Set<ScheduleGraphEdge> visitedEdges;
    private String schedulePath;
    private Integer scheduleStartId;
    private boolean initialized;
    protected Map<MapNode, ScheduleGraphNode> nodesByLocation;
    protected Map<Integer, ScheduleGraphNode> nodesByAddress;

    protected ScheduleGraph(Settings contactSettings) {
        super();
        this.nodesByLocation = new HashMap<>();
        this.nodesByAddress = new HashMap<>();
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
            ScheduleGraphEdge previousEdge = null;
            for (int i = 1; i < stops.size(); i++) {
                MapScheduledNode prevEntry = stops.get(i-1);
                MapScheduledNode cEntry = stops.get(i);
                if (!prevEntry.getNode().equals(cEntry.getNode())) {
                    ScheduleGraphEdge newEdge = new ScheduleGraphEdge(prevEntry.getNode(), prevEntry.getTime(),
                            cEntry.getNode(), cEntry.getTime(), currentAddress, previousEdge);
                    addEdge(newEdge);
                    previousEdge = newEdge;
                }
            }
            currentAddress++;
        }
    }

    public void addNode(Integer address) {
        ScheduleGraphNode addressNode = this.nodesByAddress.getOrDefault(address, null);

        if(addressNode != null) {
            nodesByAddress.put(address, new ScheduleGraphNode(address));
        }
    }

    public void addNode(Integer address, MapNode location) {
        ScheduleGraphNode addressNode, locationNode;

        addressNode = this.nodesByAddress.getOrDefault(address, null);
        locationNode = this.nodesByLocation.getOrDefault(location, null);

        if (addressNode == null && locationNode != null) {
            locationNode.setAddress(address);
            this.nodesByAddress.put(address,locationNode);
        } else if(addressNode != null && locationNode == null) {
                addressNode.setLocation(location);
                this.nodesByLocation.put(location, addressNode);
        } else if (addressNode == null && locationNode == null) {
            ScheduleGraphNode node = new ScheduleGraphNode(address, location);
            this.nodesByLocation.put(location, node);
            this.nodesByAddress.put(address, node);
        }
    }

    private void addEdge(ScheduleGraphEdge edge) {
        MapNode from = edge.getFrom();
        ScheduleGraphNode fromNode = this.nodesByLocation.getOrDefault(from, null);
        if (fromNode == null) {
            fromNode = new ScheduleGraphNode(from);
            this.nodesByLocation.put(from, fromNode);
        }
        fromNode.addOutgoingEdge(edge);

        MapNode to = edge.getTo();
        ScheduleGraphNode toNode = this.nodesByLocation.getOrDefault(to, null);
        if (toNode == null) {
            toNode = new ScheduleGraphNode(to);
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
        ScheduleGraphNode destination = this.nodesByAddress.getOrDefault(address, null);
        if (destination == null) {
            throw new RuntimeException("Requested destination for routes is not part of the contact graph.");
        }

        for (Iterator<ScheduleGraphEdge> it = destination.incomingEdges(false); it.hasNext(); ) {
            ScheduleGraphEdge edge = it.next();
            deepSearch(edge, new LinkedList<>());
            finalizeRoutes(address);
        }

        availableRoutes.add(address);
    }

    @Override
    protected ContactGraphNode getNode(Integer address) {
        return this.nodesByAddress.getOrDefault(address, null);
    }

    private void finalizeRoutes(Integer destination) {
        for (Map.Entry<Integer, ScheduleGraphNode> entry :
                nodesByAddress.entrySet()) {
            entry.getValue().persistRouteCandidate(destination);
        }
        this.visitedEdges = new HashSet<>();
    }

    private void deepSearch(ScheduleGraphEdge edge, LinkedList<Tuple<Double, Integer>> routeState) {
        if (visitedEdges.contains(edge)) { return; }
        routeState.push(new Tuple<>(edge.getDeparture(), edge.getAddress()));
        ScheduleGraphNode node = nodesByLocation.get(edge.getFrom());
        LinkedList<Tuple<Double, Integer>> clone = ( LinkedList<Tuple<Double, Integer>>) routeState.clone();
        node.setRouteCandidate(clone);
        visitedEdges.add(edge);
        List<ScheduleGraphEdge> contacts = node.getContacts(edge);
        for (ScheduleGraphEdge contact : contacts) {
            deepSearch(contact, routeState);
        }
        routeState.pop();
    }
}

package routing.contactgraph;

import movement.map.MapNode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

public class ContactGraph {
    private static ContactGraph instance;

    private Map<MapNode, ContactGraphNode> nodesByLocation;
    private Map<Integer, ContactGraphNode> nodesByAddress;
    private Set<Integer> availableRoutes;
    private Set<ContactGraphEdge> visitedEdges;

    private ContactGraph() {
        this.nodesByLocation = new HashMap<>();
        this.nodesByAddress = new HashMap<>();
        this.availableRoutes = new HashSet<>();
        this.visitedEdges = new HashSet<>();
    }

    public static ContactGraph getInstance() {
        if (ContactGraph.instance == null) {
            ContactGraph.instance = new ContactGraph();
        }
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

    public void addEdge(ContactGraphEdge edge) {
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

    private void deepSearch(ContactGraphEdge edge, LinkedList<ContactGraphEdge> routeState) {
        if (visitedEdges.contains(edge)) { return; }
        routeState.push(edge);
        ContactGraphNode node = nodesByLocation.get(edge.getFrom());
        LinkedList<ContactGraphEdge> clone = ( LinkedList<ContactGraphEdge>) routeState.clone();
        node.setRouteCandidate(clone);
        visitedEdges.add(edge);
        Set<ContactGraphEdge> contacts = node.getContacts(edge);
        for (ContactGraphEdge contact : contacts) {
            deepSearch(contact, routeState);
        }
        routeState.pop();
    }

    public LinkedList<ContactGraphEdge> getNearestRoute(int from, int to, double startTime) {
        ContactGraphNode fromNode = this.nodesByAddress.getOrDefault(from, null);
        if (fromNode == null) {
            return null;
        }
        return fromNode.getNearestRoute(to, startTime);
    }
}

package routing.contactgraph;

import movement.map.MapNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ContactGraph {
    private static ContactGraph instance;

    private Map<MapNode, ContactGraphNode> nodesByLocation;
    private Map<Integer, ContactGraphNode> nodesByAddress;
    private Set<Integer> availableRoutes;

    private ContactGraph() {
        this.nodesByLocation = new HashMap<>();
        this.nodesByAddress = new HashMap<>();
        this.availableRoutes = new HashSet<>();
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


        // TODO calculate...
        availableRoutes.add(address);
    }
}

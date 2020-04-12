package routing.contactgraph;

import movement.map.MapNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactGraphNode {
    Integer address;
    MapNode location;
    //TODO make the storage of edges faster searchable
    List<ContactGraphEdge> incomingEdges;
    List<ContactGraphEdge> outgoingEdges;

    public ContactGraphNode(Integer address, MapNode location) {
        this.address = address;
        this.location = location;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
    }

    public ContactGraphNode(MapNode location) {
        this.location = location;
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
    }

    public void setLocation(MapNode location) {
        if (this.location == null) {
            this.location = location;
        }
    }

    public void setAddress(Integer address) {
        if (this.address == null) {
            this.address = address;
        }
    }

    public Integer getAddress() {
        return address;
    }

    public MapNode getLocation() {
        return location;
    }

    public void addOutgoingEdge(ContactGraphEdge edge) {
        outgoingEdges.add(edge);
    }

    public void addIncomingEdge(ContactGraphEdge edge) {
        incomingEdges.add(edge);
    }
}

package routing.contactgraph;

import movement.map.MapNode;

import java.util.*;

public class ContactGraphNode {
    private Integer address;
    private MapNode location;
    //TODO make the storage of edges faster searchable
    private List<ContactGraphEdge> incomingEdges;
    private List<ContactGraphEdge> outgoingEdges;

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
        Collections.sort(incomingEdges, (e1, e2) -> (int) Math.floor(e1.getDeparture() - e2.getDeparture()));
    }

    public void addIncomingEdge(ContactGraphEdge edge) {
        incomingEdges.add(edge);
        Collections.sort(incomingEdges, (e1, e2) -> (int) Math.floor(e1.getArrival() - e2.getArrival()));
    }

    public Iterator<ContactGraphEdge> incomingEdges(boolean ascending) {
        // TODO
        return new Iterator<ContactGraphEdge>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public ContactGraphEdge next() {
                return null;
            }
        };
    }
}

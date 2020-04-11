package routing.contactgraph;

import movement.map.MapNode;

public class ContactGraphNode {
    Integer address;
    MapNode location;

    public ContactGraphNode(Integer address) {
        this.address = address;
    }

    public ContactGraphNode(MapNode location) {
        this.location = location;
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
        //TODO add the edge to the node and make it quickly findable by time
    }

    public void addIncomingEdge(ContactGraphEdge edge) {
        //TODO add the edge to the node and make it quickly findable by time
    }
}

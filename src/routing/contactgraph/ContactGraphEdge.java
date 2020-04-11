package routing.contactgraph;

import movement.map.MapNode;

public class ContactGraphEdge {
    private MapNode from;
    private MapNode to;
    private  Integer address;
    private double departure;
    private double arrival;

    public ContactGraphEdge(MapNode from, double departure, MapNode to, double arrival, Integer address) {
        this.from = from;
        this.to = to;
        this.address = address;
        this.departure = departure;
        this.arrival = arrival;
    }

    public MapNode getFrom() {
        return from;
    }

    public MapNode getTo() {
        return to;
    }

    public Integer getAddress() {
        return address;
    }

    public double getDeparture() {
        return departure;
    }

    public double getArrival() {
        return arrival;
    }
}

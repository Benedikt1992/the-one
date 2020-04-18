package routing.contactgraph;

import movement.map.MapNode;

public class ContactGraphEdge {
    private MapNode from;
    private MapNode to;
    private  Integer address;
    private double departure;
    private double arrival;
    private ContactGraphEdge previousEdge;
    private ContactGraphEdge nextEdge;

    public ContactGraphEdge(MapNode from, double departure, MapNode to, double arrival, Integer address, ContactGraphEdge previousEdge) {
        this.from = from;
        this.to = to;
        this.address = address;
        this.departure = departure;
        this.arrival = arrival;
        this.previousEdge = previousEdge;
        this.nextEdge = null;
        if (previousEdge != null) {
            previousEdge.setNextEdge(this);
        }
    }

    public Double getArrivalAtFrom() {
        if (previousEdge == null) {
            return null;
        }
        return previousEdge.getArrival();
    }

    private void setNextEdge(ContactGraphEdge nextEdge) {
        this.nextEdge = nextEdge;
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

    public Double getDepartureToTo() {
        //TODO is this really what should happen? Maybe rename it then?
        if (nextEdge == null) {
            return null;
        }
        return nextEdge.getDeparture();
    }
}

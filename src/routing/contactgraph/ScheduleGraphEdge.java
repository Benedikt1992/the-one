package routing.contactgraph;

import movement.map.MapNode;

public class ScheduleGraphEdge {
    // TODO rename this. It only matches the schedule based graph
    private MapNode from;
    private MapNode to;
    private  Integer address;
    private double departure;
    private double arrival;
    private ScheduleGraphEdge previousEdge;
    private ScheduleGraphEdge nextEdge;

    public ScheduleGraphEdge(MapNode from, double departure, MapNode to, double arrival, Integer address, ScheduleGraphEdge previousEdge) {
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

    public Double getArrivalFromFrom() {
        if (previousEdge == null) {
            return null;
        }
        return previousEdge.getArrival();
    }

    private void setNextEdge(ScheduleGraphEdge nextEdge) {
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
        if (nextEdge == null) {
            return null;
        }
        return nextEdge.getDeparture();
    }

    public ScheduleGraphEdge getPrevious() {
        return previousEdge;
    }
}

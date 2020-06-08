package routing.contactgraph;

import movement.map.MapNode;

import java.util.*;

public class ScheduleGraphNode  extends ContactGraphNode {
    private Integer address;
    private MapNode location;
    private List<ScheduleGraphEdge> incomingEdges;
    private List<ScheduleGraphEdge> outgoingEdges;
    private boolean incomingSorted;
    private boolean outgoingSorted;


    public ScheduleGraphNode(Integer address, MapNode location) {
        super();
        this.address = address;
        this.location = location;
        initializeFields();

    }

    public ScheduleGraphNode(MapNode location) {
        super();
        this.location = location;
        initializeFields();
    }

    public ScheduleGraphNode(Integer address) {
        super();
        this.address = address;
        initializeFields();
    }

    private void initializeFields() {
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.incomingSorted = false;
        this.outgoingSorted = false;
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

    public void addOutgoingEdge(ScheduleGraphEdge edge) {
        outgoingEdges.add(edge);
        outgoingSorted = false;
    }

    public void addIncomingEdge(ScheduleGraphEdge edge) {
        incomingEdges.add(edge);
        incomingSorted = false;
    }

    private void sortIncomingEdges() {
        if (!incomingSorted) {
            Collections.sort(incomingEdges, (e1, e2) -> (int) Math.floor(e1.getArrival() - e2.getArrival()));
            incomingSorted = true;
        }
    }

    private void sortOutgoingEdges() {
        if (!outgoingSorted) {
            Collections.sort(incomingEdges, (e1, e2) -> (int) Math.floor(e1.getArrival() - e2.getArrival()));
            outgoingSorted = true;
        }
    }

    public Iterator<ScheduleGraphEdge> incomingEdges(boolean ascending) {
        sortIncomingEdges();
        if (ascending) {
            return new AscendingEdgeIterator<>(this.incomingEdges);
        } else {
            return new DescendingEdgeIterator<>(this.incomingEdges);
        }
    }

    public List<ScheduleGraphEdge> getContacts(ScheduleGraphEdge edge) {
        Double min = edge.getArrivalFromFrom();
        double max = edge.getDeparture();
        if (min == null) {
            min = max;
        }
        List<ScheduleGraphEdge> contacts = new ArrayList<>();
        sortIncomingEdges();
        for (ScheduleGraphEdge e : incomingEdges) {
            if (e.getArrival() > max) {
                break;
            }
            Double leaveTime = e.getDepartureToTo();
            if (leaveTime != null && leaveTime >= min) {
                contacts.add(e);
            }
        }
        ScheduleGraphEdge e = edge.getPrevious();
        if (e != null) {
            contacts.add(edge.getPrevious());
        }
        return contacts;
    }

    public List<ScheduleGraphEdge> getPreviousContacts(ScheduleGraphEdge edge) {
        Double min = edge.getArrivalFromFrom();
        if (min == null) {
            min = edge.getDeparture();
        }
        List<ScheduleGraphEdge> contacts = new ArrayList<>();
        sortIncomingEdges();
        for (ScheduleGraphEdge e : incomingEdges) {
            Double leaveTime = e.getDepartureToTo();
            if (leaveTime != null && leaveTime < min) {
                contacts.add(e);
            }
        }
        return contacts;
    }
}
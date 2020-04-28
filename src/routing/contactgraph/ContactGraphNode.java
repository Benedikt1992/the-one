package routing.contactgraph;

import core.SimClock;
import movement.map.MapNode;
import util.Tuple;

import java.util.*;

public class ContactGraphNode {
    private Integer address;
    private MapNode location;
    private List<ContactGraphEdge> incomingEdges;
    private List<ContactGraphEdge> outgoingEdges;
    private LinkedList<Tuple<Double, Integer>> routeCandidate;
    private  Map<Integer, LinkedList<LinkedList<Tuple<Double, Integer>>>> routes;
    private boolean incomingSorted;
    private boolean outgoingSorted;


    public ContactGraphNode(Integer address, MapNode location) {
        this.address = address;
        this.location = location;
        initializeFields();

    }

    public ContactGraphNode(MapNode location) {
        this.location = location;
        initializeFields();
    }

    public ContactGraphNode(Integer address) {
        this.address = address;
        initializeFields();
    }

    private void initializeFields() {
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
        this.routeCandidate = null;
        this.routes = new HashMap<>();
        this.incomingSorted = false;
        this.outgoingSorted = false;
    }

    public void setRouteCandidate(LinkedList<Tuple<Double, Integer>> routeCandidate) {
        Double lastStart = getRouteCandidateStart();
        if (lastStart == null || lastStart < routeCandidate.peek().getKey()) {
            this.routeCandidate = routeCandidate;
        }
    }

    public Double getRouteCandidateStart() {
        if (routeCandidate == null) {
            return null;
        }

        return routeCandidate.peek().getKey();
    }

    public void persistRouteCandidate(Integer destination) {
        if (routeCandidate != null) {
            LinkedList<LinkedList<Tuple<Double, Integer>>> availableRoutes = this.routes.getOrDefault(destination, new LinkedList<>());
            availableRoutes.push(routeCandidate);
            this.routes.put(destination, availableRoutes);
            routeCandidate = null;
        }
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
        outgoingSorted = false;
    }

    public void addIncomingEdge(ContactGraphEdge edge) {
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

    public Iterator<ContactGraphEdge> incomingEdges(boolean ascending) {
        sortIncomingEdges();
        if (ascending) {
            return new AscendingEdgeIterator<>(this.incomingEdges);
        } else {
            return new DescendingEdgeIterator<>(this.incomingEdges);
        }
    }

    public Set<ContactGraphEdge> getContacts(ContactGraphEdge edge) {
        Double min = edge.getArrivalFromFrom();
        double max = edge.getDeparture();
        if (min == null) {
            min = max;
        }
        Set<ContactGraphEdge> contacts = new HashSet<>();
        sortIncomingEdges();
        for (ContactGraphEdge e : incomingEdges) {
            if (e.getArrival() > max) {
                break;
            }
            Double leaveTime = e.getDepartureToTo();
            if (leaveTime != null && leaveTime >= min) {
                contacts.add(e);
            }
        }
        return contacts;
    }

    public Set<ContactGraphEdge> getBufferedContacts(ContactGraphEdge edge) {
        double max = edge.getDeparture();
        Set<ContactGraphEdge> contacts = new HashSet<>();
        sortIncomingEdges();
        for (ContactGraphEdge e : incomingEdges) {
            if (e.getArrival() > max) {
                break;
            }
            contacts.add(e);
        }
        return contacts;
    }

    public LinkedList<Tuple<Double, Integer>> getNearestRoute(int to, double startTime) {
        LinkedList<LinkedList<Tuple<Double, Integer>>> routes = this.routes.getOrDefault(to, null);
        if (routes == null) {
            return null;
        }

        removeObsoleteRoutes(routes);
        for (LinkedList<Tuple<Double, Integer>> route: routes){
            if (route.getFirst().getKey() >= startTime) {
                return route;
            }
        }
        return null;
    }

    private void removeObsoleteRoutes(LinkedList<LinkedList<Tuple<Double, Integer>>> routes) {
        LinkedList<Tuple<Double, Integer>> candidate = routes.peek();
        double cTime = SimClock.getTime();
        while (candidate != null) {
            if (candidate.peek().getKey() >= cTime) {
                break;
            }
            routes.pop();
            candidate = routes.peek();
        }
    }
}

class AscendingEdgeIterator<ContactGraphEdge> implements Iterator<ContactGraphEdge> {
    private int index;
    private List<ContactGraphEdge> list;

    AscendingEdgeIterator(List<ContactGraphEdge> list) {
        this.list = list;
        index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < list.size();
    }

    @Override
    public ContactGraphEdge next() {
        return list.get(index++);
    }
}

class DescendingEdgeIterator<ContactGraphEdge> implements Iterator<ContactGraphEdge> {
    private int index;
    private List<ContactGraphEdge> list;

    DescendingEdgeIterator(List<ContactGraphEdge> list) {
        this.list = list;
        index = list.size() - 1;
    }

    @Override
    public boolean hasNext() {
        return index >= 0;
    }

    @Override
    public ContactGraphEdge next() {
        return list.get(index--);
    }
}
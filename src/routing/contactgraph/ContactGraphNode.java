package routing.contactgraph;

import core.SimClock;
import util.Tuple;

import java.util.*;

public abstract class ContactGraphNode {
    protected LinkedList<Tuple<Double, Integer>> routeCandidate;
    protected  Map<Integer, LinkedList<LinkedList<Tuple<Double, Integer>>>> routes;
    protected boolean routesSorted;

    public ContactGraphNode() {
        this.routeCandidate = null;
        this.routes = new HashMap<>();
        this.routesSorted = false;

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
            routesSorted = false;
        }
    }

    protected void sortRoutes() {
        /**
         * This will sort routes according to their latest delivery time (which is part of a route).
         * By default the routes will be sorted by the earliest possible delivery (which is not part of the route itself).
         */
        if (!routesSorted) {
            for (Map.Entry<Integer, LinkedList<LinkedList<Tuple<Double, Integer>>>> entry:
                routes.entrySet()) {
                Collections.sort(entry.getValue(), Comparator.comparingDouble(e -> e.get(e.size() - 1).getKey()));
            }

            routesSorted = true;
        }
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

class AscendingEdgeIterator<T> implements Iterator<T> {
    private int index;
    private List<T> list;

    AscendingEdgeIterator(List<T> list) {
        this.list = list;
        index = 0;
    }

    @Override
    public boolean hasNext() {
        return index < list.size();
    }

    @Override
    public T next() {
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
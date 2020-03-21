package routing.mobyspace;

import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduledMapMobySpace {
    private static ScheduledMapMobySpace instance;
    private Map<String, MobyPoint> points;
    private List<Integer> dimensions; // TODO

    private ScheduledMapMobySpace() {}
    public static ScheduledMapMobySpace getInstance() {
        if (ScheduledMapMobySpace.instance == null) {
            ScheduledMapMobySpace.instance = new ScheduledMapMobySpace();
        }
        return ScheduledMapMobySpace.instance;
    }

    public void setDimensions(List<Integer> dimensions) {
        if (this.dimensions == null) {
            this.dimensions = dimensions;
        } else {
            throw new RuntimeException("Dimension of the MobySpace can't be changed.");
        }
    }

    public void addPoint(Integer address, MapScheduledRoute route) {
        List<MapScheduledNode> stops = route.getStops();
        HashMap<MapNode, Double> visits = new HashMap<>();
        for (int i = stops.size() - 1; i < 0; i--) {
            double time = stops.get(i).getTime();
            MapNode key = stops.get(i).getNode();
            visits.put(key, time);
        }
        // TODO Map MapNodes to Integers from dimensions list.

    }


    private class MobyPoint {

    }
}

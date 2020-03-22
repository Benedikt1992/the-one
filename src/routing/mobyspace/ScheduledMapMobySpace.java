package routing.mobyspace;

import core.DTNHost;
import core.SimClock;
import core.SimScenario;
import core.World;
import movement.MovementModel;
import movement.StationaryListMovement;
import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduledMapMobySpace {
    private static ScheduledMapMobySpace instance;
    private Map<Integer, MobyPoint> points;
    private List<Integer> dimensions;
    private List<MapNode> dimensionNodes;

    private ScheduledMapMobySpace() {
        points = new HashMap<>();
    }
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

    private void convertDimensions() {
        if ( SimScenario.isInstantiated()) {
            dimensionNodes = new ArrayList<>();
            World world = SimScenario.getInstance().getWorld();
            for (Integer i: dimensions) {
                DTNHost node = world.getNodeByAddress(i);
                MovementModel mModel = node.getMovement();
                if (mModel instanceof StationaryListMovement) {
                    MapNode mapNode = ((StationaryListMovement) mModel).getMapLocation();
                    dimensionNodes.add(mapNode);
                } else {
                    throw new RuntimeException("Dimension is not a stationary node");
                }
            }
        }
    }

    public double euclideanDistance(Integer node1, Integer node2) {
        double sum = 0;
        MobyPoint point1 = points.get(node1);
        MobyPoint point2 = points.get(node2);
        if (dimensionNodes == null)  {
            convertDimensions(); // Conversion from node address to MapNodes can only be made once the simulation started.
        }
        for (MapNode dimension: dimensionNodes) {
            double value1 = point1.getValue(dimension);
            double value2 = point2.getValue(dimension);
            sum += (value2 - value1) * (value2 - value1);
        }
        return Math.sqrt(sum);
    }

    public void updatePoint(Integer address, MapNode changingStop, Double nextVisit) {
        if (nextVisit == null) {
            nextVisit = 0.0;
        }

        this.points.get(address).update(changingStop, nextVisit);
    }

    public void addPoint(Integer address, MapScheduledRoute route) {
        List<MapScheduledNode> stops = route.getStops();
        HashMap<MapNode, Double> visits = new HashMap<>();
        for (int i = stops.size() - 1; i < 0; i--) {
            double time = stops.get(i).getTime();
            MapNode key = stops.get(i).getNode();
            visits.put(key, time);
        }
        MobyPoint point = new MobyPoint(visits);
        points.put(address, point);
    }


    private static class MobyPoint {
        private HashMap<MapNode, Double> visits;
        public MobyPoint(HashMap<MapNode, Double> visits) {
            this.visits = visits;
        }

        public void update(MapNode node, double time) {
            visits.put(node, time);
        }

        public double getValue(MapNode node) {
            double time = visits.getOrDefault(node, 0.0);
            double value = 1 / (time - SimClock.getTime());

            value = value < 0 ? 0 : value;

            return value;
        }
    }
}

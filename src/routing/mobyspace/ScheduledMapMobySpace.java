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

    public void addPoint(Integer address, MapScheduledRoute route) {
        MobyPoint point = new MobyPoint(route);
        points.put(address, point);
    }

    public void addPoint(Integer address, MapNode node) {
        MobyPoint point = new MobyPoint(node);
        points.put(address, point);
    }

    private static class MobyPoint {
        private HashMap<MapNode, Double> visits;
        private MapScheduledRoute route;
        private int lastIndex;

        public MobyPoint(MapScheduledRoute route) {
            lastIndex = 0;
            this.route = route;
            List<MapScheduledNode> stops = route.getStops();
            HashMap<MapNode, Double> visits = new HashMap<>();
            for (int i = stops.size() - 1; i < 0; i--) {
                double time = stops.get(i).getTime();
                MapNode key = stops.get(i).getNode();
                visits.put(key, time);
            }
            this.visits = visits;
        }

        public MobyPoint(MapNode node) {
            HashMap<MapNode, Double> visits = new HashMap<>();
            visits.put(node, null);
            this.visits = visits;
        }

        private void update(MapNode node, double time) {
            visits.put(node, time);
        }

        public double getValue(MapNode node) {
            Double time = visits.getOrDefault(node, 0.0);
            if (time == null) {
                // we have a stationary node
                return 1;
            }

            double cTime = SimClock.getTime();
            if (time < cTime) {
                updatePoint(node);
                time = visits.get(node);
            }

            double value = 1 / (time - cTime);

            value = value < 0 ? 0 : value;

            return value;
        }

        private void updatePoint(MapNode changingDimension) {
            this.route.setNextIndex(this.lastIndex);
            MapScheduledNode nextNode = this.route.nextStop();
            double cTime = SimClock.getTime();
            while (nextNode != null) {
                if (nextNode.getTime() < cTime) {
                    lastIndex++;
                    nextNode = this.route.nextStop();
                    continue;
                }
                if (nextNode.getNode() == changingDimension) {
                    update(changingDimension, nextNode.getTime());
                    return;
                }
            }
            update(changingDimension, 0);
        }
    }
}

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

import java.util.*;

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
        this.dimensions = dimensions;
        if(this.dimensionNodes != null) {
            convertDimensions();
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
        point1.update();
        point2.update();
        double cTime = SimClock.getTime();
        if (dimensionNodes == null)  {
            convertDimensions(); // Conversion from node address to MapNodes can only be made once the simulation started.
        }
        for (MapNode dimension: dimensionNodes) {
            double value1 = point1.getValue(dimension, cTime);
            double value2 = point2.getValue(dimension, cTime);
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
        private MapNode node;
        private int lastIndex;
        private boolean isStationary;

        public MobyPoint(MapScheduledRoute route) {
            lastIndex = 0;
            this.route = route;
            List<MapScheduledNode> stops = route.getStops();
            HashMap<MapNode, Double> visits = new HashMap<>();
            for (int i = stops.size() - 1; i >= 0; i--) {
                double time = stops.get(i).getTime();
                MapNode key = stops.get(i).getNode();
                visits.put(key, time);
            }
            this.visits = visits;
            this.isStationary = false;
        }

        public MobyPoint(MapNode node) {
            this.node = node;
            this.isStationary = true;
        }

        public double getValue(MapNode node, double cTime) {
            if (isStationary) {
                return getStationaryValue(node);
            } else {
                return getRouteValue(node, cTime);
            }
        }

        private double getRouteValue(MapNode node, double cTime) {
            Double time = visits.getOrDefault(node, 0.0);

            double value = 1 / (time - cTime);

            value = value < 0 ? 0 : value;

            return value;
        }

        private double getStationaryValue(MapNode node) {
            if (node == this.node) {
                return 1;
            } else {
                return 0;
            }
        }

        public void update() {
            if (!isStationary) {
                this.route.setNextIndex(this.lastIndex);
                MapScheduledNode nextNode = this.route.nextStop();
                double cTime = SimClock.getTime();
                while (nextNode != null) {
                    if (nextNode.getTime() >= cTime) {
                        return;
                    }
                    updatePoint(nextNode.getNode());
                    lastIndex++;
                    this.route.setNextIndex(this.lastIndex);
                    nextNode = this.route.nextStop();
                }
            }
        }

        private void updatePoint(MapNode outdatedStop) {
            MapScheduledNode nextNode = this.route.nextStop();
            double cTime = SimClock.getTime();
            while (nextNode != null) {
                if (nextNode.getNode() == outdatedStop && nextNode.getTime() > cTime) {
                    visits.put(outdatedStop, nextNode.getTime());
                    return;
                }
                nextNode = this.route.nextStop();
            }
            visits.remove(outdatedStop);
        }
    }
}

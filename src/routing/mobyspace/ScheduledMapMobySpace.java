package routing.mobyspace;

import core.*;
import movement.MovementModel;
import movement.StationaryListMovement;
import movement.map.MapNode;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ScheduledMapMobySpace {
    private static ScheduledMapMobySpace instance;
    private Map<Integer, MobyPoint> points;
    private List<Integer> dimensions;
    private List<MapNode> dimensionNodes;

    /** The method used to calculate distances within the space */
    private Method distanceMetric;
    /** For Lk-norm metrics */
    private double k;

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

    public void setDistanceMetric(String methodName, Double k) {
        try {
            if (k == 2.0) {
                this.distanceMetric = this.getClass().getMethod("euclideanDistance", Integer.class, Integer.class);
            } else {
                this.distanceMetric = this.getClass().getMethod(methodName, Integer.class, Integer.class);
            }
        } catch (NoSuchMethodException e) {throw new RuntimeException(e); }

        this.k = k;
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

    public double distance(Integer node1, Integer node2) {
        try {
            return (double) this.distanceMetric.invoke(this, node1, node2);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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

    public double LNormDistance(Integer node1, Integer node2) {
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
            sum += Math.pow(value2 - value1, this.k);
        }
        // TODO replace this with an version without precision errors.
        return Math.pow(sum, 1 / this.k);
    }

    public double productDistance(Integer node1, Integer node2) {
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
            sum += Math.pow(value2 * value1, this.k);
        }
        // TODO replace this with an version without precision errors.
        return 1 / sum;
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
                List <MapScheduledNode> stops = this.route.getStops();
                double cTime = SimClock.getTime();
                if (lastIndex >= stops.size()) {
                    return;
                }
                for (int i = this.lastIndex; i < stops.size(); i++) {
                    MapScheduledNode nextNode = stops.get(i);
                    if (nextNode.getTime() >= cTime) {
                        return;
                    }
                    updatePoint(nextNode.getNode());
                    lastIndex++;
                }
            }
        }

        private void updatePoint(MapNode outdatedStop) {
            List <MapScheduledNode> stops = this.route.getStops();
            double cTime = SimClock.getTime();
            for (int i = this.lastIndex; i < stops.size(); i++) {
                MapScheduledNode nextNode = stops.get(i);
                if (nextNode.getNode() == outdatedStop && nextNode.getTime() > cTime) {
                    visits.put(outdatedStop, nextNode.getTime());
                    return;
                }
            }
            visits.remove(outdatedStop);
        }
    }
}

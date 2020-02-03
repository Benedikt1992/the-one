package movement.map.delay;

import core.Settings;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This DelayModel adds random delays to each stop of the route.
 * The amount of delay needs to be specified with Group.delayRange = min,max
 * min and max can be negative.
 * The probability to have any delay is defined with the setting delayProbability.
 * If it is not specified it will run with a probability of 1 which means every stop will get a delay.
 */
public class RandomDelay extends DelayModel {
    /** The amount of delay each train get reach for a single stop. */
    public static final String DELAY_RANGE_S = "delayRange";
    /** Probability with which a train gets a delay */
    public static final String DELAY_PROBABILITY_S = "delayProbability";
    /** Default value for delay probability */
    public static final double DEF_DELAY_PROBABILITY = 1;

    private double probability;
    private double minDelay;
    private double maxDelay;

    /**
     * Creates a new DelayModel based on a Settings object's settings.
     *
     * @param settings The Settings object where the settings are read from
     */
    public RandomDelay(Settings settings) {
        super(settings);
        probability = settings.getDouble(DELAY_PROBABILITY_S, DEF_DELAY_PROBABILITY);
        double[] delayRange = settings.getCsvDoubles(DELAY_RANGE_S, 2);
        minDelay = delayRange[0];
        maxDelay = delayRange[1];
    }

    @Override
    public MapScheduledRoute calculateDelay(MapScheduledRoute route) {
        List<MapScheduledNode> nodes = route.getStops();
        double firstTime = nodes.get(0).getTime();
        double firstX = nodes.get(0).getNode().getLocation().getX();
        double firstY = nodes.get(0).getNode().getLocation().getY();
        long seed = (long) (firstTime + firstX + firstY);
        Random rng = new Random(seed);

        List<MapScheduledNode> newNodes = new ArrayList<>();
        for (MapScheduledNode node: nodes) {
            MapScheduledNode nextNode = node;
            if (rng.nextDouble() < probability) {
                double delay = (maxDelay - minDelay) * rng.nextDouble() + minDelay;
                double newTime = node.getTime() + delay;
                try {
                    double lastTime = newNodes.get(newNodes.size() - 1).getTime();
                    if (newTime < lastTime) {
                        newTime = lastTime;
                    }
                } catch (ArrayIndexOutOfBoundsException err) {/* First element will catch. Do nothing */}
                nextNode = node.updateTime(newTime);
            } else {
                try {
                    double lastTime = newNodes.get(newNodes.size() - 1).getTime();
                    if (node.getTime() < lastTime) {
                        nextNode = node.updateTime(lastTime);
                    }
                } catch (ArrayIndexOutOfBoundsException err) {/* First element will catch. Do nothing */}
            }
            newNodes.add(nextNode);
        }
        return new MapScheduledRoute(newNodes);
    }
}

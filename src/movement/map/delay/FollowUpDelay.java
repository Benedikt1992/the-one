package movement.map.delay;

import core.Settings;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This DelayModel adds random delays to the route. Each stop recognizes the preceding delays.
 * The amount of delay that can be added t each stop needs to be specified with Group.delayRange = min,max
 * min and max can be negative.
 * The probability to have any delay change is defined with the setting delayProbability.
 * If it is not specified it will run with a probability of 1 which means every stop will get a delay change.
 */
public class FollowUpDelay extends RandomDelay {
    /**
     * Creates a new DelayModel based on a Settings object's settings.
     *
     * @param settings The Settings object where the settings are read from
     */
    public FollowUpDelay(Settings settings) {
        super(settings);
    }

    @Override
    public MapScheduledRoute calculateDelay(MapScheduledRoute route) {
        List<MapScheduledNode> nodes = route.getStops();
        double firstTime = nodes.get(0).getTime();
        double firstX = nodes.get(0).getNode().getLocation().getX();
        double firstY = nodes.get(0).getNode().getLocation().getY();
        long seed = (long) (firstTime + firstX + firstY);
        Random rng = new Random(seed);

        double currentDelay = 0;
        List<MapScheduledNode> newNodes = new ArrayList<>();
        for (MapScheduledNode node: nodes) {
            if (rng.nextDouble() < probability) {
                double delayChange = (maxDelay - minDelay) * rng.nextDouble() + minDelay;
                currentDelay = currentDelay + delayChange;
            }

            double newTime = node.getTime() + currentDelay;
            try {
                double lastTime = newNodes.get(newNodes.size() - 1).getTime();
                if (newTime < lastTime) {
                    newTime = lastTime;
                }
            } catch (ArrayIndexOutOfBoundsException err) {/* First element will catch. Do nothing */}

            if (newTime < 0) {
                newTime = 0;
            }

            MapScheduledNode nextNode = node;
            if (newTime != node.getTime()) {
                nextNode = node.updateTime(newTime);
            }

            newNodes.add(nextNode);
        }
        return new MapScheduledRoute(newNodes);
    }
}

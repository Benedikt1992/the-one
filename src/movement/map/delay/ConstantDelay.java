package movement.map.delay;

import core.Settings;
import movement.map.MapScheduledNode;
import movement.map.MapScheduledRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * This DelayModel add a constant delay to each route.
 * The amount of delay needs to be specified with Group.delay
 */
public class ConstantDelay extends DelayModel {
    /** The amount of delay each train has right from the beginning. */
    public static final String DELAY_S = "delay";
    /** The amount of delay added to each route */
    private double delay;
    /**
     * Creates a new DelayModel based on a Settings object's settings.
     *
     * @param settings The Settings object where the settings are read from
     */
    public ConstantDelay(Settings settings) {
        super(settings);
        delay = settings.getDouble(DELAY_S);
    }

    @Override
    public MapScheduledRoute calculateDelay(MapScheduledRoute route) {
        List<MapScheduledNode> nodes = route.getStops();
        List<MapScheduledNode> newNodes = new ArrayList<>();
        for (MapScheduledNode node :
                nodes) {
            newNodes.add(node.updateTime(node.getTime() + this.delay));
        }
        return new MapScheduledRoute(newNodes);
    }
}

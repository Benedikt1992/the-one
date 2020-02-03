package movement.map.delay;

import core.Settings;
import movement.map.MapScheduledRoute;

public class NoDelay extends DelayModel {
    public NoDelay(Settings settings) {
        super(settings);
    }

    /**
     * Do nothing. No delay.
     * @param route Original scheduled route
     * @return
     */
    @Override
    public MapScheduledRoute calculateDelay(MapScheduledRoute route) {
        return route;
    }
}

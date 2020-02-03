package movement.map.delay;

import core.Settings;
import input.ScheduleReader;
import movement.map.MapScheduledRoute;

/**
 * <P>Superclass for all delay models used within the {@link movement.MapScheduledMovement} model.
 * All subclasses must contain at least a constructor with one {@link Settings} parameter.
 * They must also implement the {@link #calculateDelay(MapScheduledRoute)} method which should return
 * the modified {@link MapScheduledRoute} object.</P>
 * TODO: Do we need to copy the route object?
 */
public abstract class DelayModel {
    /**
     * Creates a new DelayModel based on a Settings object's settings.
     * @param settings The Settings object where the settings are read from
     */
    public DelayModel(Settings settings) {
        super();
    }

    /**
     * Updates the timestamps of each stop in the route according to the delay model.
     * See also {@link MapScheduledRoute} and {@link ScheduleReader}
     * @param route Original scheduled route
     * @return Route with delayed time stamps
     */
    public abstract MapScheduledRoute calculateDelay(MapScheduledRoute route);
}

/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import core.Coord;
import core.Settings;
import core.SettingsError;
import input.WKTReader;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A stationary "movement" model where nodes do not move.
 * Multiple Nodes can be add via a file containing wkt POINT entries.
 * Useful to simulate stations with {@link MapScheduledMovement}.
 */
public class StationaryListMovement extends MapBasedMovement {
	/** Per node group setting for setting the location ({@value}) */
	public static final String LOCATION_FILE_S = "nodeLocationsFile";
	private Coord loc; /** The location of the node */
	private List<Coord> locations = null; /** All locations */
	private Integer nextLocationIndex = null;

	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param s The Settings object where the settings are read from
	 */
	public StationaryListMovement(Settings s) {
		super(s);
		String fileName = s.getSetting(LOCATION_FILE_S);
		WKTReader reader = new WKTReader();
		boolean mirror = getMap().isMirrored();
		double xOffset = getMap().getOffset().getX();
		double yOffset = getMap().getOffset().getY();
		try {
			File locationFile = new File(fileName);
			locations = reader.readPoints(locationFile);
		} catch (IOException ioe){
			throw new SettingsError("Couldn't read StationaryList-data file " +
					fileName + 	" (cause: " + ioe.getMessage() + ")");
		}

		for (Coord c : locations) {
			if (mirror) {
				c.setLocation(c.getX(), -c.getY());
			}
			c.translate(xOffset, yOffset);
		}


		nextLocationIndex = 0;
		loc = locations.get(nextLocationIndex).clone();
	}

	/**
	 * Copy constructor.
	 * @param sm The StationaryMovement prototype
	 */
	public StationaryListMovement(StationaryListMovement sm) {
		super(sm);
		this.loc = sm.locations.get(sm.nextLocationIndex).clone();
		sm.nextLocationIndex++;
		if (sm.nextLocationIndex >= sm.locations.size()) {
			sm.nextLocationIndex = 0;
		}
	}

	/**
	 * Returns the only location of this movement model
	 * @return the only location of this movement model
	 */
	@Override
	public Coord getInitialLocation() {
		return loc;
	}

	/**
	 * Returns a single coordinate path (using the only possible coordinate)
	 * @return a single coordinate path
	 */
	@Override
	public Path getPath() {
		Path p = new Path(0);
		p.addWaypoint(loc);
		return p;
	}

	@Override
	public double nextPathAvailable() {
		return Double.MAX_VALUE;	// no new paths available
	}

	@Override
	public StationaryListMovement replicate() {
		return new StationaryListMovement(this);
	}

}

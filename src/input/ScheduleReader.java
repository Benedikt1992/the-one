/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.Coord;
import util.Tuple;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Class for reading a schedule for MapScheduledMovement model. It consists of ROUTE entries.
 * Each ROUTE is assigned to one host in the simulation. A ROUTE directive  consists of a list of tuples of
 * simulation time x and y (e.g. ROUTE(time1 x1 y1, time2 x2 y2))
 * Entries within the route must be ordered by time. First entry is considered start point. If there is a waiting time,
 * there should be two consecutive entries with same coordinates (`ROUTE(ArrivalTime x y, DepartureTime, x y)`)
 */
public class ScheduleReader {
	public static final String ROUTE = "ROUTE";

	/** are all lines of the file read */
	private boolean done;
	/** reader for the data */
	private BufferedReader reader;

	/**
	 * Read route (ROUTE) data from a file
	 * @param file The file to read data from
	 * @return A list of time-coordinate-tuple lists read from the file
	 * @throws IOException if something went wrong while reading
	 */
	public List<List<Tuple<Double, Coord>>> readRoutes(File file) throws IOException {
		List<List<Tuple<Double, Coord>>> routes = new ArrayList<List<Tuple<Double, Coord>>>();

		String type;
		init(new FileReader(file));

		while((type = nextType()) != null) {
			if (type.equals(ROUTE)) {
				routes.add(parseRouteString(readNestedContents()));
			}
			else {
				// known type but not interesting -> skip
				readNestedContents();
			}
		}

		return routes;
	}


	/**
	 * Initialize the reader to use a certain input reader
	 * @param input The input to use
	 */
	protected void init(Reader input) {
		setDone(false);
		reader = new BufferedReader(input);
	}

	/**
	 * Returns the next type read from the reader given at init or null
	 * if no more types can be read
	 * @return the next type read from the reader given at init
	 * @throws IOException
	 */
	protected String nextType() throws IOException {
		String type = null;

		while (!done && type == null) {
			type = readWord(reader);

			if (type.length() < 1) { // discard empty lines
				type = null;
				continue;
			}
		}

		return type;
	}

	/**
	 * Returns true if type is one of the known WKT types
	 * @param type The type to check
	 * @return true if type is one of the known WKT types
	 */
	protected boolean isKnownType(String type) {
		if (type.equals(ROUTE)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Reads a "word", ie whitespace delimited string of characters, from
	 * the reader
	 * @param r Reader to read the characters from
	 * @return The word that was read (or empty string if nothing was read)
	 * @throws IOException
	 */
	protected String readWord(Reader r) throws IOException {
		StringBuffer buf = new StringBuffer();
		char c = skipAllWhitespace(r);

		// read non-whitespace part
		while(c != (char)-1 && !Character.isWhitespace(c)) {
			buf.append(c);
			c = (char)r.read();
		}

		if (c == (char)-1) {
			setDone(true);
		}
		return buf.toString();
	}

	/**
	 * Reads and skips all characters until character "until" is read or
	 * end of stream is reached. Also the expected character is discarded.
	 * @param r Reader to read characters from
	 * @param until What character to expect
	 * @throws IOException
	 */
	protected void skipUntil(Reader r, char until) throws IOException {
		char c;
		do {
			c = (char)r.read();
		} while (c != until && c != (char)-1);
	}

	/**
	 * Skips all consecutive whitespace characters from reader
	 * @param r Reader where the whitespace is skipped
	 * @return First non-whitespace character read from the reader
	 * @throws IOException
	 */
	protected char skipAllWhitespace(Reader r) throws IOException {
		char c;
		do {
			c = (char)r.read();
		} while (Character.isWhitespace(c) && c != (char)-1);

		return c;
	}

	/**
	 * Reads everything from the first opening parenthesis until line that
	 * ends to a closing parenthesis and returns the contents in one string
	 * @param r Reader to read the input from
	 * @return The text between the parentheses
	 */
	public String readNestedContents(Reader r) throws IOException {
		StringBuffer contents = new StringBuffer();
		int parOpen; // nrof open parentheses
		char c = '\0';

		skipUntil(r,'(');
		parOpen = 1;

		while (c != (char)-1 && parOpen > 0) {
			c = (char)r.read();
			if (c == '(') {
				parOpen++;
			}
			if (c == ')') {
				parOpen--;
			}
			if (Character.isWhitespace(c)) {
				c = ' '; // convert all whitespace to basic space
			}
			contents.append(c);
		}

		contents.deleteCharAt(contents.length()-1);	// remove last ')'
		return contents.toString();
	}

	/**
	 * Returns nested contents from the reader given at init
	 * @return nested contents from the reader given at init
	 * @throws IOException
	 * @see #readNestedContents(Reader)
	 */
	public String readNestedContents() throws IOException {
		return readNestedContents(reader);
	}

	/**
	 * Parses time-coordinate-tuples from "ROUTE" lines
	 * @param line String that contains the whole "ROUTE"'s content
	 * @return List of time-coordinate-tuples parsed from the route
	 */
	protected List<Tuple<Double, Coord>> parseRouteString(String line) {
		List<Tuple<Double, Coord>> tuples = new ArrayList<Tuple<Double, Coord>>();
		Scanner lineScan;
		Scanner tupleScan;
		double t,x,y;
		Coord c;

		lineScan = new Scanner(line);
		lineScan.useDelimiter(",");

		while (lineScan.hasNext()) {
			tupleScan = new Scanner(lineScan.next());
			t = Double.parseDouble(tupleScan.next());
			x = Double.parseDouble(tupleScan.next());
			y = Double.parseDouble(tupleScan.next());
			c = new Coord(x,y);

			tupleScan.close();
			tuples.add(new Tuple<Double, Coord>(t,c));
		}

		lineScan.close();
		return tuples;
	}

	/**
	 * Returns true if the whole file has been read
	 * @return true if the whole file has been read
	 */
	protected boolean isDone() {
		return this.done;
	}

	/**
	 * Sets the "is file read" state
	 * @param done If true, reading is done
	 */
	protected void setDone(boolean done) {
		this.done = done;
	}

}

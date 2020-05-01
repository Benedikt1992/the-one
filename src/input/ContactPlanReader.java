/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package input;

import core.Coord;
import util.Tuple;

import java.io.*;
import java.util.*;

/**
 * Class for reading a contact plan for ContactGraphRouter with the ContactPlanGraph.
 * Each line represents one host and its contact list during the simulation. A contact list consists of a
 * list of tuples of address of the meeting partner, start and end time (e.g. 1 (123 22.09 23.45, 321 134.00 134.23)).
 * The contact list is not ordered and each entry in the contact list means the host can send messages to the partner
 * within the time frame. There is no implication that the partner can send messages to the host.
 */
public class ContactPlanReader {

	/** are all lines of the file read */
	private boolean done;
	/** reader for the data */
	private BufferedReader reader;

	/**
	 * Read contact data from a file
	 * @param file The file to read data from
	 * @return A map with contact lists per host
	 * @throws IOException if something went wrong while reading
	 */
	public Map<Integer, List<Contact>> readContacts(File file) throws IOException {
		Map<Integer, List<Contact>> contactPlan = new HashMap<>();

		String host;
		init(new FileReader(file));

		while((host = nextHost()) != null) {
			contactPlan.put(Integer.parseInt(host), parseContactList(readNestedContents()));
		}

		return contactPlan;
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
	protected String nextHost() throws IOException {
		String host = null;

		while (!done && host == null) {
			host = readWord(reader);

			if (host.length() < 1) { // discard empty lines
				host = null;
			}
		}

		return host;
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
	protected List<Contact> parseContactList(String line) {
		List<Contact> contacts = new ArrayList<>();
		Scanner lineScan;
		Scanner tupleScan;
		int p;
		double s,e;

		lineScan = new Scanner(line);
		lineScan.useDelimiter(",");

		while (lineScan.hasNext()) {
			tupleScan = new Scanner(lineScan.next());
			p = Integer.parseInt(tupleScan.next());
			s = Double.parseDouble(tupleScan.next());
			e = Double.parseDouble(tupleScan.next());

			tupleScan.close();

			contacts.add(new Contact(p, s, e));
		}

		lineScan.close();
		return contacts;
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



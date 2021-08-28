package graph.gedcom;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

public class Util {

	public static int VERTICAL_SPACE = 90; // Vertical space between rows of nodes
	public static int HORIZONTAL_SPACE = 15; // Horizontal space between nodes
	public static int UNION_DISTANCE = 25; // Horizontal space between unions

	public static int BOND_WIDTH = 24; // Horizontal distance between partners
	public static int MINI_BOND_WIDTH = 20; // Horizontal space between ancestry husband and wife
	public static int MARRIAGE_WIDTH = 40; // Width of the marriage year oval or of the empty marriage (overlaps included)
	public static int MARRIAGE_HEIGHT = 25; // Height of marriage year oval
	public static int HEARTH_DIAMETER = 8; // Family node without date
	public static int MINI_HEARTH_DIAMETER = 6; // Mini family node without date

	public static int LITTLE_GROUP_DISTANCE = 50; // Vertical space below family node without partners or mini ancestry, both with many children
	public static int ANCESTRY_DISTANCE = 20; // Vertical space between mini ancestry and person node
	public static int PROGENY_DISTANCE = 25; // Vertical space between family node and mini progeny
	public static int PROGENY_PLAY = 15; // Horizontal space between progeny mini cards

	// Card types
	enum Card {
		FULCRUM, REGULAR, ANCESTRY, PROGENY;
	}

	// Position of the node in the group
	enum Position {
		FIRST, MIDDLE, LAST;
	}

	// Actual ancestors branch: paternal (left) or maternal (right). NONE is for single ancestors (male or female).
	enum Branch {
		NONE, PATER, MATER;
	}

	// Requested position of uncles respect to parents node: a single parent can have siblings (the uncles) on both left and right sides.
	// Same for half-siblings of fulcrum when a single parent is shown: they can be on the left and to the right of fulcrum.
	enum Side {
		NONE, LEFT, RIGHT;
	}

	// The very basic about a person
	public static String essence(Person person) {
		String str = "";
		if( person != null ) {
			// str = person.getId() + " ";
			if( !person.getNames().isEmpty() ) {
				Name name = person.getNames().get(0);
				str += name.getDisplayValue().replaceAll("/", "");
				if( name.getNickname() != null )
					str += " \"" + name.getNickname() + "\"";
			}
			// str += " " + person.hashCode();
		}
		// if( str.length() > 1 ) str = str.substring(0, 1);
		return str;
	}

	// Prints everything to the console
	public static void p(Object... objects) {
		String str = "";
		for(Object obj : objects) 
			str += obj + " ";
		System.out.println(str);
	}
}

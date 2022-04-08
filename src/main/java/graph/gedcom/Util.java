package graph.gedcom;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

public class Util {

	public static int VERTICAL_SPACE = 90; // Vertical space between rows of nodes
	public static int HORIZONTAL_SPACE = 15; // Horizontal space between nodes
	public static int UNION_DISTANCE = 25; // Horizontal space between unions

	public static int BOND_WIDTH = 23; // Horizontal distance between partners with no marriage oval
	public static int MINI_BOND_WIDTH = 18; // Horizontal space between ancestry husband and wife
	public static int MARRIAGE_WIDTH = 39; // Width of the marriage year oval (overlaps included)
	public static int MARRIAGE_INNER_WIDTH = 25;
	public static int MARRIAGE_HEIGHT = 25; // Height of marriage year oval
	public static int HEARTH_DIAMETER = 8; // Family node without date
	public static int MINI_HEARTH_DIAMETER = 6; // Mini family node without date

	public static int LITTLE_GROUP_DISTANCE = 60; // Vertical space below family node without partners or mini ancestry, both with many children
	public static int ANCESTRY_DISTANCE = 12; // Vertical space between mini ancestry and person node
	public static int PROGENY_DISTANCE = 16; // Vertical space between family node and mini progeny
	public static int PROGENY_PLAY = 12; // Horizontal space between progeny mini cards

	// Card types
	enum Card {
		FULCRUM, REGULAR, ANCESTRY, PROGENY;
	}

	// Position of the node in the group
	enum Position {
		FIRST, MIDDLE, LAST;
	}

	/* Used to point out a single or multi marriage in FamilyNode
	 * SOLE: it's a single marriage only
	 * NEAR: the marriage is close to the person
	 * FAR: it's at left or right extreme
	 */
	enum Match {
		SOLE, NEAR, MIDDLE, FAR;

		static Match get(int totFamilies, Side side, int index) {
			if( totFamilies > 1 ) {
				if( (side == Side.RIGHT && index == 0) || (side == Side.LEFT && index == totFamilies - 1) )
					return NEAR;
				else if( (side == Side.RIGHT && index == totFamilies - 1) || (side == Side.LEFT && index == 0) )
					return FAR;
				else
					return MIDDLE;
			}
			return SOLE;
		}

		// Not so elegant this duplicated method. Maybe it could be unified with the previous one.
		static Match get2(int totFamilies, Side side, int index) {
			if( (side == Side.LEFT && index == 0) || (side == Side.RIGHT && index == totFamilies - 1) )
				return FAR;
			else
				return MIDDLE;
		}
	}

	// Actual ancestors branch: paternal (left) or maternal (right). NONE is for single ancestors (male or female).
	enum Branch {
		NONE, PATER, MATER;
	}

	// Requested position of uncles respect to parents node: a single parent can have siblings (the uncles) on both left and right sides.
	// Same for half-siblings of fulcrum when a single parent is shown: they can be on the left and to the right of fulcrum.
	enum Side {
		NONE, LEFT, RIGHT;

		Branch getBranch() {
			switch( this ) {
				case LEFT: return Branch.PATER;
				case RIGHT: return Branch.MATER;
				default: return Branch.NONE;
			}
		}
	}

	public enum Gender {

		NONE, // No SEX tag
		MALE, // 'SEX M'
		FEMALE, // 'SEX F'
		UNDEFINED, // 'SEX U'
		OTHER; // Some other value

		// Find the gender of a Person
		public static Gender getGender(Person person) {
			for( EventFact fact : person.getEventsFacts() ) {
				if( fact.getTag() != null && fact.getTag().equals("SEX") ) {
					switch( fact.getValue() ) {
						case "M": return MALE;
						case "F": return FEMALE;
						case "U": return UNDEFINED;
						default: return OTHER;  // NULL or other value
					}
				}
			}
			return NONE; // There is no 'SEX' tag
		}

		public static boolean isMale(Person person) {
			return getGender(person) == MALE;
		}

		public static boolean isFemale(Person person) {
			return getGender(person) == FEMALE;
		}
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

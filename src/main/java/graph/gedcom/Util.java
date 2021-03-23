package graph.gedcom;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Person;

public class Util {
	
	public static int VERTICAL_SPACE = 90; // Vertical space between rows of nodes
	public static int HORIZONTAL_SPACE = 17; // Horizontal space between nodes
	public static int GROUP_DISTANCE = 25; // Horizontal space between groups

	public static int FAMILY_WIDTH = 40; // Standard space between husband and wife
	public static int MARRIAGE_HEIGHT = 25; // Height of marriage year oval
	public static int MARRIAGE_OVERLAP = 7; // Horizontal overlap of the marriage year oval over partners
	public static int MINI_FAMILY_WIDTH = 20; // Horizontal space between ancestry husband and wife
	public static int HEARTH_DIAMETER = 8; // Family node without date
	public static int MINI_HEARTH_DIAMETER = 6; // Mini family node without date

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

	/**
	 * Utility to know the sex of a person.
	 * 
	 * @param person The person whose sex we want to know
	 * @return int 0 no sex, 1 male, 2 female, 3 undefined, 4 other
	 */
	public static int sex(Person person) {
		for (EventFact ef : person.getEventsFacts()) {
			if (ef.getTag() != null && ef.getTag().equals("SEX")) {
				if (ef.getValue() == null)
					return 4; // SEX tag exists but without value
				else {
					switch (ef.getValue()) {
					case "M":
						return 1;
					case "F":
						return 2;
					case "U":
						return 3;
					default:
						return 4; // other value
					}
				}
			}
		}
		return 0; // no SEX tag
	}

	/**
	 * The very basic about a person.
	 * 
	 * @param person
	 * @return String
	 */
	public static String essence(Person person) {
		String str = "";
		if (person != null) {
			//str = person.getId() + " ";
			if (!person.getNames().isEmpty())
				str += person.getNames().get(0).getDisplayValue().replaceAll("/", "");
			// str += " " + person.hashCode();
		}
		return str;
	}
	
	/**
	 * About a person tells if is dead or buried.
	 * @param person The suspect
	 * @return Is their dead?
	 */
	public static boolean dead( Person person ) {
		for( EventFact fact : person.getEventsFacts() ) {
			if( fact.getTag().equals( "DEAT" ) || fact.getTag().equals( "BURI" ) )
				return true;
		}
		return false;
	}
	
	// Prints everything to the console
	public static void p(Object... objects) {
		String str = "";
		for(Object obj : objects) 
			str += obj + " ";
		System.out.println(str);
	}
}

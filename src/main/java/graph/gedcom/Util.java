package graph.gedcom;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Person;

public class Util {
	
	public static int PADDING = 25; // Horizontal space between nodes
	public static int MARGIN = 30; // Standard space between husband and wife
	public static int SPACE = 180; // Vertical space between rows of cards
	public static int TIC = 15; // Horizontal overlap of the marriage year over the cards
	public static int GAP = 40; // Vertical space between unit and progeny
	public static int PLAY = 30; // Horizontal space between progeny mini cards

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
			if (!person.getNames().isEmpty())
				str = person.getNames().get(0).getDisplayValue().replaceAll("/", "");
			// str += " " + person.hashCode();
		}
		return str;
	}
	
	/**
	 * About a person tells if is dead or buried.
	 * @param person The suspect
	 * @return Is he/she dead?
	 */
	public static boolean dead( Person person ) {
		for( EventFact fact : person.getEventsFacts() ) {
			if( fact.getTag().equals( "DEAT" ) || fact.getTag().equals( "BURI" ) )
				return true;
		}
		return false;
	}
	
	// Prints everything to the console
	public static void pr(Object... objects) {
		String str = "";
		for(Object obj : objects) 
			str += obj + " ";
		System.out.println(str);
	}
}

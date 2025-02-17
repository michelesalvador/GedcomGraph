package graph.gedcom;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

public class Util {

    public static int VERTICAL_SPACE = 90; // Vertical space between rows of nodes
    public static int VERTICAL_SPACE_CALC; // Same resized on displayNumbers
    public static int HORIZONTAL_SPACE = 15; // Horizontal space between nodes of the same group
    public static int UNION_DISTANCE = 35; // Horizontal space between unions of the same row

    public static int BOND_WIDTH = 23; // Horizontal distance between partners with no marriage oval
    public static int MINI_BOND_WIDTH = 18; // Horizontal space between ancestry husband and wife
    public static int MARRIAGE_WIDTH = 39; // Width of the marriage year oval (overlaps included)
    public static int MARRIAGE_INNER_WIDTH = 25;
    public static int MARRIAGE_HEIGHT = 25; // Height of marriage year oval
    public static int HEARTH_DIAMETER = 8; // Family node without date
    public static int MINI_HEARTH_DIAMETER = 6; // Mini family node without date

    public static int LITTLE_GROUP_DISTANCE = 60; // Vertical space below family node without partners or below mini ancestry, both with many children
    public static int LITTLE_GROUP_DISTANCE_CALC; // Same resized on displayNumbers
    public static int ANCESTRY_DISTANCE = 12; // Vertical space between mini ancestry and person node
    public static int PROGENY_DISTANCE = 16; // Vertical space between family node and mini progeny
    public static int PROGENY_PLAY = 12; // Horizontal space between progeny mini cards

    /**
     * Card types different in size and function.
     */
    enum Card {
        FULCRUM, // Fulcrum card: only one card has this type
        REGULAR, // Regular size
        ANCESTRY, // Little ancestry above
        PROGENY; // Little progeny below
    }

    /**
     * Position of the node in the group.
     */
    enum Position {
        FIRST, MIDDLE, LAST;
    }

    /**
     * Points out a node as main or multi marriage.
     */
    enum Match {
        MAIN, // Single marriage or the first of a series of multi-marriages
        NEAR, // The marriage is the other one next to the person
        MIDDLE, // Zero to N marriages between NEAR and FAR
        FAR; // The marriage is at extreme left or right

        /**
         * @param side     LEFT for husband and RIGHT for wife
         * @param straight Person inside the MAIN family is not gay and in the right role (husband or wife)
         */
        static Match get(int totFamilies, int index, Side side, boolean straight) {
            if (side == Side.RIGHT) {
                if (index == 0)
                    return MAIN;
                else if (index == 1 && straight)
                    return NEAR;
                else if (index == totFamilies - 1)
                    return FAR;
            } else {
                if (index == totFamilies - 1)
                    return MAIN;
                else if (index == totFamilies - 2 && straight)
                    return NEAR;
                else if (index == 0 /* && totFamilies > 2 */)
                    return FAR;
            }
            return MIDDLE;
        }

        // Similar to the previous one, but specific for ancestors
        static Match getForAncestors(int totFamilies, int index, Side side) {
            if (side == Side.LEFT && index == totFamilies - 1 || side == Side.RIGHT && index == 0)
                return NEAR;
            else if (side == Side.LEFT && index == 0 || side == Side.RIGHT && index == totFamilies - 1)
                return FAR;
            else
                return MIDDLE;
        }

        @Override
        public String toString() {
            switch (this) {
            case MAIN:
                return "MAIN";
            case NEAR:
                return "NEAR";
            case MIDDLE:
                return "MIDDLE";
            case FAR:
                return "FAR";
            default:
                return null;
            }
        }
    }

    /**
     * Actual ancestors branch.
     */
    enum Branch {
        NONE, // Single ancestors (male or female)
        PATER, // Paternal (left)
        MATER; // Maternal (right)
    }

    /**
     * Requested position of uncles respect to parents node: a single parent can have siblings (the uncles) on both left and right sides.
     * Same for half-siblings of fulcrum when a single parent is shown: they can be on the left and to the right of fulcrum.
     */
    enum Side {
        NONE, LEFT, RIGHT;
    }

    public enum Gender {

        NONE, // No SEX tag or null value
        MALE, // 'SEX M'
        FEMALE, // 'SEX F'
        UNDEFINED, // 'SEX U'
        OTHER; // Some other value

        // Find the gender of a Person
        public static Gender getGender(Person person) {
            for (EventFact fact : person.getEventsFacts()) {
                if (fact.getTag() != null && fact.getTag().equals("SEX") && fact.getValue() != null) {
                    switch (fact.getValue()) { // Can not be null
                    case "M":
                        return MALE;
                    case "F":
                        return FEMALE;
                    case "U":
                        return UNDEFINED;
                    default:
                        return OTHER; // Other value
                    }
                }
            }
            return NONE; // There is no 'SEX' tag or the value is null
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
        if (person != null) {
            // str = person.getId() + " ";
            if (!person.getNames().isEmpty()) {
                Name name = person.getNames().get(0);
                str += name.getDisplayValue().replaceAll("/", "");
                // if (name.getNickname() != null) str += " \"" + name.getNickname() + "\"";
            }
            // str += " " + person.hashCode();
        }
        // if (str.length() > 10) str = str.substring(0, 10);
        if (str.isBlank())
            str = "[No name]";
        return str;
    }

    // Prints everything to the console
    public static void p(Object... objects) {
        String str = "";
        for (Object obj : objects)
            str += obj + " ";
        System.out.println(str);
    }
}

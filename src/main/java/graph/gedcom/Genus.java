package graph.gedcom;

import java.util.ArrayList;

import org.folg.gedcom.model.Person;

/**
 * List of one or many nodes (FamilyNode and PersonNode) representig a single or multiple marriages of one person.
 *
 * Node > Genus > Group > Union
 */
public class Genus extends ArrayList<Node> {

    /**
     * Checks if the genus already contains the person.
     */
    boolean contains(Person person) {
        for (Node node : this) {
            for (PersonNode personNode : node.getPersonNodes()) {
                if (personNode.person.equals(person)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String txt = "[";
        for (Node node : this) {
            txt += node;
            // txt += " " + node.getMatch();
            txt += " - ";
        }
        txt = txt.replaceAll(" - $", "");
        txt += "]";
        return txt;
    }
}

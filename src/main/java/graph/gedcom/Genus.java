package graph.gedcom;

import org.folg.gedcom.model.Person;

import java.util.ArrayList;

/**
 * List of one or many nodes (FamilyNode and PersonNode) representing a single or multiple marriages of one person.
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
        StringBuilder builder = new StringBuilder("[");
        for (Node node : this) {
            builder.append(node);
            //builder.append(" ").append(node.match);
            builder.append(" - ");
        }
        builder = new StringBuilder(builder.toString().replaceAll(" - $", ""));
        builder.append("]");
        return builder.toString();
    }
}

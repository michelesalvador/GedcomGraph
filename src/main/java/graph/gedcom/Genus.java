package graph.gedcom;

import java.util.ArrayList;

/**
 * List of one or many nodes (FamilyNode and PersonNode) representig a single or multiple marriages of one person.
 *
 * Node > Genus > Group > Union
 */
public class Genus extends ArrayList<Node> {

    @Override
    public String toString() {
        String txt = "[";
        for (Node node : this) {
            txt += node;
            // txt += " " + node.getMatch();
            txt += " - ";
        }
        txt = txt.replaceAll(" - $", "]");
        return txt;
    }
}

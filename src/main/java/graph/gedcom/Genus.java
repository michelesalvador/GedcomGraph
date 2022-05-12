// List of one or many nodes (FamilyNode and PersonNode) representig a single or multiple marriages of one person
// Node -> Genus -> Group -> Union

package graph.gedcom;

import java.util.ArrayList;

public class Genus extends ArrayList<Node> {
	
	@Override
	public String toString() {
		String txt = "[";
		for( Node node : this ) {
			txt += node;
			txt += " " + node.match;
			txt += " - ";
		}
		txt = txt.replaceAll(" - $", "]");
		return txt;
	}
}

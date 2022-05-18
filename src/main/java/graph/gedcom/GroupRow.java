package graph.gedcom;

import static graph.gedcom.Util.*;
import java.util.ArrayList;
import java.util.List;

class GroupRow extends ArrayList<Group> {

	int generation;

	GroupRow(int generation) {
		this.generation = generation;
	}

	// Resolve overlaps of this row of groups
	void resolveOverlap() {
		List<Node> group = get(size() / 2).list;
		Node central = group.get(group.size() / 2); // More or less the node in the center of the group
		Node left = central;
		while( left.prev != null ) {
			float gap = left.union.equals(left.prev.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
			float overlap = left.prev.x + left.prev.width + gap - left.x;
			if( overlap > 0 ) {
				left.prev.shift(-overlap / 2);
				left.shift(overlap / 2);
			}
			left = left.prev;
		}
		Node right = central;
		while( right.next != null ) {
			float gap = right.union.equals(right.next.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
			float overlap = right.x + right.width + gap - right.next.x;
			if( overlap > 0 ) {
				right.shift(-overlap / 2);
				right.next.shift(overlap / 2);
			}
			right = right.next;
		}
	}

	// Place nodes of this ancestor row
	public void placeAncestors() {
		for( Group group : this ) {
			group.placeAncestors();
		}
	}

	@Override
	public String toString() {
		String txt = generation + ": <";
		for( Group group : this )
			txt += group + ", ";
		txt = txt.replaceAll(", $", ">");
		return txt;
	}
}

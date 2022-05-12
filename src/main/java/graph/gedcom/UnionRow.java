// List of unions of the same generation, ordered left to right

package graph.gedcom;

import static graph.gedcom.Util.*;
import java.util.ArrayList;
import java.util.List;

public class UnionRow extends ArrayList<Union> {

	int generation;
	float yAxe;

	UnionRow(int generation, float yAxe) {
		this.generation = generation;
		this.yAxe = yAxe;
	}

	void addUnion(Union union) {
		add(union);
	}

	// Resolve overlaps of this row of unions
	void resolveOverlap() {
		List<Node> union = get(size() / 2).list;
		Node central = union.get(union.size() / 2); // More or less the node in the center of the union
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

	@Override
	public String toString() {
		String txt = generation + ": <";
		for( Union union : this )
			txt += union + ", ";
		txt = txt.replaceAll(", $", ">");
		return txt;
	}
}

// Abstract class to be extended in a PersonNode or FamilyNode

package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import org.folg.gedcom.model.Family;
import graph.gedcom.Util.Match;
import static graph.gedcom.Util.*;

public abstract class Node extends Metric {

	public Family spouseFamily; // The person in this node is spouse of this family
	Group group; // List of siblings to which this node belongs
	              // Warning: an ancestor node can belong at the same time to 2 groups, making this 'group' useless
	Group youth; // List of PersonNode and FamilyNode descendants of a PersonNode or of a FamilyNode
	public int generation; // Number of the generation to which this node belongs (0 for fulcrum, negative up and positive down)
	public boolean mini; // The PersonNode will be displayed little (with just a number), and the familyNode without marriage date
	boolean isAncestor;
	Union union; // The union this node belongs to (expecially for ancestors)
	Node prev, next; // Previous and next node on the same row (same generation)
	List<Match> matches; // List of the relationship positions.
	                     // When there are 2 ancestors, match 0 is for husband (right branch MATER) and match 1 is for wife (left branch PATER).
	float force;

	Node() {
		matches = new ArrayList<>();
	}

	// Calculate the initial width of first node (complementar to getMainWidth())
	abstract float getLeftWidth(Branch branch);

	// Calculate the to-center width of the main node included bond and partner
	abstract float getMainWidth(Position pos);

	// Horizontally distribute progeny nodes
	void placeYouthX() {
		// Place stallion child and their spouses
		if( youth.stallion != null ) {
			youth.stallion.setX(centerX() - youth.stallion.getLeftWidth(null));
			Node right = youth.stallion;
			while( right.next != null ) {
				right.next.setX(right.x + right.width + HORIZONTAL_SPACE);
				right = right.next;
			}
			Node left = youth.stallion;
			while( left.prev != null ) {
				left.prev.setX(left.x - HORIZONTAL_SPACE - left.prev.width);
				left = left.prev;
			}
		} else { // Place normal youth
			float posX = centerX() - youth.getBasicLeftWidth() - youth.getBasicCentralWidth() / 2;
			for( Node child : youth.list ) {
				child.setX(posX);
				posX += child.width + HORIZONTAL_SPACE;
			}
		}
	}

	// Horizontally distribute mini progeny nodes
	void placeMiniChildrenX() {
		if( youth != null && youth.mini ) {
			float posX = 0;
			for( Node child : youth.list ) {
				child.setX(posX);
				posX += child.width + PROGENY_PLAY;
			}
			youth.setX(centerX() - youth.getWidth() / 2);
		}
	}

	// Horizontally update mini progeny position
	void setMiniChildrenX() {
		if( youth != null && youth.mini ) {
			float posX = centerX() - youth.width / 2;
			for( Node child : youth.list ) {
				child.setX(posX);
				posX += child.width + PROGENY_PLAY;
			}
		}
	}

	// Position the origin of the acquired spouse
	void placeAcquiredOriginX() {
		if( this instanceof FamilyNode ) {
			for( PersonNode partner : ((FamilyNode)this).partners ) {
				if( partner.acquired && partner.origin != null ) {
					partner.origin.setX(partner.centerX() - partner.origin.centerRelX());
				}
			}
		}
	}

	void placeAcquiredOriginY() {
		if( this instanceof FamilyNode && !mini ) {
			for( PersonNode partner : ((FamilyNode)this).partners ) {
				if( partner.acquired && partner.origin != null ) {
					partner.origin.setY(partner.y - ANCESTRY_DISTANCE - partner.origin.height);
				}
			}
		}
	}

	// Hybrid methods for FamilyNode and PersonNode

	abstract Node getOrigin();

	// A list with zero, one or two origin nodes
	abstract List<Node> getOrigins();

	abstract boolean hasOrigins();

	abstract FamilyNode getFamilyNode();

	abstract List<PersonNode> getPersonNodes();

	// Returns the first partner not acquired [or the first one available]
	abstract PersonNode getMainPersonNode();

	// Softly returns the "husband" (the first parner)
	abstract PersonNode getHusband();

	// Softly returns the "wife" (the second partner) or the first person node available
	abstract PersonNode getWife();

	// Strictly returns the requested partner or null
	abstract PersonNode getPartner(int id);

	// Relationship match of this node
	Match getMatch() {
		return getMatch(null);
	}
	abstract Match getMatch(Branch branch);

	// This node is one of the three match: NEAR, MIDDLE or FAR
	boolean isMultiMarriage(Branch branch) {
		Match match = getMatch(branch);
		return match == Match.FAR || match == Match.MIDDLE || match == Match.NEAR;
	}

	// Apply the overlap correction and propagate it to previous or next nodes
	void shift(float run) {
		setX(x + run);
		if( run > 0 && next != null ) {
			float rightOver = x + width + (union.equals(next.union) ? HORIZONTAL_SPACE : UNION_DISTANCE) - next.x;
			if( rightOver > 0 )
				next.shift(rightOver);
		} else if( run < 0 && prev != null ) {
			float leftOver = prev.x + prev.width + (union.equals(prev.union) ? HORIZONTAL_SPACE : UNION_DISTANCE) - x;
			if( leftOver > 0 )
				prev.shift(-leftOver);
		}
	}
}

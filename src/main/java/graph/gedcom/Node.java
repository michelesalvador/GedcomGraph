// Abstract class to be extended in a PersonNode or FamilyNode

package graph.gedcom;

import java.util.Collections;
import java.util.List;
import org.folg.gedcom.model.Family;
import graph.gedcom.Util.Position;
import static graph.gedcom.Util.*;

public abstract class Node extends Metric {

	// Contents
	public Family spouseFamily; // The person in this node is spouse of this family
	Group group; // List of siblings to which this node belongs
	Group youth; // List of PersonNode and FamilyNode descendants of a PersonNode or of a FamilyNode
	public int generation; // Number of the generation to which this node belongs (0 for fulcrum, negative up and positive down)
	public boolean mini; // The PersonNode will be displayed little (with just a number), and the familyNode without marriage date
	boolean isAncestor;
	Union union; // The union this node belongs to (expecially for ancestors)
	float force; // Dynamic horizontal movement

	// Calculate the initial width of first node (complementar to getMainWidth())
	abstract float getLeftWidth(Branch branch);

	// Calculate the to-center width of the main node included bond and partner
	abstract float getMainWidth(Position pos, Branch branch);

	/** Move this node in the center of children
	* @param branch Paternal side (left) or maternal side (right)
	*/
	void centerToYouth(Branch branch) {
		if( youth.stallion != null )
			setX(youth.stallion.x + youth.stallion.getLeftWidth(branch) - centerRelX());
		else
			setX(youth.x + youth.getLeftWidth(branch) + youth.getCentralWidth(branch) / 2 - centerRelX());
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

	List<Node> getGroupList() {
		if( getGroup() != null )
			return getGroup().list;
		return Collections.emptyList();
	}

	// Hybrid methods for FamilyNode and PersonNode

	abstract Group getGroup();

	abstract Node getOrigin();

	// A list with zero, one or two origin nodes
	abstract List<Node> getOrigins();

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

	// This node is one of the three match: NEAR, MIDDLE or FAR
	abstract boolean isMultiMarriage();

	void setForce(float push) {
		force += push;
	}

	// Apply the force
	float applyForce() {
		force /= 4;
		setX(x + force);
		return force;
	}
}

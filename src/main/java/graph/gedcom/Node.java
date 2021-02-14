package graph.gedcom;

// Abstract class to extend a PersonNode or FamilyNode

import static graph.gedcom.Util.p;
import java.util.ArrayList;
import java.util.List;
import org.folg.gedcom.model.Family;

public abstract class Node {
	
	public float width, height;
	public Point pos; // Absolute coordinates of the center of the node: used mostly internally to move it
	public float x, y; // Absolute coordinates of the top left corner of the node: its public position at the end of calculations
	public Family spouseFamily; // The person in this node is spouse of this family
	FamilyNode prev; // Previous coetaneous TODO
	FamilyNode next; // Next coetaneous
	List<PersonNode> children = new ArrayList<>(); // List of PersonNode descendants of a PersonNode or of a FamilyNode
	public int generation; // Number of the generation to which this node belongs (0 for fulcrum, negative up and positive down)
	public boolean mini; // The PersonNode will be displayed little (with just a number), and the familyNode without marriage date

	Vector velocity;	// the node's current velocity, expressed in vector form
	Point nextPosition;	// the node's position after the next iteration

	abstract float centerX();
	abstract float centerY();
	abstract float centerRelX();
	abstract float centerRelY();


	// Methods for PersonNode
	
	Node getOrigin() {
		if (this instanceof PersonNode) {
			return ((PersonNode)this).origin;
		}
		return null;
	}

	FamilyNode getFamilyNode() { // TODO rinomina?
		if (this instanceof PersonNode) {
			return ((PersonNode)this).familyNode;
		}
		return null;
	}
	
	// Hybrid methods for FamilyNode and PersonNode
	
	// Return the first partner not acquired or the first one available
	PersonNode getMainPersonNode() {
		if (this instanceof FamilyNode) {
			FamilyNode familyNode = (FamilyNode) this;
			for (PersonNode personNode : familyNode.partners)
				if (!personNode.acquired)
					return personNode;
			if (!familyNode.partners.isEmpty())
				return familyNode.partners.get(0);
		} else if (this instanceof PersonNode)
			return (PersonNode) this;
		return null;	
	}
	
	// Return the husband
	PersonNode getHusband() {
		if (this instanceof FamilyNode && !((FamilyNode)this).partners.isEmpty())
			return ((FamilyNode)this).partners.get(0);
		else if (this instanceof PersonNode)
			return (PersonNode) this;
		return null;	
	}
	
	PersonNode getWife() {
		if (this instanceof FamilyNode && ((FamilyNode)this).partners.size() > 1)
			return ((FamilyNode)this).partners.get(1);
		else if (this instanceof PersonNode)
			return (PersonNode) this;
		return null;	
	}
	
	public PersonNode getChild(int index) {
		return children.size() > 0 ? children.get(index) : null;
	}
}

package graph.gedcom;

// Abstract class to be extended in a PersonNode or FamilyNode

import static graph.gedcom.Util.p;
import org.folg.gedcom.model.Family;

public abstract class Node {
	
	// Geometry for position in the diagram
	public float x, y; // Absolute coordinates of the top left corner of the node
	public float width, height; // Public dimensions of node
	public boolean drag;

	// Contents
	public Family spouseFamily; // The person in this node is spouse of this family
	private Group children; // List of PersonNode descendants of a PersonNode or of a FamilyNode
	public int generation; // Number of the generation to which this node belongs (0 for fulcrum, negative up and positive down)
	public boolean mini; // The PersonNode will be displayed little (with just a number), and the familyNode without marriage date

	abstract public float centerRelX();
	abstract public float centerRelY();

	public float centerX() {
		return x + centerRelX();
	}

	public float centerY() {
		return y + centerRelY();
	}

	// Move this node in the center of children
	void centerToChildren() {
		Group children = getChildren();
		x = children.get(0).centerX() + children.getShrinkWidth() / 2 - centerRelX();
		if( this instanceof FamilyNode )
			((FamilyNode)this).placePartners();
	}

	public Group getChildren() {
		if(children == null) {
			children = new Group(this, generation+1);
		}
		return children;
	}

	public PersonNode getChild(int index) {
		Group children = getChildren();
		return children.size() > 0 ? children.get(index) : null;
	}

	// Methods for PersonNode
	
	Node getOrigin() {
		if( this instanceof PersonNode ) {
			return ((PersonNode)this).origin;
		}
		return null;
	}

	FamilyNode getFamilyNode() { // TODO rinomina?
		if( this instanceof PersonNode ) {
			return ((PersonNode)this).familyNode;
		}
		return null;
	}
	
	// Hybrid methods for FamilyNode and PersonNode
	
	// Returns the first partner not acquired or the first one available
	PersonNode getMainPersonNode() {
		if( this instanceof FamilyNode ) {
			FamilyNode familyNode = (FamilyNode)this;
			for( PersonNode personNode : familyNode.partners )
				if( !personNode.acquired )
					return personNode;
			if( !familyNode.partners.isEmpty() )
				return familyNode.partners.get(0);
		} else if( this instanceof PersonNode )
			return (PersonNode)this;
		return null;
	}
	
	// Softly returns the "husband" (the first parner)
	PersonNode getHusband() {
		if( this instanceof FamilyNode && !((FamilyNode)this).partners.isEmpty() )
			return ((FamilyNode)this).partners.get(0);
		else if( this instanceof PersonNode && ((PersonNode)this).familyNode != null )
			return ((PersonNode)this).familyNode.getHusband();
		else if( this instanceof PersonNode )
			return (PersonNode)this;
		return null;
	}

	// Softly returns the "wife" (the second partner) or the first person node available
	PersonNode getWife() {
		if( this instanceof FamilyNode && ((FamilyNode)this).partners.size() > 1 )
			return ((FamilyNode)this).partners.get(1);
		// TODO return il primo partner comunque? è possibile nel diagramma un FamilyNode con un solo partner?
		else if( this instanceof PersonNode && ((PersonNode)this).familyNode != null )
			return ((PersonNode)this).familyNode.getWife();
		else if( this instanceof PersonNode )
			return (PersonNode)this;
		return null;
	}
	
	// Strictly return the requested partner or null
	PersonNode getPartner(int id) {
		if( this instanceof FamilyNode && ((FamilyNode)this).partners.size() > id )
			return ((FamilyNode)this).partners.get(id);
		else if( this instanceof PersonNode ) {
			if( ((PersonNode)this).familyNode != null )
				return ((PersonNode)this).familyNode.getPartner(id);
			else if( id == 0 ) // Single person
				return (PersonNode)this;
		}
		return null;
	}

	// Between the children return the family node ancestor of fulcrum
	Node getChildAncestor() {
		for( PersonNode child : children ) {
			if(child.familyNode != null && !child.getHusband().acquired && !child.getWife().acquired ) // un po' empirico e potrebbe non funzionare
				return child.familyNode;
		}
		return null;
	}
}

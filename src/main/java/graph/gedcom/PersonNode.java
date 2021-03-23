package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import static graph.gedcom.Util.*;

public class PersonNode extends Node {
	
	private Gedcom gedcom;
	public Person person;
	public Node origin; // The FamilyNode or PersonNode which this person was born
	public FamilyNode familyNode; // The FamilyNode in which this person is spouse
	Card type; // Size and function
	public boolean acquired; // Is this person acquired spouse (not blood relative)?
	public boolean dead;
	public int amount; // Number to display in little ancestry or progeny
	
	/** Create a node rappresentig a person.
	 * @param gedcom
	 * @param person The dude.
	 * @param type Size and function: 0 fulcrum card, 1 regular size, 2 little ancestry, 3 little progeny
	 */
	public PersonNode(Gedcom gedcom, Person person, Card type) {
		this.gedcom = gedcom;
		this.person = person;
		this.type = type;
		if (type == Card.FULCRUM || type == Card.REGULAR) {
			if(Util.dead(person))
				dead = true;
		} else if (type == Card.ANCESTRY) {
			amount = 1;
			countAncestors(person);
			mini = true;
		} else if (type == Card.PROGENY) {
			amount = 1;
			countDescendants(gedcom);
			mini = true;
		}
	}
	
	/** Set the origin of this PersonNode and add it as child of the origin
	 * @param origin The parent node of this person
	 * @param penultimate Add the child before the last one child already existing
	 */
	void setOrigin(Node origin) {
		setOrigin(origin, false);
	}
	void setOrigin(Node origin, boolean penultimate) {
		if( origin != null ) {
			this.origin = origin;
			Group children = origin.getChildren();
			if( penultimate && children.size() > 0 )
				children.add(children.size() - 1, this);
			else
				children.add(this);
		}
	}
	
	// Recoursive count of direct ancestors
	private void countAncestors(Person ancestor) {
		if (amount < 100)
			for (Family family : ancestor.getParentFamilies(gedcom)) {
				for (Person father : family.getHusbands(gedcom)) {
					amount++;
					countAncestors(father);
				}
				for (Person mother : family.getWives(gedcom)) {
					amount++;
					countAncestors(mother);
				}
			}
	}
	
	// Recoursive count of direct descendants
	void countDescendants(Gedcom gedcom) {
		this.gedcom = gedcom;
		recoursiveCountDescendants(person);
	}
	private void recoursiveCountDescendants(Person person) {
		if (amount < 100)
			for (Family family : person.getSpouseFamilies(gedcom))
				for (Person child : family.getChildren(gedcom)) {
					amount++;
					recoursiveCountDescendants(child);
				}
	}
	
	public boolean isFulcrumNode() {
		return type == Card.FULCRUM;
	}
	
	@Override
	public float centerRelX() {
		return width / 2;
	}

	@Override
	public float centerRelY() {
		return height / 2;
	}

	// Calculate the to-center width of the node included family and partner
	float getMainWidth(Position pos) {
		float size = 0;
		if( getFamilyNode() != null ) { // With family
			FamilyNode familyNode = getFamilyNode();
			int index = familyNode.partners.indexOf(this);
			float famWidth = FAMILY_WIDTH - MARRIAGE_OVERLAP * 2;
			if( pos == Position.FIRST ) {
				size = centerRelX();
				if( index == 0 ) // Is husband
					size += famWidth + familyNode.getPartner(1).width;
			} else if( pos == Position.LAST ) {
				size = centerRelX();
				if( index > 0 ) // Is wife
					size += familyNode.getPartner(0).width + famWidth;
			} else { // The complete width
				size = familyNode.getPartner(0).width + famWidth + familyNode.getPartner(1).width;
			}
		} else { // Single
			if( pos == Position.MIDDLE )
				size = width;
			else
				size = centerRelX();
		}
		return size;
	}

	@Override
	public String toString() {
		if (mini)
			return amount + " (" + Util.essence(person) + ")";
		else {
			String txt = Util.essence(person);
			//txt += origin != null ? " " + origin.getChildren().indexOf(this) : " -";
			//txt += " " + generation;
			//txt += " " + person.getId();
			return txt;
		}
	}
}

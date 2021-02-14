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
	int type; // Size and function
	public boolean acquired; // Is this person acquired spouse (not blood relative)?
	public boolean dead;
	public int amount;
	
	/** Create a node rappresentig a person.
	 * @param gedcom
	 * @param person The dude.
	 * @param type Size and function: 0 fulcrum card, 1 regular size, 2 little ancestry, 3 little progeny
	 */
	public PersonNode(Gedcom gedcom, Person person, int type) {
		this.gedcom = gedcom;
		this.person = person;
		this.type = type;
		if (type == FULCRUM || type == REGULAR) {
			if(Util.dead(person))
				dead = true;
		} else if (type == ANCESTRY) {
			amount = 1;
			countAncestors(person);
			mini = true;
		} else if (type == PROGENY) {
			amount = 1;
			countDescendants(gedcom);
			mini = true;
		}
		pos = new Point();
	}
	
	/** Set the origin of this PersonNode and add it as child of the origin
	 * @param origin The parent of this person
	 */
	void setOrigin(Node origin) {
		if (origin != null) {
			this.origin = origin;
			origin.children.add(this);
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
		return type == FULCRUM;
	}
	
	@Override
	float centerRelX() {
		return width / 2;
	}

	@Override
	float centerRelY() {
		return height / 2;
	}

	@Override
	public float centerX() {
		return x + width / 2;
	}
	
	@Override
	public float centerY() {
		return y + height / 2;
	}

	@Override
	public String toString() {
		if (mini)
			return amount + " (" + Util.essence(person) + ")";
		else
			return Util.essence(person);
	}
}

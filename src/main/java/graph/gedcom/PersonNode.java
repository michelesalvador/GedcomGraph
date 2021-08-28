package graph.gedcom;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import java.util.ArrayList;
import java.util.List;
import static graph.gedcom.Util.*;

public class PersonNode extends Node {

	private Gedcom gedcom;
	public Person person;
	public Node origin; // The FamilyNode or PersonNode which this person was born from
	FamilyNode familyNode; // The FamilyNode in which this person is spouse
	Card type; // Size and function
	public boolean acquired; // Is this person acquired spouse (not blood relative)?
	public boolean dead;
	public int amount; // Number to display in little ancestry or progeny
	CurveLine line; // Curve line connecting this person with the origin above

	/** Create a node rappresentig a person.
	 * @param gedcom
	 * @param person The dude.
	 * @param type Size and function: 0 fulcrum card, 1 regular size, 2 little ancestry, 3 little progeny
	 */
	public PersonNode(Gedcom gedcom, Person person, Card type) {
		this.gedcom = gedcom;
		this.person = person;
		this.type = type;
		if( type == Card.FULCRUM || type == Card.REGULAR ) {
			if( isDead() )
				dead = true;
		} else if( type == Card.ANCESTRY ) {
			amount = 1;
			countAncestors(person);
			mini = true;
		} else if( type == Card.PROGENY ) {
			amount = 1;
			countDescendants(gedcom);
			mini = true;
		}
	}

	@Override
	Group getGroup() {
		if( familyNode != null )
			return familyNode.group;
		return group;
	}

	@Override
	Node getOrigin() {
		return origin;
	}

	@Override
	List<Node> getOrigins() {
		List<Node> origins = new ArrayList<>();
		if( origin != null )
			origins.add(origin);
		return origins;
	}

	@Override
	FamilyNode getFamilyNode() {
		return familyNode;
	}

	@Override
	List<PersonNode> getPersonNodes() {
		List<PersonNode> persons = new ArrayList<>();
		persons.add(this);
		return persons;
	}

	@Override
	PersonNode getMainPersonNode() {
		return this;
	}

	@Override
	PersonNode getHusband() {
		if( familyNode != null )
			return familyNode.getHusband();
		return this;
	}

	@Override
	PersonNode getWife() {
		if( familyNode != null )
			return familyNode.getWife();
		return this;
	}

	@Override
	PersonNode getPartner(int id) {
		if( familyNode != null )
			return familyNode.getPartner(id);
		else if( id == 0 ) // Single person
			return this;
		return null;
	}

	@Override
	boolean isAncestor() {
		return isAncestor;
	}

	// Recoursive count of direct ancestors
	private void countAncestors(Person ancestor) {
		if( amount <= 100 ) {
			for( Family family : ancestor.getParentFamilies(gedcom) ) {
				for( Person father : family.getHusbands(gedcom) ) {
					amount++;
					countAncestors(father);
				}
				for( Person mother : family.getWives(gedcom) ) {
					amount++;
					countAncestors(mother);
				}
			}
		}
	}

	// Recoursive count of direct descendants
	void countDescendants(Gedcom gedcom) {
		this.gedcom = gedcom;
		recoursiveCountDescendants(person);
	}

	private void recoursiveCountDescendants(Person person) {
		if( amount <= 100 ) {
			for( Family family : person.getSpouseFamilies(gedcom) )
				for( Person child : family.getChildren(gedcom) ) {
					amount++;
					recoursiveCountDescendants(child);
				}
		}
	}

	// Check if this person is dead or buried
	private boolean isDead() {
		for( EventFact fact : person.getEventsFacts() ) {
			if( fact.getTag().equals( "DEAT" ) || fact.getTag().equals( "BURI" ) )
				return true;
		}
		return false;
	}

	public boolean isFulcrumNode() {
		return type == Card.FULCRUM;
	}

	/*public CurveLine getLine() {
		if( origin != null && line == null )
			line = new CurveLine(this);
		return line;
	}*/

	@Override
	public float centerRelX() {
		return width / 2;
	}

	@Override
	public float centerRelY() {
		return height / 2;
	}

	@Override
	void setX(float x) {
		this.x = x;
	}

	@Override
	void setY(float y) {
		this.y = y;
	}

	@Override
	float getMainWidth(Position pos, Branch branch) {
		if( pos == Position.MIDDLE )
			return width;
		else
			return centerRelX();
	}

	@Override
	float getLeftWidth(Branch branch) {
		return centerRelX();
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

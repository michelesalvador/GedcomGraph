package graph.gedcom;

import org.folg.gedcom.model.Person;

public class Card {

	public int x, y, width, height;
	public boolean dead;
	public boolean acquired; // Is this person acquired spouse (not blood relative)?
	private Person person;
	public Node origin; // The node of parent(s) from which this person was born

	public Card( Person person) {
		this.person = person;
		if(Util.dead(person)) {
			dead = true;
		}
	}
	
	public int centerX() {
		return x + width / 2;
	}

	public Person getPerson() {
		return person;
	}
	
	// This card has one or two little ancestors above.
	// Adapt both for shared ancestry and for spouse ancestry
	public boolean hasAncestry() {
		return origin instanceof AncestryNode && (((AncestryNode)origin).foreFather != null || ((AncestryNode)origin).foreMother != null);
	}
	
	public String toString() {
		return Util.essence(person);
	}
}

package graph.gedcom;

import org.folg.gedcom.model.Person;

public class IndiCard extends Card {

	public boolean dead;

	public IndiCard( Person person) {
		this.person = person;
		if(Util.dead(person)) {
			dead = true;
		}
	}
	
	public int centerX() {
		return x + width / 2;
	}
	
	// This card has one or two little ancestors above.
	// Adapt both for shared ancestry and for spouse ancestry
	public boolean hasAncestry() {
		return origin instanceof AncestryNode && (((AncestryNode)origin).miniFather != null || ((AncestryNode)origin).miniMother != null);
	}
	
	public String toString() {
		return Util.essence(person);
	}
}

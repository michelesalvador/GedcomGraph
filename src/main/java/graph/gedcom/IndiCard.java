package graph.gedcom;

import org.folg.gedcom.model.Person;

public class IndiCard extends Card {

	public boolean dead;
	public boolean asterisk;

	public IndiCard(Person person) {
		this.person = person;
		if(Util.dead(person)) {
			dead = true;
		}
	}

	@Override
	float centerRelX() {
		return width / 2;
	}
	
	@Override
	public float centerX() {
		return x + width / 2;
	}
	
	// This card can have one or two little ancestors above (shared ancestry or spouse ancestry)
	public boolean hasAncestry() {
		return origin instanceof AncestryNode && (((AncestryNode)origin).miniFather != null || ((AncestryNode)origin).miniMother != null);
	}
	
	public String toString() {
		return Util.essence(person);
	}
}

package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public class IndiCard extends Card {

	public boolean dead;
	public boolean asterisk;
	public Family parentFamily; // The family in which this person is child
	public ProgenyNode progeny; // The little children below step parents

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

	public float centerY() {
		return y + height / 2;
	}

	// This card can have one or two little ancestors above (shared ancestry or spouse ancestry)
	public boolean hasAncestry() {
		return origin instanceof AncestryNode && (((AncestryNode)origin).miniFather != null || ((AncestryNode)origin).miniMother != null);
	}
	
	public void addProgeny(Gedcom gedcom, Family family) {
		if (!family.getChildRefs().isEmpty()) {
			if (progeny == null)
				progeny = new ProgenyNode(gedcom, family, this);
			else
				progeny.addFamily(family);
		}
	}

	public String toString() {
		return Util.essence(person);
	}
}

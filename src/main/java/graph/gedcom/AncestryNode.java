package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public class AncestryNode extends Node {

	public MiniCard miniFather, miniMother;
	private Gedcom gedcom;
	private int people;
	public float horizontalCenter;
	public boolean acquired; // Is this the ancestry of an acquired spouse (not blood relative)?

	public AncestryNode(Gedcom gedcom, IndiCard card) {
		this.gedcom = gedcom;
		acquired = card.acquired;
		Person person = card.person;
		if (!person.getParentFamilies(gedcom).isEmpty()) {
			Family family = person.getParentFamilies(gedcom).get(0);
			if (!family.getHusbands(gedcom).isEmpty()) {
				people = 1;
				countAncestors(family.getHusbands(gedcom).get(0));
				miniFather = new MiniCard(family.getHusbands(gedcom).get(0), people);
			}
			if (!family.getWives(gedcom).isEmpty()) {
				people = 1;
				countAncestors(family.getWives(gedcom).get(0));
				miniMother = new MiniCard(family.getWives(gedcom).get(0), people);
			}
		}
	}
	
	// Recoursive count of direct ancestors
	private void countAncestors(Person person) {
		if (people < 100)
			for (Family family : person.getParentFamilies(gedcom)) {
				for (Person father : family.getHusbands(gedcom)) {
					people++;
					countAncestors(father);
				}
				for (Person mother : family.getWives(gedcom)) {
					people++;
					countAncestors(mother);
				}
			}
	}

	public boolean isCouple() {
		return miniFather != null && miniMother != null;
	}

	@Override
	public float centerRelX() {
		return horizontalCenter;
	}

	public float centerRelY() {
		return height / 2;
	}

	@Override
	public float centerX() {
		return x + horizontalCenter;
	}

	public float centerY() {
		return y + height / 2;
	}

	@Override
	public String toString() {
		String str = "";
		if (miniFather != null)
			str += miniFather.amount;
		str += " - ";
		if (miniMother != null)
			str += miniMother.amount;
		return str;
	}
}

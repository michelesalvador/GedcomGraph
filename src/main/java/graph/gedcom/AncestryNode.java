package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public final class AncestryNode extends Node {

	public MiniCard miniFather, miniMother;
	private Gedcom gedcom;
	private int people;
	public int horizontalCenter;

	public AncestryNode(Gedcom gedcom, IndiCard card) {
		this.gedcom = gedcom;
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
	public int centerX() {
		//return x + centerXrel();
		return x + horizontalCenter;
	}

	public int centerY() {
		return y + height / 2;
	}

	@Override
	public int centerXrel() {
		/*if (isCouple())
			return foreFather.width + Util.GAP / 2;
		else if (foreFather != null)
			return width / 2;
		else if (foreMother != null)
			return width / 2;
		return 0;*/
		return horizontalCenter;
	}

	public int centerYrel() {
		return height / 2;
	}

	@Override
	public String toString() {
		String str = "";
		if (miniFather != null)
			str += miniFather.ancestry;
		str += " - ";
		if (miniMother != null)
			str += miniMother.ancestry;
		return str;
	}
}

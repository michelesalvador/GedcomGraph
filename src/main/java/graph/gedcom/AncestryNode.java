package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public final class AncestryNode extends Node {

	public Ancestor foreFather, foreMother;
	private Gedcom gedcom;
	private int people;

	public AncestryNode(Gedcom gedcom, Card card) {
		this.gedcom = gedcom;
		Person person = card.getPerson();
		if (!person.getParentFamilies(gedcom).isEmpty()) {
			Family family = person.getParentFamilies(gedcom).get(0);
			if (!family.getHusbands(gedcom).isEmpty()) {
				people = 1;
				countAncestors(family.getHusbands(gedcom).get(0));
				foreFather = new Ancestor(family.getHusbands(gedcom).get(0), people);
			}
			if (!family.getWives(gedcom).isEmpty()) {
				people = 1;
				countAncestors(family.getWives(gedcom).get(0));
				foreMother = new Ancestor(family.getWives(gedcom).get(0), people);
			}
			card.setOrigin(this);
		}
	}

	// The little card with lineage info
	public class Ancestor {
		public Person person;
		public int ancestry;
		public int x, y, width, height;

		public Ancestor(Person person, int ancestry) {
			this.person = person;
			this.ancestry = ancestry;
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
		return foreFather != null && foreMother != null;
	}

	public boolean isSingle() {
		return foreFather != null || foreMother != null;
	}

	@Override
	void calcSize() {
		if (isCouple()) {
			width = foreFather.width + Util.GAP + foreMother.width;
			height = foreFather.height;
		} else if (foreFather != null) {
			width = foreFather.width;
			height = foreFather.height;
		} else if (foreMother != null) {
			width = foreMother.width;
			height = foreMother.height;
		}
	}

	// Position of the ancestry cards
	@Override
	void positionChildren() {
		if (foreFather != null) {
			foreFather.x = x;
			foreFather.y = y;
		}
		if (isCouple()) {
			foreMother.x = x + foreFather.width + Util.GAP;
			foreMother.y = y;
		} else if (foreMother != null) {
			foreMother.x = x;
			foreMother.y = y;
		}
	}

	@Override
	public int centerX() {
		return x + centerXrel();
	}

	public int centerY() {
		return y + height / 2;
	}

	@Override
	public int centerXrel() {
		if (isCouple())
			return foreFather.width + Util.GAP / 2;
		else if (foreFather != null)
			return foreFather.width / 2;
		else if (foreMother != null)
			return foreMother.width / 2;
		return 0;
	}

	public int centerYrel() {
		return height / 2;
	}

	@Override
	public String toString() {
		String str = "";
		if (foreFather != null)
			str += foreFather.ancestry;
		str += " - ";
		if (foreMother != null)
			str += foreMother.ancestry;
		return str;
	}
}

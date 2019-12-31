package graph.gedcom;

import java.util.HashSet;
import java.util.Set;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public abstract class CardNode extends Node {

	public Card husband;
	public Card wife;
	public String marriageDate;
	Group husbandGroup; // The group to which the husband of this node belongs as child
	Group wifeGroup; // The group to which the wife of this node belongs as child

	/**
	 * This method actually replaces the CardNode constructor.
	 * It takes the parent(s) from a family to create the cards.
	 * 
	 * @param gedcom
	 * @param family
	 */
	public void init(Gedcom gedcom, Family family) {
		Person him = !family.getHusbandRefs().isEmpty() ? family.getHusbands(gedcom).get(0) : null;
		if (him != null)
			husband = new Card(him);
		Person her = !family.getWifeRefs().isEmpty() ? family.getWives(gedcom).get(0) : null;
		if (her != null)
			wife = new Card(her);
		// Gedcom date of the marriage
		for (EventFact ef : family.getEventsFacts()) {
			if (ef.getTag().equals("MARR"))
				marriageDate = ef.getDate();
		}
	}

	public abstract Card getMainCard();

	public abstract Card getSpouseCard();

	/**
	 * 
	 * @param branch 1 for the husband, 2 for the wife
	 * @return
	 */
	public Card getCard(int branch) {
		if (branch == 1)
			return husband;
		else if (branch == 2)
			return wife;
		return null;
	}

	public Set<Card> getTwo() {
		Set<Card> two = new HashSet<Card>();
		two.add(husband);
		two.add(wife);
		return two;
	}

	Person getPerson(int branch) {
		Card card = getCard(branch);
		if (card != null)
			return card.getPerson();
		return null;
	}

	public boolean isCouple() {
		return husband != null && wife != null;
	}

	public boolean isSingle() {
		return husband != null || wife != null;
	}

	// Measures of a node from the measures of its cards
	@Override
	void calcSize() {
		if (isCouple()) {
			width = husband.width + Util.MARGIN + wife.width;
			height = Math.max(husband.height, wife.height); // max height between the two
			sizeAncestry(husband);
			sizeAncestry(wife);
		} else if (husband != null) {
			width = husband.width;
			height = husband.height;
			sizeAncestry(husband);
		} else if (wife != null) {
			width = wife.width;
			height = wife.height;
			sizeAncestry(wife);
		}
	}

	// Position of the ancestry cards
	@Override
	void positionChildren() {
		if (husband != null) {
			husband.x = x;
			husband.y = y + (height - husband.height) / 2;
		}
		if (isCouple()) {
			wife.x = x + husband.width + Util.MARGIN;
			wife.y = y + (height - wife.height) / 2;
		} else if (wife != null) {
			wife.x = x;
			wife.y = y;
		}
	}

	// Measures of ancestry node from its ancestors little cards
	private void sizeAncestry(Card card) {
		if (card.ancestryNode != null) {
			card.ancestryNode.calcSize();
		}
	}

	@Override
	public int centerX() {
		if (husband != null && wife != null)
			return x + husband.width + Util.MARGIN / 2;
		else if (husband != null)
			return x + husband.width / 2;
		else if (wife != null)
			return x + wife.width / 2;
		return x;
	}

	public int centerY() {
		return y + height / 2;
	}

	@Override
	public int centerXrel() {
		return husband.width + Util.MARGIN / 2;
	}

	public int centerYrel() {
		return height / 2;
	}

	// Simple solution to retrieve the marriage year. Family Gem uses another much
	// more complex.
	public String marriageYear() {
		String year = "";
		if (marriageDate != null) {
			if (marriageDate.lastIndexOf(' ') > 0)
				year = marriageDate.substring(marriageDate.lastIndexOf(' '));
			else
				year = marriageDate;
		}
		return year;
	}

	@Override
	public String toString() {
		String str = "";
		// str += this.hashCode() + "\t";
		if (isCouple())
			str += husband + " - " + wife;
		else if (husband != null)
			str += husband;
		else if (wife != null)
			str += wife;
		return str;
	}
}

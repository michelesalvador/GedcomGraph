package graph.gedcom;

import java.util.HashSet;
import java.util.Set;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public abstract class UnitNode extends Node {

	public IndiCard husband;
	public IndiCard wife;
	public String marriageDate;
	public int bondWidth; // It dependes on marriageDate
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
			husband = new IndiCard(him);
		Person her = !family.getWifeRefs().isEmpty() ? family.getWives(gedcom).get(0) : null;
		if (her != null)
			wife = new IndiCard(her);
		// Gedcom date of the marriage
		for (EventFact ef : family.getEventsFacts()) {
			if (ef.getTag().equals("MARR"))
				marriageDate = ef.getDate();
		}
	}

	public abstract IndiCard getMainCard();

	public abstract IndiCard getSpouseCard();

	/**
	 * 
	 * @param branch 1 for the husband, 2 for the wife
	 * @return
	 */
	public IndiCard getCard(int branch) {
		if (branch == 1)
			return husband;
		else if (branch == 2)
			return wife;
		return null;
	}

	public Set<IndiCard> getTwo() {
		Set<IndiCard> two = new HashSet<IndiCard>();
		two.add(husband);
		two.add(wife);
		return two;
	}

	Person getPerson(int branch) {
		IndiCard card = getCard(branch);
		if (card != null)
			return card.person;
		return null;
	}

	public boolean isCouple() {
		return husband != null && wife != null;
	}

	@Deprecated
	public boolean isSingle() {
		return husband != null || wife != null;
	}

	// Calculate width and height of this node taking the dimensions of the cards
	void calcSize() {
		if (isCouple()) {
			width = husband.width + bondWidth + wife.width;
			if(marriageDate != null)
				width -= Util.TIC * 2;			
			height = Math.max(husband.height, wife.height); // max height between the two
		} else if (husband != null) {
			width = husband.width;
			height = husband.height;
		} else if (wife != null) {
			width = wife.width;
			height = wife.height;
		}
	}

	// Position of the ancestry cards
	@Deprecated
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

	@Override
	public int centerXrel() {
		if (isCouple())
			return husband.width + bondWidth / 2 - (marriageDate != null ? Util.TIC : 0);
		else if (husband != null)
			return husband.width / 2;
		else if (wife != null)
			return wife.width / 2;
		return 0;
	}

	public int centerYrel() {
		return height / 2;
	}

	@Override
	public int centerX() {
		return x + centerXrel();
	}

	public int centerY() {
		return y + centerYrel();
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
	
	public ProgenyNode getProgeny() {
		if(this instanceof SpouseNode && ((SpouseNode)this).progeny != null) {
			return ((SpouseNode)this).progeny;
		}
		return null;
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

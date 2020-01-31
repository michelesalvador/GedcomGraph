package graph.gedcom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import static graph.gedcom.Util.pr;

public class UnitNode extends Node {

	public IndiCard husband;
	public IndiCard wife;
	public String marriageDate;
	public float bondWidth; // It dependes on marriageDate
	public ProgenyNode progeny; // The little children below
	public Family family;
	Group husbandGroup; // The group to which the husband of this node belongs as child
	Group wifeGroup; // The group to which the wife of this node belongs as child
	
	public UnitNode(Gedcom gedcom, Person person, boolean withProgeny ) {
		this(gedcom, person, withProgeny, false);
	}

	public UnitNode(Gedcom gedcom, Person person, boolean withProgeny, boolean thisIsFulcrumNode) {
		// Couple
		List<Family> families = person.getSpouseFamilies(gedcom);
		if (!families.isEmpty()) {
			// Usually the last marriage of a person is displayed
			int whichMarriage = families.size() - 1;
			// One husband with multi marriages appears as indi card just in the fulcrum node, asterisk afterwards 
			if (thisIsFulcrumNode && families.size() > 1 && !families.get(0).getHusbands(gedcom).isEmpty() && person == families.get(0).getHusbands(gedcom).get(0))
				whichMarriage = 0;
			Family family = families.get(whichMarriage);
			init(gedcom, family);
			// Define the acquired spouse
			if (isCouple()) {
				if (person.equals(husband.person)) {
					defineSpouse(gedcom, wife);
				} else if (person.equals(wife.person)) {
					defineSpouse(gedcom, husband);
				}
			}
			if (!family.getChildRefs().isEmpty() && withProgeny)
				progeny = new ProgenyNode(gedcom, family, this);
		} // Single person
		else {
			if (Util.sex(person) == 2) {
				wife = new IndiCard(person);
			} else {
				husband = new IndiCard(person);
			}
		}
	}
	
	public UnitNode(Gedcom gedcom, Family family) {
		init(gedcom, family);
	}
	
	// The asterisk version
	public UnitNode(Gedcom gedcom, Family family, int type, boolean withProgeny) {
		init(gedcom, family);
		if(type == 1) {
			husband.asterisk = true;
			defineSpouse(gedcom, wife);
		} else if(type == 2) {
			wife.asterisk = true;
			defineSpouse(gedcom, husband);
		}
	}

	/**
	 * This method actually replaces the UnitNode constructor.
	 * It takes the parent(s) from a family to create the indi cards.
	 * 
	 * @param gedcom
	 * @param family
	 */
	public void init(Gedcom gedcom, Family family) {
		this.family = family;
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

	// Complete the definition of the acquired spouse
	void defineSpouse(Gedcom gedcom, IndiCard card) {
		if (card != null) {
			card.acquired = true;
			AncestryNode ancestry = new AncestryNode(gedcom, card);
			if(ancestry.miniFather != null || ancestry.miniMother != null)
				card.origin = ancestry;
		}
	}

	/**
	 * Retrieve the card of the blood relative (that is, not the spouse), if existing.
	 * 
	 * @return A Card
	 */
	public IndiCard getMainCard() {
		if(isCouple() && (husband.acquired || wife.acquired)) {
			if(wife.acquired)
				return husband;
			else
				return wife;
		} else if (husband != null)
			return husband;
		else if (wife != null)
			return wife;
		return null;		
	}
	
	public IndiCard getSpouseCard() {
		if(isCouple()) {
			if(husband.acquired)
				return husband;
			else
				return wife;
		}
		return null;
	}

	/**
	 * Return husband or wife, as preferred 
	 * @param branch 1 for the husband, 2 for the wife
	 * @return
	 */
	public IndiCard getCard(int branch) {
		if (branch == 1 && husband != null)
			return husband;
		else if (branch == 2 && wife != null)
			return wife;
		else if (husband != null)
			return husband;
		else if (wife != null)
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

	// If this unit has youths or progeny to display
	public boolean hasChildren() {
		return (guardGroup != null && !guardGroup.youths.isEmpty()) || progeny != null;
	}

	// Calculate width and height of this node taking the dimensions of the cards
	void calcSize() {
		if (isCouple()) {
			width = husband.width + bondWidth + wife.width - (marriageDate != null ? Util.TIC * 2 : 0);			
			height = Math.max(husband.height, wife.height); // max height between the two
		} else if (husband != null) {
			width = husband.width;
			height = husband.height;
		} else if (wife != null) {
			width = wife.width;
			height = wife.height;
		}
	}

	// Set the absolute position of the cards
	void positionCards() {
		if (husband != null) {
			husband.x = x;
			husband.y = y + (height - husband.height) / 2;
		}
		if (isCouple()) {
			wife.x = x + husband.width + bondWidth - (marriageDate != null ? Util.TIC * 2 : 0);
			wife.y = y + (height - wife.height) / 2;
		} else if (wife != null) {
			wife.x = x;
			wife.y = y;
		}
	}

	@Override
	public float centerRelX() {
		if (isCouple())
			return husband.width + bondWidth / 2 - (marriageDate != null ? Util.TIC : 0);
		else if (husband != null)
			return husband.width / 2;
		else if (wife != null)
			return wife.width / 2;
		return 0;
	}

	public float centerRelY() {
		return height / 2;
	}

	@Override
	public float centerX() {
		return x + centerRelX();
	}

	public float centerY() {
		return y + centerRelY();
	}
	
	// Useful to calculate the width of youths, excluding acquired spouses at the ends
	float getMainWidth(boolean first) {
		IndiCard mainCard = getMainCard();
		if(first) {
			if(mainCard.equals(wife))
				return wife.width;
			else
				return width;
		} else {
			if(mainCard.equals(husband))
				return husband.width;
			else
				return width;
		}
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

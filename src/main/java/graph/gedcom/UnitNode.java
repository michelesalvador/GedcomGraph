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
	Group husbandGroup; // The group to which the husband of this node belongs as child
	Group wifeGroup; // The group to which the wife of this node belongs as child
	
	/**
	 * Constructor of a node, starting from a person.
	 * @param gedcom The GEDCOM tree
	 * @param person The person on which build the node
	 * @param parentFamily The family in which 'person' is the child
 	 * @param fulcrum The fulcrum of the diagram. If 'person' contains the fulcrum and this is null, the fulcrum node will be generated.
 	 * @param withSpouse Display the acquired parner
 	 * @param withProgeny The little progeny cards are requested to appear below the node
	 */
	public UnitNode(Gedcom gedcom, Person person, Family parentFamily, Person fulcrum, boolean withSpouse, boolean withProgeny) {
		// Couple
		List<Family> families = person.getSpouseFamilies(gedcom);
		if (withSpouse && !families.isEmpty()) {
			Family family;
			// First marriage only for fulcrum in first fulcrum node
			if (fulcrum == null) {
				family = families.get(0);
				init(gedcom, family, person, withProgeny);
			} else { // Usually the last marriage of a person is displayed
				family = families.get(families.size() - 1);
				init(gedcom, family, fulcrum, withProgeny);
			}
			// Define the acquired spouse
			if (husband != null && person.equals(husband.person)) {
				husband.parentFamily = parentFamily;
				defineSpouse(gedcom, wife, fulcrum);
			} else if (wife != null && person.equals(wife.person)) {
				wife.parentFamily = parentFamily;
				defineSpouse(gedcom, husband, fulcrum);
			}
			// Restore fulcrum card not as asterisk
			if (fulcrum == null) {
				getMainCard().asterisk = false;
			}
		} else { // Single person
			singlePerson(person, parentFamily);
			// TODO Missing progeny for this single person
		}
	}
	
	/**
	 * Constructor for ancestors family and for multi marriages (after the first one)
	 * @param gedcom
	 * @param family
	 * @param fulcrum
	 * @param followingMarriage Is this a following (asterisk) marriage?
	 * @param withProgeny The little progeny cards are requested to appear below the node
	 */
	public UnitNode(Gedcom gedcom, Family family, Person fulcrum, boolean followingMarriage, boolean withSpouse, boolean withProgeny) {
		if (followingMarriage && !withSpouse) {
			singlePerson(fulcrum, family);
			getMainCard().asterisk = true;
		} else {
			init(gedcom, family, fulcrum, withProgeny);
			// For multi marriages only
			if (followingMarriage) {
				if (fulcrum.equals(husband.person)) {
					defineSpouse(gedcom, wife, fulcrum);
				} else if (fulcrum.equals(wife.person)) {
					defineSpouse(gedcom, husband, fulcrum);
				}
			}
		}
	}
	
	/**
	 * This method actually replaces the UnitNode constructor.
	 * It takes the parent(s) from a family to create the indi cards.
	 * 
	 * @param gedcom
	 * @param family
	 */
	private void init(Gedcom gedcom, Family family, Person fulcrum, boolean withProgeny) {
		this.family = family;
		// Take the first two persons as husband and wife
		List<Person> spouses = getSpouses(gedcom, family);
		if (spouses.size() > 0)
			husband = new IndiCard(spouses.get(0));
		if (spouses.size() > 1)
			wife = new IndiCard(spouses.get(1));
		// Asterisks
		if (husband != null && husband.person.equals(fulcrum)) {
			husband.asterisk = true;
		}
		if (wife != null && wife.person.equals(fulcrum)) {
			wife.asterisk = true;
		}
		if (withProgeny && !family.getChildRefs().isEmpty())
			progeny = new ProgenyNode(gedcom, family, this);
		// GEDCOM date of the marriage
		for (EventFact ef : family.getEventsFacts()) {
			if (ef.getTag().equals("MARR"))
				marriageDate = ef.getDate();
		}
	}

	// Complete the creation of a single person
	private void singlePerson(Person person, Family parentFamily) {
		if (Util.sex(person) == 2) {
			wife = new IndiCard(person);
			wife.parentFamily = parentFamily;
		} else {
			husband = new IndiCard(person);
			husband.parentFamily = parentFamily;
		}
	}

	// Complete the definition of the acquired spouse
	private void defineSpouse(Gedcom gedcom, IndiCard card, Person fulcrum) {
		if (card != null) {
			card.acquired = true;
			AncestryNode ancestry = new AncestryNode(gedcom, card);
			if(ancestry.miniFather != null || ancestry.miniMother != null)
				card.origin = ancestry;
			card.parentFamily = ancestry.family;
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

	// Simple solution to retrieve the marriage year.
	// Family Gem uses another much more complex.
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

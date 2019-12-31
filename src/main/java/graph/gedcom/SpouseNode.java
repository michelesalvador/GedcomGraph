package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public final class SpouseNode extends CardNode {
	
	public SpouseNode(Gedcom gedcom, Person person) {
		// Couple
		if (!person.getSpouseFamilies(gedcom).isEmpty()) {
			Family family = person.getSpouseFamilies(gedcom).get(0);
			init(gedcom, family);
			// Define the acquired spouse
			if (isCouple()) {
				if (person.equals(husband.getPerson())) {
					wife.acquired = true;
					wife.ancestryNode = new AncestryNode(gedcom, wife);
				} else if (person.equals(wife.getPerson())) {
					husband.acquired = true;
					husband.ancestryNode = new AncestryNode(gedcom, husband);
				}
			}
		} // Single person
		else {
			if (Util.sex(person) == 2) {
				wife = new Card(person);
			} else {
				husband = new Card(person);
			}
		}
	}
	
	/**
	 * Retrieve the card of the blood relative (that is, not the spouse).
	 * 
	 * @return A Card
	 */
	@Override
	public Card getMainCard() {
		if(isCouple()) {
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
	
	@Override
	public Card getSpouseCard() {
		if(isCouple()) {
			if(husband.acquired)
				return husband;
			else
				return wife;
		}
		return null;
	}
}

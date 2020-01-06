package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public final class SpouseNode extends UnitNode {
	
	public ProgenyNode progeny;
	
	public SpouseNode(Gedcom gedcom, Person person, boolean withProgeny) {
		// Couple
		if (!person.getSpouseFamilies(gedcom).isEmpty()) {
			Family family = person.getSpouseFamilies(gedcom).get(0);
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
	
	private void defineSpouse(Gedcom gedcom, IndiCard card) {
		card.acquired = true;
		AncestryNode ancestry = new AncestryNode(gedcom, card);
		if(ancestry.miniFather != null || ancestry.miniMother != null)
			card.origin = ancestry;
	}
	
	/**
	 * Retrieve the card of the blood relative (that is, not the spouse).
	 * 
	 * @return A Card
	 */
	@Override
	public IndiCard getMainCard() {
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
	public IndiCard getSpouseCard() {
		if(isCouple()) {
			if(husband.acquired)
				return husband;
			else
				return wife;
		}
		return null;
	}
}

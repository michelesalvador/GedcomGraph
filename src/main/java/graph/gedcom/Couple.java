package graph.gedcom;

import java.util.HashSet;
import java.util.Set;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Person;

public final class Couple extends Node {

	public Card husband;
	public Card wife;
	int target;
	public String marriageDate;
	
	public Couple(Person husband, Person wife, Family family, int target) {
		this.husband = new Card(husband);
		this.wife = new Card(wife);
		this.target = target;
		// Gedcom date of the marriage
		for( EventFact ef : family.getEventsFacts() ) {
			if( ef.getTag().equals("MARR") )
				marriageDate = ef.getDate();
		}
	}

	@Override
	public Card getMainCard() {
		return target == 1 ? husband : wife;
	}
	
	@Override
	public Card getCard(int branch) {
		if (branch == 1 )
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
		
	public int centerX() {
		return x + husband.width + Util.MARGIN / 2;
	}
	
	public int centerXrel() {
		return husband.width + Util.MARGIN / 2;
	}
	
	// Simple solution to retrieve the marriage year. Family Gem uses another much more complex.
	public String marriageYear() {
		String year = "";
		if (marriageDate != null) {
			if(marriageDate.lastIndexOf(' ') > 0)
				year = marriageDate.substring(marriageDate.lastIndexOf(' '));
			else
				year = marriageDate;
		}
		return year	;
	}
}

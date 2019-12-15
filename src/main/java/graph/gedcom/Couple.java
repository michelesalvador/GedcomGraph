package graph.gedcom;

import java.util.HashSet;
import java.util.Set;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Person;

public final class Couple extends Node {

	public Card husband;
	public Card wife;
	int target;
	public String marriage;
	
	public Couple(Class<? extends Card> genericCard, Person husband, Person wife, Family family, int target) {
		this.genericCard = genericCard;
		//this.husband = new Card(husband);
		//this.wife = new Card(wife);
		//Util.p("genericCard "+genericCard);
		try {
			this.husband = genericCard.getDeclaredConstructor(Person.class).newInstance(husband);
			this.wife = genericCard.getDeclaredConstructor(Person.class).newInstance(wife);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//if(family.getEventsFacts().get(0)) // TODO Get marriage
		marriage = "1971";
		this.target = target;
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
	
	public int centerY() {
		return y + height / 2;
	}
}

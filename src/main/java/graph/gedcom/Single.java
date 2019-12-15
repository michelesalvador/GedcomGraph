package graph.gedcom;

import org.folg.gedcom.model.Person;

public final class Single extends Node {
	
	public Card one;
	
	public Single(Class<? extends Card> genericCard, Person one) {
		this.genericCard = genericCard;
		//this.one = new Card(one);
		try {
			this.one = genericCard.getDeclaredConstructor(Person.class).newInstance(one);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Card getMainCard() {
		return one;
	}
	
	@Override
	public Card getCard(int branch) {
		if(branch == 0)
			return one;
		return null;
	}
	
	int centerX() {
		return one.x + one.width/2;
	}
}

package graph.gedcom;

import org.folg.gedcom.model.Person;

public final class Single extends Node {
	
	public Card one;
	
	public Single(Person one) {
		this.one = new Card(one);
	}

	@Override
	public Card getMainCard() {
		return one;
	}
	
	@Override
	public Card getCard(int branch) {
		return one;
	}
	
	public int centerX() {
		return one.x + one.width/2;
	}
	
	public int centerXrel() {
		return one.width / 2;
	}
}

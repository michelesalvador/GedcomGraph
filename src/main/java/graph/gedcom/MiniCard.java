package graph.gedcom;

import org.folg.gedcom.model.Person;

public class MiniCard extends Card {

	public int amount;

	public MiniCard(Person person, int amount) {
		this.person = person;
		this.amount = amount;
	}

	@Override
	int centerXrel() {
		return width / 2;
	}

	@Override
	public int centerX() {
		return x + width / 2;
	}
}

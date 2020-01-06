package graph.gedcom;

import org.folg.gedcom.model.Person;

public class MiniCard extends Card {

	public int ancestry;

	public MiniCard(Person person, int ancestry) {
		this.person = person;
		this.ancestry = ancestry;
	}

	@Override
	public int centerX() {
		return x + width / 2;
	}
}

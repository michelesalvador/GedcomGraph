package graph.gedcom;

import org.folg.gedcom.model.Person;

public abstract class CardInterface {

	private Person person;

	public CardInterface( Person person) {
		this.person = person; 
	}
}

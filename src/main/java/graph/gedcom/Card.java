package graph.gedcom;

import org.folg.gedcom.model.Person;

public class Card {

	public int x, y, width, height;
	private Person person;
	public boolean dead;
	private Node origin; // The node of parent(s) from which this person was born

	public Card( Person person) {
		this.person = person;
		if(Util.dead(person)) {
			dead = true;
		}
	}
	
	int centerX() {
		return x + width / 2;
	}

	public Person getPerson() {
		return person;
	}
	
	void setOrigin(Node origin) {
		this.origin = origin;
	}
	
	Node getOrigin() {
		return origin;
	}
	
	public String toString() {
		return Util.essence(person);
	}
}

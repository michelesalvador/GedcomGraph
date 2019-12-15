package graph.gedcom;

import org.folg.gedcom.model.Person;

public abstract class Card { //  extends CardInterface

	private Person person;
	private Node origin; // The node of parent(s) from which this person was born
	public int x=0, y, width, height = 0;
	int[] ancestors;

	public Card( Person person) {
		this.person = person;
	}
	
	//public abstract OnCreate(Person p) {}
	
	int centerX() {
		return x + width / 2;
	}
	
	/*public void setPerson (Person person) {
		this.person = person;
	}*/

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

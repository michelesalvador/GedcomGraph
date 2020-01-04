package graph.gedcom;

import org.folg.gedcom.model.Person;

abstract class Card {
	
	public int x, y, width, height;
	public Person person;
	public Node origin; // The node of parent(s) from which this person was born
	public boolean acquired; // Is this person acquired spouse (not blood relative)?

	public abstract int centerX();
}

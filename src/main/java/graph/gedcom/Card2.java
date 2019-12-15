package graph.gedcom;

import org.folg.gedcom.model.Person;

public class Card2<Generic> {

	private Class<Generic> clazz;
	
	private Person person;
	private Node origin;
	public int x, y, width, height;
	int[] ancestors;

	public Card2( Class<Generic> clazz, Person person) {
		this.clazz = clazz;
		this.person = person; 
		// Initial values ??
		width = 120;
		height = 30;
	}

    public Generic buildOne() throws InstantiationException, IllegalAccessException {
    	return clazz.newInstance();
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
	
	/*void setCoord(int x, int y) {
		this.x = x;
		this.y = y;
	}*/
	
	public String toString() {
		return Util.essence(person);
	}
}

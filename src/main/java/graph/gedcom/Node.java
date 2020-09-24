package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public abstract class Node {
	
	public float x, y, width, height;
	public Family family; // The node persons are the spouses in this family
	public Group guardGroup; // The group to which this node belongs as guardian
	int branch; // 0 doesn't matter, 1 husband, 2 wife

	abstract float centerX();

	abstract float centerRelX();

	// Create a list of all spouses alternating husbands and wives
	List<Person> getSpouses(Gedcom gedcom, Family family) {
		List<Person> persons = new ArrayList<>();
		for (Person husband : family.getHusbands(gedcom))
			persons.add(husband);
		int pos = persons.size() > 0 ? 1 : 0;
		for (Person wife : family.getWives(gedcom)) {
			persons.add(pos, wife);
			pos += (persons.size() > pos+1) ? 2 : 1;
		}
		return persons;
	}
}

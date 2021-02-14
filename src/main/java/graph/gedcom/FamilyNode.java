package graph.gedcom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import static graph.gedcom.Util.*;

public class FamilyNode extends Node {

	List<PersonNode> partners;
	public String marriageDate;
	
	/**
	 * Constructor for ancestors families and for fulcrum marriages after the first one
	 */
	public FamilyNode(Gedcom gedcom, Family spouseFamily, int type) {
		this.spouseFamily = spouseFamily;
		partners = new ArrayList<>();
		if (type == FULCRUM || type == REGULAR) {
			// GEDCOM date of the marriage
			for (EventFact ef : spouseFamily.getEventsFacts()) {
				if (ef.getTag().equals("MARR"))
					marriageDate = ef.getDate();
			}
		} else
			mini = true;
	}
	
	// Add a spouse to this family
	void addPartner(PersonNode partner) {
		this.partners.add(partner);
		partner.familyNode = this;
	}

	// If this node has children
	public boolean hasChildren() {
		return !children.isEmpty();
	}

	@Override
	public float centerRelX() {
		return width / 2;
	}

	@Override
	public float centerRelY() {
		return height / 2;
	}

	@Override
	public float centerX() {
		return x + centerRelX();
	}

	@Override
	public float centerY() {
		return y + centerRelY();
	}
	
	// Simple solution to retrieve the marriage year.
	// Family Gem uses another much more complex.
	public String marriageYear() {
		String year = "";
		if (marriageDate != null) {
			if (marriageDate.lastIndexOf(' ') > 0)
				year = marriageDate.substring(marriageDate.lastIndexOf(' '));
			else
				year = marriageDate;
		}
		return year;
	}

	@Override
	public String toString() {
		String str = "Family: ";
		for (PersonNode personNode : partners)
			str += personNode + ", ";
		str += children;
		return str;
	}
}

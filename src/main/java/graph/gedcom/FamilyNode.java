package graph.gedcom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

import graph.gedcom.Util.Card;

import static graph.gedcom.Util.*;

public class FamilyNode extends Node {

	List<PersonNode> partners;
	public String marriageDate;
	
	/**
	 * Constructor for ancestors families and for fulcrum marriages after the first one
	 */
	public FamilyNode(Gedcom gedcom, Family spouseFamily, Card type) {
		this.spouseFamily = spouseFamily;
		partners = new ArrayList<>();
		if (type == Card.FULCRUM || type == Card.REGULAR) {
			// GEDCOM date of the marriage
			for (EventFact ef : spouseFamily.getEventsFacts()) {
				if (ef.getTag().equals("MARR"))
					marriageDate = ef.getDate();
			}
		} else {
			mini = true;
		}
	}
	
	// Add a spouse to this family
	void addPartner(PersonNode partner) {
		this.partners.add(partner);
		partner.familyNode = this;
		partner.spouseFamily = spouseFamily;
	}

	// If this node has children
	public boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	@Override
	public float centerRelX() {
		return width / 2;
	}

	@Override
	public float centerRelY() {
		return MARRIAGE_HEIGHT / 2;
	}

	// Place the partners besides to this family node
	void placePartners() {
		float overlap = mini ? 0 : MARRIAGE_OVERLAP;
		for( int i = 0; i < partners.size(); i++ ) {
			PersonNode partner = partners.get(i);
			if( i == 0 ) {
				partner.x = x - partner.width + overlap;
			} else {
				partner.x = x + width - overlap;
			}
			partner.y = centerY() - partner.centerRelY();
		}
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
		str += getChildren();
		return str;
	}
}

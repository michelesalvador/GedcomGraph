// Container of the parners (PersonNode) and the link between them (Bond).
// Being a Node, can be the origin of a youth Group.

package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import graph.gedcom.Util.Card;
import graph.gedcom.Util.Position;
import static graph.gedcom.Util.*;

public class FamilyNode extends Node {

	List<PersonNode> partners;
	Group stepYouthLeft, stepYouthRight; // Half-siblings of youth
	Card type;
	Bond bond;

	// Constructor for ancestors families and for fulcrum marriages after the first one
	public FamilyNode(Gedcom gedcom, Family spouseFamily, Card type) {
		this.spouseFamily = spouseFamily;
		this.type = type;
		partners = new ArrayList<>();
		if( type == Card.ANCESTRY || type == Card.PROGENY ) {
			mini = true;
		}
	}

	@Override
	Group getGroup() {
		return group;
	}

	@Override
	Node getOrigin() {
		PersonNode mainPerson = getMainPersonNode();
		if( mainPerson != null )
			return mainPerson.origin;
		return null;
	}

	@Override
	List<Node> getOrigins() {
		List<Node> origins = new ArrayList<>();
		for( PersonNode partner : partners ) {
			if( partner.origin != null )
				origins.add(partner.origin);
		}
		return origins;
	}

	@Override
	FamilyNode getFamilyNode() {
		return this;
	}

	@Override
	List<PersonNode> getPersonNodes() {
		return partners;
	}

	@Override
	PersonNode getMainPersonNode() {
		for( PersonNode partner : partners ) {
			if( !partner.acquired )
				return partner;
		}
		return null;
	}

	@Override
	PersonNode getHusband() {
		if( !partners.isEmpty() )
			return partners.get(0);
		return null;
	}

	@Override
	PersonNode getWife() {
		if( partners.size() > 1 )
			return partners.get(1);
		return null;
	}

	@Override
	PersonNode getPartner(int id) {
		if( partners.size() > id )
			return partners.get(id);
		return null;
	}

	@Override
	boolean isAncestor() {
		return isAncestor;
	}

	// Add a spouse to this family
	void addPartner(PersonNode partner) {
		this.partners.add(partner);
		partner.familyNode = this;
		partner.spouseFamily = spouseFamily;
	}

	// Crate bond if there are no partners or many partners
	void createBond() {
		if( partners.isEmpty() || partners.size() > 1 ) {
			bond = new Bond(this);
			if( type == Card.FULCRUM || type == Card.REGULAR ) {
				// GEDCOM date of the marriage
				for( EventFact ef : spouseFamily.getEventsFacts() ) {
					if( ef.getTag().equals("MARR") )
						bond.marriageDate = ef.getDate();
				}
			}
		}
	}

	// If this node has children
	boolean hasChildren() {
		if( type == Card.ANCESTRY ) // Mini ancestry are a little bit an exception
			return true;
		return youth != null;
	}

	@Override
	public float centerRelX() {
		if( partners.isEmpty() )
			return (mini ? MINI_BOND_WIDTH : MARRIAGE_WIDTH) / 2;
		else if( partners.size() == 1 )
			return partners.get(0).width / 2;
		else
			return partners.get(0).width + getBondWidth() / 2;
	}

	@Override
	public float centerRelY() {
		return height / 2;
	}

	float marriageOverlap = (MARRIAGE_WIDTH - BOND_WIDTH) / 2;

	// Place partners and bond
	@Override
	void setX(float x) {
		this.x = x;
		if( partners.isEmpty() ) { // Mini ancestry without partners
			bond.x = x;
		} else {
			for( int i = 0; i < partners.size(); i++ ) {
				PersonNode partner = partners.get(i);
				if( i == 0 ) {
					partner.x = x;
					x += partner.width;
					if( bond != null ) {
						bond.x = x - (mini ? 0 : marriageOverlap);
						x += getBondWidth();
					}
				} else {
					partner.x = x;
				}
				partner.y = centerY() - partner.centerRelY();
			}
		}
	}

	@Override
	void setY(float y) {
		this.y = y;
		for( PersonNode partner: partners ) {
			partner.setY(centerY() - partner.centerRelY());
		}
		if( bond != null )
			bond.y = y;
	}

	@Override
	float getLeftWidth(Branch branch) {
		if( branch == Branch.MATER || partners.indexOf(getMainPersonNode()) > 0 ) { // Is wife
			return getPartner(0).width + getBondWidth() + getPartner(1).centerRelX();
		} else { // Is husband
			return getPartner(0).centerRelX();
		}
	}

	@Override
	float getMainWidth(Position pos, Branch branch) {
		float size = 0;
		int index = partners.indexOf(getMainPersonNode());
		if( pos == Position.FIRST ) {
			if( branch == Branch.MATER ) {
				size = getWife().centerRelX();
			} else {
				size = getMainPersonNode().centerRelX();
				if( index == 0 && partners.size() > 1 ) // Is husband
					size += getBondWidth() + getPartner(1).width;
			}
		} else if( pos == Position.LAST ) {
			size = getMainPersonNode().centerRelX();
			if( index > 0 ) // Is wife
				size += getPartner(0).width + getBondWidth();
		} else { // The complete width
			size = width;
		}
		return size;
	}

	// Bond width excluding overlapping
	float getBondWidth() {
		if( bond != null )
			return mini ? MINI_BOND_WIDTH : BOND_WIDTH;
		return 0;
	}

	@Override
	public String toString() {
		String txt = "{";
		for (PersonNode personNode : partners)
			txt += personNode + ", ";
		if( txt.lastIndexOf(", ") > 0 )
			txt = txt.replaceAll(", $", "");
		//txt += " " + hashCode();
		txt += "}";
		return txt;
	}
}

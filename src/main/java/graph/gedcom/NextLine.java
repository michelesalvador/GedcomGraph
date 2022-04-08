// Little horizontal continuous line to connect following partners

package graph.gedcom;

import graph.gedcom.Util.Side;

public class NextLine extends Line {
	
	Bond bond;
	PersonNode partner;
	Side side;

	public NextLine(FamilyNode familyNode) {
		bond = familyNode.bond;
		partner = familyNode.partners.get(0);
		side = familyNode.side;
	}

	@Override
	void update() {
		x1 = bond.centerX();
		y1 = bond.centerY();
		x2 = side == Side.LEFT ? bond.x : partner.x; // otherwise partner.centerX();
		y2 = partner.centerY();
	}
}

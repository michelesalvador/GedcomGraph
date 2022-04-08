// Horizontal dashed line to connect following multiple partners

package graph.gedcom;

import graph.gedcom.Util.Match;
import graph.gedcom.Util.Side;
import static graph.gedcom.Util.*;

public class BackLine extends Line {
	
	Bond bond;
	Side side;
	Match match;
	boolean noPartners;

	public BackLine(FamilyNode familyNode) {
		bond = familyNode.bond;
		side = familyNode.side;
		match = familyNode.match;
		noPartners = familyNode.partners.isEmpty();
	}

	@Override
	void update() {
		x1 = side == Side.LEFT ? bond.x + bond.width + HORIZONTAL_SPACE : bond.x - HORIZONTAL_SPACE;
		y1 = bond.centerY();
		if( bond.marriageDate != null )
			x2 = side == Side.LEFT ? bond.x + bond.width : bond.x;
		else			
			x2 = noPartners && match == Match.MIDDLE ? side == Side.LEFT ? bond.x + bond.overlap : bond.x + bond.width - bond.overlap
				: bond.centerX();
		y2 = bond.centerY();
	}
}

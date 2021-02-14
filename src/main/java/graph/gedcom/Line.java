package graph.gedcom;

import static graph.gedcom.Util.p;

public class Line implements Comparable<Line> {

	public float x1, y1, x2, y2;

	public Line(PersonNode card) {
		Node origin = card.origin;
		x1 = origin.centerX();
		y1 = origin.y + origin.height;
		x2 = card.centerX();
		y2 = card.y;
	}
	
	// Only to add the lines between FamilyNode and partners
	// TODO find a better solution!
	public Line(PersonNode card, boolean thisIsAPartner) {
		Node group = card.familyNode;
		x1 = group.centerX();
		y1 = group.centerY();
		x2 = card.centerX();
		y2 = card.centerY();
	}
	
	// Compare this line with another to establish the horizontal order 
	@Override
	public int compareTo(Line line) {
		return Float.compare( Math.min(x1,x2), Math.min(line.x1,line.x2) );
	}
}

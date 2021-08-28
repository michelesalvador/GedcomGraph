// Straight short horizontal line between partners

package graph.gedcom;

public class HorizontalLine extends Line {
	
	PersonNode leftPerson, rightPerson;

	public HorizontalLine(FamilyNode familyNode) {
		leftPerson = familyNode.partners.get(0);
		rightPerson = familyNode.partners.get(1);
	}

	@Override
	void update() {
		x1 = leftPerson.x + leftPerson.width;
		y1 = leftPerson.centerY();
		x2 = rightPerson.x;
		y2 = rightPerson.centerY();
	}
}

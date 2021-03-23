// Straight short line between FamilyNode and partners

package graph.gedcom;

//import static graph.gedcom.Util.p;

public class StraightLine extends Line {
	
	FamilyNode familyNode;

	public StraightLine(PersonNode personNode) {
		super(personNode);
		familyNode = personNode.familyNode; // TODO usare personNode.getFamilyNode()
	}

	@Override
	void update() {
		x1 = familyNode.centerX();
		y1 = familyNode.centerY();
		x2 = personNode.centerX();
		y2 = personNode.centerY();
	}
}

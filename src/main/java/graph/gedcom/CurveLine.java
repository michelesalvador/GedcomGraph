// Curve long line between family and child or person and child

package graph.gedcom;

//import static graph.gedcom.Util.p;

public class CurveLine extends Line {

	Node origin;

	public CurveLine(PersonNode personNode) {
		super(personNode);
		origin = personNode.origin;
	}
	
	@Override
	void update() {
		x1 = origin.centerX();
		y1 = origin.y + origin.height;
		x2 = personNode.centerX();
		y2 = personNode.y;
	}
}

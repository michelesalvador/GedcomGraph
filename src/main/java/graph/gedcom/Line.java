package graph.gedcom;

public class Line {

	public float x1, y1, x2, y2;

	public Line( Card card ) {
		Node origin = card.origin;
		if( origin != null ) {
			x1 = origin.centerX();
			y1 = origin.y + origin.height;
			x2 = card.centerX();
			y2 = card.y;
		}
	}
}

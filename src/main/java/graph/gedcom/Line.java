package graph.gedcom;

public class Line {

	public int x1, y1, x2, y2;

	public Line( Card end ) {
		Node start = end.origin;
		if( start != null ) {
			x1 = start.centerX();
			y1 = start.y + start.height;
			x2 = end.centerX();
			y2 = end.y;
		}
	}
}

package graph.gedcom;

public class Line implements Comparable<Line> {

	public float x1, y1, x2, y2;

	public Line( Card card ) {
		Node origin = card.origin;
		x1 = origin.centerX();
		y1 = origin.y + origin.height;
		x2 = card.centerX();
		y2 = card.y;
	}
	
	// Compare this line with another to establish the horizontal order 
	@Override
	public int compareTo(Line line) {
		return Float.compare( Math.min(x1,x2), Math.min(line.x1,line.x2) );
	}
}

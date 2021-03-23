package graph.gedcom;

public abstract class Line implements Comparable<Line> {

	PersonNode personNode;
	public float x1, y1, x2, y2;
	
	Line(PersonNode personNode) {
		this.personNode = personNode;
	}

	abstract void update();

	// Compare this line with another to establish the horizontal order 
	@Override
	public int compareTo(Line line) {
		return Float.compare( Math.min(x1,x2), Math.min(line.x1,line.x2) );
	}
}

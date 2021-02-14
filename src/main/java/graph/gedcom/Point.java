package graph.gedcom;

// Used by Node to store its center position 

public class Point {
	public float x;
	public float y;

	public Point() {}

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public String toString() {
		return "[" + x + ", " + y + "]";
	}
}

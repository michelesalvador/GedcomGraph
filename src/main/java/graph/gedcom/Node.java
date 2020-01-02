package graph.gedcom;

public abstract class Node {
	
	public int x, y, width, height;
	public Group guardGroup; // The group to which this node belongs as guardian

	public abstract int centerX();
	
	abstract int centerXrel();
}

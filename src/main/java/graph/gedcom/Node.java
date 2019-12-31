package graph.gedcom;

public abstract class Node {
	
	public int x, y, width, height;
	public Group guardGroup; // The group to which this node belongs as guardian

	public abstract int centerX();
	
	abstract int centerXrel();
	
	// Calculate width and height of the node taking the dimensions from the children cards
	abstract void calcSize();
	
	abstract void positionChildren();
}

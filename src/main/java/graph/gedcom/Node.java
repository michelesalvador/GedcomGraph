package graph.gedcom;

public abstract class Node {
	
	public int x, y, width, height;
	public Group guardGroup; // The group to which this node belongs as guardian
	int branch; // 0 doesn't matter, 1 husband, 2 wife

	abstract int centerX();
	
	abstract int centerRelX();

}

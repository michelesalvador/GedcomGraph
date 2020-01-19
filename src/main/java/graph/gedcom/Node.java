package graph.gedcom;

public abstract class Node {
	
	public float x, y, width, height;
	public Group guardGroup; // The group to which this node belongs as guardian
	int branch; // 0 doesn't matter, 1 husband, 2 wife

	abstract float centerX();
	
	abstract float centerRelX();

}

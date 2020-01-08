package graph.gedcom;

import java.util.List;

public abstract class Node {
	
	public int x, y, width, height;
	public Group guardGroup; // The group to which this node belongs as guardian
	//TODO public List<Card> babies;

	public abstract int centerX();
	
	abstract int centerXrel();
	
	
}

// List of person nodes: used to store children of an origin with their spouses

package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import static graph.gedcom.Util.*;

public class Group extends Metric {

	List<Node> list; // List of PersonNodes and FamilyNodes of siblings and brothers-in-law, children of the origin
	Node origin; // Is the same of the first Person or Family node of the list
	int generation;
	boolean mini;
	Branch branch;

	Group(int generation, boolean mini, Branch branch) {
		list = new ArrayList<>();
		this.generation = generation;
		this.mini = mini;
		this.branch = branch;
	}

	// Add a node to this group and vice-versa
	void addNode(Node node) {
		addNode(node, -1);
	}
	void addNode(Node node, int index) {
		if(index > -1 ) {
			list.add(index, node);
		} else
			list.add(node);
		// The group of the node
		node.group = this;
	}

	// Set the origin to this group taking from the first node of the list
	// And set this group as youth of the origin
	void setOrigin() {
		boolean found = false;
		// For ancestors 
		for( Node node : list ) {
			if( node.isAncestor && !node.getPersonNodes().isEmpty() ) {
				origin = node.getPartner(branch == Branch.MATER ? 1 : 0).getOrigin();
				found = true;
			}
		}
		if( !found ) {
			PersonNode personNode = list.get(0).getMainPersonNode();
			if( personNode != null )
				origin = personNode.getOrigin();
		}
		if( origin != null )
			origin.youth = this;
	}

	// Check if origin is mini or without partners
	boolean isOriginMiniOrEmpty() {
		if( origin != null ) {
			return origin.mini || origin.getPersonNodes().isEmpty();
		}
		return false;
	}

	// Place horizontally the origin centered to this group nodes
	void placeOriginX() {
		x = list.get(0).x;
		origin.setX(x + getLeftWidth(branch) + getCentralWidth(branch) / 2 - origin.centerRelX());
	}

	// Place mini origin or regular origin without partners
	// Different distance whether this group has one node or multiple nodes
	void placeOriginY() {
		origin.setY(y - (list.size() > 1 ? LITTLE_GROUP_DISTANCE : ANCESTRY_DISTANCE) - origin.height);
	}

	// Excluded acquired spouses at right
	@Override
	public float centerRelX() {
		return getLeftWidth(branch) + getCentralWidth(branch) / 2;
	}

	@Override
	public float centerRelY() {
		return getHeight() / 2;
	}
	
	@Override
	void setX(float x) {
		float diff = x - this.x;
		for( Node node : list ) {
			node.setX(node.x + diff);
		}
		this.x = x;
	}

	@Override
	void setY(float y) {
		for( Node node : list ) {
			node.setY(y);
		}
		this.y = y;
	}

	@Override
	void setForce(float push) {
		for( Node node : list ) {
			node.setForce(push);
		}
	}

	// Total width of the group considering all nodes
	float getWidth() {
		if( width == 0 ) {
			Node lastChild = list.get(list.size() - 1);
			width = lastChild.x + lastChild.width - x;
		}
		return width;
	}

	// Left width including acquired spouses
	float getLeftWidth(Branch branch) {
		return list.get(0).getLeftWidth(branch);
	}

	// Children's center-to-center width excluding acquired spouses at the extremes
	float getCentralWidth(Branch branch) {
		float width = 0;
		if( list.size() > 1 ) {
			for( int i = 0; i < list.size(); i++ ) {
				Position pos;
				if( i == 0 )
					pos = Position.FIRST;
				else if( i == list.size() - 1 )
					pos = Position.LAST;
				else
					pos = Position.MIDDLE;
				width += list.get(i).getMainWidth(pos, branch) + HORIZONTAL_SPACE;
			}
			width -= HORIZONTAL_SPACE;
		} else if( list.size() == 1 ) {
			width = 0;
		}
		return width;
	}

	float getHeight() {
		height = 0;
		for( Node node : list )
			height = Math.max(height, node.height);
		return height;
	}

	@Override
	public String toString() {
		String txt = "";
		txt += list.toString();
		return txt;
	}
}

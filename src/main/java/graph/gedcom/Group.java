// List of person nodes: used to store children of an origin with their spouses

package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import static graph.gedcom.Util.*;

public class Group extends Metric {

	List<Node> list; // List of PersonNodes and FamilyNodes of siblings and brothers-in-law, children of the origin
	Node origin; // Is the same origin of the Person or Family nodes of the list
	Node stallion; // The main (NEAR) node when this group is a multi marriage only, that is without siblings
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

	// Set the origin to this group taking from the first not-aquired node of the list
	// And set this group as youth of the origin
	void setOrigin() {
		boolean found = false;
		// For ancestors 
		for( Node node : list ) {
			if( node.isAncestor && node.getPersonNodes().size() > 1 ) {
				origin = node.getPartner(branch == Branch.MATER ? 1 : 0).getOrigin();
				found = true;
			}
		}
		// All other groups, fulcrum group included
		if( !found ) {
			for( Node node : list ) {
				PersonNode personNode = node.getMainPersonNode();
				if( personNode != null ) {
					origin = personNode.getOrigin();
					break;
				}
			}
		}
		if( origin != null )
			origin.youth = this;
		// Find the stallion
		stallion = getStallion();
	}

	// Check if origin is mini or without partners
	boolean isOriginMiniOrEmpty() {
		if( origin != null ) {
			if( origin.isMultiMarriage() )
				return false;
			return origin.mini || origin.getPersonNodes().isEmpty();
		}
		return false;
	}

	// Nodes of this group have some child (not mini)
	public boolean hasChildren() {
		for( Node node : list ) {
			if( node.youth != null && !node.youth.mini ) {
				return true;
			}
		}
		return false;
	}

	// If this group is a multimarriage only (without any sibling) returns the main person node, otherwise returns null.
	private Node getStallion() {
		Node nearNode = null;
		for( Node node : list ) {
			if( !node.isMultiMarriage() )
				return null;
			else if( node.match == Match.NEAR )
				// TODO Il NEAR che qui permette di identificare lo stallion, in un FamilyNode ancestor dovrebbe stare in ogni singola Person,
				// Invece è associato al FamilyNode, rendendolo di fatto inutile.
				nearNode = node;
		}
		return nearNode;
	}

	// Place origin and uncles
	public void placeAncestors() {
		if( origin != null ) {
			placeOriginX();
			Union union = origin.union;
			if( union != null ) {
				// Place paternal uncles
				float posX = origin.x;
				for( int i = union.list.indexOf(origin) - 1; i >= 0; i-- ) {
					Node node = union.list.get(i);
					posX -= node.width + HORIZONTAL_SPACE;
					node.setX(posX);
				}
				// Place maternal uncles
				posX = origin.x + origin.width + HORIZONTAL_SPACE;
				for( int i = union.list.indexOf(origin) + 1; i < union.list.size(); i++ ) {
					Node node = union.list.get(i);
					node.setX(posX);
					posX += node.width + HORIZONTAL_SPACE;
				}
			}
		}
	}

	// Place horizontally the origin centered to this group nodes
	void placeOriginX() {
		if( stallion != null )
			origin.setX(stallion.x + stallion.getLeftWidth(branch) - origin.centerRelX());
		else {
			x = list.get(0).x;
			origin.setX(x + getLeftWidth() + getCentralWidth() / 2 - origin.centerRelX());
		}
	}

	// Place mini origin or regular origin without partners
	// Different distance whether this group has one node or multiple nodes
	void placeOriginY() {
		origin.setY(y - (list.size() > 1 && stallion == null ? LITTLE_GROUP_DISTANCE : ANCESTRY_DISTANCE) - origin.height);
	}

	// How much this descendant group can move to the left (negative) or to the right (positive) respect origin center
	float spaceAround() {
		float distance = origin.centerX() - centerX();
		if( distance < 0 ) { // Want move to the left
			//p("\t", generation, origin.centerX(), centerX(), distance, this, origin);
			Node prev = list.get(0).prev;
			if( prev != null ) {
				float left = Math.min(0, prev.x + prev.width + UNION_DISTANCE - x); // Can't be positive
				return Math.max(left, distance);
			} else
				return distance;
		} else if( distance > 0 ) { // Want move to the right
			//p("\t", generation,origin.centerX(), centerX(), distance, this, origin);
			Node next = list.get(list.size() - 1).next;
			if( next != null ) {
				float right = Math.max(0, next.x - x - getWidth() - UNION_DISTANCE); // Can't be negative
				return Math.min(right, distance);
			} else
				return distance;
		}
		return 0;
	}

	// Excluded acquired spouses at right
	@Override
	public float centerRelX() {
		if( stallion != null )
			return stallion.x + stallion.getLeftWidth(branch) - x;
		else
			return getLeftWidth() + getCentralWidth() / 2;
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

	void updateX() {
		x = list.get(0).x;
	}

	// Total width of the group considering all nodes
	float getWidth() {
		Node lastChild = list.get(list.size() - 1);
		width = lastChild.x + lastChild.width - x;
		return width;
	}

	// Group left width relative to the first not-multimarriage node, including acquired spouses
	float getLeftWidth() {
		return list.get(0).getLeftWidth(branch);
	}

	// Children's center-to-center fixed width excluding acquired spouses at the extremes
	float getBasicCentralWidth() {
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
				width += list.get(i).getMainWidth(pos) + HORIZONTAL_SPACE;
			}
			width -= HORIZONTAL_SPACE;
		}
		return width;
	}

	// Return the no fixed width of the group excluding acquired spouses at the extremes
	float getCentralWidth() {
		float width = 0;
		if( list.size() > 1 ) {
			Node first = list.get(0);
			Node last = list.get(list.size()-1);
			width = first.getMainWidth(Position.FIRST)
					+ last.x - (first.x + first.width)
					+ last.getMainWidth(Position.LAST);
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
		txt += list;
		//txt += " " + branch;
		//txt += " " + hashCode();
		return txt;
	}
}

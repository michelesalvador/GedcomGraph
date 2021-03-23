// List of person nodes: used to store children of an origin.

package graph.gedcom;

import static graph.gedcom.Util.*;
import java.util.ArrayList;

public class Group extends ArrayList<PersonNode> {

	Node origin;
	int generation;
	private float width = -1; // Total width of the children
	private float shrinkWidth = -1; // Children width excluding acquired spouses at the extremes

	Group(Node origin, int generation) {
		this.origin = origin;
		this.generation = generation;
	}

	void addNode(PersonNode personNode) {
		add(personNode);
		//personNode.siblings = this;
		personNode.setOrigin(origin);
	}

	// Horizontally distribute children relatively to origin
	void centerToOrigin() {
		if( origin != null && size() > 0 ) {
			float posX = origin.centerX() - getWidth() / 2;
			for( int i = 0; i < size(); i++ ) {
				Node child = get(i);
				child.x = posX;
				posX += child.width + HORIZONTAL_SPACE;
			}
		}
	}

	// Distribute next nodes after the first one
	void distributeAfterFirst() {
		Node firstChild = get(0);
		float posX = firstChild.x + firstChild.width + HORIZONTAL_SPACE;
		for( int i = 1; i < size(); i++ ) {
			Node child = get(i);
			child.x = posX;
			posX += child.width + HORIZONTAL_SPACE;
		}
	}

	// Total width of the group NOT COUNTING SPOUSES
	float getWidth() {
		if( width < 0 ) {
			for( int i = 0; i < size() - 1; i++ ) {
				width += get(i).getMainWidth(Position.MIDDLE) + HORIZONTAL_SPACE;
			}
			width += get(size() - 1).getMainWidth(Position.MIDDLE);
		}
		return width;
	}
	
	// Children's center-to-center width excluding acquired spouses at the extremes
	float getShrinkWidth() {
		if( shrinkWidth < 0 ) {
			if( size() > 1 ) {
				for( int i = 0; i < size(); i++ ) {
					Position pos;
					if( i == 0 )
						pos = Position.FIRST;
					else if( i == size() - 1 )
						pos = Position.LAST;
					else
						pos = Position.MIDDLE;
					shrinkWidth += get(i).getMainWidth(pos) + HORIZONTAL_SPACE;
				}
				shrinkWidth -= HORIZONTAL_SPACE;
			} else if( size() == 1 ) {
				shrinkWidth = 0;
			}
		}
		return shrinkWidth;
	}
}

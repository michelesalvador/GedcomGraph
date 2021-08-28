/* Union stores a list of PersonNodes and/or FamilyNodes that move horizontally together, ordered left to right.
 * It can coincide with a Group of descendants, or it can be the result of two Groups of ancestors merged together,
 * two Groups sharing the same ancestor(s) Node.
 */

package graph.gedcom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Union extends Metric {

	List<Node> list; // List of PersonNodes and FamilyNodes that move horizontally together
	Node ancestor; // The ancestor(s) of fulcrum, can be null
	float prevOverlap = -1;
	float nextOverlap = -1;

	Union() {
		list = new ArrayList<>();
	}

	// For ancestors where there are multiple origins
	List<Node> getOrigins() {
		if( ancestor != null )
			return ancestor.getOrigins();
		return Collections.emptyList();
	}

	// For descendants where union = group
	Node getOrigin() {
		return list.get(0).getOrigin();
	}

	public int getGeneration() {
		return list.get(0).generation;
	}

	// Excluded spuses at extremes
	@Override
	public float centerRelX() {
		Group group = list.get(0).getGroup();
		return group.getLeftWidth(null) + group.getCentralWidth(null) / 2;
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
		setForce(push, 0);
	}

	// ancestorsDescendantsToo 0: none, 1: ancestors too, 2: descendants too
	void setForce(float push, int ancestorsDescendantsToo) {
		for( Node node : list ) {
			node.setForce(push);
			// Update the descendants too
			if( ancestorsDescendantsToo == 2 && node.generation >= 0 ) {
				forceChildren(node, push);
			}
		}
		// And the ancestors
		if( ancestorsDescendantsToo == 1 && getGeneration() < 0 ) {
			forceOrigin(this, push);
		}
	}
	
	// Recoursively pass the force to uncestors
	void forceOrigin(Union union, float push) {
		for( Node origin : union.getOrigins() ) {
			if( !origin.mini && origin.union != null ) {
				origin.union.setForce(push, 1);
			}
		}
	}

	// Recoursively pass the force to children
	void forceChildren(Node node, float push) {
		if( node.youth != null && !node.youth.mini ) {
			for( Node child : node.youth.list ) {
				child.setForce(push);
				forceChildren(child, push);
			}
		}
	}

	// Total width of the union considering all nodes
	float getWidth() {
		if( width == 0 ) {
			Node lastChild = list.get(list.size() - 1);
			if( list.size() == 1 )
				width = lastChild.width;
			else
				width = lastChild.x + lastChild.width - x;
		}
		return width;
	}

	float getHeight() {
		float height = 0;
		for( Node node : list )
			height = Math.max(height, node.height);
		return height;
	}

	@Override
	public String toString() {
		return list.toString();
	}
}

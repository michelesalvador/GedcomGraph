package graph.gedcom;

import static graph.gedcom.Util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Animator {
	
	float width, height;
	PersonNode fulcrumNode;
	int maxAbove;
	List<Node> nodes;
	List<Line> lines;
	Map<Integer, Float> axes; // Number of the generation and vertical center of the rows
	
	Animator() {
		nodes = new ArrayList<>();
		lines = new ArrayList<>();
	}

	void addNode(Node newNode) {
		nodes.add(newNode);
	}
	
	// Preparing the nodes
	void initNodes(PersonNode fulcrumNode, int maxAbove, int maxBelow) {

		this.fulcrumNode = fulcrumNode;
		this.maxAbove = maxAbove;
		width = 0;
		height = 0;

		// Array with max height of each row of nodes
		int totalRows = maxAbove + 1 + maxBelow;
		float[] rowMaxHeight = new float[totalRows];

		for( Node node : nodes ) {
			// Discover the maximum height of rows
			if( !node.mini && node.height > rowMaxHeight[node.generation + maxAbove] )
				rowMaxHeight[node.generation + maxAbove] = node.height;

			// Calculate sizes of each family node
			if( node instanceof FamilyNode ) {
				node.width = node.mini ? MINI_FAMILY_WIDTH : FAMILY_WIDTH;
				for(Node partner : ((FamilyNode)node).partners ) {
					node.height = Math.max(node.height, partner.height);
				}
				node.height =  node.height / 2 + MARRIAGE_HEIGHT / 2;
			}
		}

		// Vertical position of the generation rows
		axes = new HashMap<>();
		float posY = rowMaxHeight[0] / 2;
		for( int gen = -maxAbove; gen < totalRows - maxAbove; gen++ ) {
			axes.put(gen, posY);
			posY += rowMaxHeight[gen + maxAbove] + VERTICAL_SPACE;
		}

		// Create the Lines
		lines.clear();
		for (Node node : nodes) {
			if (node instanceof PersonNode && node.getOrigin() != null)
				lines.add(new CurveLine((PersonNode)node));
			if (node instanceof PersonNode && node.getFamilyNode() != null)
				lines.add(new StraightLine((PersonNode)node));
		}
	}

	// Marriage nodes are added in the layout

	// First positioning of all nodes, with some nodes still possibly overlapping
	public void placeNodes() {

		// Vertical positioning

		// Put each regular (not mini) node on its generation row
		for( Node node : nodes ) {
			if( !node.mini )
				node.y = axes.get(node.generation) - node.centerRelY(); // TODO controlla vertical center
		}

		// Places mini origin above and mini children below
		for( Node origin : nodes ) {
			Group children = origin.getChildren();
			for( PersonNode child : children ) {
				if( origin.mini ) {
					distanceAbove(child, origin, children.size() > 1 ? VERTICAL_SPACE : ANCESTRY_DISTANCE);
				} else if( child.mini ) {
					distanceBelow(origin, child, PROGENY_DISTANCE);
				}
			}
		}

		// First horizontal positioning

		// Dispose horizontally the nodes of the fulcrum generation
		PersonNode previousWife = null;
		for( int i = 0; i < nodes.size(); i++ ) {
			Node node = nodes.get(i);
			if( node instanceof PersonNode && !((PersonNode)node).acquired && !node.mini && node.generation == 0) {
				placeFamily(previousWife, (PersonNode)node);
				previousWife = node.getWife();
			}
		}

		// Place fulcrum's ancestors and uncles with a recoursive method
		Node fulcrumOrigin = fulcrumNode.getOrigin();
		if( fulcrumOrigin != null && !fulcrumOrigin.mini ) {
			fulcrumOrigin.centerToChildren();
			placeUncles(fulcrumOrigin);
			for( PersonNode person : fulcrumOrigin.getChildren() )
				placeChildren(person);
		} else // Center fulcrum children
			placeChildren(fulcrumNode);

		// Centers all mini ancestry relatively to children and all mini progeny relatively to origin
		for( Node origin : nodes ) {
			Group children = origin.getChildren();
			if( children.size() > 0 ) {
				if( origin.mini ) //
					origin.centerToChildren();
				else if( children.get(0).mini )
					children.centerToOrigin();
			}
		}
		
		setDiagramBounds();
	}

	// Place the second node above the first one
	private void distanceAbove(Node a, Node b, float gap) {
		b.y = a.y - gap - b.height;
	}

	// Place the second node below the first one
	private void distanceBelow(Node a, Node b, float gap) {
		b.y = a.y + a.height + gap;
	}

	// Recursive method to place uncles bisides ancestors
	private void placeUncles(Node ancestorNode) {
		if( ancestorNode.getPartner(0) != null ) {
			Node origin = ancestorNode.getPartner(0).getOrigin();
			if( origin != null ) {
				Group children = origin.getChildren();
				for( int i = children.size() - 1; i >= 0; i-- ) {
					PersonNode uncle = children.get(i).getHusband();
					if( i > 0 ) {
						PersonNode prevUncle = children.get(i - 1).getWife();
						prevUncle.x = uncle.x - HORIZONTAL_SPACE - prevUncle.width;
						placeFamily(prevUncle);
					}
					if(uncle.generation == -1)
						placeChildren(uncle);
				}
				origin.centerToChildren();
				if( !origin.mini )
					placeUncles(origin);
			}
		}
		if( ancestorNode.getPartner(1) != null ) {
			Node origin = ancestorNode.getPartner(1).getOrigin();
			if( origin != null ) {
				Group children = origin.getChildren();
				for( int i = 0; i < children.size() - 1; i++ ) {
					PersonNode uncle = children.get(i).getWife();
					if( i < children.size() - 1 ) {
						PersonNode nextUncle = children.get(i + 1).getHusband();
						nextUncle.x = uncle.x + uncle.width + HORIZONTAL_SPACE;
						placeFamily(nextUncle);
						if(uncle.generation == -1)
							placeChildren(nextUncle);
					}
				}
				origin.centerToChildren();
				if( !origin.mini )
					placeUncles(origin);
			}
		}
	}

	// Recoursive position of all the children nodes
	void placeChildren(Node startNode) {
		Node origin;
		if( startNode.getFamilyNode() != null )
			origin = startNode.getFamilyNode();
		else
			origin = startNode;
		Group children = origin.getChildren();
		for( int i = 0; i < children.size(); i++ ) {
			PersonNode child = children.get(i);
			if( !child.mini ) { // Regular children
				if( i == 0) {
					child.x = origin.centerX() - child.centerRelX() - children.getShrinkWidth() / 2;
					placeFamily(child);
				}
				if( i < children.size() - 1 ) {
					PersonNode nextChild = children.get(i + 1);
					placeFamily(child.getWife(), nextChild);
				}
				placeChildren(child);
			}
		}
	}

	/** Place a family including partners starting from a main partner, to the right of a reference person
	 * @param reference
	 * @param mainPartner
	 */
	private float placeFamily(PersonNode reference, PersonNode mainPartner) {
		float distance = 0;
		if( reference == null )
			reference = mainPartner.getWife();
		if( mainPartner.familyNode != null ) {
			FamilyNode family = mainPartner.familyNode;
			if( mainPartner.equals(family.getHusband()) ) {
				distance = reference.width + HORIZONTAL_SPACE;
				mainPartner.x = reference.x + distance;
				distance += mainPartner.width - MARRIAGE_OVERLAP;
				family.x = reference.x + distance;
				distance += family.width - MARRIAGE_OVERLAP;
				family.getWife().x = reference.x + distance;
			} else {
				distance = reference.width + HORIZONTAL_SPACE;
				family.getHusband().x = reference.x + distance;
				distance += family.getHusband().width - MARRIAGE_OVERLAP;
				family.x = reference.x + distance;
				distance += family.width - MARRIAGE_OVERLAP;
				mainPartner.x = reference.x + distance;
			}
		} else {
			mainPartner.x = reference.x + reference.width + HORIZONTAL_SPACE;
		}
		return distance;
	}

	/** Generate the next position of each node and line, resolving overlapping (TODO)
	* @return false if movement is complete
	*/
	public boolean playNodes() {
		// Detect collision between overlapping groups of the same generation
		float totalOverlap = 0;
		for( int i = 0; i < nodes.size(); i++ ) {
			Node originA = nodes.get(i);
			if( !originA.getChildren().isEmpty() ) {
				Group groupA = originA.getChildren();
				inner: for( int j = i + 1; j < nodes.size(); j++ ) {
					Node originB = nodes.get(j);
					if( !originB.getChildren().isEmpty() ) {
						Group groupB = originB.getChildren();
						PersonNode personA = groupA.get(0).getHusband();
						PersonNode personB = groupB.get(0).getHusband();

						float groupAright = personA.x + groupA.getWidth();
						float groupBleft = personB.x;
						FamilyNode lastFamilyA = groupA.get(groupA.size()-1).getFamilyNode();
						if( groupA.generation == groupB.generation // They are cousins
								&& personA.mini == personB.mini // Both mini or both regular
								&& !(personA.acquired || personB.acquired) // Acquired are excluded
								&& (lastFamilyA == null || !lastFamilyA.equals(groupB.get(0).getFamilyNode())) // Not married
								&& groupAright + GROUP_DISTANCE > groupBleft ) { // They are overlapping
							float overlap = groupAright + GROUP_DISTANCE - groupBleft;
							totalOverlap += overlap;

							personA.x -= overlap/2;
							personB.x += overlap/2;

							placeFamily(personA);
							placeFamily(personB);
							placeGroup(groupA);
							placeGroup(groupB);

							for( Node node : groupA )
								placeChildren(node);
							for( Node node : groupB )
								placeChildren(node);

							//originA.centerToChildren();
							//originB.centerToChildren();

							break inner;
						}
					}
				}
			}
		}

		// Centers all mini ancestry relatively to children and all mini progeny relatively to origin
		for( Node origin : nodes ) {
			Group children = origin.getChildren();
			if( children.size() > 0 ) {
				if( origin.mini ) //
					origin.centerToChildren();
				else if( children.get(0).mini )
					children.centerToOrigin();
			}
		}

		// Final position of the nodes
		setDiagramBounds();

		return totalOverlap > 0.01; // Stop playing when no more overlap
	}

	// Position all the nodes to the right of the first node of a group
	private void placeGroup(Group group) {
		PersonNode previousWife = group.get(0).getWife();
		for( int i = 1; i < group.size(); i++ ) {
			PersonNode personNode = group.get(i);
			placeFamily(previousWife, personNode);
			previousWife = personNode.getWife();
		}
	}

	// Update the position of a family node starting from a partner
	private void placeFamily(PersonNode personNode) {
		if( personNode.familyNode != null ) {
			FamilyNode family = personNode.familyNode;
			if( personNode.equals(family.getHusband()) ) {
				family.x = personNode.x + personNode.width - MARRIAGE_OVERLAP;
				family.getWife().x = family.x + family.width - MARRIAGE_OVERLAP;
			} else {
				family.x = personNode.x - family.width + MARRIAGE_OVERLAP;
				family.getHusband().x = family.x + MARRIAGE_OVERLAP - family.getHusband().width;
			}
		}
	}

	private void setDiagramBounds() {
		// Find the diagram margins to fit exactly around every node
		float minX = 999999, minY = 999999;
		float maxX = -999999, maxY = -999999;
		for( Node node : nodes ) {
			if( node.x < minX ) minX = node.x;
			if( node.x + node.width > maxX ) maxX = node.x + node.width;
			if( node.y < minY ) minY = node.y;
			if( node.y + node.height > maxY ) maxY = node.y + node.height;
		}
		width = maxX - minX;
		height = maxY - minY;

		// Correct the position of each node
		for( Node node : nodes ) {
			node.x -= minX;
			node.y -= minY;
		}

		// Update lines position
		for( Line line : lines )
			line.update();

		// Order lines from left to right
		Collections.sort(lines, new Comparator<Line>() {
			@Override
			public int compare(Line line1, Line line2) {
				return line1.compareTo(line2);
			}
		});

	}
}

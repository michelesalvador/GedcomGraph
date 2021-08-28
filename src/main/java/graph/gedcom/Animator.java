package graph.gedcom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static graph.gedcom.Util.*;

public class Animator {
	
	float width, height;
	PersonNode fulcrumNode;
	int maxAbove;
	List<Node> nodes; // All person and family nodes regular and mini
	List<PersonNode> personNodes;
	List<Bond> bonds;
	List<Line> lines; // All the lines ordered from left to right
	List<LinesRow> lineRows; // All the lines divided in groups in a 2D array
	List<Set<Line>> linesGroups; // All the lines distributed in groups by proximity
	List<Group> groups; // Array of groups of PersonNodes and FamilyNodes (not mini)
	List<Row> unionRows; // Array of rows of Unions
	int maxBitmapWidth, maxBitmapHeight;
	float prevTotalForces;
	float maxForces;
	List<Float> allFractions;

	
	Animator() {
		nodes = new ArrayList<>();
		personNodes = new ArrayList<>();
		bonds = new ArrayList<>();
		lines = new ArrayList<>();
		lineRows = new ArrayList<>();
		linesGroups = new ArrayList<>();
		groups = new ArrayList<>();
		unionRows = new ArrayList<>();
		allFractions = new ArrayList<>();
	}

	void addNode(Node newNode) {
		// Add node to nodes
		nodes.add(newNode);

		// Add partners to personNodes list
		personNodes.addAll(newNode.getPersonNodes());
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
			// Calculate sizes of each family node
			if( node instanceof FamilyNode ) {
				FamilyNode familyNode = (FamilyNode)node;
				for( Node partner : familyNode.partners ) {
					familyNode.width += partner.width;
					familyNode.height = Math.max(familyNode.height, partner.height);
				}
				if( familyNode.height == 0 ) // Regular or mini ancestor without partners
					familyNode.height = 20;
				// Bond sizes
				Bond bond = familyNode.bond;
				if( bond != null ) {
					float bondWidth = familyNode.mini ? MINI_BOND_WIDTH : MARRIAGE_WIDTH;
					familyNode.bond.width = bondWidth;
					familyNode.bond.height = familyNode.height;
					bonds.add(familyNode.bond);
					familyNode.width += familyNode.getBondWidth();
				}
			}
			// Discover the maximum height of rows
			if( !node.mini && node.height > rowMaxHeight[node.generation + maxAbove] )
				rowMaxHeight[node.generation + maxAbove] = node.height;
		}

		// Vertical position of the generation rows
		unionRows.clear();
		float posY = rowMaxHeight[0] / 2;
		for( int gen = -maxAbove; gen < totalRows - maxAbove; gen++ ) {
			unionRows.add(new Row(gen, posY));
			if( gen + maxAbove < totalRows - 1 )
				posY += rowMaxHeight[gen + maxAbove] / 2 + VERTICAL_SPACE + rowMaxHeight[gen + maxAbove + 1] / 2;
		}

		// Initialize the relation between groups and their origin
		for( Group group : groups ) {
			group.setOrigin();
		}

		// Create the Lines
		lines.clear();
		for( Node node : nodes ) {
			for( PersonNode personNode : node.getPersonNodes() ) {
				if( personNode.getOrigin() != null ) {
					lines.add(new CurveLine(personNode));
				}
			}
			if( node instanceof FamilyNode ) {
				FamilyNode familyNode = (FamilyNode)node;
				if( familyNode.partners.size() > 1 && familyNode.bond.marriageDate == null )
					lines.add(new HorizontalLine(familyNode));
				if( familyNode.hasChildren() && familyNode.bond != null )
					lines.add(new VerticalLine(familyNode));
			}
		}

		// Populate unionRows from groups
		// Couples of ancestors groups with common ancestor node are joined in a single Union
		for( Group group : groups ) {
			if( !group.mini && !group.list.isEmpty() ) {
				Row row = unionRows.get(group.generation + maxAbove);
				Union newUnion = null;
				boolean joinExistingGroup = false;
				find:
				for( Node node : group.list ) {
					if( node.isAncestor() ) {
						// Search an existing union to join
						for( Union union : row ) {
							if( node.equals(union.ancestor) ) {
								newUnion = union;
								joinExistingGroup = true;
								break find;
							}
						}
						newUnion = new Union();
						newUnion.ancestor = node;
						newUnion.ancestor.union = newUnion;
						break;
					}
				}
				if( newUnion == null ) { // Union without ancestor
					newUnion = new Union();
				}
				newUnion.list.addAll(group.list);
				if( joinExistingGroup ) {
					newUnion.list.remove(newUnion.ancestor); // remove duplicated ancestor node
				} else {
					row.addUnion(newUnion);
				}
			}
		}
	}

	// At this point marriage bonds are added in the layout

	// First positioning of all nodes, with some nodes still possibly overlapping
	public void placeNodes() {

		// Vertical positioning

		// Vertically place each regular (not mini) node
		for( Row row : unionRows ) {
			for( Union union : row ) {
				union.y = row.yAxe - union.centerRelY();
				for( Node node : union.list ) {
					node.setY(row.yAxe - node.centerRelY());
				}
			}
		}

		// Places vertically mini origins and mini youths
		for( Group group : groups ) {
			if( group.isOriginMiniOrEmpty() ) {
				group.y = unionRows.get(group.generation + maxAbove).yAxe - group.centerRelY();
				group.placeOriginY();
			}
			for( Node node : group.list ) {
				node.placeAcquiredOriginY();
				// Set youth y position
				Group youth = node.youth;
				if( youth != null && youth.mini )
					youth.setY(node.y + node.height + PROGENY_DISTANCE);
			}
		}

		// Horizontal positioning

		// Place horizontally the nodes of the fulcrum generation
		float posX = 0;
		for( Node node : fulcrumNode.getGroupList() ) {
			node.setX(posX);
			posX += node.width + HORIZONTAL_SPACE;
			placeChildren(node);
		}

		// Place fulcrum's ancestors and uncles with a recoursive method
		Node fulcrumOrigin = fulcrumNode.getOrigin();
		if( fulcrumOrigin != null ) {
			placeAncestors(fulcrumOrigin, Branch.NONE);
		}

		// Horizontally place acquired mini ancestry and all mini progeny
		for( Node node : nodes ) {
			node.placeAcquiredOriginX();
			node.placeMiniChildrenX();
		}
		
		setDiagramBounds();

		// Update unions x position and width
		for( Row row : unionRows ) {
			for( Union union : row ) {
				union.x = union.list.get(0).x;
				union.width = union.getWidth();
			}
		}

		prevTotalForces = 0;
		maxForces = 0;
		allFractions.clear();
	}

	// Recursive method to horizontally place ancestors and uncles
	private void placeAncestors(Node ancestorNode, Branch branch) {
		ancestorNode.centerToYouth(branch);
		Union union = ancestorNode.union;
		if( union != null ) {
			// Place paternal uncles
			float posX = ancestorNode.x;
			for( int i = union.list.indexOf(ancestorNode) - 1; i >= 0; i-- ) {
				Node node = union.list.get(i);
				posX -= node.width + HORIZONTAL_SPACE;
				node.setX(posX);
				// Cousins
				if( node.generation == -1) {
					placeChildren(node);
				}
			}
			// Place maternal uncles
			posX = ancestorNode.x + ancestorNode.width + HORIZONTAL_SPACE;
			for( int i = union.list.indexOf(ancestorNode) + 1; i < union.list.size(); i++ ) {
				Node node = union.list.get(i);
				node.setX(posX);
				posX += node.width + HORIZONTAL_SPACE;
				// Cousins
				if( node.generation == -1) {
					placeChildren(node);
				}
			}
			// Step-siblings
			if( union.getGeneration() == -1 ) {
				Group fulcrumGroup = fulcrumNode.getGroup();
				// Paternal
				Group leftStepGroup = ancestorNode.getFamilyNode().stepYouthLeft;
				if( leftStepGroup != null ) {
					posX = fulcrumGroup.x;
					for( int i = leftStepGroup.list.size() - 1; i >= 0; i-- ) {
						Node node = leftStepGroup.list.get(i);
						posX -= HORIZONTAL_SPACE + node.width; // Per la precisione il primo dovrebbe essere -UNION_DISTANCE
						node.setX(posX);
						placeChildren(node);
					}
				}
				// Maternal
				Group rightStepGroup = ancestorNode.getFamilyNode().stepYouthRight;
				if( rightStepGroup != null ) {
					posX = fulcrumGroup.x + fulcrumGroup.getWidth() + UNION_DISTANCE;
					for( Node node : rightStepGroup.list ) {
						node.setX(posX);
						posX += node.width + HORIZONTAL_SPACE;
						placeChildren(node);
					}
				}
			}
			// Recoursive calls
			for( Node origin: union.getOrigins() ) {
				origin.youth.x = origin.youth.list.get(0).x; // Update youth's x position
				placeAncestors(origin, origin.youth.branch);
			}
		}
	}

	// Recoursive horizontal position of all the regular children nodes
	void placeChildren(Node node) {
		Group youth = node.youth;
		if( youth != null && !youth.mini ) { // Existing regular children only
			float posX = node.centerX() - youth.getLeftWidth(Branch.NONE) - youth.getCentralWidth(Branch.NONE) / 2;
			float distance = youth.mini ? PROGENY_PLAY : HORIZONTAL_SPACE;
			for( Node child : youth.list ) {
				child.setX(posX);
				posX += child.width + distance;
				placeChildren(child);
			}
		}
	}

	/** Generate the next position of each node and line, resolving overlaps.
	* @return false if movement is complete
	*/
	public boolean playNodes() {

		// Reset forces
		for( Node node : nodes ) {
			node.force = 0;
		}

		// Detect collision between overlapping unions of the same generation
		for( Row row : unionRows ) {
			float prevOverlap = 0;
			for( int i = 0; i < row.size(); i++ ) {
				Union union = row.get(i);
				float nextOverlap = 0;
				if( i < row.size() - 1 ) {
					Union nextUnion = row.get(i + 1);
					float marginRight = union.x + union.getWidth() + UNION_DISTANCE;
					nextOverlap = marginRight - nextUnion.x;
					if( nextOverlap > 0 ) { // Repulsion between unions
						// Shift to the left this union
						union.setForce(-nextOverlap);
						// and all the unions to its left
						for( int l = i; l > 0; l-- ) {
							Union leftUnion = row.get(l);
							Union prevLeftUnion = row.get(l - 1);
							float leftOverlap = prevLeftUnion.x + prevLeftUnion.getWidth() + UNION_DISTANCE	- leftUnion.x;
							if( leftOverlap > 0 )
								prevLeftUnion.setForce(-nextOverlap);
							else
								break;
						}
						// Shift to the right this union
						nextUnion.setForce(nextOverlap);
						// and all the unions to its right
						for( int r = i + 1; r < row.size() - 1; r++ ) {
							Union rightUnion = row.get(r);
							Union nextRightUnion = row.get(r + 1);
							float rightOverlap = rightUnion.x + rightUnion.getWidth() + UNION_DISTANCE - nextRightUnion.x;
							if( rightOverlap > 0 )
								nextRightUnion.setForce(nextOverlap);
							else
								break;
						}
					}
				}

				if( i == 0 )
					prevOverlap = Integer.MIN_VALUE;
				if( i == row.size() - 1 )
					nextOverlap = Integer.MIN_VALUE;
				// Try to center descendants below the origin
				Node origin = union.getOrigin();
				if( row.generation >= 0 && origin != null ) {
					float distance = origin.centerX() - union.centerX();
					float push = 0;
					if( distance > 0 )
						push = Math.min(distance, -nextOverlap);
					else if( distance < 0)
						push = Math.max(distance, prevOverlap);
					union.setForce(push);
					// This union moving to the left drags unions at its right
					if( push < 0 ) {
						for( int r = i + 1; r < row.size(); r++ ) {
							Union rightUnion = row.get(r);
							Node rightOrigin = rightUnion.getOrigin();
							float rightDistance = rightOrigin.centerX() - rightUnion.centerX();
							if( rightDistance < 0 ) {
								push = Math.max(rightDistance, push);
								rightUnion.setForce(push, 2);
							} else
								break;
						}
					} // This union moving to the right drags unions at its left
					else if( push > 0 ) {
						for( int l = i - 1; l >= 0; l-- ) {
							Union leftUnion = row.get(l);
							Node leftOrigin = leftUnion.getOrigin();
							float leftDistance = leftOrigin.centerX() - leftUnion.centerX();
							if( leftDistance > 0 ) {
								push = Math.min(leftDistance, push);
								leftUnion.setForce(push, 2);
							} else
								break;
						}
					}
				}

				// Try to center hancestors above descendants
				if( row.generation < -1 ) {
					Group youth = union.ancestor.youth;
					youth.x = youth.list.get(0).x;
					float distance = youth.centerX() - union.ancestor.centerX();
					float push = 0;
					if( distance > 0 )
						push = Math.min(distance, -nextOverlap);
					else if( distance < 0)
						push = Math.max(distance, prevOverlap);
					union.setForce(push);
					// This union moving to the left drags unions at its right
					if( push < 0 ) {
						for( int r = i + 1; r < row.size(); r++ ) {
							Union rightUnion = row.get(r);
							Group rightYouth = rightUnion.ancestor.youth;
							rightYouth.x = rightYouth.list.get(0).x;
							float rightDistance = rightYouth.centerX() - rightUnion.ancestor.centerX();
							if( rightDistance < 0 ) {
								push = Math.max(rightDistance, push);
								rightUnion.setForce(push, 1);
							} else
								break;
						}
					} // This union moving to the right drags unions at its left
					else if( push > 0 ) {
						for( int l = i - 1; l >= 0; l-- ) {
							Union leftUnion = row.get(l);
							Group leftYouth = leftUnion.ancestor.youth;
							float leftDistance = leftYouth.centerX() - leftUnion.ancestor.centerX();
							if( leftDistance > 0 ) {
								push = Math.min(leftDistance, push);
								leftUnion.setForce(push, 1);
							} else
								break;
						}
					}
				}
				prevOverlap = nextOverlap;
			}
		}

		// Apply forces
		float totalForces = 0;
		for( Node node : nodes ) {
			totalForces += Math.abs(node.applyForce());
		}

		// Place horizontally acquired mini ancestry and all mini progeny
		for( Group group : groups ) {
			if( group.isOriginMiniOrEmpty() ) {
				group.placeOriginX();
			}
			for( Node node : group.list ) {
				node.placeAcquiredOriginX();
				node.setMiniChildrenX();
			}
		}

		// Final position of the nodes
		setDiagramBounds();

		// Update unions x position
		for( Row row : unionRows ) {
			for( Union union : row ) {
				union.x = union.list.get(0).x;
			}
		}

		maxForces = Math.max(maxForces, totalForces);
		allFractions.add(Math.abs(prevTotalForces - totalForces) / maxForces);
		prevTotalForces = totalForces;
		float average = 0;
		for( float fraction : allFractions ) {
			average += fraction;
		}
		average /= allFractions.size();
		return average > .04;
	}

	private void setDiagramBounds() {
		// Find the diagram margins to fit exactly around every node
		float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		float maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
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
			node.setX(node.x - minX);
			node.setY(node.y - minY);
		}

		if( maxBitmapWidth > 0 && maxBitmapHeight > 0 ) {
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

			// Clear lineRows
			for( LinesRow row : lineRows ) {
				row.reset();
			}

			// Distribute lines inside 'lineRows'
			for( Line line : lines ) {
				float lineWidth = Math.abs(line.x1 - line.x2);
				if( lineWidth <= maxBitmapWidth ) {
					int rowNum = (int)(line.y2 / maxBitmapHeight);
					while( rowNum >= lineRows.size() ) {
						lineRows.add(new LinesRow());
					}
					LinesRow row = lineRows.get(rowNum);
					if( row.restartX < 0)
						row.restartX = Math.min(line.x1, line.x2);
					float groupRight = Math.max(line.x1, line.x2) - row.restartX;
					// Start another group
					if( groupRight > maxBitmapWidth ) {
						row.activeGroup++;
						row.restartX = Math.min(line.x1, line.x2);
					}
					if( row.activeGroup >= row.size() )
						row.add(new HashSet<Line>());
					row.get(row.activeGroup).add(line);
				}
			}

			// Populate linesGroups with existing groups of lines
			linesGroups.clear();
			for( LinesRow row : lineRows ) {
				for( Set<Line> group : row ) {
					if( group.size() > 0 )
						linesGroups.add(group);
				}
			}
		}
	}

	// One row of the 2D array 'lineRows'
	class LinesRow extends ArrayList<Set<Line>> {

		int activeGroup; // Index of the used group
		float restartX = -1; // (re)starting point of every group of lines inside this row

		void reset() {
			activeGroup = 0;
			restartX = -1;
			for( Set<Line> group : this )
				group.clear();
		}
	}

	@Override
	public String toString() {
		String txt = "";
		for( Row row : unionRows )
			txt += row + "\n";
		return txt;
	}
}

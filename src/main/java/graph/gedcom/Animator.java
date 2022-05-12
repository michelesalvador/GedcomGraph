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
	List<Line> lines; // All the continuous lines ordered from left to right
	List<LinesRow> lineRows; // All the continuous lines divided in groups in a 2D array
	List<Set<Line>> lineGroups; // All the continuous lines distributed in groups by proximity
	List<Line> backLines; // All the back (dashed) lines ordered from left to right
	List<LinesRow> backLineRows; // All the back (dashed) lines divided in groups in a 2D array
	List<Set<Line>> backLineGroups; // All the back (dashed) lines distributed in groups by proximity
	List<Group> groups; // Array of groups of PersonNodes and FamilyNodes (not mini)
	List<GroupRow> groupRows;
	List<UnionRow> unionRows; // Array of rows of Unions
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
		lineGroups = new ArrayList<>();
		backLines = new ArrayList<>();
		backLineRows = new ArrayList<>();
		backLineGroups = new ArrayList<>();
		groups = new ArrayList<>();
		groupRows = new ArrayList<>();
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
					float bondWidth = familyNode.mini ? MINI_BOND_WIDTH : bond.marriageDate != null ? MARRIAGE_WIDTH : BOND_WIDTH;
					bond.width = bondWidth;
					bond.height = familyNode.height;
					if( bond.marriageDate != null ) {
						bond.overlap = (MARRIAGE_WIDTH - MARRIAGE_INNER_WIDTH) / 2;
						if( familyNode.side == Side.LEFT || familyNode.side == Side.RIGHT )
							familyNode.width += bond.overlap;
					}
					bonds.add(bond);
					familyNode.width += familyNode.getBondWidth();
				}
			}
			// Discover the maximum height of rows
			if( !node.mini && node.height > rowMaxHeight[node.generation + maxAbove] )
				rowMaxHeight[node.generation + maxAbove] = node.height;
		}

		// Vertical position of the generation rows
		unionRows.clear();
		groupRows.clear();
		float posY = rowMaxHeight[0] / 2;
		for( int gen = -maxAbove; gen < totalRows - maxAbove; gen++ ) {
			unionRows.add(new UnionRow(gen, posY));
			groupRows.add(new GroupRow(gen));
			if( gen + maxAbove < totalRows - 1 )
				posY += rowMaxHeight[gen + maxAbove] / 2 + VERTICAL_SPACE + rowMaxHeight[gen + maxAbove + 1] / 2;
		}

		// Initialize the relation between groups and their origin
		for( Group group : groups ) {
			group.setOrigin();
		}

		// Create the Lines
		lines.clear();
		backLines.clear();
		for( Node node : nodes ) {
			// Curve lines
			for( PersonNode personNode : node.getPersonNodes() ) {
				if( personNode.getOrigin() != null ) {
					lines.add(new CurveLine(personNode));
				}
			}
			// All other lines
			if( node instanceof FamilyNode ) {
				FamilyNode familyNode = (FamilyNode)node;
				if( familyNode.partners.size() > 0 && (familyNode.match == Match.MIDDLE || familyNode.match == Match.FAR) )
					lines.add(new NextLine(familyNode));
				else if( familyNode.partners.size() > 1 && familyNode.bond.marriageDate == null )
					lines.add(new HorizontalLine(familyNode));
				if( familyNode.match == Match.MIDDLE || familyNode.match == Match.FAR )
					backLines.add(new BackLine(familyNode));
				if( familyNode.hasChildren() && familyNode.bond != null )
					lines.add(new VerticalLine(familyNode));
			}
		}

		// Populate groupRows from groups
		for( Group group : groups ) {
			if( !group.mini && !group.list.isEmpty() ) {
				groupRows.get(group.generation + maxAbove).add(group);
			}
		}

		// Add prev and next to each node
		for( GroupRow row : groupRows ) {
			Node previous = null;
			for( Group group : row ) {
				for( int n = 0; n < group.list.size(); n++ ) {
					Node node = group.list.get(n);
					if( !(node.getPersonNodes().isEmpty() && node.isAncestor) ) { // Empty ancestors are excluded
						if( !node.equals(previous) )
							node.prev = previous;
						if( node.prev != null )
							node.prev.next = node;
						previous = node;
					}
				}
			}
		}

		// Populate unionRows from groups
		// Couples of ancestors groups with common ancestor node are joined in a single Union
		for( Group group : groups ) {
			if( !group.mini && !group.list.isEmpty() ) {
				UnionRow row = unionRows.get(group.generation + maxAbove);
				Union union = null;
				boolean joinExistingGroup = false;
				find:
				for( Node node : group.list ) {
					if( node.isAncestor ) {
						// Search an existing union to join
						for( Union un : row ) {
							if( node.equals(un.ancestor) ) {
								union = un;
								joinExistingGroup = true;
								break find;
							}
						}
						union = new Union();
						union.ancestor = node;
						break;
					}
				}
				if( union == null ) { // Union without ancestor
					union = new Union();
				}
				// Add the group to the union
				if( joinExistingGroup ) { // Already populated union
					for( Node node : group.list ) {
						if( !node.equals(union.ancestor)) { // Avoid duplicated ancestor node
							if( group.branch == Branch.PATER ) // Insert before ancestor
								union.list.add(union.list.indexOf(union.ancestor), node);
							else {
								union.list.add(node); // Add at the end
							}
						}
					}
				} else { // Empty union
					union.list.addAll(group.list);
					row.addUnion(union);
				}
				for( Node node : union.list )
					node.union = union; // TODO A ben guardare assegna 2 volte la union ai medesimi nodi
			}
		}
	}

	// At this point marriage bonds are added in the layout

	// First positioning of all nodes, resolving all the overlaps
	public void placeNodes() {

		// Vertical positioning

		// Vertically place each regular (not mini) node
		for( UnionRow row : unionRows ) {
			for( Union union : row ) {
				union.y = row.yAxe - union.centerRelY();
				for( Node node : union.list ) {
					node.setY(row.yAxe - node.centerRelY());
				}
			}
		}

		// Places vertically mini origins and mini youths
		for( Group group : groups ) {
			if( !group.mini && group.isOriginMiniOrEmpty() ) {
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

		// The fulcrum family could be the only one and need to be placed
		if( fulcrumNode.familyNode != null ) {
			fulcrumNode.familyNode.setX(0);
		}

		// Ascend generations resolving overlaps and disposing ancestors and uncles
		// starting from generation -1 (if exists) or from fulcrum generation up
		for( int r = maxAbove > 0 ? maxAbove - 1 : maxAbove; r >= 0; r-- ) {
			GroupRow groupRow = groupRows.get(r);
			groupRow.resolveOverlap();
			// Place ancestors of the above generation
			for( Group group : groupRow ) {
				Node ancestor = group.origin;
				if( ancestor != null ) {
					group.placeOriginX();
					Union union = ancestor.union;
					if( union != null ) {
						// Place paternal uncles
						float posX = ancestor.x;
						for( int i = union.list.indexOf(ancestor) - 1; i >= 0; i-- ) {
							Node node = union.list.get(i);
							posX -= node.width + HORIZONTAL_SPACE;
							node.setX(posX);
						}
						// Place maternal uncles
						posX = ancestor.x + ancestor.width + HORIZONTAL_SPACE;
						for( int i = union.list.indexOf(ancestor) + 1; i < union.list.size(); i++ ) {
							Node node = union.list.get(i);
							node.setX(posX);
							posX += node.width + HORIZONTAL_SPACE;
						}
					}
				}
			}
		}

		// Position the descendants starting from generation -1 (if existing) or from fulcrum generation down
		for( int r = maxAbove > 0 ? maxAbove - 1 : maxAbove; r < unionRows.size(); r++ ) {
			UnionRow unionRow = unionRows.get(r);
			unionRow.resolveOverlap();
			for( Union union : unionRow ) {
				for( Node node : union.list ) {
					Group youth = node.youth;
					if( youth != null && !youth.mini ) { // Existing regular children only
						// Place stallion child and their spouses
						if( youth.stallion != null ) {
							youth.stallion.setX(node.centerX() - youth.stallion.getLeftWidth(null));
							Node right = youth.stallion;
							while( right.next != null ) {
								right.next.setX(right.x + right.width + HORIZONTAL_SPACE);
								right = right.next;
							}
							Node left = youth.stallion;
							while( left.prev != null ) {
								left.prev.setX(left.x - HORIZONTAL_SPACE - left.prev.width);
								left = left.prev;
							}
						} else { // Place normal youth
							float posX = node.centerX() - youth.getLeftWidth() - youth.getBasicCentralWidth() / 2;
							for( Node child : youth.list ) {
								child.setX(posX);
								posX += child.width + HORIZONTAL_SPACE;
							}
						}
					}
				}
			}
		}

		// Horizontally place acquired mini ancestry and all mini progeny
		for( Node node : nodes ) {
			node.placeAcquiredOriginX();
			node.placeMiniChildrenX();
		}

		setDiagramBounds();

		// Update unions x position and width
		for( UnionRow row : unionRows ) {
			for( Union union : row ) {
				union.updateX();
				union.width = union.getWidth();
			}
		}
		prevTotalForces = 0;
		maxForces = 0;
		allFractions.clear();
	}

	/** Generate the next position of each node and line, resolving overlaps.
	* @return false if movement is complete
	*/
	public boolean playNodes() {

		// Reset forces
		for( Node node : nodes ) {
			node.force = 0;
		}

		// Loop inside groups and inside nodes to distribute forces
		for(GroupRow row : groupRows) {
			for( int i = 0; i < row.size(); i++ ) {
				Group group = row.get(i);
				group.updateX();
				Node origin = group.origin;

				// Align origin centered above group
				/*if( origin != null && !origin.mini  ) {
					float distance = group.centerX() - origin.centerX();
					origin.setForce(distance);
				}*/

				// Keep origin node inside margins of youth group
				if( origin != null && !origin.mini ) {
					float distance = group.centerX() - origin.centerX();
					float push = 0;
					if( distance > 0 ) {
						push = Math.max(0, group.leftX() - origin.centerX());
					} else if( distance < 0 )
						push = Math.min(0, group.rightX() - origin.centerX());
					origin.setForce(push);
				}

				// Align group below origin respecting distance from other groups
				if( origin != null && !origin.mini && row.generation >= 0 ) {
					float distance = (origin.centerX() - group.centerX());
					/*if( distance > 0 && i < row.size() - 1 ) {
						Group nextGroup = row.get(i + 1);
						distance = Math.min(0, nextGroup.x - (group.x + group.getWidth() + UNION_DISTANCE));
					} else if( distance < 0 && i > 0 ) {
						Group prevGroup = row.get(i - 1);
						distance = Math.max(0, prevGroup.x + prevGroup.getWidth() + UNION_DISTANCE - group.x);
					}*/
					group.setForce(distance);
				}

				// Keep youth group almost below origin
				/*if( origin != null && !origin.mini ) {
					float distance = origin.centerX() - group.centerX();
					float push = 0;
					if( distance > 0 ) {
						push = Math.min(0, origin.centerX() - group.leftX());
					} else if( distance < 0 )
						push = Math.max(0, origin.centerX() - group.rightX());
					group.setForce(push);
				}*/

				// Keep each node near the next one of the same group
				for( int n = 0; n < group.list.size() - 1; n++ ) {
					Node node = group.list.get(n);
					Node nextNode = group.list.get(n + 1);
					float marginRight = node.x + node.width + HORIZONTAL_SPACE;
					float overlap = (marginRight - nextNode.x) /1;
					if( overlap < 0 ) { // If they are distant only
						node.setForce(-overlap);
						nextNode.setForce(overlap);
					}
				}

				// Distance each group from the next of the same generation
				/*if( i < row.size() - 1 ) {
					Group nextGroup = row.get(i + 1);
					group.updateX();
					nextGroup.updateX();
					float marginRight = group.x + group.getWidth() + UNION_DISTANCE;
					float groupOverlap = marginRight - nextGroup.x;
					if( groupOverlap > 0 && !group.containsAncestor() ) { // Repulsion between groups  	row.generation >= 0 &&
						// Shift to the left this group
						group.setForce(-groupOverlap);
						// and all the unions to its left
						for( int l = i; l > 0; l-- ) {
							Group leftUnion = row.get(l);
							Group prevLeftUnion = row.get(l - 1);
							float leftOverlap = prevLeftUnion.x + prevLeftUnion.getWidth() + UNION_DISTANCE - leftUnion.x;
							if( leftOverlap > 0 )
								prevLeftUnion.setForce(-nextOverlap);
							else
								break;
						}
						// Shift to the right next group
						nextGroup.setForce(groupOverlap);
						// and all the unions to its right
						for( int r = i + 1; r < row.size() - 1; r++ ) {
							Group rightUnion = row.get(r);
							Group nextRightUnion = row.get(r + 1);
							float rightOverlap = rightUnion.x + rightUnion.getWidth() + UNION_DISTANCE - nextRightUnion.x;
							if( rightOverlap > 0 )
								nextRightUnion.setForce(nextOverlap);
							else
								break;
						}
					}
				}*/
			}
		}

		// Apply forces
		float totalForces = 0;
		for( Node node : nodes ) {
			totalForces += Math.abs(node.applyForce());
		}

		resolveOverlap();

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
		for( UnionRow row : unionRows ) {
			for( Union union : row ) {
				union.updateX();
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

	private void resolveOverlap() {
		for(GroupRow groupRow : groupRows) {
			groupRow.resolveOverlap();
		}
	}

	// Other attempts to resolve overlaps after forces been applied

	/*private float resolveOverlap() {
		float forces = 0;
		for( UnionRow row : unionRows) {
			for( int i = 0; i < row.size(); i++ ) {
				Union union = row.get(i);
				if( i < row.size() - 1 ) {
					union.updateX();
					Union nextUnion = row.get(i + 1);
					nextUnion.updateX();
					float unionOverlap = (union.x + union.getWidth() + UNION_DISTANCE - nextUnion.x) / 2;
					if( unionOverlap > 0 ) {
						forces += unionOverlap;
						union.setX(union.x - unionOverlap);
						nextUnion.setX(nextUnion.x + unionOverlap);
						union.shift(-unionOverlap);
						nextUnion.shift(unionOverlap);
					}
				}
				for( int n = 0; n < union.list.size() - 1; n++ ) {
					Node node = union.list.get(n);
					Node nextNode = union.list.get(n + 1);
					float nodeOverlap = (node.x + node.width + HORIZONTAL_SPACE - nextNode.x) / 2;
					if( nodeOverlap > 0 ) {
						forces += nodeOverlap;
						node.setX(node.x - nodeOverlap);
						nextNode.setX(nextNode.x + nodeOverlap);
						node.shift(-nodeOverlap);
						nextNode.shift(nodeOverlap);
					}
				}
			}
		}
		return forces;
	}*/

	/*private float resolveOverlap() {
		float forces = 0;
		for (Node node : nodes) {
			if( node.prev != null ) {
				float gap = node.union.equals(node.prev.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
				float leftOver = (node.prev.x + node.prev.width + gap - node.x) / 2;
				if( leftOver > 0 ) {
					node.setX(node.x + leftOver);
					node.shift(leftOver);
					node.prev.shift(-leftOver);
					node.prev.setX(node.prev.x - leftOver);
					forces += leftOver;
				}
			}
			if( node.next != null ) {
				float gap = node.union.equals(node.next.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
				float rightOver = (node.x + node.width + gap - node.next.x) / 2;
				if( rightOver > 0 ) {
					node.setX(node.x - rightOver);
					node.shift(-rightOver);
					node.next.shift(rightOver);
					node.next.setX(node.next.x + rightOver);
					forces += rightOver;
				}
			}
		}
		return forces;
	}*/

	/*private float resolveOverlap() {
		float forces = 0;
		for(GroupRow row : groupRows) {
			for( int i = 0; i < row.size(); i++ ) {
				Group group = row.get(i);
				if( i < row.size() - 1 ) {
					Group nextGroup = row.get(i + 1);
					float marginRight = group.x + group.getWidth() + UNION_DISTANCE;
					float groupOverlap = (marginRight - nextGroup.x) /2;
					if( groupOverlap > 0 ) { // Repulsion between groups
						forces += groupOverlap;
						// Shift to the left this group
						group.shift(-groupOverlap);
						// Shift to the right next group
						nextGroup.shift(groupOverlap);
					}
				}
			}
		}
		return forces;
	}*/

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
			distributeLines(lines, lineRows, lineGroups);
			distributeLines(backLines, backLineRows, backLineGroups);
		}
	}

	private void distributeLines(List<Line> lines, List<LinesRow> lineRows, List<Set<Line>> lineGroups) {
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
		lineGroups.clear();
		for( LinesRow row : lineRows ) {
			for( Set<Line> group : row ) {
				if( group.size() > 0 )
					lineGroups.add(group);
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
		for( Group group : groups ) {
			txt += group.generation + ": ";
			txt += group + "\n";
		}
		txt += "- - - - - - - - - - -\n";
		for( UnionRow row : unionRows )
			txt += row + "\n";
		return txt;
	}

	/*@Override
	public String toString() {
		String txt = "";
		for( NodeRow row : nodeRows )
			txt += row + "\n";
		return txt;
	}*/

	/*@Override
	public String toString() {
		String txt = "";
		for( GroupRow row : groupRows )
			txt += row + "\n";
		return txt;
	}*/
}

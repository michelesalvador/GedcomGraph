package graph.gedcom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import static graph.gedcom.Util.pr;

public class Graph {

	public float width, height;
	private Gedcom gedcom;
	// Settings for public methods with default values
	public int whichFamily; // Which family display if the fulcrum is child in more than one family
	private int ancestorGenerations = 3; // Generations to display
	private int greatUnclesGenerations = 2; // Uncles and great-uncles. Can't be more than ancestor generations.
	private boolean withSpouses = true;
	private int descendantGenerations = 3;
	private int siblingNephewGenerations = 2; // Siblings and generations of their descendants
	private int uncleCousinGenerations = 2; // Uncles and cousins. First uncle generation overlaps to great-uncle generations.
	private List<List<Node>> nodeRows; // A list of lists of all the nodes
	private Set<Node> nodes;
	private List<Line> lines;
	private UnitNode fulcrumNode;
	private Group baseGroup; // In which fulcrum is one of the children (youth)
	private int fulcrumRow; // Number of row in nodeRows where is fulcrum
	private float negativeHorizontal; // Max shift to the left of the graph

	public Graph(Gedcom gedcom) {
		this.gedcom = gedcom;
		nodeRows = new ArrayList<>();
		nodes = new HashSet<>();
		lines = new ArrayList<>();
	}

	// Public methods

	/**
	 * If the fulcrum is child in more than one family, you can choose wich family to display.
	 * 
	 * @param num The number of the family (0 if fulcrum has only one family)
	 * @return The diagram, just for methods concatenation
	 */
	public Graph showFamily(int num) {
		whichFamily = num;
		return this;
	}

	public Graph maxAncestors(int num) {
		ancestorGenerations = num;
		return this;
	}

	public Graph maxGreatUncles(int num) {
		greatUnclesGenerations = num;
		return this;
	}
	
	public Graph displaySpouses(boolean display) {
		withSpouses = display;
		return this;
	}
	
	public Graph maxDescendants(int num) {
		descendantGenerations = num;
		return this;
	}
	
	public Graph maxSiblingsNephews(int num) {
		siblingNephewGenerations = num;
		return this;
	}
	
	public Graph maxUnclesCousins(int num) {
		uncleCousinGenerations = num;
		return this;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public List<Line> getLines() {
		return lines;
	}
	
	public IndiCard getFulcrum() {
		return fulcrumNode.getMainCard();
	}

	/**
	 * The diagram fulcrum will be the first person found with the provided ids.
	 * @param ids One or more id to search the person that becomes the diagram fulcrum
	 * @return True if everything is ok, false if any fulcrum is not found
	 */
	public void startFrom(Person fulcrum) {
		
		// Reset all values
		baseGroup = null;
		nodeRows.clear();
		nodes.clear();
		width = 0;
		height = 0;
		
		fulcrumNode = new UnitNode(gedcom, fulcrum, null, null, withSpouses, descendantGenerations == 0); // last 'null' means this is the fulcrum
		nodeRows.add(new ArrayList<Node>());

		// Create all the nodes of the diagram
		List<Family> fulcrumParents = fulcrum.getParentFamilies(gedcom);
		if (!fulcrumParents.isEmpty()) {
			// Creation of parent node
			if (whichFamily >= fulcrumParents.size())
				whichFamily = fulcrumParents.size() - 1; // To prevent IndexOutOfBoundsException
			Family parentFamily = fulcrumParents.get(whichFamily);
			fulcrumNode.getMainCard().parentFamily = parentFamily;
			Node parentNode = null;
			if (ancestorGenerations > 0)
				parentNode = new UnitNode(gedcom, parentFamily, fulcrum, false, withSpouses, false);
			else
				parentNode = new AncestryNode(gedcom, fulcrumNode.getMainCard());
			fulcrumNode.getMainCard().origin = parentNode;
			baseGroup = new Group(parentNode); // In which the fulcrum is child

			fulcrumRow = 0;

			// Ancestors of father
			if ((parentNode instanceof UnitNode && ((UnitNode)parentNode).husband != null)
					|| (parentNode instanceof AncestryNode && ((AncestryNode)parentNode).miniFather != null))
				findAncestors(baseGroup, 1, 1);

			// Step siblings of father
			if(parentNode instanceof UnitNode) {
				stepFamilies(((UnitNode)parentNode).husband, parentFamily, fulcrumRow);
				if (((UnitNode)parentNode).husband != null)
					((UnitNode)parentNode).husband.guardGroup = baseGroup;
			}

			// Fulcrum with marriages and siblings
			for (Person sibling : parentFamily.getChildren(gedcom)) {
				if (sibling.equals(fulcrum)) {
					marriageAndChildren(fulcrum, parentFamily, fulcrumRow);
				} else if (siblingNephewGenerations > 0) {
					UnitNode siblingNode = new UnitNode(gedcom, sibling, parentFamily, fulcrum, withSpouses, siblingNephewGenerations == 1);
					baseGroup.addYoung(siblingNode, false);
					siblingNode.getMainCard().origin = parentNode;
					nodeRows.get(fulcrumRow).add(siblingNode);
					if (siblingNephewGenerations > 1)
						findDescendants(siblingNode, fulcrumRow, siblingNephewGenerations - 1);
				}
			}
			// Step siblings of mother
			if(parentNode instanceof UnitNode) {
				stepFamilies(((UnitNode)parentNode).wife, parentFamily, fulcrumRow);
				if (((UnitNode)parentNode).wife != null)
					((UnitNode)parentNode).wife.guardGroup = baseGroup;
			}
			// Ancestors of mother
			if ((parentNode instanceof UnitNode && ((UnitNode)parentNode).wife != null)
					|| (parentNode instanceof AncestryNode && ((AncestryNode)parentNode).miniMother != null))
				findAncestors(baseGroup, 2, 1);

			// Fulcrum has parent family but without parents
			if (parentFamily.getHusbandRefs().isEmpty() && parentFamily.getWifeRefs().isEmpty()) {
				nodeRows.add(0, new ArrayList<Node>());
				nodeRows.get(0).add(parentNode);
			}	
		} else {
			// Fulcrum without parent family
			fulcrumRow = 0;
			marriageAndChildren(fulcrum, null, 0);
		}

		// All the nodes are stored in a set of nodes
		for (List<Node> row : nodeRows) {
			for (Node node : row) {
				nodes.add(node);
				if (node instanceof UnitNode) {
					// Also the ancestry nodes for acquired spouses or progeny nodes for parents with step families 
					UnitNode unitNode = (UnitNode) node;
					if (unitNode.husband != null) {
						if (unitNode.husband.acquired && unitNode.husband.hasAncestry())
							nodes.add(unitNode.husband.origin);
						else if (unitNode.husband.progeny != null)
							nodes.add(unitNode.husband.progeny);
					}
					if (unitNode.wife != null) {
						if (unitNode.wife.acquired && unitNode.wife.hasAncestry())
							nodes.add(unitNode.wife.origin);
						else if (unitNode.wife.progeny != null)
							nodes.add(unitNode.wife.progeny);
					}
					// Also normal progeny nodes
					if (unitNode.progeny != null)
						nodes.add(unitNode.progeny);
				}
			}
		}
	}

	/**
	 * Find the step siblings of fulcum in other marriages of the parents
	 * @param parent	Parent of whom we are looking the step children
	 * @param family	Parent family of fulcrum to be removed
	 * @param fulcrumRow Number of fulcrum row in nodeRows
	 */
	private void stepFamilies(IndiCard parent, Family family, int fulcrumRow) {
		if (parent != null) {
			List<Family> stepFamilies = parent.person.getSpouseFamilies(gedcom);
			stepFamilies.remove( family );
			for( Family stepFamily : stepFamilies ) {
				if (siblingNephewGenerations > 0) {
					Group stepGroup = new Group(parent);
					for( Person stepSibling : stepFamily.getChildren(gedcom) ) {
						UnitNode stepSiblingNode = new UnitNode(gedcom, stepSibling, stepFamily,
								fulcrumNode.getMainCard().person, withSpouses, siblingNephewGenerations == 1);
						stepSiblingNode.getMainCard().origin = parent;
						nodeRows.get(fulcrumRow).add(stepSiblingNode);
						stepGroup.addYoung(stepSiblingNode, false);
						if (siblingNephewGenerations > 1)
							findDescendants(stepSiblingNode, fulcrumRow, siblingNephewGenerations - 1);
					}
				} else {
					parent.addProgeny(gedcom, stepFamily);
				}
			}
		}
	}
	
	// Fulcrum with one or many marriages and their children
	void marriageAndChildren(Person fulcrum, Family parentFamily, int fulcrumRow) {
		// Multi marriages and children
		List<Family> spouseFamilies = fulcrum.getSpouseFamilies(gedcom);
		if (!spouseFamilies.isEmpty()) {
			for(int i = 0; i < spouseFamilies.size(); i++) {
				Family marriageFamily = spouseFamilies.get(i);
				UnitNode marriageNode;
				// Following marriages of fulcrum represented by an asterisk
				if (i > 0) {
					marriageNode = new UnitNode(gedcom, marriageFamily, fulcrum, true, withSpouses, descendantGenerations == 0);
					marriageNode.getMainCard().parentFamily = parentFamily;
				} else
					marriageNode = fulcrumNode;
				if (baseGroup != null)
					baseGroup.addYoung(marriageNode, false);
				nodeRows.get(fulcrumRow).add(marriageNode);
				if(descendantGenerations > 0 && !marriageFamily.getChildren(gedcom).isEmpty() ) {
					Group spouseGroup = new Group(marriageNode);
					if (nodeRows.size()-1 <= fulcrumRow)
						nodeRows.add(new ArrayList<Node>());
					for (Person child : marriageFamily.getChildren(gedcom)) {
						UnitNode childNode = new UnitNode(gedcom, child, marriageFamily, fulcrum, withSpouses, descendantGenerations == 1);
						spouseGroup.addYoung(childNode, false);
						childNode.getMainCard().origin = marriageNode;
						nodeRows.get(fulcrumRow+1).add(childNode);
						if (descendantGenerations > 1)
							findDescendants(childNode, fulcrumRow+1, descendantGenerations);
					}
				}
			}
		} else { // Fulcrum has no marriages
			if (baseGroup != null)
				baseGroup.addYoung(fulcrumNode, false);
			nodeRows.get(fulcrumRow).add(fulcrumNode);
		}
	}

	/**
	 * Recursive method to put in nodeRows the ancestor families.
	 * 
	 * @param descendantGroup The group in which the commonNode is already the guardian
	 * @param branch          Which branch to investigate in commonNode: 0 none, 1 husband, 2 wife
	 * @param rowNum          Number of the generation of ancestors: 1 for parents, 2 for grand-parents etc.
	 */
	private void findAncestors(Group descendantGroup, int branch, int rowNum) {
		Node guardian = descendantGroup.guardian;
		if (guardian instanceof UnitNode) {
			UnitNode commonNode = (UnitNode) guardian;
			Person ancestor = commonNode.getPerson(branch);
			if (ancestor != null && !ancestor.getParentFamilies(gedcom).isEmpty()) {
				List<Family> parentFamilies = ancestor.getParentFamilies(gedcom);
				Family family = parentFamilies.get(parentFamilies.size()-1);
				commonNode.getCard(branch).parentFamily = family;
				Node parentNode = null;
				if (rowNum < ancestorGenerations)
					parentNode = new UnitNode(gedcom, family, fulcrumNode.getMainCard().person, false, withSpouses, false);
				else
					parentNode = new AncestryNode(gedcom, commonNode.getCard(branch));
				parentNode.branch = branch;
				Group group = new Group(parentNode); // In which commonNode is youth
				commonNode.getCard(branch).origin = parentNode;

				// Possibly add a new row to nodeRows list 
				List<Node> nodeRow;
				if (rowNum > fulcrumRow) {
					nodeRow = new ArrayList<>();
					nodeRows.add(0, nodeRow);
					fulcrumRow++;
				} else
					nodeRow = nodeRows.get(fulcrumRow - rowNum);

				// Mother branch node into nodeRows, avoiding duplicates
				if (branch == 2 && nodeRow.indexOf(commonNode) < 0)
					nodeRow.add(commonNode);
				// Add uncles (with their spouses) to the group
				if (rowNum <= greatUnclesGenerations || uncleCousinGenerations > 0 )
					for (Person uncle : family.getChildren(gedcom)) {
						if (!uncle.equals(ancestor)) {
							UnitNode uncleNode = new UnitNode(gedcom, uncle, family, fulcrumNode.getMainCard().person,
									withSpouses, (rowNum==1 ? uncleCousinGenerations < 2 : true) );
							group.addYoung(uncleNode, false);
							uncleNode.getMainCard().origin = parentNode;
							// Add the uncle node to nodeRows
							nodeRow.add(uncleNode);
							// Add the cousins and possibly their descendants
							if (rowNum == 1 && uncleCousinGenerations > 1)
								findDescendants(uncleNode, fulcrumRow-1, uncleCousinGenerations-2); // -2 because the limit is relative to fulcrumRow
						}
					}
				// Add the common node to the group
				if (branch == 1) {
					group.addYoung(commonNode, false);
					commonNode.husbandGroup = group;
				} else if (branch == 2) {
					group.addYoung(commonNode, true);
					commonNode.wifeGroup = group;
				}

				// Father branch: add the common node to nodeRows
				if (branch == 1)
					nodeRow.add(commonNode);
				
				// Recall this method for the husband and the wife
				rowNum++;
				if ((parentNode instanceof UnitNode && ((UnitNode)parentNode).husband != null)
						|| (parentNode instanceof AncestryNode && ((AncestryNode)parentNode).miniFather != null))
					findAncestors(group, 1, rowNum);
				if ((parentNode instanceof UnitNode && ((UnitNode)parentNode).wife != null)
						|| (parentNode instanceof AncestryNode && ((AncestryNode)parentNode).miniMother != null))
					findAncestors(group, 2, rowNum);
				// parentNode has no husband nor wife
				if (family.getHusbands(gedcom).isEmpty() && family.getWives(gedcom).isEmpty()) {
					findAncestors(group, 0, rowNum);
				}
			} else {
				// Populate the nodeRows list with the parentNode that has no other parent family
				if (fulcrumRow < rowNum) {
					nodeRows.add(0, new ArrayList<Node>());
					fulcrumRow++;
				}
				if (nodeRows.get(fulcrumRow - rowNum).indexOf(commonNode) < 0) // Avoid duplicates
					nodeRows.get(fulcrumRow - rowNum).add(commonNode);
			}
		} // Add the ancestry node to nodeRows list
		else if (guardian instanceof AncestryNode) {
			if (fulcrumRow < rowNum) {
				nodeRows.add(0, new ArrayList<Node>());
				fulcrumRow++;
			}
			if (nodeRows.get(fulcrumRow - rowNum).indexOf(guardian) < 0) // Avoid duplicates
				nodeRows.get(fulcrumRow - rowNum).add(guardian);
		}
	}

	/**
	 * Recoursive method to find the descendants
	 * @param commonNode Node containing the person of whom to find descendants
	 * @param rowNum     Number of the generation of 'commonNode', relative to fulcrumRow:
	 *                   fulcrumRow + 1 are the children, fulcrumRow + 2 are the grandchildren etc.
	 * @param maxGenerations Limit of the number of generations to search
	 */
	private void findDescendants(UnitNode commonNode, int rowNum, int maxGenerations) {
		Person person = commonNode.getMainCard().person;
		List<Family> spouseFamilies = person.getSpouseFamilies(gedcom);
		if (!spouseFamilies.isEmpty()) {
			Family spouseFamily = spouseFamilies.get(spouseFamilies.size() - 1); // Last spouse family of the person
			if (maxGenerations > rowNum - fulcrumRow) {
				rowNum++;
				Group spouseGroup = new Group(commonNode); // In which the person is a parent
				if (!spouseFamily.getChildRefs().isEmpty() && nodeRows.size() <= rowNum)
					nodeRows.add(new ArrayList<Node>()); // Add a row
				for (Person child : spouseFamily.getChildren(gedcom)) {
					UnitNode childNode = new UnitNode(gedcom, child, spouseFamily, fulcrumNode.getMainCard().person,
							withSpouses, rowNum - fulcrumRow == maxGenerations);
					spouseGroup.addYoung(childNode, false);
					childNode.getMainCard().origin = commonNode;
					nodeRows.get(rowNum).add(childNode);
					findDescendants(childNode, rowNum, maxGenerations);
				}
			}
		}
	}

	// Set x and y coordinates for each node
	public void arrange() {

		// Array with max height of each row of nodes
		float[] rowMaxHeight = new float[nodeRows.size()];

		// Let every unit node calculate its own size (width and height) from the size of its cards
		for (List<Node> row : nodeRows) {
			for (Node node : row) {
				if (node instanceof UnitNode)
					((UnitNode)node).calcSize();
				// Meanwhile discover the maximum height of rows
				if (node.height > rowMaxHeight[nodeRows.indexOf(row)])
					rowMaxHeight[nodeRows.indexOf(row)] = node.height;
			}
		}

		// Vertical arrangement of all the nodes
		float posY = rowMaxHeight[0] / 2;
		for (List<Node> row : nodeRows) {
			for (Node node : row) {
				node.y = posY - node.height / 2;
			}
			// Update vertical position for the next row
			if (nodeRows.indexOf(row) < nodeRows.size() - 1)
				posY += rowMaxHeight[nodeRows.indexOf(row)] / 2 + Util.SPACE + rowMaxHeight[nodeRows.indexOf(row) + 1] / 2;
		}

		height = posY + rowMaxHeight[rowMaxHeight.length - 1] / 2;
		negativeHorizontal = 0; // Maximum left overflow of diagram

		// Horizontal arrangement of all the nodes

		// Fulcrum generation
		correctRow(fulcrumRow);
		
		// Arrange the parents/uncles generation, centering horizontaly the parents in between of the children
		if (baseGroup != null) {
			Node guardian = baseGroup.guardian;
			if (guardian != null) {
				guardian.x = baseGroup.centerX(0) - guardian.centerRelX();
				if (guardian.x < negativeHorizontal) negativeHorizontal = guardian.x;
				if (guardian instanceof UnitNode) {
					((UnitNode) guardian).positionCards();
					arrangeHusbandGroup((UnitNode) guardian);
	 				arrangeWifeGroup((UnitNode) guardian);
				}
			}
		}

		// Arrangement of the ancestors from bottom up
		for (int r = fulcrumRow-1; r >= 0; r--) {
			for (Node node : nodeRows.get(r)) {
				if (node instanceof UnitNode) {
					UnitNode unitNode = (UnitNode) node;
					// Position the guardian of the husband group
					Node guardian = unitNode.husbandGroup != null ? unitNode.husbandGroup.guardian : null;
					if (guardian != null) {
						guardian.x = unitNode.husbandGroup.centerX(1) - guardian.centerRelX();
						if (guardian instanceof UnitNode) {
							((UnitNode)guardian).positionCards();
							arrangeHusbandGroup((UnitNode) guardian);
							arrangeWifeGroup((UnitNode) guardian);
						}
					}
					// And the guardian of wife group
					Node guardiana = unitNode.wifeGroup != null ? unitNode.wifeGroup.guardian : null;
					if (guardiana != null) {
						guardiana.x = unitNode.wifeGroup.centerX(2) - guardiana.centerRelX();
						if (guardiana instanceof UnitNode) {
							((UnitNode) guardiana).positionCards();
							arrangeHusbandGroup((UnitNode) guardiana);
							arrangeWifeGroup((UnitNode) guardiana);
						}
					}
				}
			}
			if (r > 0) correctRow(r - 1);
		}

		// Positioning the descendants
		float posX = 0;
		int start = fulcrumRow > 0 ? fulcrumRow - 1 : 0; // Start from parents to position the cousins also
		for(int r = start; r < nodeRows.size()-1; r++) {
			for (Node guardian : nodeRows.get(r)) {
				if (guardian.guardGroup != null) {
					posX = guardian.centerX() - guardian.guardGroup.getYouthWidth() / 2;
					if(guardian.guardGroup.youths.size() > 0)
						posX -= (guardian.guardGroup.getYouth(0).width - guardian.guardGroup.getYouth(0).getMainWidth(true));
					for (UnitNode youth : guardian.guardGroup.youths) {
						youth.x = posX;
						posX += youth.width + Util.PADDING;
					}
				}
			}
			correctRow(r+1);
		}

		// Positioning the progeny nodes and acquired ancestry nodes
		boolean plusGap = false;
		for (Node node : nodes) {
			if(node instanceof UnitNode) {
				UnitNode unitNode = (UnitNode) node;
				unitNode.positionCards();
				ProgenyNode progeny = unitNode.progeny;
				if (progeny != null) {
					progeny.x = node.centerX() - progeny.width/2;
					progeny.y = node.y + node.height + Util.GAP;
					progeny.positionCards();
					if (progeny.x < negativeHorizontal) negativeHorizontal = progeny.x;
					plusGap = true;
				}
				arrangeAncestry(unitNode.husband);
				arrangeAncestry(unitNode.wife);
			}
			// Step parents progeny
			if (node instanceof ProgenyNode) {
				ProgenyNode progeny = (ProgenyNode) node;
				if (progeny.origin instanceof IndiCard) {
					IndiCard indiCard = (IndiCard) progeny.origin;
					progeny.x = indiCard.centerX() - progeny.width/2;
					progeny.y = indiCard.y + indiCard.height + Util.GAP;
					progeny.positionCards();
					if (progeny.x < negativeHorizontal) negativeHorizontal = progeny.x;
					plusGap = true;
				}
			}
		}
		height += plusGap ? Util.GAP : 0;
		
		// Horizontal shift to the right of all the nodes
		for (Node node : nodes) {
			node.x -= negativeHorizontal;
			if (node instanceof UnitNode)
				((UnitNode) node).positionCards();
			else if (node instanceof ProgenyNode)
				((ProgenyNode) node).positionCards();
			// Total width
			if (node.x + node.width > width) width = node.x + node.width;
		}
		
		// Create the Lines
		lines.clear();
		for (Node node : nodes) {
			if (node instanceof UnitNode) {
				UnitNode unitNode = (UnitNode) node;
				if (unitNode.husband != null && !unitNode.husband.acquired && unitNode.husband.origin != null)
					lines.add(new Line(unitNode.husband));
				if (unitNode.wife != null && !unitNode.wife.acquired && unitNode.wife.origin != null)
					lines.add(new Line(unitNode.wife));
			} else if (node instanceof ProgenyNode) {
				for( MiniCard miniCard : ((ProgenyNode) node).miniChildren )
					lines.add(new Line(miniCard));
			}
		}
		
		// Order lines from left to right
		Collections.sort(lines, new Comparator<Line>() {
			@Override
			public int compare(Line line1, Line line2) {
				return line1.compareTo(line2);
			}
		});
	}
	
	// Position the little ancestry above the card
	void arrangeAncestry(IndiCard indiCard) {
		if (indiCard != null && indiCard.acquired && indiCard.hasAncestry()) {
			indiCard.origin.x = indiCard.centerX() - indiCard.origin.centerRelX();
			indiCard.origin.y = indiCard.y - indiCard.origin.height;
		}
	}

	// Arrange horizontally the youths of the group in which the husband is child
	private void arrangeHusbandGroup(UnitNode commonNode) {
		Group group = commonNode.husbandGroup;
		if (group != null) {
			if (group.youths.size() > 1) {
				float posX = commonNode.x;
				for (int i = group.youths.size() - 2; i >= 0; i--) {
					UnitNode uncleNode = group.getYouth(i);
					posX -= (Util.PADDING + uncleNode.width);
					uncleNode.x = posX;
					uncleNode.positionCards();
					if (posX < negativeHorizontal) negativeHorizontal = posX;
				}
			}
		}
	}

	// And of the group in which the wife is child
	private void arrangeWifeGroup(UnitNode commonNode) {
		Group group = commonNode.wifeGroup;
		if (group != null) {
			if (group.youths.size() > 1) {
				float posX = commonNode.x + commonNode.width + Util.PADDING;
				for (int i = 1; i < group.youths.size(); i++) {
					UnitNode uncleNode = group.getYouth(i);
					uncleNode.x = posX;
					posX += uncleNode.width + Util.PADDING;
					uncleNode.positionCards();
				}
			}
		}
	}

	// Horizontal correction of overlapping nodes in one row
	private void correctRow(int rowNum) {
		List<Node> row = nodeRows.get(rowNum);
		int center = row.size() / 2; // If odd take the central one, if even take the first after the half (N / 2 + 1)
		Node centerNode = row.get(center);
		if (centerNode.x < negativeHorizontal) negativeHorizontal = centerNode.x; // Single node overflowing on the left

		if (row.size() > 1) {
			float posX = centerNode.x + centerNode.width + Util.PADDING;
			int startRight = center; // Index of the first node to move
			
			if ((row.size() & 1) == 0) { // even (at least 2)
				Node previousLeft = row.get(center-1);
				float overlap = previousLeft.x + previousLeft.width - centerNode.x;
				posX = centerNode.x + overlap / 2;
			} else if (row.size() > 2) { // odd (at least 3)
				startRight = center + 1;
			}
			// Nodes at right shift to right
			for (int i = startRight; i < row.size(); i++) {
				Node node = row.get(i);
				if (node.x < posX) {
					node.x = posX;
				}
				posX = node.x + node.width + Util.PADDING;
				if(node instanceof UnitNode) ((UnitNode) node).positionCards();
				if (node.x < negativeHorizontal) negativeHorizontal = node.x;
			}
			// Nodes at the left of center shift to left
			posX = centerNode.x;
			for (int i = center-1; i >= 0; i--) {
				Node node = row.get(i);
				if (node.x + node.width + Util.PADDING > posX) {
					node.x = posX - node.width - Util.PADDING;
				}
				posX = node.x;
				if(node instanceof UnitNode) ((UnitNode) node).positionCards();
				if (node.x < negativeHorizontal) negativeHorizontal = node.x;
			}
		}
	}

	// Nodes to string
	public String toString() {
		String str = "";
		for (int n = 0; n < nodeRows.size(); n++) {
			List<Node> row = nodeRows.get(n);
			str += n == fulcrumRow ? "["+n+"]" : n;
			str += " - - - - - - - - - - - - - - - - - -\n";
			for (Node node : row) {
				if (node == null)
					str += "null";
				else
					str += node;
				str += "\n";
			}
		}
		str += "- - - - - - - - - - - - - - - - - - -\n";
		return str;
	}
}

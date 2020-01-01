package graph.gedcom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import graph.gedcom.AncestryNode.Ancestor;
import static graph.gedcom.Util.pr;

public class Graph {

	public int width, height;
	private Gedcom gedcom;
	private int whichFamily; // Which family display if the fulcrum is child in more than one family
	private int ancestorGenerations; // Generations to display
	private List<List<Node>> nodeRows; // A list of lists of all the nodes
	private Set<Node> nodes;
	private Set<Card> cards;
	private Set<Ancestor> ancestors;
	private Set<Line> lines;
	CardNode fulcrumNode;
	Group baseGroup; // In which fulcrum is one of the children (youth)

	public Graph(Gedcom gedcom) {
		this.gedcom = gedcom;
		nodeRows = new ArrayList<>();
		nodes = new HashSet<>();
		cards = new HashSet<>();
		ancestors = new HashSet<>();
		lines = new HashSet<>();
		// default values
		ancestorGenerations = 2;
	}

	// Options

	/**
	 * If the fulcrum is child in more than one family, you can choose wich family
	 * to display.
	 * 
	 * @param num The number of the family (0 if fulcrum has only one family)
	 * @return The Diagram, just for methods concatenation
	 */
	public Graph showFamily(int num) {
		whichFamily = num;
		return this;
	}

	public Graph maxAncestors(int num) {
		ancestorGenerations = num;
		return this;
	}

	/**
	 * Start the diagram of the first person it can find with the provided ids.
	 * @param ids One or more id to search the person that become the diagram fulcrum
	 * @return True if everything is ok, false if doesn't find a fulcrum
	 */
	public boolean startFrom(String... ids) {
		
		// Reset all values
		baseGroup = null;
		nodeRows.clear();
		nodes.clear();
		cards.clear();
		ancestors.clear();
		lines.clear();
		width = 0;
		height = 0;
		
		Person fulcrum = null;
		for (String id : ids) {
			fulcrum = gedcom.getPerson(id);
			if (fulcrum != null)
				break;
		}
		if (fulcrum == null)
			return false;
		fulcrumNode = new SpouseNode(gedcom, fulcrum);
		nodeRows.add(new ArrayList<Node>());

		// Create nodes for ancestors
		if (!fulcrum.getParentFamilies(gedcom).isEmpty()) {
			Family parentFamily = fulcrum.getParentFamilies(gedcom).get(whichFamily);
			baseGroup = new Group(); // In which the fulcrum is child
			CardNode parentNode = null;
			if (ancestorGenerations > 0)
				parentNode = new ParentNode(gedcom, parentFamily);
			fulcrumNode.getMainCard().setOrigin(parentNode);
			baseGroup.setGuardian(parentNode);
			parentNode.guardGroup = baseGroup;
			for (Person sibling : parentFamily.getChildren(gedcom)) {
				if (sibling.equals(fulcrum)) {
					baseGroup.addYoung(fulcrumNode, false);
					nodeRows.get(0).add(fulcrumNode);
				} else {
					CardNode siblingNode = new SpouseNode(gedcom, sibling);
					baseGroup.addYoung(siblingNode, false);
					siblingNode.getMainCard().setOrigin(parentNode);
					nodeRows.get(0).add(siblingNode);
				}
			}
			// Recoursive call
			if (!parentFamily.getHusbands(gedcom).isEmpty())
				findAncestors(baseGroup, 1, 1);
			if (!parentFamily.getWives(gedcom).isEmpty())
				findAncestors(baseGroup, 2, 1);
			// Fulcrum has parent family but without parents
			if (parentFamily.getHusbands(gedcom).isEmpty() && parentFamily.getWives(gedcom).isEmpty()) {
				nodeRows.add(0, new ArrayList<Node>());
				nodeRows.get(0).add(parentNode);
			}
		} else {
			// Fulcrum without parent family
			nodeRows.get(0).add(fulcrumNode);
		}

		// Create nodes for descendants
		if (!fulcrum.getSpouseFamilies(gedcom).isEmpty()) {
			Family spouseFamily = fulcrum.getSpouseFamilies(gedcom).get(0); // TODO loop for multiple marriages
			Group spouseGroup = new Group(); // In which the fulcrum is the parent
			spouseGroup.setGuardian(fulcrumNode);
			fulcrumNode.guardGroup = spouseGroup;
			int rowNum = nodeRows.size();
			nodeRows.add(new ArrayList<Node>());
			for (Person child : spouseFamily.getChildren(gedcom)) {
				CardNode childNode = new SpouseNode(gedcom, child);
				spouseGroup.addYoung(childNode, false);
				childNode.getMainCard().setOrigin(fulcrumNode);
				nodeRows.get(rowNum).add(childNode);
				findDescendants(childNode, rowNum);
			}
		}

		// All the nodes are stored in a set of nodes
		// All the cards are stored in a set of cards
		// All the ancestors are stored in a set of ancestors
		for (List<Node> row : nodeRows) {
			for (Node n : row) {
				nodes.add(n);
				if (n instanceof CardNode) {
					CardNode node = (CardNode) n;
					if (node.husband != null) {
						cards.add(node.husband);
						if (node.husband.ancestryNode != null)
							addAncestry(node.husband.ancestryNode);
					}
					if (node.wife != null) {
						cards.add(node.wife);
						if (node.wife.ancestryNode != null)
							addAncestry(node.wife.ancestryNode);
					}
				} else if (n instanceof AncestryNode) {
					addAncestry((AncestryNode) n);
				}
			}
		}
		return true;
	}

	private void addAncestry(AncestryNode node) {
		if (node.foreFather != null)
			ancestors.add(node.foreFather);
		if (node.foreMother != null)
			ancestors.add(node.foreMother);
	}

	/**
	 * Succesive calls to redraw the graph.
	 * 
	 * @param id Id of the person
	 */
	@Deprecated
	public void restartFrom(String id) {
		baseGroup = null;
		nodeRows.clear();
		nodes.clear();
		cards.clear();
		ancestors.clear();
		lines.clear();
		startFrom(id);
	}

	public String getStartId() {
		return fulcrumNode.getMainCard().getPerson().getId();
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public Set<Card> getCards() {
		return cards;
	}

	public Set<Ancestor> getAncestors() {
		return ancestors;
	}

	public Set<Line> getLines() {
		return lines;
	}

	/**
	 * Recursive method to put in nodeRows the ancestor families.
	 * 
	 * @param descendantGroup The group in which the {@link #Node} commonNode is
	 *                        already the guardian
	 * @param branch          Which branch to investigate in commonNode: 1 husband,
	 *                        2 wife
	 * @param rowNum          Number of the generation of ancestors
	 */
	private void findAncestors(Group descendantGroup, int branch, int rowNum) {
		rowNum++;
		Node guardian = descendantGroup.getGuardian();
		if (guardian instanceof CardNode) {
			CardNode commonNode = (CardNode) guardian;
			Person person = commonNode.getPerson(branch);
			if (person != null && !person.getParentFamilies(gedcom).isEmpty()) {
				Family family = person.getParentFamilies(gedcom).get(0);
				Group group = new Group(); // In which commonNode is youth
				Node parentNode = null;
				if (rowNum <= ancestorGenerations)
					parentNode = new ParentNode(gedcom, family);
				else
					parentNode = new AncestryNode(gedcom, commonNode.getCard(branch));
				if (parentNode != null) {
					group.setGuardian(parentNode);
					commonNode.getCard(branch).setOrigin(parentNode);
					parentNode.guardGroup = group;
				}
				// Eventually add a new row to the list nodeRows

				List<Node> nodeRow;
				if (nodeRows.size() < rowNum) {
					nodeRow = new ArrayList<>();
					nodeRows.add(0, nodeRow);
				} else
					nodeRow = nodeRows.get(nodeRows.size() - rowNum);

				// Mother branch node into nodeRows, avoiding duplicates
				if (branch == 2 && nodeRow.indexOf(commonNode) < 0)
					nodeRow.add(commonNode);
				// Add brothers and sisters (with their spouses) to the group
				// TODO if (rowNum < uncleGenerations)
				for (Person sibling : family.getChildren(gedcom)) {
					if (!sibling.equals(person)) {
						CardNode siblingNode = new SpouseNode(gedcom, sibling);
						group.addYoung(siblingNode, false);
						siblingNode.getMainCard().setOrigin(parentNode);
						// Add the sibling node to nodeRows
						nodeRow.add(siblingNode);
					}
				}
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
				if (!family.getHusbands(gedcom).isEmpty())
					findAncestors(group, 1, rowNum);
				if (!family.getWives(gedcom).isEmpty())
					findAncestors(group, 2, rowNum);
				// parentNode has no husband nor wife
				if (family.getHusbands(gedcom).isEmpty() && family.getWives(gedcom).isEmpty()) {
					findAncestors(group, 0, rowNum);
				}
			} else {
				// Populate the nodeRows list with the parentNode that has no other parent
				// family
				if (nodeRows.size() < rowNum)
					nodeRows.add(0, new ArrayList<Node>());
				// Avoid duplicates
				if (nodeRows.get(nodeRows.size() - rowNum).indexOf(commonNode) < 0)
					nodeRows.get(nodeRows.size() - rowNum).add(commonNode);
			}
		} // Add the ancestry node to nodeRows list
		else if (guardian instanceof AncestryNode) {
			if (nodeRows.size() < rowNum)
				nodeRows.add(0, new ArrayList<Node>());
			nodeRows.get(nodeRows.size() - rowNum).add(guardian);
		}
	}

	// Recoursive method to find the descendants
	private void findDescendants(CardNode commonNode, int rowNum) {
		rowNum++;
		Person person = commonNode.getMainCard().getPerson();
		if (!person.getSpouseFamilies(gedcom).isEmpty()) {
			Family spouseFamily = person.getSpouseFamilies(gedcom).get(0);
			Group spouseGroup = new Group(); // In which the person is a parent
			spouseGroup.setGuardian(commonNode);
			commonNode.guardGroup = spouseGroup;
			if (nodeRows.size() <= rowNum)
				nodeRows.add(new ArrayList<Node>());
			for (Person child : spouseFamily.getChildren(gedcom)) {
				CardNode childNode = new SpouseNode(gedcom, child);
				spouseGroup.addYoung(childNode, false);
				childNode.getMainCard().setOrigin(commonNode);
				nodeRows.get(rowNum).add(childNode);
				findDescendants(childNode, rowNum);
			}
		}
	}

	/**
	 * Set x and y coordinates for each {@link #Card}.
	 */
	public void arrange() {

		// Array with max height of each row of nodes
		int[] rowMaxHeight = new int[nodeRows.size()];
		// Max shift to the left of the graph
		int negativeHorizontal = 0;

		// Let every node calculate its own size (width and height) from the size of its cards
		for (List<Node> row : nodeRows) {
			for (Node node : row) {
				node.calcSize(); // TODO rimasto solo per CardNode
				// Meanwhile discover the maximum height of rows
				if (node.height > rowMaxHeight[nodeRows.indexOf(row)])
					rowMaxHeight[nodeRows.indexOf(row)] = node.height;
			}
		}

		// Vertical arrangement of all the nodes
		int posY = rowMaxHeight[0] / 2;
		for (List<Node> row : nodeRows) {
			for (Node node : row) {
				node.y = posY - node.height / 2;
			}
			// Update vertical position for the next row
			if (nodeRows.indexOf(row) < nodeRows.size() - 1)
				posY += rowMaxHeight[nodeRows.indexOf(row)] / 2 + Util.SPACE
						+ rowMaxHeight[nodeRows.indexOf(row) + 1] / 2;
		}
		height = posY + rowMaxHeight[rowMaxHeight.length - 1] / 2;

		// Horizontal arrangement of all the nodes
		// Bottom-up starting from fulcrum as youth
		int posX = 0;
		if (baseGroup != null) {
			for (CardNode youth : baseGroup.getYouths()) {
				youth.x = posX;
				posX += youth.width + Util.PADDING;
				setCardCoordinates(youth);
			}
			// Center horizontaly the parents in between of the children
			Node guardian = baseGroup.getGuardian();
			if (guardian != null) {
				guardian.x = baseGroup.getYouth(0).getMainCard().centerX() + baseGroup.getArcWidth(0) / 2 - guardian.centerX();
				if (guardian instanceof CardNode) {
					setCardCoordinates((CardNode) guardian);
					// Call recoursive methods for ancestors
					arrangeHusbandGroup((CardNode) guardian);
					arrangeWifeGroup((CardNode) guardian);
				}
			}
		} else { // Fulcrum hasn't a parent family
			setCardCoordinates(fulcrumNode);
		}

		// Top-down placement of descendants starting from fulcrum
		arrangeYouths(fulcrumNode);

		// Horizontal correction of overlapping nodes
		for (List<Node> row : nodeRows) {
			posX = 0;
			Node center = null;
			if (!row.isEmpty())
				posX = row.get(0).x;
			// Shift half of the nodes to the right
			for (Node node : row) {
				if (posX > fulcrumNode.centerX()) {
					if (center == null)
						center = node;
					if (node.x < posX) {
						node.x = posX;
						setCardCoordinates(node);
					}
				}
				posX = node.x + node.width + Util.PADDING;
				if (node.x < negativeHorizontal)
					negativeHorizontal = node.x;
				if (posX > width)
					width = posX;
			}
			// Shift half of the nodes to the left
			if (center != null)
				posX = center.x + center.width + Util.PADDING;
			for (int i = row.indexOf(center); i >= 0; i--) {
				if (row.get(i).x + row.get(i).width + Util.PADDING > posX) {
					row.get(i).x = posX - row.get(i).width - Util.PADDING;
					setCardCoordinates(row.get(i));
				}
				posX = row.get(i).x;
				if (row.get(i).x < negativeHorizontal)
					negativeHorizontal = row.get(i).x;
				if (posX > width)
					width = posX;
			}
		}
		width -= negativeHorizontal + Util.PADDING;

		// Horizontal shift to the right of all the nodes
		for (Node node : nodes) {
			node.x -= negativeHorizontal;
			setCardCoordinates(node);
		}
		
		// Create the Lines
		for (Card card : cards) {
			lines.add(new Line(card));
		}
	}

	/**
	 * Set the absolute coordinates of the cards of a node, deducing from the
	 * absolute coordinates of this node.
	 * 
	 * @param node The node to consider
	 */
	private void setCardCoordinates(Node n) {
		if (n instanceof CardNode) {
			CardNode node = (CardNode) n;
			node.positionChildren();
			// Position of ancestry node and ancestry cards above acquired relative
			Card spouse = node.getSpouseCard();
			if (spouse != null && spouse.ancestryNode != null) {
				spouse.ancestryNode.x = spouse.centerX() - spouse.ancestryNode.centerXrel();
				spouse.ancestryNode.y = spouse.y - spouse.ancestryNode.height - 15;
				spouse.ancestryNode.positionChildren();
			}
		} // Ancestry node on top of the graph
		else if (n instanceof AncestryNode) {
			((AncestryNode) n).positionChildren();
		}
	}

	// Recoursive methods to arrange horizontally youths and guardian of a group
	// The group in which the husband is child
	private void arrangeHusbandGroup(CardNode commonNode) {
		Group group = commonNode.husbandGroup;
		if (group != null) {
			if (group.getYouths().size() > 1) {
				int moveX = commonNode.x;
				for (int i = group.getYouths().size() - 2; i >= 0; i--) {
					CardNode uncle = group.getYouth(i);
					moveX -= (Util.PADDING + uncle.width);
					uncle.x = moveX;
					setCardCoordinates(uncle);
				}
			}
			arrangeGuardian(group, 1);
		}
	}

	// The group in which the wife is child
	private void arrangeWifeGroup(CardNode commonNode) {
		Group group = commonNode.wifeGroup;
		if (group != null) {
			if (group.getYouths().size() > 1) {
				int moveX = commonNode.x + commonNode.width + Util.PADDING;
				for (int i = 1; i < group.getYouths().size(); i++) {
					CardNode uncle = group.getYouth(i);
					uncle.x = moveX;
					moveX += uncle.width + Util.PADDING;
					setCardCoordinates(uncle);
				}
			}
			arrangeGuardian(group, 2);
		}
	}

	/**
	 * Common conclusion for above 2 methods.
	 * 
	 * @param group  The group of which we want to arrange horizontally the guardian
	 * @param branch 1 if the child is a husband, 2 if the child is a wife
	 */
	private void arrangeGuardian(Group group, int branch) {
		Node guardian = group.getGuardian();
		if (guardian != null) {
			if (group.getYouths().size() > 1) {
				if (branch == 1) // Husband branch
					guardian.x = group.getYouth(0).getMainCard().centerX() + group.getArcWidth(branch) / 2 - guardian.centerX();
				else if (branch == 2) // Wife branch
					guardian.x = group.getYouth(0).getCard(2).centerX() + group.getArcWidth(branch) / 2 - guardian.centerX();
			} else // Only one child
				guardian.x = group.getYouth(0).getCard(branch).centerX() - guardian.centerX();
			if (guardian instanceof CardNode) {
				setCardCoordinates((CardNode) guardian);
				arrangeHusbandGroup((CardNode) guardian);
				arrangeWifeGroup((CardNode) guardian);
			}
		}
	}

	// Recoursive method for horizontal position of the descendants
	private void arrangeYouths(CardNode guardian) {
		if (guardian.guardGroup != null) {
			int moveX = guardian.centerX() - guardian.guardGroup.getYouthWidth() / 2; // TODO non no non ono
			for (CardNode youth : guardian.guardGroup.getYouths()) {
				youth.x = moveX;
				moveX += youth.width + Util.PADDING;
				setCardCoordinates(youth);
				arrangeYouths(youth);
			}
		}
	}

	// Nodes to string
	public String toString() {
		String str = "";
		for (List<Node> row : nodeRows) {
			for (Node node : row) {
				if (node == null)
					str += "null";
				else
					str += node;
				str += "\n";
			}
			str += "- - - - - - - - - - - - - - - - - -\n";
		}
		return str;
	}
}

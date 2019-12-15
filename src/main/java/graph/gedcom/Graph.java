package graph.gedcom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import static graph.gedcom.Util.print;

public class Graph {

	private Gedcom gedcom;
	private int whichFamily; // Which family display if the fulcrum is child in more than one family
	private int ancestorGenerations;	// Generations to display
	private List<List<Group>> groupRows; //
	private List<Set<Node>> nodeRows; // A list of lists of all the nodes
	private Set<Node> nodes;
	private Set<Card> cards;
	private Set<Line> lines;
	private Class<? extends Card> genericCard;
	Node fulcrumNode;

	public Graph(Gedcom gedcom) {
		this.gedcom = gedcom;
		groupRows = new ArrayList<>();
		nodeRows = new ArrayList<>();
		nodes = new HashSet<>();
		cards = new HashSet<Card>();
		lines = new HashSet<>();
		
		ancestorGenerations = 3;
	}

	// Options

	/**
	 * If the fulcrum is child in more than one family, you can choose wich family
	 * display.
	 * 
	 * @param num The number of the family (0 if fulcrum has only one family)
	 * @return The Diagram, just for methods concatenation
	 */
	public Graph showFamily(int num) {
		whichFamily = num;
		return this;
	}

	public Graph setCardClass(Class<? extends Card> clazz) {
		this.genericCard = clazz;
		return this;
	}
	
	public Graph maxAncestors(int num) {
		ancestorGenerations = num;
		return this;
	}

	public void startFrom(String id) {
		Person fulcrum = gedcom.getPerson(id);
		if (fulcrum != null ) {
			fulcrumNode = spouseNode(fulcrum);
			nodeRows.add(new LinkedHashSet<Node>());
			nodeRows.get(0).add(fulcrumNode);
			// Create nodes for ancestors
			if(!fulcrum.getParentFamilies(gedcom).isEmpty()) {
				Family parentFamily = fulcrum.getParentFamilies(gedcom).get(whichFamily);
				Group baseGroup = new Group(); // In which the fulcrum is child
				Node parentNode = null;
				if(ancestorGenerations > 0)
					parentNode = parentNode(parentFamily);
				fulcrumNode.getMainCard().setOrigin(parentNode);
				baseGroup.setGuardian(parentNode);
				for(Person sibling : parentFamily.getChildren(gedcom)) {
					if(sibling.equals(fulcrum)) {
						baseGroup.addYoung(fulcrumNode, false);
						nodeRows.get(0).add(fulcrumNode);
					} else {
						Node siblingNode = spouseNode(sibling);
						baseGroup.addYoung(siblingNode, false);
						siblingNode.getMainCard().setOrigin(parentNode);
						nodeRows.get(0).add(siblingNode);
					}
				}
				groupRows.add(new ArrayList<Group>());
				groupRows.get(0).add(baseGroup);
				
				if(parentNode != null) {
					if(parentNode.isCouple()) {
						findAncestors(baseGroup, 1, 1);
						findAncestors(baseGroup, 2, 1);
					} else if(parentNode.isSingle()) {
						findAncestors(baseGroup, 0, 1);
					}
				}
			}
			// Create nodes for descendants
			if(!fulcrum.getSpouseFamilies(gedcom).isEmpty()) {
				Family spouseFamily = fulcrum.getSpouseFamilies(gedcom).get(0); // TODO for multiple marriages
				Group spouseGroup = new Group(); // In which the fulcrum is a parent
				fulcrumNode.guardGroup = spouseGroup;
				spouseGroup.setGuardian(fulcrumNode);
				nodeRows.add(new LinkedHashSet<Node>());
				for(Person child : spouseFamily.getChildren(gedcom)) {
					Node childNode = spouseNode(child);
					spouseGroup.addYoung(childNode, false);
					childNode.getMainCard().setOrigin(fulcrumNode);
					nodeRows.get(nodeRows.size()-1).add(childNode);
				}
				groupRows.add(new ArrayList<Group>());
				groupRows.get(groupRows.size()-1).add(spouseGroup);
			}
		}

		// All the nodes are stored in a set of nodes
		// All the cards are stored in a set of cards
		for (List<Group> row : groupRows) {
			for (Group group : row) {
				Node guardian = group.getGuardian();
				if (guardian != null) {
					nodes.add(guardian);
					addToCards(guardian);
				}
				for (Node node : group.getYouths()) {
					nodes.add(node);
					addToCards(node);
				}
			}
		}
		// The same with nodeRows
		/*for (Set<Node> row : nodeRows) {
			for (Node node : row) {
				nodes.add(node);
				addToCards(node);
			}
		}*/
	}

	/**
	 * Succesive calls to redraw the graph.
	 * @param id Id of the person
	 */
	public void restartFrom(String id) {
		groupRows.clear();
		nodeRows.clear();
		nodes.clear();
		cards.clear();
		lines.clear();
		startFrom(id);
	}
	
	public String getStartId() {
		return fulcrumNode.getMainCard().getPerson().getId();
	}
	
	private void addToCards(Node node) {
		if (node.isCouple()) {
			cards.add(((Couple) node).husband);
			cards.add(((Couple) node).wife);
		} else if (node.isSingle()) {
			cards.add(((Single) node).one);
		}
	}

	// public List<List<Group>> getGroups() { return groupRows; }

	public Set<Node> getNodes() {
		return nodes;
	}

	public Set<? extends Card> getCards() {
		return cards;
	}

	public Set<Line> getLines() {
		return lines;
	}

	/**
	 * Put the parent(s) of a family into a Node.
	 * 
	 * @param family
	 * @return The new Node created, null if there isn't any parent
	 */
	private Node parentNode(Family family) {
		Node node = null;
		Person husband = !family.getHusbandRefs().isEmpty() ? family.getHusbands(gedcom).get(0) : null;
		Person wife = !family.getWifeRefs().isEmpty() ? family.getWives(gedcom).get(0) : null;
		if (husband != null && wife != null)
			node =  new Couple(genericCard, husband, wife, family, 3);
		else if (husband != null)
			node =  new Single(genericCard, husband);
		else if (wife != null)
			node =  new Single(genericCard, wife);
		// Check if there is already an identical node...
		// NO, there must be 2 or more different nodes that contain the same people!
		//for (List<Node> row : nodeRows)
		//	for (Node n : row )
		//		if( node!= null && node.identical(n) )
		//			return n;
		return node;
	}

	/**
	 * Creates a {@link #Node} containing a single person or a couple with the person and
	 * his/her spouse.
	 * 
	 * @param person The starting one
	 * @return a Node
	 */
	private Node spouseNode(Person person) { // , Node nodoSopra, boolean conDiscendenti
		Node node;
		if (!person.getSpouseFamilies(gedcom).isEmpty()) {
			Family family = person.getSpouseFamilies(gedcom).get(0);
			if (Util.sex(person) == 1 && !family.getWives(gedcom).isEmpty()) // Maschio ammogliato
				node = new Couple(genericCard, person, family.getWives(gedcom).get(0), family, 1);
			else if (Util.sex(person) == 2 && !family.getHusbands(gedcom).isEmpty()) // Femmina ammogliata
				node = new Couple(genericCard, family.getHusbands(gedcom).get(0), person, family, 2);
			else
				node = new Single(genericCard, person); // senza sesso (o senza coniuge?)
		} else
			node = new Single(genericCard, person);
		return node;
	}

	/**
	 * Recursive method to put in {@link #groupRows} the ancestor families.
	 * 
	 * @param descendantGroup    The group in which the {@link #Node} commonNode is already the guardian with a couple or a single inside
	 * @param branch	Which branch to investigate in commonNode: 0 single, 1 husband, 2 wife
	 * @param rowNum     Number of the generation of ancestors
	 */
	private void findAncestors(Group descendantGroup, int branch, int rowNum) {
		Node commonNode = descendantGroup.getGuardian();
		if( commonNode != null ) {
			Person person = commonNode.getPerson(branch);
			if (person != null && !person.getParentFamilies(gedcom).isEmpty()) {
				Family family = person.getParentFamilies(gedcom).get(0);
				Group group = new Group();
				descendantGroup.preceding = group; // <---- questo è SBAGLIATO! un group può averne anche 2 precedenti
				Node parentNode = null;
				if(rowNum < ancestorGenerations)
					parentNode = parentNode(family);
				group.setGuardian(parentNode);
	
				if (commonNode.getCard(branch) != null)
					commonNode.getCard(branch).setOrigin(parentNode);
				
				if (branch == 1)
					commonNode.husbandGroup = group;
				else if (branch == 2)
					commonNode.wifeGroup = group;
				
				// Eventually add a new row to the list nodeRows
				rowNum++;
				Set<Node> nodeRow;
				if (nodeRows.size() < rowNum) {
					nodeRow = new LinkedHashSet<>();
					nodeRows.add(0, nodeRow);
				} else
					nodeRow = nodeRows.get(nodeRows.size() - rowNum);
				
				// Mother branch node into nodeRows
				if (branch == 2)
			 		nodeRow.add(commonNode);
				// Add brothers and sisters (with their spouses) to the group
				 for (Person sibling : family.getChildren(gedcom)) {
					if (!sibling.equals(person)) {
						Node siblingNode = spouseNode(sibling);
					 	group.addYoung(siblingNode, false);
					 	siblingNode.getMainCard().setOrigin(parentNode);
					 	
					 	// Add the sibling node to nodeRows
				 		nodeRow.add(siblingNode);
					}
				 }
				if (branch == 1)
					group.addYoung(commonNode, false);
				else if (branch == 2)
					group.addYoung(commonNode, true);
	
			 	// Father branch: add the common node to nodeRows
			 	if (branch == 1)
			 		nodeRow.add(commonNode);
				
				// Eventually add a new row to the list of groups
				if (groupRows.size() < rowNum) {
					groupRows.add(0, new ArrayList<Group>());
				}
				groupRows.get(groupRows.size() - rowNum).add(group);
	
				// Recall this method for the husband and the wife
				if (!family.getHusbands(gedcom).isEmpty()) {
					findAncestors(group, 1, rowNum);
				}
				if (!family.getWives(gedcom).isEmpty()) {
					findAncestors(group, 2, rowNum);
				}
			} else {
				// Populate the nodeRows list with the parentNode that has no other parent family
				rowNum++;
				if (nodeRows.size() < rowNum) {
					nodeRows.add(0, new LinkedHashSet<Node>());
				}
				nodeRows.get(nodeRows.size() - rowNum).add(commonNode);
			}
		}
	}

	/* TODO
	private void findDescendants(Node commonNode, Group parentGroup, int rowNum) {
		if(!fulcrum.getSpouseFamilies(gedcom).isEmpty()) {
			Family spouseFamily = fulcrum.getSpouseFamilies(gedcom).get(0); // TODO for multiple marriages
			Group spouseGroup = new Group(); // In which the fulcrum is a parent
			fulcrumNode.guardGroup = spouseGroup;
			spouseGroup.setGuardian(fulcrumNode);
			nodeRows.add(new LinkedHashSet<Node>());
			for(Person child : spouseFamily.getChildren(gedcom)) {
				Node childNode = spouseNode(child);
				spouseGroup.addYoung(childNode, false);
				childNode.getMainCard().setOrigin(fulcrumNode);
				nodeRows.get(nodeRows.size()-1).add(childNode);
			}
			groupRows.add(new ArrayList<Group>());
			groupRows.get(groupRows.size()-1).add(spouseGroup);
		}
	}*/
	
	/**
	 * Set x and y coordinates for each {@link #Card}.
	 */
	public void arrange() {

		// Array with max height of each row of nodes
		int[] rowMaxHeight = new int[nodeRows.size()];
		
		// Deduce measures of each node from measures of its cards
		for (Set<Node> row : nodeRows) {
			for (Node node : row ) {
				if (node.isCouple() && ((Couple) node).husband != null && ((Couple) node).wife != null) {
					node.width = ((Couple) node).husband.width + Util.MARGIN + ((Couple) node).wife.width;
					node.height = maxHeight(((Couple) node).getTwo()); // max height between the two
				} else if (node.isSingle() && ((Single) node).one != null) {
					node.width = ((Single) node).one.width;
					node.height = ((Single) node).one.height;
				}
				if( node.height > rowMaxHeight[nodeRows.indexOf(row)] )
					rowMaxHeight[nodeRows.indexOf(row)] = node.height;
			}
			
		}
		
		// Vertical arrangement of all the nodes
		int moveY = Util.SPACE/2 + rowMaxHeight[0]/2;
		for (Set<Node> row : nodeRows) {
			for (Node node : row ) {
				node.y = moveY - node.height / 2;
			}
			// Update vertical shift for the next row
			if( nodeRows.indexOf(row) < nodeRows.size()-1 )
				moveY += rowMaxHeight[nodeRows.indexOf(row)]/2 + Util.SPACE + rowMaxHeight[nodeRows.indexOf(row)+1]/2;
		}
		
		// Horizontal arrangement of all the nodes
		
		// Bottom-up starting from fulcrum as youth
		//Group baseGroup = groupRows.get(groupRows.size()-2).get(0);
		Group baseGroup = fulcrumNode.husbandGroup;
		print(baseGroup);
		int moveX = 0;
		for( Node youth : baseGroup.getYouths() ) {
			youth.x = moveX;
			moveX += youth.width + Util.PADDING;
			setCardCoordinates(youth);
		}
		// Center horizontaly the parents in between of the children 
		Node guardian = baseGroup.getGuardian();
		print(guardian);
		guardian.x = baseGroup.getYouth(0).getMainCard().centerX()+  baseGroup.getArcWidth(0)/2 - guardian.centerX();
		setCardCoordinates(guardian);
		// Call recoursive methods for ancestors
		arrangeHusbandGroup(guardian);
		arrangeWifeGroup(guardian);

		// Top-down starting from fulcrum as guardian
		if( fulcrumNode.guardGroup != null ) {
			moveX = fulcrumNode.centerX() - fulcrumNode.guardGroup.getYouthWidth() /2;
			for( Node youth : fulcrumNode.guardGroup.getYouths() ) {
				youth.x = moveX;
				moveX += youth.width + Util.PADDING;
				setCardCoordinates(youth);
			}
		}
		
		// Correct horizontal overlapping of groups
		/*for (List<Group> row : groupRows) {
			int nodeX = row.get(0).getYouth(0).x;
			Node lastNode = null;
			for (Group group : row) {
				for (Node youth : group.getYouths()) {
					// Doesn't shift twice the same node
					if (youth.equals(lastNode)) continue;
					// Shift youths to the right
					if (youth.x < nodeX ) {
						youth.x = nodeX;
						setCardCoordinates(youth);
					}
					nodeX += youth.width + Util.PADDING;
					lastNode = youth;
				}
				//nodeX += 50;
			}
		}*/
		
		// Create the Lines
		for (Node node : nodes) {
			if (node.isCouple()) {
				lines.add(new Line(((Couple) node).husband));
				lines.add(new Line(((Couple) node).wife));
			} else if (node.isSingle()) {
				lines.add(new Line(((Single) node).one));
			}
		}
	}

	// Maximum height of a node from the height of its cards
	int maxHeight(Set<? extends Card> cards) {
		int max = 0;
		for (Card card : cards) {
			if (card.height > max)
				max = card.height;
		}
		return max;
	}

	/**
	 * Set the absolute coordinates of the cards of a node, deducing from the
	 * absolute coordinates of this node.
	 * 
	 * @param node The node to consider
	 */
	private void setCardCoordinates(Node node) {
		if (node.isCouple() && ((Couple) node).husband != null && ((Couple) node).wife != null) {
			((Couple) node).husband.x = node.x;
			((Couple) node).husband.y = node.y + (node.height - ((Couple)node).husband.height)/2;
			((Couple) node).wife.x = node.x + ((Couple) node).husband.width + Util.MARGIN;
			((Couple) node).wife.y = node.y + (node.height - ((Couple)node).wife.height)/2;
		} else if (node.isSingle() && ((Single) node).one != null) {
			((Single) node).one.x = node.x;
			((Single) node).one.y = node.y;
		}
	}

	// Recoursive methods to arrange horizontally youths and guardian of a group
	// The group in which the husband is child
	private void arrangeHusbandGroup(Node commonNode) {
		Group group = commonNode.husbandGroup;
		if (group != null) {
			if (group.getYouths().size() > 1) {
				int moveX = commonNode.x;
				for( int i=group.getYouths().size()-2; i>=0; i-- ) {
				//for( Node uncle : fatherGroup.getYouths() ) {
					Node uncle = group.getYouth(i);
					moveX -= (Util.PADDING + uncle.width);
					uncle.x = moveX;
					setCardCoordinates(uncle);
				}
			}
			arrangeGuardian(group, 1);
		}
	}

	// The group in which the wife is child
	private void arrangeWifeGroup(Node commonNode) {
		Group group = commonNode.wifeGroup;
		if (group != null) {
			if (group.getYouths().size() > 1) {
				int moveX = commonNode.x + commonNode.width + Util.PADDING;
				for( int i=1; i<group.getYouths().size(); i++ ) {
					Node uncle = group.getYouth(i);
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
	 * @param group The group of which we want to arrange horizontally the guardian
	 * @param branch 1 if the child is a husband, 2 if the child is a wife 
	 */
	private void arrangeGuardian(Group group, int branch) {
		Node guardian = group.getGuardian();
		if(guardian != null ) {
			if(group.getYouths().size() > 1) {
				if (branch == 1 ) // Husband branch
					guardian.x = group.getYouth(0).getMainCard().centerX() + group.getArcWidth(branch)/2 - guardian.centerX();
				else if (branch == 2 ) // Wife branch
					guardian.x = group.getYouth(0).getCard(2).centerX() + group.getArcWidth(branch)/2 - guardian.centerX();
			} else // Only one child
				guardian.x = group.getYouth(0).getCard(branch).centerX() - guardian.centerX();
			setCardCoordinates(guardian);
			arrangeHusbandGroup(guardian);
			arrangeWifeGroup(guardian);
		}
	}

	// Group version
	public String toString() {
		String str = "";
		for (List<Group> row : groupRows) {
			for (Group group : row) {
				str += group.toString();
				str += "\n- - - - - - - - - - - - - - - -\n";
			}
			str += "--------------------------------\n";
		}
		return str;
	}
	
	// Node version
	public String toString1() {
		String str = "";
		for (Set<Node> row : nodeRows) {
			for (Node node : row) {
				if (node == null)
					str += "null";
				else
					str += node.toString();
				str += "\n";
			}
			str += "- - - - - - - - - - - - - - - - - -\n";
		}
		return str;
	}
}

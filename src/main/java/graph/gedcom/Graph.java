package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import graph.gedcom.Util.Card;
import static graph.gedcom.Util.*;

public class Graph {

	private Gedcom gedcom;
	private Animator animator;

	// Settings for public methods with default values
	public int whichFamily; // Which family display if the fulcrum is child in more than one family
	private int ancestorGenerations = 3; // Generations to display
	private int greatUnclesGenerations = 2; // Uncles and great-uncles. Can't be more than ancestor generations.
	private boolean withSpouses = true;
	private int descendantGenerations = 3;
	private int siblingNephewGenerations = 2; // Siblings and generations of their descendants
	private int uncleCousinGenerations = 2; // Uncles and cousins. First uncle generation overlaps to great-uncle generations.
	private Person fulcrum;
	private PersonNode fulcrumNode;
	private Group fulcrumGroup;
	private int maxAbove; // Max upper generation of ancestors (positive number), excluding mini ancestries
	private int maxBelow; // Max generation of descendants, excluding mini progenies

	public Graph(Gedcom gedcom) {
		this.gedcom = gedcom;
		animator = new Animator();
	}

	// Public methods

	/** If the fulcrum is child in more than one family, you can choose wich family to display.
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

	public float getWidth() {
		return animator.width;
	}

	public float getHeight() {
		return animator.height;
	}

	public List<PersonNode> getPersonNodes() {
		return animator.personNodes;
	}

	public List<Bond> getBonds() {
		return animator.bonds;
	}

	public boolean needMaxBitmap() {
		return animator.maxBitmapWidth == 0 && animator.maxBitmapHeight == 0;
	}

	public void setMaxBitmap(int width, int height) {
		animator.maxBitmapWidth = width;
		animator.maxBitmapHeight = height;
	}

	public int getMaxBitmapWidth(){
		return animator.maxBitmapWidth;
	}
	public int getMaxBitmapHeight(){
		return animator.maxBitmapHeight;
	}

	public List<Set<Line>> getLines() {
		return animator.linesGroups;
	}

	// Preparation of the nodes: to be called once
	public void initNodes() {
		animator.initNodes(fulcrumNode, maxAbove, maxBelow);
	}

	// First displacement of the nodes: to be called once
	public void placeNodes() {
		animator.placeNodes();
	}

	/** Generate the next "frame" of the node positions: to be callad multiple times
	 * @return true if there is still something to move, false if positioning is complete
	 */
	public boolean playNodes() {
		return animator.playNodes();
	}


	/** Draw the diagram starting from fulcrum.
	 * @param fulcrum The person that becomes the diagram center
	 */
	public void startFrom(Person fulcrum) {
		this.fulcrum = fulcrum;
		
		// Reset all values
		animator.nodes.clear();
		animator.personNodes.clear();
		animator.bonds.clear();
		animator.groups.clear();
		maxAbove = 0;
		maxBelow = 0;

		// Create all the nodes of the diagram
		List<Family> fulcrumParents = fulcrum.getParentFamilies(gedcom);
		fulcrumGroup = createGroup(null, 0, false, Branch.NONE);
		if (!fulcrumParents.isEmpty()) {

			// Creation of parent nodes of fulcrum
			if (whichFamily >= fulcrumParents.size())
				whichFamily = fulcrumParents.size() - 1; // To prevent IndexOutOfBoundsException
			Family parentFamily = fulcrumParents.get(whichFamily);
			boolean parentMini = ancestorGenerations == 0;
			Group firstParentGroup = createGroup(null, -1, parentMini, null);
			Node parentNode = createNodeFromFamily(parentFamily, -1, parentMini ? Card.ANCESTRY : Card.REGULAR, false);
			parentNode.isAncestor = true;
			Branch parentBranch = parentNode.getPersonNodes().size() > 1 ? Branch.PATER : Branch.NONE;
			firstParentGroup.branch = parentBranch;
			firstParentGroup.addNode(parentNode);
			if( !parentMini )
				maxAbove = 1;

			// Find ancestors of father
			PersonNode father = parentNode.getPartner(0);
			if (ancestorGenerations > 0 && parentNode.getPartner(0) != null) {
				// Grand-parents
				findAncestors(father, firstParentGroup, 1, parentBranch);
				// Uncles
				findUncles(father, firstParentGroup, parentNode.getPersonNodes().size() == 1 ? Side.LEFT : Side.NONE);
				// Step siblings
				stepFamilies(father, parentFamily, parentBranch, parentNode.getPersonNodes().size() == 1 ? Side.LEFT : Side.NONE);
			}

			// Fulcrum with marriages and siblings
			for( Person sibling : parentFamily.getChildren(gedcom) ) {
				if( sibling.equals(fulcrum) ) {
					marriageAndChildren(fulcrum, parentNode, fulcrumGroup);
				} else if( siblingNephewGenerations > 0 ) {
					Node siblingNode = createNodeFromPerson(sibling, parentNode, 0, Card.REGULAR);
					fulcrumGroup.addNode(siblingNode);
					findDescendants(siblingNode, 0, siblingNephewGenerations, Branch.NONE);
				}
			}

			// Find ancestors of mother
			if( ancestorGenerations > 0 ) {
				PersonNode mother = parentNode.getPartner(1);
				if( mother != null ) {
					Group secondParentGroup = createGroup(null, -1, parentMini, Branch.MATER);
					secondParentGroup.addNode(parentNode);
					// Step siblings
					stepFamilies(mother, parentFamily, Branch.MATER, Side.NONE);
					// Uncles and grand-parents
					findAncestors(mother, secondParentGroup, 1, Branch.MATER);
					// Uncles
					findUncles(mother, secondParentGroup, Side.NONE);
				} else if( parentNode.getPersonNodes().size() == 1 ) { // Single parent
					stepFamilies(father, parentFamily, Branch.NONE, Side.RIGHT);
					findUncles(father, firstParentGroup, Side.RIGHT);
				}
			}
		} else {
			// Fulcrum without parent family
			marriageAndChildren(fulcrum, null, fulcrumGroup);
		}
	}

	/** Recursive method to generate the nodes of siblings and parents of an ancestor.
	 * @param commonNode The node with the person to start from
	 * @param generUp Number of the generation of the commonNode: 0 for fulcrum, 1 for parents, 2 for grand-parents etc.
	 * @param branch We are looking for ancestors and uncles of the "paternal" (left) or "maternal" (right) branch
	 */
	private void findAncestors(PersonNode commonNode, Group group, int generUp, Branch branch) {
		Person ancestor = commonNode.person;
		if( !ancestor.getParentFamilies(gedcom).isEmpty() ) {
			List<Family> parentFamilies = ancestor.getParentFamilies(gedcom);
			Family family = parentFamilies.get(parentFamilies.size() - 1);
			int parentGen = generUp + 1;
			boolean parentMini = parentGen > ancestorGenerations;
			if( !parentMini && parentGen > maxAbove )
				maxAbove = parentGen;

			Node parentNode = createNodeFromFamily(family, -parentGen, parentMini ? Card.ANCESTRY : Card.REGULAR, false);
			parentNode.isAncestor = true;
			commonNode.origin = parentNode;

			// Add the great-uncles (siblings of ancestor) with their spouses
			if( generUp > 1 ) // Uncles of the parents generation (1) are found before
				findUncles(commonNode, group, Side.NONE);

			if( parentNode.getPersonNodes().size() == 0 ) // Ancestor's parent family without partners
				return;

			boolean manyParents = parentNode.getPersonNodes().size() > 1;
			Branch parentBranch = manyParents ? Branch.PATER : Branch.NONE;
			Group firstParentGroup = createGroup(null, -parentGen, parentMini, parentBranch);
			firstParentGroup.addNode(parentNode);

			// Recall this method
			if( generUp < ancestorGenerations ) {
				List<PersonNode> parents = parentNode.getPersonNodes();
				for( int i = 0; i < parents.size(); i++ ) {
					PersonNode parent = parents.get(i);
					if( i == 0 ) {
						findAncestors(parent, firstParentGroup, parentGen, parents.size() > 1 ? Branch.PATER : Branch.NONE);
					} else {
						Group secondParentGroup = createGroup(null, -parentGen, parentMini, Branch.MATER);
						secondParentGroup.addNode(parentNode);
						findAncestors(parent, secondParentGroup, parentGen, Branch.MATER);
					}
				}
			}
		}
	}

	/** Find uncles and their descendants.
	 * @param node	The fulcrum's ancestor of which to find siblings (the uncles)
	 * @param group	Uncles will be put inside this group
	 * @param side	In case of Branch.NONE, find the uncles only to the left or right of the person. Otherwise both (Side.NONE)
	 */
	void findUncles(PersonNode personNode, Group group, final Side side) {
		final int generUp = -personNode.generation;
		if( generUp <= greatUnclesGenerations || (generUp == 1 && uncleCousinGenerations > 0) ) {
			Node origin = personNode.origin;
			if( origin != null ) {
				Branch branch = group.branch;
				Family family = origin.spouseFamily;
				Person person = personNode.person;
				boolean ancestorFound = false;
				for( Person uncle : family.getChildren(gedcom) ) {
					int index = -1; // The uncle will be added at the end of the group
					if( branch == Branch.NONE ) {
						if( uncle.equals(person) ) {
							if( generUp == 1 && side == Side.LEFT )
								break; // All left uncles found, we can exit the for loop
							ancestorFound = true;
						}
						if( generUp == 1 && side == Side.RIGHT && !ancestorFound )
							continue; // Continues the for loop to reach the uncles to the right of person
					}
					if( !uncle.equals(person) ) {
						Node uncleNode = createNodeFromPerson(uncle, origin, -generUp, Card.REGULAR); // uncles if visible are never mini card
						if( branch == Branch.PATER || (branch == Branch.NONE && !ancestorFound) )
							index = group.list.indexOf(personNode.getFamilyNode()); // Uncle will be put before the ancestor node
						group.addNode(uncleNode, index);
						if( generUp == 1 ) // Add the cousins and possibly their descendants
							findDescendants(uncleNode, -1, uncleCousinGenerations, index > -1 ? Branch.PATER : branch);
						else // Mini progeny of great-uncles
							findDescendants(uncleNode, -generUp, 1, Branch.NONE);
					}
				}
			}
		}
	}

	/** Find the step siblings of fulcum in other marriages of the parents
	 * @param parentNode	Parent of whom we are looking the step children
	 * @param family	Parent family of fulcrum to be removed
	 * @param branch	Paternal, maternal or single parent branch
	 * @param side	Which half-siblings get and where to put them: those on the left or those on the right of fulcrum group
	 */
	private void stepFamilies(PersonNode parentNode, Family parentFamily, Branch branch, Side side) {
		if( siblingNephewGenerations > 0 ) {
			List<Family> stepFamilies = parentNode.person.getSpouseFamilies(gedcom);
			boolean parentFamilyFound = false;
			for( Family stepFamily : stepFamilies ) {
				if( stepFamily.equals(parentFamily) ) {
					if( side == Side.LEFT )
						break;
					parentFamilyFound = true;
				} else {
					if( side == Side.RIGHT && !parentFamilyFound )
						continue;
					Group stepGroup = branch == Branch.MATER || side == Side.RIGHT
							? parentNode.familyNode.stepYouthRight : parentNode.familyNode.stepYouthLeft;
					if( stepGroup == null && !stepFamily.getChildren(gedcom).isEmpty() ) {
						Branch br = side == Side.LEFT ? Branch.PATER : branch;
						stepGroup = createGroup(null, 0, false, br);
						if( branch == Branch.MATER || side == Side.RIGHT )
							parentNode.familyNode.stepYouthRight = stepGroup;
						else
							parentNode.familyNode.stepYouthLeft = stepGroup;
					}
					for( Person stepSibling : stepFamily.getChildren(gedcom) ) {
						Node stepSiblingNode = createNodeFromPerson(stepSibling, parentNode, 0, Card.REGULAR);
						stepGroup.addNode(stepSiblingNode);
						if( siblingNephewGenerations >= 1 )
							findDescendants(stepSiblingNode, 0, siblingNephewGenerations, branch);
					}
				}
			}
		}
	}

	// Fulcrum with one or many marriages and their children
	void marriageAndChildren(Person fulcrum, Node parentNode, Group group) {
		// Multi marriages and children
		List<Family> spouseFamilies = fulcrum.getSpouseFamilies(gedcom);
		if( !spouseFamilies.isEmpty() ) {
			for( int i = 0; i < spouseFamilies.size(); i++ ) {
				Family marriageFamily = spouseFamilies.get(i);
				Node marriageNode;
				if( i == 0 ) { // For the first marriage only
					marriageNode = createNodeFromPerson(fulcrum, parentNode, 0, Card.FULCRUM);
				} else {
					marriageNode = createNodeFromFamily(marriageFamily, 0, Card.REGULAR, true);
				}
				group.addNode(marriageNode);
				findDescendants(marriageNode, 0, descendantGenerations + 1, Branch.NONE); // + 1 because we start from the generation before
			}
		} else { // Fulcrum has no marriages
			Node singleNode = createNodeFromPerson(fulcrum, parentNode, 0, Card.FULCRUM);
			group.addNode(singleNode);
		}
	}

	/** Recoursive method to find the descendants.
	 * @param commonNode Node containing the person of whom to find descendants. Can be a PersonNode or a FamilyNode.
	 * @param startGeneration Number of the generation of the first 'commonNode': -1 for parents, 0 for fulcrum, 1 for children etc.
	 * @param maxGenerations Limit of the number of generations to search
	 * @param branch
	 */
	private void findDescendants(Node commonNode, int startGeneration, int maxGenerations, Branch branch) {
		Family spouseFamily = commonNode.spouseFamily;
		if( spouseFamily != null && !spouseFamily.getChildren(gedcom).isEmpty() ) {
			int generChild = commonNode.generation + 1;
			boolean childMini = generChild >= maxGenerations + startGeneration;
			if( !childMini && generChild > maxBelow )
				maxBelow = generChild;
			Group childGroup = createGroup(null, generChild, childMini, branch);
			for( Person child : spouseFamily.getChildren(gedcom) ) {
				Node childNode = createNodeFromPerson(child, commonNode, generChild, childMini ? Card.PROGENY : Card.REGULAR);
				childGroup.addNode(childNode);
				if( !childMini ) {
					findDescendants(childNode, startGeneration, maxGenerations, branch);
				}
			}
		}
	}

	// Container for siblings and their spouses
	Group createGroup(Node parentNode, int generation, boolean mini, Branch branch) {
		Group group = new Group(generation, mini, branch);
		// Add it to groups list
		if( branch == Branch.PATER && generation == 0 ) { // Group of paternal cousins
			int index = animator.groups.indexOf(fulcrumGroup);
			animator.groups.add(index, group);
		} else
			animator.groups.add(group);
		return group;
	}

	// Find little ancestry above the acquired spouse
	void findAcquiredAncestry(PersonNode card) {
		card.acquired = true;
		List<Family> parentFamilies = card.person.getParentFamilies(gedcom);
		if( !parentFamilies.isEmpty() ) {
			Family family = parentFamilies.get(parentFamilies.size() - 1);
			int generation = card.generation - 1;
			Node ancestry = createNodeFromFamily(family, generation, Card.ANCESTRY, false);
			card.origin = ancestry;
			if( ancestry.getHusband() != null )
				ancestry.getHusband().acquired = true;
			if( ancestry.getWife() != null )
				ancestry.getWife().acquired = true;
		}
	}

	/** Create a PersonNode starting from a person. Possibly can create the FamilyNode and return it.
	 * Used by fulcrum, uncles, cousins, descendants
	 * @param person The dude to create the node of
	 * @param parentNode Node (family or person) origin of this person
	 * @param generation The generation of the person: negative above, 0 for fulcrum row, positive below
	 * @param type Fashion of the node: FULCRUM, REGULAR, ANCESTRY or PROGENY
	 * @return A PersonNode or FamilyNode
	 */
	private Node createNodeFromPerson(Person person, Node parentNode, int generation, Card type) {
		// Single person
		PersonNode personNode = new PersonNode(gedcom, person, type);
		personNode.generation = generation;
		personNode.origin = parentNode;
		if( type == Card.FULCRUM )
			fulcrumNode = personNode;

		// Possible family with at least two members
		FamilyNode familyNode = null;
		List<Family> families = person.getSpouseFamilies(gedcom);
		if( (type == Card.FULCRUM || type == Card.REGULAR) && !families.isEmpty() ) {
			int whichMarriage = type == Card.FULCRUM ? 0 : families.size() - 1; // Usually the last marriage of a person is displayed
			Family spouseFamily = families.get(whichMarriage);
			List<Person> spouses = getSpouses(spouseFamily);
			if( spouses.size() > 1 && withSpouses ) { // Many spouses
				familyNode = new FamilyNode(gedcom, spouseFamily, type);
				familyNode.generation = generation;
				for( Person spouse : spouses ) {
					if( spouse.equals(person) ) {
						familyNode.addPartner(personNode);
					} else {
						PersonNode partnerNode = new PersonNode(gedcom, spouse, type == Card.FULCRUM ? Card.REGULAR : type);
						partnerNode.generation = generation;
						familyNode.addPartner(partnerNode);
						findAcquiredAncestry(partnerNode);
					}
				}
				familyNode.createBond();
			} else { // One spouse only
				personNode.spouseFamily = spouseFamily;
			}
		}
		if( familyNode != null ) {
			animator.addNode(familyNode);
			return familyNode; // familyNode is returned to keep finding descendants
		} else {
			animator.addNode(personNode);
			return personNode;
		}
	}
	
	/** Create a node starting from a family: used to find direct ancestors, for fulcrum's following marriages,
	 * and for acquired mini ancestry.
	 * @param spouseFamily The spouse family of which create the FamilyNode
	 * @param generation Number of the generation, negative for ancestors
	 * @param type Fulcrum card, regular card, little ancestry, or little progeny
	 * @param nextFulcrumMarriage Is this a next marriage of fulcrum?
	 */
	private Node createNodeFromFamily(Family spouseFamily, int generation, Card type, boolean nextFulcrumMarriage) {
		Node newNode = null;
		List<Person> spouses = getSpouses(spouseFamily);
		// Single fulcrum
		if( nextFulcrumMarriage && !withSpouses ) {
			newNode = new PersonNode(gedcom, fulcrum, type);
			newNode.spouseFamily = spouseFamily;
		} else { // Family with many spouses
			newNode = new FamilyNode(gedcom, spouseFamily, type);
			for( Person spouse : spouses ) {
				PersonNode personNode = new PersonNode(gedcom, spouse, type);
				personNode.generation = generation;
				((FamilyNode)newNode).addPartner(personNode);
				if( nextFulcrumMarriage && !spouse.equals(fulcrum) ) { // Only for followings fulcrum's marriages
					findAcquiredAncestry(personNode);
				}
			}
			((FamilyNode)newNode).createBond();
		}
		newNode.generation = generation;
		animator.addNode(newNode);
		return newNode;
	}

	// Return a list of all spouses in a family alternating husbands and wives
	List<Person> getSpouses(Family family) {
		List<Person> persons = new ArrayList<>();
		for( Person husband : family.getHusbands(gedcom) )
			persons.add(husband);
		int pos = persons.size() > 0 ? 1 : 0;
		for( Person wife : family.getWives(gedcom) ) {
			persons.add(pos, wife);
			pos += (persons.size() > pos + 1) ? 2 : 1;
		}
		return persons;
	}

	@Override
	public String toString() {
		String str = "";
		for (Node node : animator.nodes) {
			str += node.generation + ": ";
			str +=  " | " + node + " | " ;
			str += "\n";
		}
		return str;
	}
}

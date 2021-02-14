package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
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
	private int maxAbove; // Max upper generation of ancestors (positive number)
	private int maxBelow; // Max generation of descendants

	public Graph(Gedcom gedcom) {
		this.gedcom = gedcom;
		animator = new Animator();
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
	
	public float getWidth() {
		return animator.width;
	}
	
	public float getHeight() {
		return animator.height;
	}

	public Set<Node> getNodes() {
		return animator.nodes;
	}

	public List<Line> getLines() {
		return animator.lines;
	}
	
	@Deprecated
	public Node getFulcrum() {
		return fulcrumNode;
	}
	
	// Preparation of the node movement: to be called once
	public void arrangeNodes() {
		animator.arrangeNodes(fulcrumNode, maxAbove, maxBelow);
	}
	
	// Generate the next "frame" of the node positions: to be callad multiple times
	public void playNodes() {
		animator.playNodes();
	}


	/**
	 * Draw the diagram starting from fulcrum.
	 * @param fulcrum The person that becomes the diagram center
	 */
	public void startFrom(Person fulcrum) {
		this.fulcrum = fulcrum;
		
		// Reset all values
		animator.nodes.clear();
		maxAbove = 0;
		maxBelow = 0;

		// Create all the nodes of the diagram
		List<Family> fulcrumParents = fulcrum.getParentFamilies(gedcom);
		if (!fulcrumParents.isEmpty()) {

			// Creation of parent nodes of fulcrum
			if (whichFamily >= fulcrumParents.size())
				whichFamily = fulcrumParents.size() - 1; // To prevent IndexOutOfBoundsException
			Family parentFamily = fulcrumParents.get(whichFamily);
			Node parentNode = createNodeFromFamily(parentFamily, -1, ancestorGenerations == 0 ? PROGENY : REGULAR, false);
			maxAbove = 1;

			// Find ancestors of father
			if (ancestorGenerations > 0 && parentNode.getHusband() != null) {
				// Uncles and grand-parents
				findAncestors(parentNode.getHusband(), 1);
				// Step siblings
				stepFamilies(parentNode.getHusband(), parentFamily);
			}

			// Fulcrum with marriages and siblings
			for (Person sibling : parentFamily.getChildren(gedcom)) {
				if (sibling.equals(fulcrum)) {
					marriageAndChildren(fulcrum, parentNode);
				} else if (siblingNephewGenerations > 0) {
					Node siblingNode = createNodeFromPerson(sibling, parentNode, 0, REGULAR);
					findDescendants(siblingNode, 0, siblingNephewGenerations);
				}
			}

			// Find ancestors of mother
			if (ancestorGenerations > 0 && parentNode.getWife() != null) {
				// Step siblings
				stepFamilies(parentNode.getWife(), parentFamily);
				// Uncles and grand-parents
				findAncestors(parentNode.getWife(), 1);
			}
		} else {
			// Fulcrum without parent family
			marriageAndChildren(fulcrum, null);
		}
	}
	
	/**
	 * Recursive method to generate the nodes of siblings and parents of an ancestor
	 * @param commonNode The node with the person to start from
	 * @param generUp Number of the generation of the commonNode: 0 for fulcrum, 1 for parents, 2 for grand-parents etc.
	 */
	private void findAncestors(PersonNode commonNode, int generUp) {
		if (generUp > maxAbove)
			maxAbove = generUp;
		Person ancestor = commonNode.person;
		if (!ancestor.getParentFamilies(gedcom).isEmpty()) {
			List<Family> parentFamilies = ancestor.getParentFamilies(gedcom);
			Family family = parentFamilies.get(parentFamilies.size()-1);
			Node parentNode = null;
			int parentGen = generUp + 1;
			if (parentGen > maxAbove)
				maxAbove = parentGen;
			parentNode = createNodeFromFamily(family, -parentGen, parentGen > ancestorGenerations ? ANCESTRY : REGULAR, false);
			commonNode.setOrigin(parentNode);

			// Create uncles (with their spouses)
			if (generUp <= greatUnclesGenerations || uncleCousinGenerations > 0 )
				for (Person uncle : family.getChildren(gedcom)) {
					if (!uncle.equals(ancestor)) {
						Node uncleNode = createNodeFromPerson(uncle, parentNode, -1, REGULAR); // uncles if visible are never mini card
						// Add the cousins and possibly their descendants
						if (generUp == 1 && uncleCousinGenerations > 1)
							findDescendants(uncleNode, 0, uncleCousinGenerations);
					}
				}
			// Recall this method
			if (generUp < ancestorGenerations) {
				if (parentNode.getHusband() != null)
					findAncestors(parentNode.getHusband(), generUp + 1);
				if (parentNode.getWife() != null)
					findAncestors(parentNode.getWife(), generUp + 1);			
			}

		}
	}

	/**
	 * Find the step siblings of fulcum in other marriages of the parents
	 * @param parent	Parent of whom we are looking the step children
	 * @param family	Parent family of fulcrum to be removed
	 */
	private void stepFamilies(PersonNode parent, Family family) {
		if (siblingNephewGenerations > 0) {
			List<Family> stepFamilies = parent.person.getSpouseFamilies(gedcom);
			stepFamilies.remove( family );
			for( Family stepFamily : stepFamilies ) {
				for( Person stepSibling : stepFamily.getChildren(gedcom) ) {
					Node stepSiblingNode = createNodeFromPerson(stepSibling, null, 0, REGULAR);
					stepSiblingNode.getMainPersonNode().setOrigin(parent);
					if (siblingNephewGenerations >= 1)
						findDescendants(stepSiblingNode, 1, siblingNephewGenerations);
				}
			}
		}
	}
	
	// Fulcrum with one or many marriages and their children
	void marriageAndChildren(Person fulcrum, Node parentNode) {
		// Multi marriages and children
		List<Family> spouseFamilies = fulcrum.getSpouseFamilies(gedcom);
		if (!spouseFamilies.isEmpty()) {
			for (int i = 0; i < spouseFamilies.size(); i++) {
				Family marriageFamily = spouseFamilies.get(i);
				Node marriageNode;
				if (i == 0) { // For the first marriage only
					marriageNode = createNodeFromPerson(fulcrum, parentNode, 0, FULCRUM);
				} else {
					marriageNode = createNodeFromFamily(marriageFamily, 0, REGULAR, true);
				}
				findDescendants(marriageNode, 0, descendantGenerations);
			}
		} else // Fulcrum has no marriages
			fulcrumNode = (PersonNode) createNodeFromPerson(fulcrum, parentNode, 0, FULCRUM);
	}


	/**
	 * Recoursive method to find the descendants
	 * @param commonNode Node containing the person of whom to find descendants. Can be a PersonNode OR a FamilyNode.
	 * @param generDown  Number of the generation of 'commonNode': 0 for fulcrum, 1 for children, 2 for grandchildren etc.
	 * @param maxGenerations Limit of the number of generations to search
	 */
	private void findDescendants(Node commonNode, int generDown, int maxGenerations) {
		if (generDown > maxBelow)
			maxBelow = generDown;
		Family spouseFamily = commonNode.spouseFamily;
		if (spouseFamily != null) { // && maxGenerations >= generDown
			int generChild = generDown + 1;
			if (generChild > maxBelow)
				maxBelow = generChild;
			for (Person child : spouseFamily.getChildren(gedcom)) {
				Node childNode = createNodeFromPerson(child, commonNode, generChild, generChild > maxGenerations ? PROGENY : REGULAR);
				if (maxGenerations >= generChild) {
					findDescendants(childNode, generChild, maxGenerations);
				}
			}
		}
	}

	// Find little ancestry above the acquired spouse
	void findAcquiredAncestry(PersonNode card) {
		card.acquired = true;
		List<Family> parentFamilies = card.person.getParentFamilies(gedcom);
		if(!parentFamilies.isEmpty()) {
			Family family = parentFamilies.get(parentFamilies.size()-1);
			int generation = card.generation - 1;
			if (maxAbove < Math.abs(generation)) // maxAbove is positive, generation is negative
				maxAbove = Math.abs(generation);
			Node ancestry = createNodeFromFamily(family, generation, ANCESTRY, false);
			card.setOrigin(ancestry);
			if (ancestry.getHusband() != null)
				ancestry.getHusband().acquired = true;
			if (ancestry.getWife() != null)
				ancestry.getWife().acquired = true;
		}
	}

	/** Create a PersonNode starting from a person. Possibly can create the FamilyNode and return it.
	 * Used by fulcrum, uncles, cousins, descendants
	 * @param person The dude to create the node
	 * @param parentFamily Parent node of this person
	 * @param generation The generation of the person: negative up, 0 for fulcrum row, positive down
	 * @param type Complete card or small progeny
	 * @return A PersonNode or FamilyNode
	 */
	private Node createNodeFromPerson(Person person, Node parentNode, int generation, int type) {
		// Single person
		PersonNode personNode = new PersonNode(gedcom, person, type);
		personNode.generation = generation;
		personNode.setOrigin(parentNode);
		animator.addNode(personNode);
		
		// Possible family with at least two members
		FamilyNode familyNode = null;
		List<Family> families = person.getSpouseFamilies(gedcom); 
		if (withSpouses && !families.isEmpty()) {
			int whichMarriage = type == FULCRUM ? 0 : families.size() - 1; // Usually the last marriage of a person is displayed
			Family spouseFamily = families.get(whichMarriage);
			familyNode = new FamilyNode(gedcom, spouseFamily, type);
			familyNode.generation = generation;
			animator.addNode(familyNode);
			for (Person spouse : getSpouses(spouseFamily)) {
				if (spouse.equals(person)) {
					familyNode.addPartner(personNode);
				} else {
					PersonNode partnerNode = new PersonNode(gedcom, spouse, type == FULCRUM ? REGULAR : type);
					partnerNode.generation = generation;
					familyNode.addPartner(partnerNode);
					animator.addNode(partnerNode);
					findAcquiredAncestry(partnerNode);
				}
			}
		}
		// familyNode is returned to keep finding descendants
		return familyNode != null ? familyNode : personNode;
	}
	
	/** Create a node starting from a family: used to find direct ancestors and for fulcrum's following marriages.
	 * @param spouseFamily The spouse family of which create the FamilyNode
	 * @param generation Number of the generation, negative for ancestors
	 * @param type 0 fulcrum card, 1 regular card, 2 little ancestry, 3 little progeny
	 * @param nextFulcrumMarriage Is this a next marriage of fulcrum?
	 */
	private Node createNodeFromFamily(Family spouseFamily, int generation, int type, boolean nextFulcrumMarriage) {
		Node newNode = null;
		List<Person> spouses = getSpouses(spouseFamily);
		// No spouses
		if (spouses.isEmpty()) {
			newNode = new FamilyNode(gedcom, spouseFamily, type);
		}
		// Single person
		else if (spouses.size() == 1 || (nextFulcrumMarriage && !withSpouses)) {
			newNode = new PersonNode(gedcom, fulcrum, 0);
		} // Family with many spouses
		else if (spouses.size() > 1) {
			newNode = new FamilyNode(gedcom, spouseFamily, type);
			for (Person spouse : spouses) {
				PersonNode personNode = new PersonNode(gedcom, spouse, type);
				personNode.generation = generation;
				((FamilyNode)newNode).addPartner(personNode);
				animator.addNode(personNode);
				if (nextFulcrumMarriage && !spouse.equals(fulcrum)) { // Only for followings fulcrum's marriages
					findAcquiredAncestry(personNode);
				}
			}
		}
		newNode.generation = generation;
		animator.addNode(newNode);
		return newNode;
	}

	// Return a list of all spouses in a family alternating husbands and wives
	List<Person> getSpouses(Family family) {
		List<Person> persons = new ArrayList<>();
		for (Person husband : family.getHusbands(gedcom))
			persons.add(husband);
		int pos = persons.size() > 0 ? 1 : 0;
		for (Person wife : family.getWives(gedcom)) {
			persons.add(pos, wife);
			pos += (persons.size() > pos+1) ? 2 : 1;
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

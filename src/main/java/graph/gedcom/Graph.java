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

	// Settings for public methods with default values
	public int whichFamily; // Which family display if the fulcrum is child in more than one family
	private int ancestorGenerations = 3; // Generations to display
	private int greatUnclesGenerations = 2; // Uncles and great-uncles. Can't be more than ancestor generations.
	private boolean withSpouses = true;
	private int descendantGenerations = 3;
	private int siblingNephewGenerations = 2; // Siblings and generations of their descendants
	private int uncleCousinGenerations = 2; // Uncles and cousins. First uncle generation overlaps to great-uncle generations.

	private Gedcom gedcom;
	private Animator animator;
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

	public boolean needMaxBitmapSize() {
		return animator.maxBitmapSize == 0;
	}

	public void setMaxBitmapSize(float size) { // Size in dip
		animator.maxBitmapSize = size;
	}

	public float getMaxBitmapSize() { // Larger path width in dip
		return animator.maxBitmapSize;
	}

	public float getBiggestPathSize() {
		return animator.biggestPathSize;
	}

	public List<Set<Line>> getLines() {
		return animator.lineGroups;
	}
	public List<Set<Line>> getBackLines() {
		return animator.backLineGroups;
	}

	// Preparation of the nodes
	public void initNodes() {
		animator.initNodes(fulcrumNode, maxAbove, maxBelow);
	}

	// Final displacement of the nodes
	public void placeNodes() {
		animator.placeNodes();
	}

	/* Update only lines position
	public void updateLines() {
		animator.updateLines();
	}*/

	/** Prepare the diagram starting from fulcrum.
	 * @param fulcrum The person that becomes the diagram center
	 */
	public void startFrom(Person fulcrum) {
		
		// Reset all values
		fulcrumNode = null;
		animator.nodes.clear();
		animator.personNodes.clear();
		animator.bonds.clear();
		animator.groups.clear();
		maxAbove = 0;
		maxBelow = 0;

		// Create all the nodes of the diagram
		List<Family> fulcrumParents = fulcrum.getParentFamilies(gedcom);
		fulcrumGroup = createGroup(0, false, Branch.NONE);
		if( !fulcrumParents.isEmpty() ) {

			// Creation of parent nodes of fulcrum
			if( whichFamily >= fulcrumParents.size() )
				whichFamily = fulcrumParents.size() - 1; // To prevent IndexOutOfBoundsException
			else if( whichFamily < 0 )
				whichFamily = 0; // To prevent ArrayIndexOutOfBoundsException
			Family parentFamily = fulcrumParents.get(whichFamily);
			boolean parentMini = ancestorGenerations == 0;
			Group firstParentGroup = createGroup(-1, parentMini, null);
			Node parentNode = createNodeFromFamily(parentFamily, -1, parentMini ? Card.ANCESTRY : Card.REGULAR);
			parentNode.isAncestor = true;
			boolean parentSingle = parentNode.getPersonNodes().size() == 1
					&& parentNode.getPartner(0).person.getSpouseFamilyRefs().size() <= 1; // Single parent with no other marriages
			parentNode.match = (!parentMini && !parentSingle) ? Match.NEAR : Match.SOLE;
			Branch parentBranch = parentNode.getPersonNodes().size() > 1 ? Branch.PATER : Branch.NONE;
			firstParentGroup.branch = parentBranch;
			firstParentGroup.addNode(parentNode);
			if( !parentMini )
				maxAbove = 1;

			PersonNode first = parentNode.getPartner(0);
			PersonNode second = parentNode.getPartner(1);
			boolean femaleAlone = false;
			// Find ancestors on the left
			if (ancestorGenerations > 0 ) {
				if( first != null) {
					// Paternal grand-parents
					findAncestors(first, firstParentGroup, 1);
					// Paternal uncles
					findUncles(first, firstParentGroup, parentNode.getPersonNodes().size() == 1 ? Side.LEFT : Side.NONE);
				}
				// Other partners of the second parent
				if( second != null ) {
					findAncestorGenus(second, firstParentGroup, Side.LEFT);
				} // Other partners of a female alone parent
				else if( first != null && Gender.isFemale(first.person) ) {
					findAncestorGenus(first, firstParentGroup, Side.LEFT);
					femaleAlone = true;
				}
			}

			// Fulcrum with marriages and siblings
			for( Person sibling : parentFamily.getChildren(gedcom) ) {
				if( sibling.equals(fulcrum) && fulcrumNode == null ) {
					marriageAndChildren(fulcrum, parentNode, fulcrumGroup);
				} else if( siblingNephewGenerations > 0 ) {
					Genus siblingGenus = findPersonGenus(sibling, parentNode, 0, Card.REGULAR, fulcrumGroup);
					for( Node siblingNode : siblingGenus ) {
						findDescendants(siblingNode, 0, siblingNephewGenerations, false);
					}
				}
			}

			// Find ancestors on the right
			if( ancestorGenerations > 0 ) {
				if( parentSingle ) {
					findUncles(first, firstParentGroup, Side.RIGHT);
				} else {
					Group secondParentGroup = createGroup(-1, parentMini, Branch.MATER);
					secondParentGroup.addNode(parentNode);
					// Other marriages of the first not-female-alone partner
					if( first != null && !femaleAlone) {
						findAncestorGenus(first, secondParentGroup, Side.RIGHT);
					}
					if( second != null ) {
						// Uncles and grand-parents
						findAncestors(second, secondParentGroup, 1);
						// Uncles
						findUncles(second, secondParentGroup, Side.NONE);
					}
				}
			}
		} else {
			// Fulcrum without parent family
			marriageAndChildren(fulcrum, null, fulcrumGroup);
		}
	}

	/** Recursive method to generate the nodes of siblings and parents of an ancestor.
	 * @param commonNode The node with the person to start from
	 * @param group Where to add the ancestor and their siblings (great-uncles)
	 * @param generUp Number of the generation of the commonNode: 0 for fulcrum, 1 for parents, 2 for grand-parents etc.
	 */
	private void findAncestors(PersonNode commonNode, Group group, int generUp) {
		Person ancestor = commonNode.person;
		if( !ancestor.getParentFamilies(gedcom).isEmpty() ) {
			List<Family> parentFamilies = ancestor.getParentFamilies(gedcom);
			Family family = parentFamilies.get(parentFamilies.size() - 1);
			int parentGen = generUp + 1;
			boolean parentMini = parentGen > ancestorGenerations;
			if( !parentMini && parentGen > maxAbove )
				maxAbove = parentGen;

			Group firstParentGroup = createGroup(-parentGen, parentMini, null);
			FamilyNode parentNode = createNodeFromFamily(family, -parentGen, parentMini ? Card.ANCESTRY : Card.REGULAR);
			parentNode.isAncestor = true;
			parentNode.match = Match.SOLE;

			Branch parentBranch = parentNode.getPersonNodes().size() > 1 ? Branch.PATER : Branch.NONE;
			firstParentGroup.branch = parentBranch;
			commonNode.origin = parentNode;
			firstParentGroup.addNode(parentNode);

			// Add the great-uncles (siblings of ancestor) with their spouses
			if( generUp > 1 ) { // Uncles of the parents generation (1) are found before
				findUncles(commonNode, group, Side.NONE);
			}

			if( parentNode.getPersonNodes().isEmpty() ) // Ancestor's parent family without partners
				return;

			// Recall this method
			if( generUp < ancestorGenerations ) {
				List<PersonNode> parents = parentNode.getPersonNodes();
				if( parents.size() > 1 ) {
					Group secondParentGroup = createGroup(-parentGen, parentMini, Branch.MATER);
					secondParentGroup.addNode(parentNode);
					for( int i = 0; i < parents.size(); i++ ) {
						PersonNode parent = parents.get(i);
						if( i == 0 ) {
							findAncestorGenus(parent, secondParentGroup, Side.RIGHT);
							findAncestors(parent, firstParentGroup, parentGen);
						} else {
							findAncestorGenus(parent, firstParentGroup, Side.LEFT);
							findAncestors(parent, secondParentGroup, parentGen);
						}
					}
				} else { // Single parent 	TODO verifica con famiglia antenati SENZA genitori
					PersonNode parent = parents.get(0);
					findAncestorGenus(parent, firstParentGroup, Gender.isFemale(parent.person) ? Side.LEFT : Side.RIGHT);
					findAncestors(parent, firstParentGroup, parentGen);
				}
			}
		}
	}

	// Find multiple marriages of direct ancestor
	void findAncestorGenus(PersonNode personNode, Group group, Side side) {
		Genus genus = new Genus();
		genus.add(personNode.familyNode);
		if( personNode.type == Card.REGULAR ) {
			List<Family> families = personNode.person.getSpouseFamilies(gedcom);
			if( families.size() > 1 ) {
				personNode.familyNode.match = Match.NEAR;
				families.remove(personNode.spouseFamily);
				int generation = personNode.generation;
				for( int i = 0; i < families.size(); i++ ) {
					Family nextFamily = families.get(i);
					Match match = Match.get2(families.size(), side, i);
					FamilyNode nextFamilyNode = createNextFamilyNode(nextFamily, personNode.person, generation, side, match);
					if( side == Side.LEFT ) {
						group.addNode(nextFamilyNode, group.list.indexOf(personNode.familyNode));
						genus.add(genus.indexOf(personNode.familyNode), nextFamilyNode);
					} else {
						group.addNode(nextFamilyNode);
						genus.add(nextFamilyNode);
					}
				}
				for( Node node : genus )
					if( !node.equals(personNode.familyNode) ) {
						if( generation < -1 ) // Mini progeny
							findDescendants(node, generation, 0, false);
						else
							findDescendants(node, -1, siblingNephewGenerations + 1, side == Side.LEFT ? true : false);
					}
			}
		}
	}

	/** Find uncles and their descendants.
	 * @param personNode The fulcrum's ancestor of which to find siblings (the uncles)
	 * @param group	Uncles will be put inside this group
	 * @param side In case of Branch.NONE, find the uncles only to the left or right of the person. Otherwise both (Side.NONE)
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
					if( branch == Branch.NONE ) { // Not particulary paternal or maternal uncles (could be on both sides of ancestor)
						if( uncle.equals(person) ) {
							if( generUp == 1 && side == Side.LEFT )
								break; // All left uncles found, we can exit the for loop
							ancestorFound = true;
						}
						if( generUp == 1 && side == Side.RIGHT && !ancestorFound )
							continue; // Continues the for loop to reach the uncles to the right of person
					}
					if( !uncle.equals(person) ) {
						Genus uncleGenus = findPersonGenus(uncle, origin, -generUp, Card.REGULAR, null); // uncles if visible are never mini card
						for( Node uncleNode : uncleGenus ) {
							if( branch == Branch.PATER || (branch == Branch.NONE && !ancestorFound) )
								index = group.list.indexOf(personNode.getFamilyNode()); // Uncle will be put before the ancestor node
							group.addNode(uncleNode, index);
							if( generUp == 1 ) // Add the cousins and possibly their descendants
								findDescendants(uncleNode, -1, uncleCousinGenerations, index > -1 ? true : false);
							else // Mini progeny of great-uncles
								findDescendants(uncleNode, -generUp, 1, false);

						}
					}
				}
			}
		}
	}

	// Fulcrum with one or many marriages and their children
	void marriageAndChildren(Person fulcrum, Node parentNode, Group group) {
		Genus fulcrumGenus = findPersonGenus(fulcrum, parentNode, 0, Card.FULCRUM, group);
		for( Node node : fulcrumGenus ) {
			findDescendants(node, 0, descendantGenerations + 1, false); // + 1 because we start from the generation before
		}
	}

	/** Recoursive method to find the descendants.
	 * @param commonNode Node containing the person of whom to find descendants. Can be a PersonNode or a FamilyNode.
	 * @param startGeneration Number of the generation of the first 'commonNode': -1 for parents, 0 for fulcrum, 1 for children etc.
	 * @param maxGenerations Limit of the number of generations to search
	 * @param toTheLeft The new group will be placed to the left of fulcrum group
	 */
	private void findDescendants(Node commonNode, int startGeneration, int maxGenerations, boolean toTheLeft) {
		Family spouseFamily = commonNode.spouseFamily;
		if( spouseFamily != null && !spouseFamily.getChildren(gedcom).isEmpty() ) {
			int generChild = commonNode.generation + 1;
			boolean childMini = generChild >= maxGenerations + startGeneration;
			if( !childMini && generChild > maxBelow )
				maxBelow = generChild;
			Group childGroup = createGroup(generChild, childMini, Branch.NONE, toTheLeft);
			for( Person child : spouseFamily.getChildren(gedcom) ) {
				Genus childGenus = findPersonGenus(child, commonNode, generChild, childMini ? Card.PROGENY : Card.REGULAR, childGroup);
				if( !childMini ) {
					for( Node childNode : childGenus ) {
						findDescendants(childNode, startGeneration, maxGenerations, false);
					}
				}
			}
		}
	}

	// Find one or multiple marriages of one person. Used for fulcrum, their siblings, descendants, uncles.
	Genus findPersonGenus(Person person, Node parentNode, int generation, Card type, Group group) {
		Genus genus = new Genus();
		List<Family> families = person.getSpouseFamilies(gedcom);
		if( families.isEmpty() || (type == Card.PROGENY) ) {
			Node singleNode = createNodeFromPerson(person, null, parentNode, generation, type, Match.SOLE);
			if( group != null ) group.addNode(singleNode);
			genus.add(singleNode);
		} else {
			Side side = Gender.isFemale(person) ? Side.LEFT : Side.RIGHT; // Partner position respect the person (wife LEFT, husband RIGHT)
			for( int i = 0; i < families.size(); i++ ) {
				Family family = families.get(i);
				Match match = Match.get(families.size(), side, i);
				Node partnerNode;
				switch( match ) {
					case SOLE:
					case NEAR: // First or last marriage only
						partnerNode = createNodeFromPerson(person, family, parentNode, generation, type, match);
						break;
					default:
						partnerNode = createNextFamilyNode(family, person, generation, side, match);
				}
				if( group != null ) group.addNode(partnerNode);
				genus.add(partnerNode);
			}
		}
		return genus;
	}

	// Find little ancestry above the acquired spouse
	void findAcquiredAncestry(PersonNode card) {
		card.acquired = true;
		List<Family> parentFamilies = card.person.getParentFamilies(gedcom);
		if( !parentFamilies.isEmpty() ) {
			Family family = parentFamilies.get(parentFamilies.size() - 1);
			Node ancestry = createNodeFromFamily(family, card.generation - 1, Card.ANCESTRY);
			card.origin = ancestry;
			if( ancestry.getHusband() != null )
				ancestry.getHusband().acquired = true;
			if( ancestry.getWife() != null )
				ancestry.getWife().acquired = true;
		}
	}

	// Create the container for siblings and their spouses (containing multiple marriages also)
	Group createGroup(int generation, boolean mini, Branch branch) {
		return createGroup(generation, mini, branch, false);
	}

	Group createGroup(int generation, boolean mini, Branch branch, boolean beforeFulcrumGroup) {
		Group group = new Group(generation, mini, branch);
		// Add it to groups list
		if( beforeFulcrumGroup ) { // Group of paternal cousins
			int index = animator.groups.indexOf(fulcrumGroup);
			animator.groups.add(index, group);
		} else
			animator.groups.add(group);
		return group;
	}

	/** Create a PersonNode starting from a person. Possibly can create the FamilyNode and return it.
	 * @param person The dude to create the node of
	 * @param spouseFamily
	 * @param parentNode Node (family or person) origin of this person
	 * @param generation The generation of the person: negative above, 0 for fulcrum row, positive below
	 * @param type Fashion of the node: FULCRUM, REGULAR, ANCESTRY or PROGENY
	 * @param match
	 * @return A PersonNode or FamilyNode
	 */
	private Node createNodeFromPerson(Person person, Family spouseFamily, Node parentNode, int generation, Card type, Match match) {
		// Single person
		PersonNode personNode = new PersonNode(gedcom, person, type);
		personNode.generation = generation;
		personNode.origin = parentNode;
		personNode.match = match;
		if( type == Card.FULCRUM )
			fulcrumNode = personNode;

		// Possible family with at least two members
		FamilyNode familyNode = null;
		if( (type == Card.FULCRUM || type == Card.REGULAR) && spouseFamily != null ) {
			List<Person> spouses = getSpouses(spouseFamily);
			if( spouses.size() > 1 && withSpouses ) { // Many spouses
				familyNode = new FamilyNode(spouseFamily, false, Side.NONE);
				familyNode.generation = generation;
				familyNode.match = match;
				for( Person spouse : spouses ) {
					if( spouse.equals(person) && !familyNode.partners.contains(personNode) ) {
						familyNode.addPartner(personNode);
					} else {
						PersonNode partnerNode = new PersonNode(gedcom, spouse, Card.REGULAR);
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
	
	/** Create a node starting from a family: used to find direct ancestors and acquired mini ancestry.
	 * @param spouseFamily The spouse family of which create the FamilyNode
	 * @param generation Number of the generation, negative for ancestors
	 * @param type Card REGULAR or ANCESTRY
	 * @return A PersonNode or a FamilyNode
	 */
	private FamilyNode createNodeFromFamily(Family spouseFamily, int generation, Card type) {
		FamilyNode newNode = new FamilyNode(spouseFamily, type == Card.ANCESTRY, Side.NONE);
		newNode.generation = generation;
		List<Person> spouses = getSpouses(spouseFamily);
		for( Person spouse : spouses ) {
			PersonNode personNode = new PersonNode(gedcom, spouse, type);
			personNode.generation = generation;
			newNode.addPartner(personNode);
		}
		newNode.createBond();
		animator.addNode(newNode);
		return newNode;
	}

	/* Create a FamilyNode for following marriages.
	 * @param excluded Person that already has a main marriage
	 */
	private FamilyNode createNextFamilyNode(Family spouseFamily, Person excluded, int generation, Side side, Match match) {
		FamilyNode familyNode = new FamilyNode(spouseFamily, false, side);
		familyNode.generation = generation;
		familyNode.match = match;
		if( withSpouses ) {
			for( Person partner : getSpouses(spouseFamily) ) {
				if( !partner.equals(excluded) ) {
					PersonNode personNode = new PersonNode(gedcom, partner, Card.REGULAR);
					personNode.generation = generation; // Necessario?
					findAcquiredAncestry(personNode);
					familyNode.addPartner(personNode);
				}
			}
		}
		familyNode.createBond();
		animator.addNode(familyNode);
		return familyNode;
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

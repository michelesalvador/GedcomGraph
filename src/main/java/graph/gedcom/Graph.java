package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

import graph.gedcom.Util.Branch;
import graph.gedcom.Util.Card;
import graph.gedcom.Util.Gender;
import graph.gedcom.Util.Match;
import graph.gedcom.Util.Side;

/**
 * Main public class to build the tree diagram, responsible to collect all the relatives around a fulcrum person.
 */
public class Graph {

    public Person fulcrum; // The central person of the diagram
    public int whichFamily; // Which family display if the fulcrum is child in more than one family

    // Settings for public methods with default values
    private int ancestorGenerations = 3; // Generations to display
    private int greatUnclesGenerations = 2; // Uncles and great-uncles. Can't be more than ancestor generations.
    private boolean withSpouses = true;
    private int descendantGenerations = 3;
    private int siblingNephewGenerations = 2; // Siblings and generations of their descendants
    private int uncleCousinGenerations = 2; // Uncles and cousins. First uncle generation overlaps to great-uncle generations.
    private boolean withNumbers = true; // Displays ancestor and descendant little numbers

    private Gedcom gedcom;
    private Animator animator;
    private PersonNode fulcrumNode;
    private Group fulcrumGroup;
    private int maxAbove; // Max upper generation of ancestors (positive number), excluding mini ancestries
    private int maxBelow; // Max generation of descendants, excluding mini progenies
    private boolean leftToRight;

    public Graph() {
        animator = new Animator();
    }

    // Public methods

    public Graph setGedcom(Gedcom gedcom) {
        this.gedcom = gedcom;
        return this;
    }

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

    public Graph displayNumbers(boolean display) {
        withNumbers = display;
        Util.VERTICAL_SPACE_CALC = display ? Util.VERTICAL_SPACE : Util.VERTICAL_SPACE / 2;
        Util.LITTLE_GROUP_DISTANCE_CALC = display ? Util.LITTLE_GROUP_DISTANCE : Util.LITTLE_GROUP_DISTANCE / 2;
        return this;
    }

    public Graph setLayoutDirection(boolean leftToRight) {
        this.leftToRight = leftToRight;
        animator.leftToRight = leftToRight;
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
        animator.initNodes(fulcrumNode, maxAbove, maxBelow, withNumbers);
    }

    // Final displacement of the nodes
    public void placeNodes() {
        animator.placeNodes();
    }

    /**
     * Prepares the diagram starting from fulcrum.
     *
     * @param fulcrum The person that becomes the diagram center
     */
    public void startFrom(Person fulcrum) {
        this.fulcrum = fulcrum;

        // Reset all values
        fulcrumNode = null;
        animator.nodes.clear();
        animator.personNodes.clear();
        animator.bonds.clear();
        animator.groups.clear();
        maxAbove = 0;
        maxBelow = 0;

        // Creates all the nodes of the diagram
        List<Family> fulcrumParents = fulcrum.getParentFamilies(gedcom);
        fulcrumGroup = createGroup(0, false, Branch.NONE);
        if (!fulcrumParents.isEmpty()) {

            // Creation of parent nodes of fulcrum
            if (whichFamily >= fulcrumParents.size())
                whichFamily = fulcrumParents.size() - 1; // To prevent IndexOutOfBoundsException
            else if (whichFamily < 0)
                whichFamily = 0; // To prevent ArrayIndexOutOfBoundsException
            Family parentFamily = fulcrumParents.get(whichFamily);
            boolean parentMini = ancestorGenerations == 0;
            Group firstParentGroup = createGroup(-1, parentMini, null);
            Node parentNode = createNodeFromFamily(parentFamily, -1, parentMini ? Card.ANCESTRY : Card.REGULAR);
            parentNode.isAncestor = true;
            Branch parentBranch = parentNode.getPersonNodes().size() > 1 ? Branch.PATER : Branch.NONE;
            firstParentGroup.branch = parentBranch;
            firstParentGroup.addNode(parentNode);
            if (!parentMini)
                maxAbove = 1;

            PersonNode first = parentNode.getPartner(0);
            PersonNode second = parentNode.getPartner(1);
            boolean femaleAlone = false;
            // Find ancestors on the left
            if (ancestorGenerations > 0) {
                if (first != null) {
                    // Paternal grand-parents
                    findAncestors(first, firstParentGroup, 1);
                    // Paternal uncles
                    findUncles(first, firstParentGroup, parentNode.getPersonNodes().size() == 1 ? Side.LEFT : Side.NONE);
                    // Paternal half-siblings
                    findHalfSiblings(first);
                }
                // Other partners of the second parent
                if (second != null) {
                    findAncestorGenus(second, firstParentGroup, Side.LEFT);
                } // Other partners of a female alone parent
                else if (first != null && Gender.isFemale(first.person)) {
                    findAncestorGenus(first, firstParentGroup, Side.LEFT);
                    femaleAlone = true;
                }
            }

            // Fulcrum with marriages and siblings
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                if (sibling.equals(fulcrum) && fulcrumNode == null) {
                    marriageAndChildren(parentNode, fulcrumGroup);
                } else if (siblingNephewGenerations > 0) {
                    Genus siblingGenus = findPersonGenus(sibling, parentNode, 0, Card.REGULAR, fulcrumGroup);
                    for (Node siblingNode : siblingGenus) {
                        findDescendants(siblingNode, 0, siblingNephewGenerations, false);
                    }
                }
            }

            // Find ancestors on the right
            if (ancestorGenerations > 0) {
                // Single parent with no other marriages
                if (parentNode.getPersonNodes().size() == 1 && parentNode.getPartner(0).person.getSpouseFamilyRefs().size() <= 1) {
                    findUncles(first, firstParentGroup, Side.RIGHT);
                } else {
                    Group secondParentGroup = createGroup(-1, parentMini, Branch.MATER);
                    secondParentGroup.addNode(parentNode);
                    // Other marriages of the first not-female-alone partner
                    if (first != null && !femaleAlone) {
                        findAncestorGenus(first, secondParentGroup, Side.RIGHT);
                    }
                    if (second != null) {
                        // Half-siblings
                        findHalfSiblings(second);
                        // Uncles and grand-parents
                        findAncestors(second, secondParentGroup, 1);
                        // Uncles
                        findUncles(second, secondParentGroup, Side.NONE);
                    }
                }
            }
        } else {
            // Fulcrum without parent family
            marriageAndChildren(null, fulcrumGroup);
        }
    }

    /**
     * Recursive method to generate the nodes of siblings and parents of an ancestor.
     *
     * @param commonNode The node with the person to start from
     * @param group      Where to add the ancestor and their siblings (great-uncles)
     * @param generUp    Number of the generation of the commonNode: 0 for fulcrum, 1 for parents, 2 for grand-parents etc.
     */
    private void findAncestors(PersonNode commonNode, Group group, int generUp) {
        Person ancestor = commonNode.person;
        if (!ancestor.getParentFamilies(gedcom).isEmpty()) {
            List<Family> parentFamilies = ancestor.getParentFamilies(gedcom);
            Family family = parentFamilies.get(parentFamilies.size() - 1);
            int parentGen = generUp + 1;
            boolean parentMini = parentGen > ancestorGenerations;
            if (!parentMini && parentGen > maxAbove)
                maxAbove = parentGen;

            Group firstParentGroup = createGroup(-parentGen, parentMini, null);
            FamilyNode parentNode = createNodeFromFamily(family, -parentGen, parentMini ? Card.ANCESTRY : Card.REGULAR);
            parentNode.isAncestor = true;

            Branch parentBranch = parentNode.getPersonNodes().size() > 1 ? Branch.PATER : Branch.NONE;
            firstParentGroup.branch = parentBranch;
            commonNode.origin = parentNode;
            firstParentGroup.addNode(parentNode);

            // Add the great-uncles (siblings of ancestor) with their spouses
            if (generUp > 1) { // Uncles of the parents generation (1) are found before
                findUncles(commonNode, group, Side.NONE);
            }

            if (parentNode.getPersonNodes().isEmpty()) // Ancestor's parent family without partners
                return;

            // Recall this method
            if (generUp < ancestorGenerations) {
                List<PersonNode> parents = parentNode.getPersonNodes();
                if (parents.size() > 1) {
                    Group secondParentGroup = createGroup(-parentGen, parentMini, Branch.MATER);
                    secondParentGroup.addNode(parentNode);
                    for (int i = 0; i < parents.size(); i++) {
                        PersonNode parent = parents.get(i);
                        if (i == 0) {
                            findAncestorGenus(parent, secondParentGroup, Side.RIGHT);
                            findAncestors(parent, firstParentGroup, parentGen);
                        } else {
                            findAncestorGenus(parent, firstParentGroup, Side.LEFT);
                            findAncestors(parent, secondParentGroup, parentGen);
                        }
                    }
                } else { // Single parent TODO verifica con famiglia antenati SENZA genitori
                    PersonNode parent = parents.get(0);
                    findAncestorGenus(parent, firstParentGroup, Gender.isFemale(parent.person) ? Side.LEFT : Side.RIGHT);
                    findAncestors(parent, firstParentGroup, parentGen);
                }
            }
        }
    }

    /**
     * Finds multiple marriages of direct ancestor.
     */
    void findAncestorGenus(PersonNode personNode, Group group, Side side) {
        Genus genus = new Genus();
        genus.add(personNode.familyNode);
        if (personNode.type == Card.REGULAR && withSpouses) {
            List<Family> families = personNode.person.getSpouseFamilies(gedcom);
            if (families.size() > 1) {
                families.remove(personNode.spouseFamily);
                int generation = personNode.generation;
                for (int i = 0; i < families.size(); i++) {
                    Family nextFamily = families.get(i);
                    Match match = Match.get2(families.size(), side, i);
                    FamilyNode nextFamilyNode = createNextFamilyNode(nextFamily, personNode.person, generation, side, match);
                    nextFamilyNode.children.addAll(nextFamily.getChildren(gedcom));
                    if (side == Side.LEFT) {
                        group.addNode(nextFamilyNode, group.list.indexOf(personNode.familyNode));
                        genus.add(genus.indexOf(personNode.familyNode), nextFamilyNode);
                    } else {
                        group.addNode(nextFamilyNode);
                        genus.add(nextFamilyNode);
                    }
                }
                for (Node node : genus)
                    if (!node.equals(personNode.familyNode)) {
                        if (generation < -1) { // Mini progeny
                            if (withNumbers && -generation <= greatUnclesGenerations + 1)
                                findDescendants(node, generation, 0, false);
                        } else if (siblingNephewGenerations > 0) // Regular descendants
                            findDescendants(node, -1, siblingNephewGenerations + 1, side == Side.LEFT ? true : false);
                    }
            }
        }
    }

    /**
     * Finds uncles and their descendants.
     *
     * @param personNode The fulcrum's ancestor of which to find siblings (the uncles)
     * @param group      The uncles will be put inside this group
     * @param side       In case of Branch.NONE, finds the uncles only to the left or right of the person. Otherwise both (Side.NONE)
     */
    void findUncles(PersonNode personNode, Group group, final Side side) {
        final int generUp = -personNode.generation;
        if (generUp <= greatUnclesGenerations || (generUp == 1 && uncleCousinGenerations > 0)) {
            Node origin = personNode.origin;
            if (origin != null) {
                Branch branch = group.branch;
                Family family = origin.spouseFamily;
                Person person = personNode.person;
                boolean ancestorFound = false;
                for (Person uncle : family.getChildren(gedcom)) {
                    int index = -1; // The uncle will be added at the end of the group
                    if (branch == Branch.NONE) { // Not particulary paternal or maternal uncles (could be on both sides of ancestor)
                        if (uncle.equals(person)) {
                            if (generUp == 1 && side == Side.LEFT)
                                break; // All left uncles found, we can exit the for loop
                            ancestorFound = true;
                        }
                        if (generUp == 1 && side == Side.RIGHT && !ancestorFound)
                            continue; // Continues the for loop to reach the uncles to the right of person
                    }
                    if (!uncle.equals(person)) {
                        // Uncles if visible are always regular, never mini card
                        Genus uncleGenus = findPersonGenus(uncle, origin, -generUp, Card.REGULAR, null);
                        for (Node uncleNode : uncleGenus) {
                            if (branch == Branch.PATER || (branch == Branch.NONE && !ancestorFound))
                                // Uncle will be put before the ancestor node
                                index = group.list.indexOf(personNode.getFamilyNode());
                            group.addNode(uncleNode, index);
                            if (generUp == 1) // Add the cousins and possibly their descendants
                                findDescendants(uncleNode, -1, uncleCousinGenerations, index > -1 ? true : false);
                            else // Mini progeny of great-uncles
                                findDescendants(uncleNode, -generUp, 1, false);

                        }
                    }
                }
            }
        }
    }

    /**
     * In case the acquired spouses are hidden, finds half-siblings of fulcrum.
     *
     * @param parentNode The parent of fulcrum
     */
    private void findHalfSiblings(PersonNode parentNode) {
        if (!withSpouses) {
            for (Person halfSibling : parentNode.children) {
                if (siblingNephewGenerations > 0) {
                    Genus halfSiblingGenus = findPersonGenus(halfSibling, parentNode, 0, Card.REGULAR, fulcrumGroup);
                    for (Node halfSiblingNode : halfSiblingGenus) { // Actually is only one node
                        ((PersonNode)halfSiblingNode).isHalfSibling = true;
                        findDescendants(halfSiblingNode, 0, siblingNephewGenerations, false);
                    }
                }
            }
        }
    }

    /**
     * Finds one or many marriages of fulcrum and their children.
     */
    void marriageAndChildren(Node parentNode, Group group) {
        Genus fulcrumGenus = findPersonGenus(fulcrum, parentNode, 0, Card.FULCRUM, group);
        for (Node node : fulcrumGenus) {
            // + 1 because we start from the generation before
            findDescendants(node, 0, descendantGenerations + 1, false);
        }
    }

    /**
     * Recoursive method to find the descendants.
     *
     * @param commonNode      Node containing the person of whom to find descendants. Can be a PersonNode or a FamilyNode.
     * @param startGeneration Number of the generation of the first 'commonNode': -1 for parents, 0 for fulcrum, 1 for children etc.
     * @param maxGenerations  Limit of the number of generations to search
     * @param toTheLeft       The new group will be placed to the left of fulcrum group
     */
    private void findDescendants(Node commonNode, int startGeneration, int maxGenerations, boolean toTheLeft) {
        if (!commonNode.children.isEmpty()) {
            int generChild = commonNode.generation + 1;
            boolean childMini = generChild >= maxGenerations + startGeneration;
            if (childMini && !withNumbers)
                return;
            if (!childMini && generChild > maxBelow)
                maxBelow = generChild;
            Group childGroup = createGroup(generChild, childMini, Branch.NONE, toTheLeft);
            for (Person child : commonNode.children) {
                Genus childGenus = findPersonGenus(child, commonNode, generChild, childMini ? Card.PROGENY : Card.REGULAR, childGroup);
                if (!childMini) {
                    for (Node childNode : childGenus) {
                        findDescendants(childNode, startGeneration, maxGenerations, false);
                    }
                }
            }
        }
    }

    /**
     * Finds one or multiple marriages of a person. Used for fulcrum, their (half-)siblings, descendants, uncles.
     */
    Genus findPersonGenus(Person person, Node parentNode, int generation, Card type, Group group) {
        Genus genus = new Genus();
        List<Family> families = person.getSpouseFamilies(gedcom);
        if (families.isEmpty() || !withSpouses || type == Card.PROGENY) {
            Node singleNode = createNodeFromPerson(person, null, parentNode, generation, type, Match.SOLE);
            if (type != Card.PROGENY) {
                for (Family family : families) {
                    singleNode.children.addAll(family.getChildren(gedcom));
                }
            }
            if (group != null)
                group.addNode(singleNode);
            genus.add(singleNode);
        } else {
            // Partner position respect the person (wife LEFT, husband RIGHT)
            Side side = Gender.isFemale(person) ? Side.LEFT : Side.RIGHT;
            for (int i = 0; i < families.size(); i++) {
                Family family = families.get(i);
                Match match = Match.get(families.size(), side, i);
                Node partnerNode;
                switch (match) {
                case SOLE: // Lonley marriage
                case NEAR: // First marriage
                    partnerNode = createNodeFromPerson(person, family, parentNode, generation, type, match);
                    break;
                default: // Middle or last marriage
                    partnerNode = createNextFamilyNode(family, person, generation, side, match);
                }
                partnerNode.children.addAll(family.getChildren(gedcom));
                if (group != null)
                    group.addNode(partnerNode);
                genus.add(partnerNode);
            }
        }
        return genus;
    }

    // Finds little ancestry above the acquired spouse
    void findAcquiredAncestry(PersonNode card) {
        card.acquired = true;
        if (withNumbers) {
            List<Family> parentFamilies = card.person.getParentFamilies(gedcom);
            if (!parentFamilies.isEmpty()) {
                Family family = parentFamilies.get(parentFamilies.size() - 1);
                Node ancestry = createNodeFromFamily(family, card.generation - 1, Card.ANCESTRY);
                card.origin = ancestry;
                if (ancestry.getHusband() != null)
                    ancestry.getHusband().acquired = true;
                if (ancestry.getWife() != null)
                    ancestry.getWife().acquired = true;
            }
        }
    }

    /**
     * Creates the container for siblings and their spouses (containing also multiple marriages).
     */
    Group createGroup(int generation, boolean mini, Branch branch) {
        return createGroup(generation, mini, branch, false);
    }

    Group createGroup(int generation, boolean mini, Branch branch, boolean beforeFulcrumGroup) {
        Group group = new Group(generation, mini, branch);
        // Add it to groups list
        if (beforeFulcrumGroup) { // Group of paternal cousins
            int index = animator.groups.indexOf(fulcrumGroup);
            animator.groups.add(index, group);
        } else
            animator.groups.add(group);
        return group;
    }

    /**
     * Creates a PersonNode starting from a person. Possibly creates the FamilyNode and returns it.
     *
     * @param person     The dude to create the node of
     * @param parentNode Node (family or person) origin of this person
     * @param generation The generation of the person: negative above, 0 for fulcrum row, positive below
     * @param type       Fashion of the node: FULCRUM, REGULAR, ANCESTRY or PROGENY
     * @return A PersonNode or FamilyNode
     */
    private Node createNodeFromPerson(Person person, Family spouseFamily, Node parentNode, int generation, Card type, Match match) {
        // Single person
        PersonNode personNode = new PersonNode(gedcom, person, type);
        personNode.generation = generation;
        personNode.origin = parentNode;
        personNode.matches.add(match);
        if (type == Card.FULCRUM)
            fulcrumNode = personNode;

        // Possible family with at least two members
        FamilyNode familyNode = null;
        if ((type == Card.FULCRUM || type == Card.REGULAR) && spouseFamily != null) {
            List<Person> spouses = getSpouses(spouseFamily);
            if (spouses.size() > 1 && withSpouses) { // Many spouses
                familyNode = new FamilyNode(spouseFamily, false, Side.NONE, leftToRight);
                familyNode.generation = generation;
                familyNode.matches.add(match);
                for (Person spouse : spouses) {
                    if (spouse.equals(person) && !familyNode.partners.contains(personNode)) {
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
        if (familyNode != null) {
            animator.addNode(familyNode);
            return familyNode; // familyNode is returned to keep finding descendants
        } else {
            animator.addNode(personNode);
            return personNode;
        }
    }

    /**
     * Creates a FamilyNode starting from a family: used to find direct ancestors and acquired mini ancestry.
     *
     * @param spouseFamily The spouse family of which create the FamilyNode
     * @param generation   Number of the generation, negative for ancestors
     * @param type         Card REGULAR or ANCESTRY
     * @return A PersonNode or a FamilyNode
     */
    private FamilyNode createNodeFromFamily(Family spouseFamily, int generation, Card type) {
        FamilyNode newNode = new FamilyNode(spouseFamily, type == Card.ANCESTRY, Side.NONE, leftToRight);
        newNode.generation = generation;
        if (type == Card.REGULAR || withNumbers) {
            List<Person> spouses = getSpouses(spouseFamily);
            for (Person spouse : spouses) {
                PersonNode personNode = new PersonNode(gedcom, spouse, type);
                personNode.generation = generation;
                if (type == Card.REGULAR) { // Mini ancestry excluded
                    // Adds a match to newNode
                    newNode.matches.add(spouse.getSpouseFamilyRefs().size() > 1 ? Match.NEAR : Match.SOLE);
                    // Adds children to personNode
                    List<Family> otherFamilies = spouse.getSpouseFamilies(gedcom);
                    otherFamilies.remove(spouseFamily);
                    for (Family family : otherFamilies)
                        personNode.children.addAll(family.getChildren(gedcom));
                }
                newNode.addPartner(personNode);
            }
        }
        newNode.createBond();
        animator.addNode(newNode);
        return newNode;
    }

    /**
     * Creates a FamilyNode for following marriages.
     *
     * @param excluded Person that already has a main marriage
     */
    private FamilyNode createNextFamilyNode(Family spouseFamily, Person excluded, int generation, Side side, Match match) {
        FamilyNode familyNode = new FamilyNode(spouseFamily, false, side, leftToRight);
        familyNode.generation = generation;
        familyNode.matches.add(match);
        if (withSpouses) {
            for (Person partner : getSpouses(spouseFamily)) {
                if (!partner.equals(excluded)) {
                    PersonNode personNode = new PersonNode(gedcom, partner, Card.REGULAR);
                    personNode.generation = generation; // Necessary?
                    findAcquiredAncestry(personNode);
                    familyNode.addPartner(personNode);
                }
            }
        }
        familyNode.createBond();
        animator.addNode(familyNode);
        return familyNode;
    }

    /**
     * @return A list of all spouses in a family alternating husbands and wives
     */
    List<Person> getSpouses(Family family) {
        List<Person> persons = new ArrayList<>();
        for (Person husband : family.getHusbands(gedcom))
            persons.add(husband);
        int pos = persons.size() > 0 ? 1 : 0;
        for (Person wife : family.getWives(gedcom)) {
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
            str += " | " + node + " | ";
            str += "\n";
        }
        return str;
    }
}

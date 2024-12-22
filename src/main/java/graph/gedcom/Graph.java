package graph.gedcom;

import java.util.ArrayList;
import java.util.Iterator;
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
    private boolean withDuplicateLines = true; // Displays lines connecting duplicate persons

    private Gedcom gedcom;
    private Animator animator;
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

    public Graph displayDuplicateLines(boolean display) {
        withDuplicateLines = display;
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

    public List<DuplicateLine> getDuplicateLines() {
        return animator.duplicateLines;
    }

    // Preparation of the nodes
    public void initNodes() {
        animator.initNodes(fulcrumGroup, maxAbove, maxBelow, withNumbers);
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
        animator.nodes.clear();
        animator.personNodes.clear();
        animator.bonds.clear();
        animator.groups.clear();
        animator.duplicateLines.clear();
        maxAbove = 0;
        maxBelow = 0;

        // Creates all the nodes of the diagram
        List<Family> parentFamilies = fulcrum.getParentFamilies(gedcom);
        fulcrumGroup = createGroup(0, false, Branch.NONE);
        if (!parentFamilies.isEmpty()) {
            // Creation of parent nodes of fulcrum
            if (whichFamily >= parentFamilies.size())
                whichFamily = parentFamilies.size() - 1; // To prevent IndexOutOfBoundsException
            else if (whichFamily < 0)
                whichFamily = 0; // To prevent ArrayIndexOutOfBoundsException
            Family parentFamily = parentFamilies.get(whichFamily);
            boolean parentMini = ancestorGenerations == 0;
            Group firstParentGroup = null;
            Node parentNode = createNodeFromFamily(parentFamily, -1, parentMini ? Card.ANCESTRY : Card.REGULAR);
            int parentSize = parentNode.getPersonNodes().size();
            PersonNode first = parentNode.getPartner(0);
            PersonNode second = parentNode.getPartner(1);
            boolean parentSiblings = areSiblings(first, second);
            if (parentSize > 0 && ancestorGenerations > 0) {
                parentNode.isAncestor = true;
                if (parentSiblings)
                    second.origin = parentNode;
                firstParentGroup = createGroup(-1, parentMini, null);
                firstParentGroup.branch = parentSize > 1 && !parentSiblings ? Branch.PATER : Branch.NONE;
                firstParentGroup.addNode(parentNode);
                if (!parentMini)
                    maxAbove = 1;
                // Finds relatives of the first parent
                findAncestors(first, firstParentGroup, 1, parentSiblings);
                findUncles(first, firstParentGroup, parentSize == 1 || parentSiblings ? Side.LEFT : Side.NONE);
                findAncestorGenus(first, firstParentGroup, Side.LEFT);
                findHalfSiblings(first, parentFamily, parentSize == 1 ? Side.LEFT : Side.NONE);
            }
            // Fulcrum with marriages and siblings
            Genus fulcrumGenus = findPersonGenus(fulcrum, parentNode, 0, Card.FULCRUM, null);
            for (Person sibling : parentFamily.getChildren(gedcom)) {
                if (sibling.equals(fulcrum)) {
                    for (Node node : fulcrumGenus) {
                        fulcrumGroup.addNode(node);
                        findDescendants(node, 0, descendantGenerations + 1, false); // + 1 because we start from the generation before
                    }
                } else if (siblingNephewGenerations > 0 && !fulcrumGenus.contains(sibling)) {
                    Genus siblingGenus = findPersonGenus(sibling, parentNode, 0, Card.REGULAR, fulcrumGroup);
                    for (Node siblingNode : siblingGenus) {
                        findDescendants(siblingNode, 0, siblingNephewGenerations, false);
                    }
                }
            }
            // Find relatives on the right of fulcrum
            if (parentSize > 0 && ancestorGenerations > 0) {
                if (second == null) { // Single parent
                    // if (first != null /*&& first.person.getSpouseFamilyRefs().size() <= 1*/) {
                    findHalfSiblings(first, parentFamily, Side.RIGHT);
                    findUncles(first, firstParentGroup, Side.RIGHT);
                    // }
                } else if (parentSiblings) { // Sibling parents
                    findHalfSiblings(second, parentFamily, Side.NONE);
                    findAncestors(second, firstParentGroup, 1, true);
                    findAncestorGenus(second, firstParentGroup, Side.RIGHT);
                    findUncles(first, firstParentGroup, Side.RIGHT);
                } else { // Regular two parents
                    Group secondParentGroup = createGroup(-1, parentMini, Branch.MATER);
                    secondParentGroup.addNode(parentNode);
                    findHalfSiblings(second, parentFamily, Side.NONE);
                    findAncestors(second, secondParentGroup, 1, false);
                    findAncestorGenus(second, secondParentGroup, Side.RIGHT);
                    findUncles(second, secondParentGroup, Side.NONE);
                }
            }
        } else {
            // Fulcrum without parent family
            Genus fulcrumGenus = findPersonGenus(fulcrum, null, 0, Card.FULCRUM, fulcrumGroup);
            for (Node node : fulcrumGenus) {
                findDescendants(node, 0, descendantGenerations + 1, false); // + 1 because we start from the generation before
            }
        }
    }

    /**
     * Recursive method to generate the nodes of siblings (great-uncles) and parents of an ancestor.
     *
     * @param commonNode     The node with the person to start from
     * @param group          Where to add the ancestor and their siblings (great-uncles)
     * @param generUp        Number of the generation of the commonNode: 0 for fulcrum, 1 for parents, 2 for grand-parents etc.
     * @param siblingPartner If true, means that commonNode is sibling of their partner
     */
    private void findAncestors(PersonNode commonNode, Group group, int generUp, boolean siblingPartner) {
        // In case commonNode is the second parner and sibling of first one
        List<PersonNode> parners = commonNode.getFamilyNode().getPersonNodes();
        if (siblingPartner && parners.indexOf(commonNode) == 1) {
            commonNode.origin = parners.get(0).origin;
            return;
        }
        if (commonNode.duplicate)
            return;
        List<Family> parentFamilies = commonNode.person.getParentFamilies(gedcom);
        if (!parentFamilies.isEmpty()) {
            Family family = parentFamilies.get(parentFamilies.size() - 1); // Always last family
            int parentGen = generUp + 1;
            boolean parentMini = parentGen > ancestorGenerations;
            Group firstParentGroup = createGroup(-parentGen, parentMini, null);
            FamilyNode parentNode = createNodeFromFamily(family, -parentGen, parentMini ? Card.ANCESTRY : Card.REGULAR);
            commonNode.origin = parentNode;
            // Add the great-uncles (siblings of ancestor) with their spouses
            if (generUp > 1) { // Uncles of the parents generation (1) are found before
                if (commonNode.getFamilyNode().getPersonNodes().size() > 1 && !siblingPartner) {
                    findUncles(commonNode, group, Side.NONE);
                } else {
                    findUncles(commonNode, group, Side.LEFT);
                    findUncles(commonNode, group, Side.RIGHT);
                }
            }
            int parentSize = parentNode.getPersonNodes().size();
            if (parentSize == 0) // Parent family without partners
                return;
            parentNode.isAncestor = true;
            PersonNode first = parentNode.getPartner(0);
            PersonNode second = parentSize > 1 ? parentNode.getPartner(1) : null;
            boolean siblingParents = areSiblings(first, second);
            firstParentGroup.branch = parentSize > 1 && !siblingParents ? Branch.PATER : Branch.NONE;
            firstParentGroup.addNode(parentNode);
            if (parentGen > maxAbove && !parentMini)
                maxAbove = parentGen;
            // Recalls this method
            if (generUp < ancestorGenerations) {
                if (second != null) {
                    // First parent
                    findAncestors(first, firstParentGroup, parentGen, siblingParents);
                    findAncestorGenus(first, firstParentGroup, Side.LEFT); // Other marriages of first
                    // Second parent
                    if (siblingParents) {
                        findAncestors(second, firstParentGroup, parentGen, true);
                        findAncestorGenus(second, firstParentGroup, Side.RIGHT);
                    } else {
                        Group secondParentGroup = createGroup(-parentGen, parentMini, Branch.MATER);
                        secondParentGroup.addNode(parentNode);
                        findAncestors(second, secondParentGroup, parentGen, false);
                        findAncestorGenus(second, secondParentGroup, Side.RIGHT);
                    }
                } else { // Single parent
                    findAncestors(first, firstParentGroup, parentGen, false);
                    findAncestorGenus(first, firstParentGroup, Gender.isFemale(first.person) ? Side.RIGHT : Side.LEFT);
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
        if (personNode.type == Card.REGULAR && withSpouses && !personNode.duplicate) {
            List<Family> families = personNode.person.getSpouseFamilies(gedcom);
            if (families.size() > 1) {
                families.remove(personNode.spouseFamily);
                int generation = personNode.generation;
                for (int i = 0; i < families.size(); i++) {
                    Family nextFamily = families.get(i);
                    Match match = Match.getForAncestors(families.size(), i, side);
                    FamilyNode nextFamilyNode = createNextFamilyNode(nextFamily, personNode.person, generation, side, match, personNode.origin);
                    if (side == Side.LEFT) {
                        group.addNode(nextFamilyNode, group.list.indexOf(personNode.familyNode));
                        genus.add(genus.indexOf(personNode.familyNode), nextFamilyNode);
                    } else {
                        group.addNode(nextFamilyNode, group.list.indexOf(personNode.familyNode) + genus.size());
                        genus.add(nextFamilyNode);

                    }
                }
                for (Node node : genus) {
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
    }

    /**
     * Finds uncles and their descendants.
     *
     * @param personNode The fulcrum's ancestor of which to find siblings (the uncles)
     * @param group      The uncles will be put inside this group
     * @param side       If LEFT or RIGHT, in case of Branch.NONE, finds the uncles only to the left or right of the person node. Otherwise
     *                   (Side.NONE) puts all uncles to one side (Branch.PATER or Branch.MATER).
     */
    void findUncles(PersonNode personNode, Group group, final Side side) {
        final int generUp = -personNode.generation;
        if (generUp <= greatUnclesGenerations || (generUp == 1 && uncleCousinGenerations > 0)) {
            Node origin = personNode.origin;
            if (origin != null) {
                Branch branch = group.branch;
                Person person = personNode.person;
                List<Person> uncles = origin.spouseFamily.getChildren(gedcom);
                int start = 0;
                int end = uncles.size();
                if (branch == Branch.NONE) { // Not particulary paternal or maternal uncles (could be on both sides of ancestor)
                    if (side == Side.LEFT)
                        end = uncles.indexOf(person); // From beginning until ancestor
                    else if (side == Side.RIGHT)
                        start = uncles.indexOf(person) + 1; // From ancestor + 1 to the end
                }
                int position = 0; // To place uncles and their spouses at the beginning
                for (int i = start; i < end; i++) {
                    Person uncle = uncles.get(i);
                    if (!group.contains(uncle)) {
                        // Uncles if visible are always regular, never mini card
                        Genus uncleGenus = findPersonGenus(uncle, origin, -generUp, Card.REGULAR, null);
                        for (Node uncleNode : uncleGenus) {
                            if (branch == Branch.PATER || branch == Branch.NONE && side == Side.LEFT) {
                                group.addNode(uncleNode, position++); // Uncle will be put at the beginning
                            } else { // Maternal uncles at far right
                                group.addNode(uncleNode);
                            }
                            if (generUp == 1) // Add the cousins and possibly their descendants
                                findDescendants(uncleNode, -1, uncleCousinGenerations, position > 0 ? true : false);
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
    private void findHalfSiblings(PersonNode parentNode, Family excluded, Side side) {
        if (!withSpouses) {
            // Finds suitable half-siblings
            List<Person> halfSiblings = new ArrayList<>();
            List<Family> families = parentNode.person.getSpouseFamilies(gedcom);
            int start = 0;
            int end = families.size();
            if (side == Side.LEFT) {
                end = families.indexOf(excluded);
            } else if (side == Side.RIGHT) {
                start = families.indexOf(excluded) + 1;
            }
            for (int i = start; i < end; i++) {
                Family family = families.get(i);
                if (!family.equals(excluded))
                    halfSiblings.addAll(family.getChildren(gedcom));
            }
            // Creates nodes
            for (Person halfSibling : halfSiblings) {
                if (siblingNephewGenerations > 0) {
                    Genus halfSiblingGenus = findPersonGenus(halfSibling, parentNode, 0, Card.REGULAR, fulcrumGroup);
                    for (Node halfSiblingNode : halfSiblingGenus) { // Actually is always only one node
                        ((PersonNode)halfSiblingNode).isHalfSibling = true;
                        findDescendants(halfSiblingNode, 0, siblingNephewGenerations, false);
                    }
                }
            }
        }
    }

    /**
     * Recoursive method to find the descendants.
     *
     * @param commonNode      Node containing the person/family of whom to find descendants. Can be a PersonNode or a FamilyNode.
     * @param startGeneration Number of the generation of the first 'commonNode': -1 for parents, 0 for fulcrum, 1 for children etc.
     * @param maxGenerations  Limit of the number of generations to search
     * @param toTheLeft       The new group will be placed to the left of fulcrum group
     */
    private void findDescendants(Node commonNode, int startGeneration, int maxGenerations, boolean toTheLeft) {
        if (!commonNode.isDuplicate()) {
            // Finds children of commonNode
            List<Person> children = new ArrayList<>();
            if (commonNode.spouseFamily != null) {
                children.addAll(commonNode.spouseFamily.getChildren(gedcom));
            } else {
                for (Family family : ((PersonNode)commonNode).person.getSpouseFamilies(gedcom))
                    children.addAll(family.getChildren(gedcom));
            }
            // Creates descent
            if (!children.isEmpty()) {
                int childGeneration = commonNode.generation + 1;
                boolean childMini = childGeneration >= maxGenerations + startGeneration;
                if (childMini && !withNumbers)
                    return;
                if (!childMini && childGeneration > maxBelow)
                    maxBelow = childGeneration;
                Group childGroup = createGroup(childGeneration, childMini, Branch.NONE, toTheLeft);
                for (Person child : children) {
                    if (child != null) {
                        Genus childGenus = findPersonGenus(child, commonNode, childGeneration, childMini ? Card.PROGENY : Card.REGULAR, childGroup);
                        if (childGenus != null && !childMini) {
                            for (Node childNode : childGenus) {
                                findDescendants(childNode, startGeneration, maxGenerations, false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds one or multiple marriages of a person. Used for fulcrum, their siblings (regular and half), descendants, uncles.
     */
    Genus findPersonGenus(Person person, Node parentNode, int generation, Card type, Group group) {
        Genus genus = new Genus();
        if (group != null && group.contains(person))
            return genus;
        List<Family> families = person.getSpouseFamilies(gedcom);
        if (families.isEmpty() || !withSpouses || type == Card.PROGENY) {
            Node singleNode = createNodeFromPerson(person, null, parentNode, generation, type, Match.MAIN);
            if (group != null)
                group.addNode(singleNode);
            genus.add(singleNode);
        } else {
            // Partners position respect the person (male LEFT, female RIGHT)
            Side side = Gender.isFemale(person) ? Side.RIGHT : Side.LEFT;
            boolean straight = true; // Person inside the MAIN family is gay in the "wrong" spouse role
            if (families.size() > 1) {
                if (side == Side.LEFT && getSpouses(families.get(families.size() - 1), null).indexOf(person) == 1 // Male in the last family
                        || side == Side.RIGHT && getSpouses(families.get(0), null).indexOf(person) == 0) { // Female in the first family
                    straight = false;
                }
            }
            for (int i = 0; i < families.size(); i++) {
                Family family = families.get(i);
                Match match = Match.get(families.size(), i, side, straight);
                Node partnerNode;
                switch (match) {
                case MAIN: // First or unique marriage, or lonley person
                    partnerNode = createNodeFromPerson(person, family, parentNode, generation, type, match);
                    break;
                default: // Near, middle or far marriage
                    partnerNode = createNextFamilyNode(family, person, generation, side, match, parentNode);
                }
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
        personNode.match = match;
        checkForDuplicate(personNode, spouseFamily);
        // Possible family with at least two members
        FamilyNode familyNode = null;
        if ((type == Card.FULCRUM || type == Card.REGULAR) && spouseFamily != null && !personNode.duplicate) {
            List<Person> spouses = getSpouses(spouseFamily, null);
            if (spouses.size() > 1 && withSpouses) { // Many spouses
                familyNode = new FamilyNode(spouseFamily, false, Side.NONE, leftToRight);
                familyNode.generation = generation;
                familyNode.match = match;
                for (Person spouse : spouses) {
                    if (spouse.equals(person) && !familyNode.partners.contains(personNode)) {
                        familyNode.addPartner(personNode);
                    } else {
                        PersonNode partnerNode = new PersonNode(gedcom, spouse, Card.REGULAR);
                        partnerNode.generation = generation;
                        familyNode.addPartner(partnerNode);
                        if (parentNode != null && spouse.getParentFamilies(gedcom).contains(parentNode.spouseFamily)) // They are married siblings
                            partnerNode.origin = parentNode;
                        else
                            findAcquiredAncestry(partnerNode);
                        checkForDuplicate(partnerNode, spouseFamily);
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
            newNode.match = Match.MAIN;
            List<Person> spouses = getSpouses(spouseFamily, null);
            for (Person spouse : spouses) {
                PersonNode personNode = new PersonNode(gedcom, spouse, type);
                personNode.generation = generation;
                checkForDuplicate(personNode, spouseFamily);
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
     * @param excluded   Person that already has a main marriage
     * @param parentNode Useful to check if the created personNode is a married sibling
     */
    private FamilyNode createNextFamilyNode(Family spouseFamily, Person excluded, int generation, Side side, Match match, Node parentNode) {
        FamilyNode familyNode = new FamilyNode(spouseFamily, false, side, leftToRight);
        familyNode.generation = generation;
        familyNode.match = match;
        if (withSpouses) {
            for (Person partner : getSpouses(spouseFamily, excluded)) {
                PersonNode personNode = new PersonNode(gedcom, partner, Card.REGULAR);
                personNode.generation = generation;
                if (parentNode != null && partner.getParentFamilies(gedcom).contains(parentNode.spouseFamily)) { // They are married siblings
                    personNode.origin = parentNode;
                } else {
                    findAcquiredAncestry(personNode);
                }
                familyNode.addPartner(personNode);
                checkForDuplicate(personNode, spouseFamily);
            }
        }
        familyNode.createBond();
        animator.addNode(familyNode);
        return familyNode;
    }

    /**
     * Checks if a person node has duplicates in already existing person nodes.
     *
     * @param spouseFamily Family in which the person is spouse
     */
    private void checkForDuplicate(PersonNode newPersonNode, Family spouseFamily) {
        for (PersonNode oldPersonNode : animator.personNodes) {
            if (!newPersonNode.mini && !oldPersonNode.mini) {
                if (oldPersonNode.person.equals(newPersonNode.person)) {
                    if (spouseFamily == null || oldPersonNode.familyNode == null || oldPersonNode.familyNode.spouseFamily.equals(spouseFamily)) {
                        newPersonNode.duplicate = true;
                    }
                    if (withDuplicateLines)
                        animator.duplicateLines.add(new DuplicateLine(oldPersonNode, newPersonNode));
                } else if (oldPersonNode.generation == newPersonNode.generation && oldPersonNode.familyNode != null
                        && oldPersonNode.familyNode.spouseFamily.equals(spouseFamily)) {
                    // Specific for next marriages
                    newPersonNode.duplicate = true;
                }
            }
        }
    }

    /**
     * Two PersonNodes are children in the same last family.
     */
    private boolean areSiblings(PersonNode first, PersonNode second) {
        if (first != null && second != null) {
            List<Family> firstFamilies = first.person.getParentFamilies(gedcom);
            List<Family> secondFamilies = second.person.getParentFamilies(gedcom);
            if (!firstFamilies.isEmpty() && !secondFamilies.isEmpty()
                    && firstFamilies.get(firstFamilies.size() - 1).equals(secondFamilies.get(secondFamilies.size() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param excluded When provided, maximum one person is returned
     * @return The first one or two husbands and wives of a family, or an empty list
     */
    List<Person> getSpouses(Family family, Person excluded) {
        // Adds spouses
        List<Person> spouses = new ArrayList<>();
        if (!family.getHusbandRefs().isEmpty())
            spouses.add(family.getHusbandRefs().get(0).getPerson(gedcom));
        if (!family.getWifeRefs().isEmpty())
            spouses.add(family.getWifeRefs().get(0).getPerson(gedcom));
        for (int i = 1; i < family.getHusbandRefs().size(); i++)
            spouses.add(family.getHusbandRefs().get(i).getPerson(gedcom));
        for (int i = 1; i < family.getWifeRefs().size(); i++)
            spouses.add(family.getWifeRefs().get(i).getPerson(gedcom));
        // Removes null and excluded
        Iterator<Person> iterator = spouses.iterator();
        while (iterator.hasNext()) {
            Person person = iterator.next();
            if (person == null || person.equals(excluded))
                iterator.remove();
        }
        if (spouses.size() > 2)
            spouses = spouses.subList(0, 2); // Only two persons
        if (excluded != null && spouses.size() == 2) {
            spouses = spouses.subList(0, 1); // Only one person
        }
        return spouses;
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

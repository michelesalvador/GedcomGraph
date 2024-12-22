package graph.gedcom;

import static graph.gedcom.Util.ANCESTRY_DISTANCE;
import static graph.gedcom.Util.HORIZONTAL_SPACE;
import static graph.gedcom.Util.PROGENY_PLAY;
import static graph.gedcom.Util.UNION_DISTANCE;

import java.util.ArrayList;
import java.util.List;

import org.folg.gedcom.model.Family;

import graph.gedcom.Util.Branch;
import graph.gedcom.Util.Match;

/**
 * Abstract class to be extended in a PersonNode or FamilyNode.
 */
public abstract class Node extends Metric {

    public Family spouseFamily; // The person in this node is spouse of this family
    // List of siblings to which this node belongs
    // Warning: an ancestor node can belong at the same time to 2 groups, making this 'group' quite useless
    Group group;
    Group youth; // List of PersonNode and FamilyNode descendants of a PersonNode or of a FamilyNode
    public int generation; // Number of the generation to which this node belongs (0 for fulcrum, negative up and positive down)
    public boolean mini; // The PersonNode will be displayed little (with just a number), and the familyNode without marriage date
    boolean isAncestor;
    boolean marriedSiblings; // Especially for FamilyNode, the two partners are also siblings
    Union union; // The union this node belongs to (expecially for ancestors)
    Node prev, next; // Previous and next node on the same row (same generation)
    Match match; // Position of this node inside possible marriages
    List<Node> origins; // Ordered chain of origins up until generation -1
    float force;

    /**
     * Returns the width from left until the middle of the main node.
     */
    abstract float getLeftWidth(Branch branch);

    /**
     * Alternative to centerX(), called eventually by curve lines, when bond is already in place.
     */
    abstract float simpleCenterX();

    /**
     * Shifts horizontally this node propagating shift on descendants.
     */
    void moveDescending(float shift) {
        setX(x + shift);
        if (youth != null) {
            for (Node child : youth.list) {
                child.moveDescending(shift);
            }
        }
    }

    /**
     * Horizontally distributes progeny nodes.
     */
    void placeYouthX() {
        if (youth != null && !youth.mini) { // Existing regular children only
            youth.placeNodes(centerX());
        }
    }

    /**
     * Horizontally distributes mini progeny nodes.
     */
    void placeMiniChildrenX() {
        if (youth != null && youth.mini) {
            float posX = centerX();
            for (Node child : youth.list) {
                child.x = posX;
                posX += child.width + PROGENY_PLAY;
            }
            youth.updateX();
            youth.setX(youth.x - youth.getWidth() / 2);
        }
    }

    // Position the origin of the acquired spouse
    void placeAcquiredOriginX() {
        if (this instanceof FamilyNode) {
            for (PersonNode partner : ((FamilyNode)this).partners) {
                if (partner.acquired && partner.origin != null) {
                    partner.origin.setX(partner.centerX() - partner.origin.centerRelX());
                }
            }
        }
    }

    void placeAcquiredOriginY() {
        if (this instanceof FamilyNode && !mini) {
            for (PersonNode partner : ((FamilyNode)this).partners) {
                if (partner.acquired && partner.origin != null) {
                    partner.origin.setY(partner.y - ANCESTRY_DISTANCE - partner.origin.height);
                }
            }
        }
    }

    /**
     * Horizontally aligns this mini or empty node over youth.
     */
    void alignMiniEmptyOverYouth() {
        if (youth != null && youth.isOriginMiniOrEmpty()) {
            youth.updateX();
            setX(youth.centerX() - centerRelX());
        }
    }

    /**
     * Applies the shift to this node and propagates the overlap correction to previous or next node.
     */
    void slide(float shift) {
        setX(x + shift);
        if (shift > 0 && next != null) {
            float rightOver = x + width + (union.equals(next.union) ? HORIZONTAL_SPACE : UNION_DISTANCE) - next.x;
            if (rightOver > 0)
                next.slide(rightOver);
        } else if (shift < 0 && prev != null) {
            float leftOver = prev.x + prev.width + (union.equals(prev.union) ? HORIZONTAL_SPACE : UNION_DISTANCE) - x;
            if (leftOver > 0)
                prev.slide(-leftOver);
        }
    }

    // Hybrid methods for FamilyNode and PersonNode

    abstract Node getOrigin();

    /**
     * Returns a list with zero, one or more origin nodes. Excluded mini and empty origins.
     */
    abstract List<Node> getOrigins();

    /**
     * This node contains at least one duplicate PersonNode.
     */
    abstract boolean isDuplicate();

    /**
     * @return FamilyNode if available, otherwise PersonNode.
     */
    abstract Node getFamilyNode();

    abstract List<PersonNode> getPersonNodes();

    // Returns the first partner not acquired [or the first one available]
    abstract PersonNode getMainPersonNode();

    // Softly returns the "husband" (the first parner)
    abstract PersonNode getHusband();

    // Softly returns the "wife" (the second partner) or the first person node available
    abstract PersonNode getWife();

    // Strictly returns the requested partner or null
    abstract PersonNode getPartner(int id);

    /**
     * This node is one of the three matches: NEAR, MIDDLE or FAR.
     */
    boolean isMultiMarriage() {
        return match == Match.FAR || match == Match.MIDDLE || match == Match.NEAR;
    }

    /**
     * Populates origins list.
     */
    public void initializeOrigins() {
        if (generation >= -1 && !mini) {
            origins = new ArrayList<>();
            Node node = this;
            while (node != null && node.generation >= 0) {
                node = node.group.origin;
                if (node != null && !node.mini)
                    origins.add(node);
            }
        }
    }

    float columnShift; // How much to shift horizontally this node and its descendants

    /**
     * Starts from this capital node to move horizontally the descendants column.
     */
    void outdistanceDescendantColumn() {
        if (youth != null && !youth.mini) { // Node with regular descendants
            columnShift = 0;
            // This node itself
            if (prev != null && union.equals(prev.union)) {
                columnShift = prev.x + prev.width + HORIZONTAL_SPACE - x;
            }
            findDescendantColumnShift(this);
            if (columnShift != 0) {
                moveDescending(columnShift);
            }
        }
    }

    /**
     * Searches correct shift (positive or negative) in descendants of the same column. Recursive call.
     *
     * @param node The actual node to investigate the youth left overlap
     */
    private void findDescendantColumnShift(Node node) {
        if (node.youth != null && !node.youth.mini) { // Node with regular descendants
            Node first = node.youth.list.get(0);
            if (first.prev != null && !first.prev.origins.contains(this)) {
                float leftShift = first.prev.x + first.prev.width + UNION_DISTANCE - first.x; // Positive overlap or negative distance
                if (leftShift > columnShift) {
                    columnShift = leftShift;
                }
            }
            for (Node youthNode : node.youth.list) {
                findDescendantColumnShift(youthNode);
            }
        }
    }
}

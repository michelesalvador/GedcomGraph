package graph.gedcom;

import static graph.gedcom.Util.HORIZONTAL_SPACE;
import static graph.gedcom.Util.UNION_DISTANCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores a list of PersonNodes and/or FamilyNodes that move horizontally together, ordered left to right.
 *
 * It may coincide with a Group of descendants, or it may be the result of two Groups of ancestors merged together, two Groups sharing the
 * same ancestor(s) Node.
 */
public class Union extends Metric {

    List<Node> list; // List of PersonNodes and FamilyNodes that move horizontally together
    int generation;
    Node ancestor; // The ancestor node of fulcrum, or fulcrum itself FamilyNode/PersonNode. Null for cousins and descendants.
    Union prev, next; // Previous and next union on the same row (same generation)
    List<Union> descendants; // List of unions descendant of this one, especially for ancestor unions down until generation -1
    List<Group> youths; // Youths belonging to this union, especially for descendant union

    Union(int generation) {
        list = new ArrayList<>();
        this.generation = generation;
    }

    /**
     * For ancestors where there are multiple origins.
     */
    List<Node> getOrigins() {
        if (ancestor != null)
            return ancestor.getOrigins();
        return Collections.emptyList();
    }

    void updateX() {
        x = list.get(0).x;
    }

    /**
     * Populates the list of unions descendant of this union, until generation -1 included.
     */
    public void initializeDescendants() {
        if (generation < 0) {
            descendants = new ArrayList<>();
            Union union = this;
            while (union.generation < -1) {
                union = union.ancestor.youth.list.get(0).union;
                descendants.add(union);
            }
        }
    }

    /**
     * Populates youths list for this union. To be called once.
     */
    public void initializeYouths() {
        youths = new ArrayList<>();
        for (Node node : list) {
            if (node.youth != null && !node.youth.mini)
                youths.add(node.youth);
        }
    }

    /**
     * Removes overlap between two origin unions and propagates the shift on above ancestors.
     */
    void placeOriginsAscending() {
        List<Node> origins = getOrigins();
        if (origins.size() > 1) {
            Union leftUnion = origins.get(0).union;
            Union rightUnion = origins.get(1).union;
            leftUnion.updateX();
            rightUnion.updateX();
            float overlap = leftUnion.x + leftUnion.getWidth() + UNION_DISTANCE - rightUnion.x;
            if (overlap > 0) {
                leftUnion.moveAscending(-overlap / 2);
                rightUnion.moveAscending(overlap / 2);
            }
        }
    }

    void moveAscending(float shift) {
        updateX();
        setX(x + shift);
        if (ancestor != null) { // First cousin does not have ancestor
            for (Node origin : ancestor.getOrigins()) {
                origin.union.moveAscending(shift);
            }
        }
    }

    void moveDescending(float shift) {
        setX(x + shift);
        for (Node node : list) {
            if (node.youth != null) { // TODO && !node.youth.mini ?
                node.youth.moveDescending(shift);
            }
        }
    }

    float columnShift;

    /**
     * Shifts horizontally the ancestor column starting from this basement union.
     */
    void outdistanceAncestorColumn() {
        columnShift = 0;
        // Finds how much to shift this union respect previous union
        ancestor.youth.updateX(); // Useful
        float youthDistance = centerX() - ancestor.youth.centerX();
        if (prev != null && prev.descendants.get(0).equals(descendants.get(0)) && youthDistance > 1) { // youthDistance may have a micro error
            columnShift = prev.x + prev.getWidth() + UNION_DISTANCE - x; // Positive overlap or negative distance
        }
        findAncestorColumnShift(ancestor);
        if (columnShift != 0) {
            moveAscending(columnShift);
        }
    }

    /**
     * Recursive method to find the maximum shift to move the union column.
     */
    private void findAncestorColumnShift(Node node) {
        for (Node origin : node.getOrigins()) {
            Union prev = origin.union.prev;
            if (prev != null && !prev.descendants.contains(this)) {
                float leftShift = prev.x + prev.getWidth() + UNION_DISTANCE - origin.union.x; // Positive overlap or negative distance
                if (leftShift > columnShift) {
                    columnShift = leftShift;
                }
            }
            findAncestorColumnShift(origin);
        }
    }

    /**
     * Returns the shift necessary to align this union exactly between the two origins or under a single origin.
     */
    float alignBetweenOrigins() {
        List<Node> origins = getOrigins();
        if (origins.size() > 1) {
            Node firstOrigin = origins.get(0);
            Node secondOrigin = origins.get(1);
            float origin1X = firstOrigin.centerX();
            float origin2X = secondOrigin.centerX();
            updateX(); // Necessary
            firstOrigin.youth.updateX();
            secondOrigin.youth.updateX();
            float youthsDistance = secondOrigin.youth.centerX() - firstOrigin.youth.centerX();
            float discrepance = origin2X - origin1X - youthsDistance;
            return origin1X - firstOrigin.youth.centerRelX() - x + discrepance / 2;
        } else if (origins.size() > 0) {
            Node origin = origins.get(0);
            if (origin.union != null) {
                updateX();
                origin.youth.updateX();
                return origin.centerX() - origin.youth.centerX();
            }
        }
        return 0;
    }

    /**
     * Places horizontally nodes above their youth and distributes remaining nodes at extremes and in the middle of union.
     */
    void distributeNodesOverYouth() {
        if (!youths.isEmpty()) {
            // Places nodes over youths
            for (Group youth : youths) {
                youth.updateX();
                youth.origin.setX(youth.centerX() - youth.origin.centerRelX());
            }
            // Left nodes
            Node node = youths.get(0).origin;
            while (node.prev != null && node.prev.union.equals(this)) {
                node.prev.setX(node.x - HORIZONTAL_SPACE - node.prev.width);
                node = node.prev;
            }
            // Middle nodes
            for (int i = 0; i < youths.size() - 1; i++) {
                Node left = youths.get(i).origin;
                Node right = youths.get(i + 1).origin;
                node = left.next;
                while (!node.equals(right)) {
                    float space = node.next.x - node.prev.x - node.prev.width - node.width;
                    node.setX(node.prev.x + node.prev.width + space / 2);
                    node = node.next;
                }
            }
            // Right nodes
            node = youths.get(youths.size() - 1).origin;
            while (node.next != null && node.next.union.equals(this)) {
                node.next.setX(node.x + node.width + HORIZONTAL_SPACE);
                node = node.next;
            }
        }
    }

    // Excluded spuses at extremes
    @Override
    public float centerRelX() {
        // Is always ancestor or fulcrum union
        return ancestor.centerX() - x;
    }

    @Override
    public float centerRelY() {
        return getHeight() / 2;
    }

    @Override
    void setX(float x) {
        float diff = x - this.x;
        for (Node node : list) {
            node.setX(node.x + diff);
        }
        this.x = x;
    }

    @Override
    void setY(float y) {
        for (Node node : list) {
            node.setY(y);
        }
        this.y = y;
    }

    // Total width of the union considering all nodes
    float getWidth() {
        Node lastChild = list.get(list.size() - 1);
        if (list.size() == 1)
            width = lastChild.width;
        else
            width = lastChild.x + lastChild.width - x;
        return width;
    }

    float getHeight() {
        float height = 0;
        for (Node node : list)
            height = Math.max(height, node.height);
        return height;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}

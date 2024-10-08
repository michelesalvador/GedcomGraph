package graph.gedcom;

import static graph.gedcom.Util.ANCESTRY_DISTANCE;
import static graph.gedcom.Util.HORIZONTAL_SPACE;
import static graph.gedcom.Util.LITTLE_GROUP_DISTANCE_CALC;

import java.util.ArrayList;
import java.util.List;

import graph.gedcom.Util.Branch;
import graph.gedcom.Util.Match;
import graph.gedcom.Util.Position;

/**
 * List of person nodes: used to store children of an origin with their spouses.
 */
public class Group extends Metric {

    List<Node> list; // List of PersonNodes and FamilyNodes of siblings and brothers-in-law, children of the origin
    Node origin; // Is the same origin of the Person or Family nodes of the list
    Node stallion; // The main (NEAR) node when this group is a multi marriage only, that is without siblings
    int generation;
    boolean mini;
    Branch branch;

    Group(int generation, boolean mini, Branch branch) {
        list = new ArrayList<>();
        this.generation = generation;
        this.mini = mini;
        this.branch = branch;
    }

    // Adds a node to this group and vice-versa
    void addNode(Node node) {
        addNode(node, -1);
    }

    void addNode(Node node, int index) {
        if (index > -1) {
            list.add(index, node);
        } else
            list.add(node);
        // The group of the node
        node.group = this;
    }

    // Sets the origin to this group taking it from the first not-acquired node of the list
    // And sets this group as youth of the origin
    void setOrigin() {
        boolean found = false;
        // For ancestors
        for (Node node : list) {
            if (node.isAncestor && node.getPersonNodes().size() > 1) {
                origin = node.getPartner(branch == Branch.MATER ? 1 : 0).getOrigin();
                found = true;
            }
        }
        // All other groups, fulcrum group included
        if (!found) {
            for (Node node : list) {
                PersonNode personNode = node.getMainPersonNode();
                if (personNode != null) {
                    origin = personNode.getOrigin();
                    break;
                }
            }
        }
        if (origin != null)
            origin.youth = this;
        // Finds the stallion
        stallion = getStallion();
    }

    // Checks if origin is mini or without partners
    boolean isOriginMiniOrEmpty() {
        if (origin != null) {
            if (origin.isMultiMarriage(branch))
                return false;
            return origin.mini || origin.getPersonNodes().isEmpty();
        }
        return false;
    }

    /**
     * If this group is a multimarriage only (without any sibling) returns the NEAR person node, otherwise returns null.
     */
    private Node getStallion() {
        if (origin != null && origin.children.size() > 1) { // Stallion is a single child of origin
            return null;
        }
        Node nearNode = null;
        for (Node node : list) {
            if (!node.isMultiMarriage(branch))
                return null;
            else if (node.getMatch(branch) == Match.NEAR)
                nearNode = node;
        }
        return nearNode;
    }

    /**
     * Horizontally distributes nodes of this group centered to centerX.
     */
    public void placeNodes(float centerX) {
        // Place stallion child and their spouses
        if (stallion != null) {
            stallion.setX(centerX - stallion.getLeftWidth(null));
            Node right = stallion;
            while (right.next != null && right.next.group == this) {
                right.next.setX(right.x + right.width + HORIZONTAL_SPACE);
                right = right.next;
            }
            Node left = stallion;
            while (left.prev != null && left.prev.group == this) {
                left.prev.setX(left.x - HORIZONTAL_SPACE - left.prev.width);
                left = left.prev;
            }
        } else { // Place normal youth
            float posX = centerX - getBasicLeftWidth() - getBasicCentralWidth() / 2;
            for (Node child : list) {
                child.setX(posX);
                posX += child.width + HORIZONTAL_SPACE;
            }
        }
    }

    /**
     * Places horizontaly origin and uncles above this group.
     */
    public void placeAncestors() {
        if (origin != null) {
            placeOriginX();
            Union union = origin.union;
            if (union != null) {
                // Place paternal uncles
                float posX = origin.x;
                for (int i = union.list.indexOf(origin) - 1; i >= 0; i--) {
                    Node node = union.list.get(i);
                    posX -= node.width + HORIZONTAL_SPACE;
                    node.setX(posX);
                }
                // Place maternal uncles
                posX = origin.x + origin.width + HORIZONTAL_SPACE;
                for (int i = union.list.indexOf(origin) + 1; i < union.list.size(); i++) {
                    Node node = union.list.get(i);
                    node.setX(posX);
                    posX += node.width + HORIZONTAL_SPACE;
                }
            }
        }
    }

    /**
     * Places horizontally the origin centered to this group nodes.
     */
    void placeOriginX() {
        if (stallion != null)
            origin.setX(stallion.x + stallion.getLeftWidth(branch) - origin.centerRelX());
        else {
            updateX();
            origin.setX(x + getLeftWidth() + getCentralWidth() / 2 - origin.centerRelX());
        }
    }

    /**
     * Places mini origin or regular origin without partners. Different distance whether this group has one node or multiple nodes.
     */
    void placeOriginY() {
        origin.setY(y - (list.size() > 1 && stallion == null ? LITTLE_GROUP_DISTANCE_CALC : ANCESTRY_DISTANCE) - origin.height);
    }

    void moveDescending(float shift) {
        updateX();
        setX(x + shift);
        for (Node node : list) {
            if (node.youth != null) {
                node.youth.moveDescending(shift);
            }
        }
    }

    // Excluded acquired spouses at right
    @Override
    public float centerRelX() {
        if (stallion != null)
            return stallion.x + stallion.getLeftWidth(branch) - x;
        else
            return getLeftWidth() + getCentralWidth() / 2;
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

    void updateX() {
        x = list.get(0).x;
    }

    // Total width of the group considering all nodes
    float getWidth() {
        Node lastChild = list.get(list.size() - 1);
        width = lastChild.x + lastChild.width - x;
        return width;
    }

    // Fixed left width relative to the first SOLE or NEAR node of the group, including acquired spouses
    float getBasicLeftWidth() {
        float width = 0;
        for (int i = 0; i < list.size(); i++) {
            Node node = list.get(i);
            if (node.getMatch() == Match.SOLE || node.getMatch() == Match.NEAR) {
                width += node.getLeftWidth(branch);
                break;
            } else {
                width += node.width + HORIZONTAL_SPACE;
            }
        }
        return width;
    }

    // Children's center-to-center fixed width excluding acquired spouses at the extremes
    float getBasicCentralWidth() {
        float width = 0;
        if (list.size() > 1) {
            // Width of the first useful node
            int start = 0;
            for (int i = 0; i < list.size(); i++) {
                Node node = list.get(i);
                if (node.getMatch() == Match.SOLE || node.getMatch() == Match.NEAR) {
                    width = node.getMainWidth(Position.FIRST) + HORIZONTAL_SPACE;
                    start = i;
                    break;
                }
            }
            // Width of the last useful node starting from the end
            int end = 0;
            for (int i = list.size() - 1; i > 0; i--) {
                Node node = list.get(i);
                if (node.getMatch() == Match.SOLE || node.getMatch() == Match.NEAR) {
                    width += node.getMainWidth(Position.LAST);
                    end = i;
                    break;
                }
            }
            // Width of nodes in the middle
            for (int i = start + 1; i < end; i++) {
                width += list.get(i).getMainWidth(Position.MIDDLE) + HORIZONTAL_SPACE;
            }
        }
        return width;
    }

    // Not fixed to-center width of the first SOLE or NEAR node from the left
    private float getLeftWidth() {
        for (int i = 0; i < list.size(); i++) {
            Node node = list.get(i);
            if (node.getMatch() == Match.SOLE || node.getMatch() == Match.NEAR) {
                return node.x - list.get(0).x + node.getLeftWidth(branch);
            }
        }
        return 0;
    }

    // Returns the no fixed central width of the group excluding acquired spouses at extremes
    float getCentralWidth() {
        float width = 0;
        if (list.size() > 1) {
            Node first = null;
            for (int i = 0; i < list.size(); i++) {
                first = list.get(i);
                if (first.getMatch() == Match.SOLE || first.getMatch() == Match.NEAR)
                    break;
            }
            Node last = null;
            for (int i = list.size() - 1; i > 0; i--) {
                last = list.get(i);
                if (last.getMatch() == Match.SOLE || last.getMatch() == Match.NEAR)
                    break;
            }
            width = first.getMainWidth(Position.FIRST) + last.x - (first.x + first.width) + last.getMainWidth(Position.LAST);
        }
        return width;
    }

    float getHeight() {
        height = 0;
        for (Node node : list)
            height = Math.max(height, node.height);
        return height;
    }

    @Override
    public String toString() {
        String txt = "";
        txt += list;
        // txt += " " + branch;
        // txt += " " + hashCode();
        return txt;
    }
}

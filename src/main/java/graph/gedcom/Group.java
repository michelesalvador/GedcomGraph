package graph.gedcom;

import static graph.gedcom.Util.ANCESTRY_DISTANCE;
import static graph.gedcom.Util.HORIZONTAL_SPACE;
import static graph.gedcom.Util.LITTLE_GROUP_DISTANCE_CALC;

import java.util.ArrayList;
import java.util.List;

import org.folg.gedcom.model.Person;

import graph.gedcom.Util.Branch;

/**
 * List of person nodes: used to store children of an origin with their spouses.
 */
public class Group extends Metric {

    List<Node> list; // List of PersonNodes and FamilyNodes of siblings and brothers-in-law, children of the origin
    Node origin; // Is the same origin of the Person or Family nodes of the list
    PersonNode first; // First not-aquired personNode of the group
    PersonNode last; // Last not-aquired personNode of the group, may coincide with first
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

    /**
     * Sets the origin to this group taking it from the first not-acquired node of the list. And sets this group as youth of the origin.
     */
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

        // Populates first and last person node
        if (!mini) {
            outer: for (Node node : list) {
                if (node.isAncestor && branch == Branch.MATER && !node.marriedSiblings) {
                    first = node.getWife();
                    break outer;
                } else {
                    for (PersonNode personNode : node.getPersonNodes()) {
                        if (!personNode.acquired) {
                            first = personNode;
                            break outer;
                        }
                    }
                }
            }
            outer: for (int i = list.size() - 1; i >= 0; i--) {
                Node node = list.get(i);
                if (node.isAncestor && branch == Branch.PATER && !node.marriedSiblings) {
                    last = node.getHusband();
                    break outer;
                } else {
                    List<PersonNode> personNodes = node.getPersonNodes();
                    for (int j = personNodes.size() - 1; j >= 0; j--) {
                        PersonNode personNode = personNodes.get(j);
                        if (!personNode.acquired) {
                            last = personNode;
                            break outer;
                        }
                    }
                }
            }
        }
    }

    // Checks if origin is mini or without partners
    boolean isOriginMiniOrEmpty() {
        if (origin != null) {
            if (origin.isMultiMarriage())
                return false;
            return origin.mini || origin.getPersonNodes().isEmpty();
        }
        return false;
    }

    /**
     * Checks if the group list already contains the person.
     */
    boolean contains(Person person) {
        for (Node node : list) {
            for (PersonNode personNode : node.getPersonNodes()) {
                if (personNode.person.equals(person))
                    return true;
            }
        }
        return false;
    }

    /**
     * Horizontally distributes nodes of this group centered to centerX.
     */
    public void placeNodes(float centerX) {
        float posX = centerX - getBasicLeftWidth() - getBasicCentralWidth() / 2;
        for (Node child : list) {
            child.setX(posX);
            posX += child.width + HORIZONTAL_SPACE;
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
        origin.setX(first.centerX() + (last.centerX() - first.centerX()) / 2 - origin.centerRelX());
    }

    /**
     * Places mini origin or regular origin without partners. Different distance whether this group has one node or multiple nodes.
     */
    void placeOriginY() {
        origin.setY(y - (first.equals(last) ? ANCESTRY_DISTANCE : LITTLE_GROUP_DISTANCE_CALC) - origin.height);
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

    @Override
    public float centerRelX() {
        // Acquired spouses included at left, but excluded at right
        return first.centerX() - x + (last.centerX() - first.centerX()) / 2;
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

    /**
     * @return The fixed left width relative to the first MAIN node of the group, including acquired spouses
     */
    private float getBasicLeftWidth() {
        float width = 0;
        for (Node node : list) {
            // if (node.match == Match.MAIN) { // TODO verifica in albero con multimariagges tra fratelli senza zii
            if (node.getPersonNodes().contains(first)) {
                width += node.getLeftWidth(branch);
                break;
            } else {
                width += node.width + HORIZONTAL_SPACE;
            }
        }
        return width;
    }

    /**
     * @return The nodes center-to-center fixed width excluding acquired spouses at the extremes.
     */
    private float getBasicCentralWidth() {
        float width = 0;
        if (!first.equals(last)) {
            Node start = first.getFamilyNode();
            Node end = last.getFamilyNode();
            for (int i = list.indexOf(start); i < list.indexOf(end); i++) {
                width += list.get(i).width + HORIZONTAL_SPACE;
            }
            width = width - start.getLeftWidth(branch) + end.getLeftWidth(branch);
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
        // txt += generation + ": ";
        txt += list;
        // txt += " " + branch;
        // txt += " " + hashCode();
        return txt;
    }
}

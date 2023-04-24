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
    Node ancestor; // The ancestor node of fulcrum, or fulcrum itself FamilyNode/PersonNode. Null for cousins and descendants.

    Union() {
        list = new ArrayList<>();
    }

    // For ancestors where there are multiple origins
    List<Node> getOrigins() {
        if (ancestor != null)
            return ancestor.getOrigins();
        return Collections.emptyList();
    }

    void updateX() {
        x = list.get(0).x;
    }

    // Distributes horizontaly the nodes in between their siblings and position the extreme nodes.
    // Valid for descendants only (from -1 generation included down).
    void distributeSiblings() {
        if (hasChildren()) { // This union has some child
            for (int i = 0; i < list.size(); i++) {
                Node node = list.get(i);
                if (node.youth == null) { // The node has no children
                    // Put the node in the middle of prev and next
                    if (node.prev != null && node.prev.union.equals(this) && node.next != null && node.next.union.equals(this)) {
                        float space = node.next.x - node.prev.x - node.prev.width - node.width;
                        node.setX(node.prev.x + node.prev.width + space / 2);
                    } // First node of union
                    else if (i == 0 && node.next != null) {
                        node.setX(node.next.x - node.width - HORIZONTAL_SPACE);
                    } // Last node of union
                    else if (i == list.size() - 1 && node.prev != null) {
                        node.setX(node.prev.x + node.prev.width + HORIZONTAL_SPACE);
                    }
                }
            }
        }
    }

    boolean hasChildren() {
        for (Node node : list) {
            if (node.youth != null && !node.youth.mini)
                return true;
        }
        return false;
    }

    // Find the left or right space to move respect youth center
    float spaceAround() {
        ancestor.youth.updateX();
        float distance = ancestor.youth.centerX() - centerX();
        if (distance < 0) { // Could move to the left
            Node prev = list.get(0).prev;
            if (prev != null) {
                float left = prev.x + prev.width + UNION_DISTANCE - x;
                return Math.max(left, distance);
            } else
                return distance;
        } else if (distance > 0) { // Could move to the right
            Node next = list.get(list.size() - 1).next;
            if (next != null) {
                float right = next.x - x - getWidth() - UNION_DISTANCE;
                return Math.min(right, distance);
            } else
                return distance;
        }
        return 0;
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

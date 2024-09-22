package graph.gedcom;

import static graph.gedcom.Util.HORIZONTAL_SPACE;
import static graph.gedcom.Util.UNION_DISTANCE;

import java.util.ArrayList;
import java.util.List;

/**
 * List of unions of the same generation, ordered left to right.
 */
public class UnionRow extends ArrayList<Union> {

    int generation;
    float yAxe;
    Node central; // For generation -1 is fulcrum's parents, otherwise more or less the central node

    UnionRow(int generation, float yAxe) {
        this.generation = generation;
        this.yAxe = yAxe;
    }

    void addUnion(Union union) {
        add(union);
    }

    /**
     * Finds central node of the row, for the benefit of resolveOverlap() and maybe others.
     */
    void findCentralNode() {
        if (generation == -1) {
            central = get(0).ancestor; // Parents union is always the only one of the row
        } else {
            List<Node> list = get(size() / 2).list; // More or less the central union
            central = list.get(list.size() / 2); // More or less the central node
        }
    }

    /**
     * Resolves overlaps of all nodes in this row of unions.
     */
    void resolveOverlap() {
        Node left = central;
        while (left.prev != null) {
            float gap = left.union.equals(left.prev.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
            float overlap = left.prev.x + left.prev.width + gap - left.x;
            if (overlap > 0)
                left.prev.slide(-overlap);
            left = left.prev;
        }
        Node right = central;
        while (right.next != null) {
            float gap = right.union.equals(right.next.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
            float overlap = right.x + right.width + gap - right.next.x;
            if (overlap > 0)
                right.next.slide(overlap);
            right = right.next;
        }
    }

    /**
     * Separates each couple of ancestor unions resolving their overlap only.
     */
    void placeOriginsAscending() {
        for (Union union : this) {
            union.placeOriginsAscending();
        }
    }

    /**
     * Shifts horizontally ancestors resolving overlap and also compacting excessive space.
     */
    void outdistanceAncestorColumns() {
        for (Union union : this) {
            union.outdistanceAncestorColumn();
        }
    }

    /**
     * Shifts horizontally descendants resolving overlap and also compacting excessive space.
     */
    void outdistanceDescendantColumns() {
        for (Union union : this) {
            for (Node node : union.list) {
                node.outdistanceDescendantColumn();
            }
            union.distributeNodesOverYouth();
        }
    }

    void placeYouths() {
        for (Union union : this) {
            for (Node node : union.list) {
                node.placeYouthX();
            }
        }
    }

    @Override
    public String toString() {
        String txt = generation + ": <";
        for (Union union : this)
            txt += union + ", ";
        txt = txt.replaceAll(", $", ">");
        return txt;
    }
}

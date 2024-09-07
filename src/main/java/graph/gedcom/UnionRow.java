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

    // Find central node of the row, for the benefit of resolveOverlap()
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
                left.prev.shift(-overlap);
            left = left.prev;
        }
        Node right = central;
        while (right.next != null) {
            float gap = right.union.equals(right.next.union) ? HORIZONTAL_SPACE : UNION_DISTANCE;
            float overlap = right.x + right.width + gap - right.next.x;
            if (overlap > 0)
                right.next.shift(overlap);
            right = right.next;
        }
    }

    /**
     * Horizontaly aligns ancestor unions both to origins and to youth, starting from central union. To be called many times.
     */
    void alignToEverything() {
        // Probably could be done in a simple single loop
        int center = indexOf(central.union);
        for (int i = center; i >= 0; i--) {
            completeAlignToEverything(i);
        }
        for (int i = center + 1; i < size(); i++) {
            completeAlignToEverything(i);
        }
        resolveOverlap();
    }

    private void completeAlignToEverything(int i) {
        Union union = get(i);
        union.updateX(); // Necessary
        float shift = union.alignOverYouth() + union.alignUnderOrigins(true);
        if (shift != 0) {
            union.setX(union.x + shift / 2); // An average of the two values
        }
    }

    /**
     * Final alignement of unions under double origins to remove lines overlap. To be called once.
     */
    void alignUnderOrigins() {
        for (Union union : this) {
            float shift = union.alignUnderOrigins(false);
            if (shift != 0) {
                union.setX(union.x + shift);
            }
        }
        resolveOverlap(); // Sometimes is necessary
    }

    public void placeYouths() {
        for (Union union : this) {
            for (Node node : union.list) {
                Group youth = node.youth;
                if (youth != null && !youth.mini) { // Existing regular children only
                    node.placeYouthX();
                }
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

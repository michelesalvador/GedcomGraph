package graph.gedcom;

import java.util.ArrayList;

class GroupRow extends ArrayList<Group> {

    int generation;

    GroupRow(int generation) {
        this.generation = generation;
    }

    /**
     * Place nodes of this ancestor row.
     */
    void placeAncestors() {
        for (Group group : this) {
            group.placeAncestors();
        }
    }

    @Override
    public String toString() {
        String txt = generation + ": <";
        for (Group group : this)
            txt += group + ", ";
        txt = txt.replaceAll(", $", ">");
        return txt;
    }
}

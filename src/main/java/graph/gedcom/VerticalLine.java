package graph.gedcom;

/**
 * Straight short vertical line below the marriage.
 */
public class VerticalLine extends Line {

    Bond bond;

    public VerticalLine(FamilyNode familyNode) {
        bond = familyNode.bond;
    }

    @Override
    void update() {
        x1 = bond.centerX();
        y1 = bond.centerY();
        x2 = bond.centerX();
        y2 = bond.y + bond.height;
    }
}

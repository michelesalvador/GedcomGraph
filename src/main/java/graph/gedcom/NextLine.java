package graph.gedcom;

import graph.gedcom.Util.Side;

/**
 * Little horizontal continuous line to connect following partners.
 */
public class NextLine extends Line {

    Bond bond;
    PersonNode partner;
    Side side;
    boolean leftToRight;

    public NextLine(FamilyNode familyNode) {
        bond = familyNode.bond;
        partner = familyNode.partners.get(0);
        side = familyNode.side;
        leftToRight = familyNode.leftToRight;
    }

    @Override
    void update() {
        if (bond != null) {
            x1 = bond.centerX();
            y1 = bond.centerY();
            if (leftToRight) {
                x2 = side == Side.LEFT ? bond.x : partner.x; // otherwise partner.centerX();
            } else {
                x2 = side == Side.RIGHT ? bond.x : partner.x;
            }
            y2 = partner.centerY();
        }
    }
}

package graph.gedcom;

import graph.gedcom.Util.Match;
import graph.gedcom.Util.Side;

/**
 * Horizontal continous or dashed line to connect following multiple partners.
 */
public class BackLine extends Line {

    Bond bond;
    Side side;
    Match match;
    boolean noPartners;
    boolean leftToRight;
    FamilyNode node;

    public BackLine(FamilyNode familyNode) {
        bond = familyNode.bond;
        side = familyNode.side;
        match = familyNode.match;
        noPartners = familyNode.partners.isEmpty();
        leftToRight = familyNode.leftToRight;
        node = familyNode;
    }

    @Override
    void update() {
        if (bond != null) {
            if (leftToRight) {
                x1 = side == Side.LEFT ? node.next.x : node.prev.x + node.prev.width;
            } else {
                x1 = side == Side.RIGHT ? node.prev.x : node.next.x + node.next.width;
            }
            y1 = bond.centerY();
            if (leftToRight) {
                if (bond.marriageDate != null)
                    x2 = side == Side.LEFT ? bond.x + bond.width : bond.x;
                else
                    x2 = noPartners && match == Match.MIDDLE ? side == Side.LEFT ? bond.x + bond.overlap : bond.x + bond.width - bond.overlap
                            : bond.centerX();
            } else {
                if (bond.marriageDate != null)
                    x2 = side == Side.RIGHT ? bond.x + bond.width : bond.x;
                else
                    x2 = noPartners && match == Match.MIDDLE ? side == Side.RIGHT ? bond.x + bond.overlap : bond.x + bond.width - bond.overlap
                            : bond.centerX();
            }
            y2 = bond.centerY();
        }
    }
}

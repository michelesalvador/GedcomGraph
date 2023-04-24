package graph.gedcom;

import graph.gedcom.Util.Match;
import graph.gedcom.Util.Side;

/**
 * Horizontal dashed line to connect following multiple partners.
 */
public class BackLine extends Line {

    Bond bond;
    Side side;
    Match match;
    boolean noPartners;
    FamilyNode node;

    public BackLine(FamilyNode familyNode) {
        bond = familyNode.bond;
        side = familyNode.side;
        match = familyNode.getMatch();
        noPartners = familyNode.partners.isEmpty();
        node = familyNode;
    }

    @Override
    void update() {
        if (bond != null) {
            x1 = side == Side.LEFT ? node.next.x : node.prev.x + node.prev.width;
            y1 = bond.centerY();
            if (bond.marriageDate != null)
                x2 = side == Side.LEFT ? bond.x + bond.width : bond.x;
            else
                x2 = noPartners && match == Match.MIDDLE ? side == Side.LEFT ? bond.x + bond.overlap : bond.x + bond.width - bond.overlap
                        : bond.centerX();
            y2 = bond.centerY();
        }
    }
}

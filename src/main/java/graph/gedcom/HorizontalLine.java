package graph.gedcom;

/**
 * Straight short horizontal line between partners.
 */
public class HorizontalLine extends Line {

    PersonNode leftPerson, rightPerson;
    boolean leftToRight;

    public HorizontalLine(FamilyNode familyNode) {
        leftPerson = familyNode.partners.get(0);
        rightPerson = familyNode.partners.get(1);
        leftToRight = familyNode.leftToRight;
    }

    @Override
    void update() {
        x1 = leftToRight ? leftPerson.x + leftPerson.width : leftPerson.x;
        y1 = leftPerson.centerY();
        x2 = leftToRight ? rightPerson.x : rightPerson.x + rightPerson.width;
        y2 = rightPerson.centerY();
    }
}

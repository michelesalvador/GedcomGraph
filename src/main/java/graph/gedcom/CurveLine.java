package graph.gedcom;

/**
 * Long curve line between family and child or between person and child.
 */
public class CurveLine extends Line {

    Node origin;
    PersonNode personNode;

    public CurveLine(PersonNode personNode) {
        this.personNode = personNode;
        origin = personNode.origin;
    }

    @Override
    void update() {
        x1 = origin.simpleCenterX();
        y1 = origin.y + origin.height;
        x2 = personNode.centerX();
        y2 = personNode.y;
    }
}

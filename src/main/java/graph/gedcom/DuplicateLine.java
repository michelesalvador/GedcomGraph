package graph.gedcom;

import graph.gedcom.Util.Gender;

/**
 * Line connecting two duplicated persons.
 */
public class DuplicateLine extends Line {

    private PersonNode firstNode;
    private PersonNode secondNode;
    public Gender gender;
    public float x3, y3;

    public DuplicateLine(PersonNode firstNode, PersonNode secondNode) {
        this.firstNode = firstNode;
        this.secondNode = secondNode;
        gender = Gender.getGender(firstNode.person);
    }

    @Override
    void update() {
        float shift = 1.5F; // To make line better overlap with curved corner
        // Curved bottom line to connect nodes at the same generation
        if (firstNode.generation == secondNode.generation) {
            if (secondNode.x > firstNode.x) {
                x1 = firstNode.x + firstNode.width - shift;
                x2 = secondNode.x + shift;
            } else {
                x1 = firstNode.x + shift;
                x2 = secondNode.x + secondNode.width - shift;
            }
            y1 = firstNode.y + firstNode.height - shift;
            y2 = secondNode.y + secondNode.height - shift;
            x3 = x1 + (x2 - x1) / 2;
            y3 = Math.max(y1, y2) + Math.min(Util.VERTICAL_SPACE_CALC, Math.abs(x2 - x1));
        } else { // Straight line between nodes at different generations
            if (firstNode.x < secondNode.x) {
                if (secondNode.x > firstNode.x + firstNode.width) { // Line between corners
                    x1 = firstNode.x + firstNode.width - shift;
                    x2 = secondNode.x + shift;
                } else { // Almost horizontaly aligned
                    x1 = firstNode.x + firstNode.width / 4 * 3;
                    x2 = secondNode.x + secondNode.width / 4;
                }
            } else {
                if (firstNode.x > secondNode.x + secondNode.width) {
                    x1 = firstNode.x + shift;
                    x2 = secondNode.x + secondNode.width - shift;
                } else {
                    x1 = firstNode.x + firstNode.width / 4;
                    x2 = secondNode.x + secondNode.width / 4 * 3;
                }
            }
            if (firstNode.y < secondNode.y) {
                y1 = firstNode.y + firstNode.height - shift;
                y2 = secondNode.y + shift;
            } else {
                y1 = firstNode.y + shift;
                y2 = secondNode.y + secondNode.height - shift;
            }
            x3 = x1 + (x2 - x1) / 2;
            y3 = y1 + (y2 - y1) / 2;
        }
    }
}

package graph.gedcom;

/**
 * The connection between two partners belonging to the same family.
 */
public class Bond extends Metric {

    public String marriageDate;
    public FamilyNode familyNode;
    float overlap;

    Bond(FamilyNode familyNode) {
        this.familyNode = familyNode;
    }

    @Override
    public float centerRelX() {
        return width / 2;
    }

    @Override
    public float centerRelY() {
        return height / 2;
    }

    @Override
    void setX(float x) {
        this.x = x - overlap;
    }

    @Override
    void setY(float y) {
        this.y = y;
    }

    // Simple solution to retrieve the marriage year.
    // Family Gem uses another much more complex.
    public String marriageYear() {
        String year = "";
        if (marriageDate != null) {
            if (marriageDate.lastIndexOf(' ') > 0)
                year = marriageDate.substring(marriageDate.lastIndexOf(' '));
            else
                year = marriageDate;
        }
        return year;
    }

    @Override
    public String toString() {
        String txt = marriageYear();
        return txt.isEmpty() ? "-•-" : txt;
    }
}

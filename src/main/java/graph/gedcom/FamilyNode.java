package graph.gedcom;

import static graph.gedcom.Util.BOND_WIDTH;
import static graph.gedcom.Util.MARRIAGE_INNER_WIDTH;
import static graph.gedcom.Util.MINI_BOND_WIDTH;

import java.util.ArrayList;
import java.util.List;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;

import graph.gedcom.Util.Branch;
import graph.gedcom.Util.Match;
import graph.gedcom.Util.Position;
import graph.gedcom.Util.Side;

/**
 * Container of the parners (PersonNode) and the link between them (Bond).
 *
 * Being a Node, can be the origin of a youth Group.
 */
public class FamilyNode extends Node {

    List<PersonNode> partners;
    Bond bond;
    Side side; // Following or previous marriage: LEFT is a husband, RIGHT is a wife
    boolean leftToRight; // Not used here but passed to lines

    public FamilyNode(Family spouseFamily, boolean mini, Side side, boolean leftToRight) {
        super();
        this.spouseFamily = spouseFamily;
        this.mini = mini;
        this.side = side;
        this.leftToRight = leftToRight;
        partners = new ArrayList<>();
    }

    @Override
    Node getOrigin() {
        PersonNode mainPerson = getMainPersonNode();
        if (mainPerson != null)
            return mainPerson.origin;
        return null;
    }

    @Override
    List<Node> getOrigins() {
        List<Node> origins = new ArrayList<>();
        for (PersonNode partner : partners) {
            if (partner.origin != null && !partner.origin.mini)
                origins.add(partner.origin);
        }
        return origins;
    }

    @Override
    boolean hasOrigins() {
        for (PersonNode partner : partners) {
            if (partner.origin != null && !partner.origin.mini)
                return true;
        }
        return false;
    }

    @Override
    FamilyNode getFamilyNode() {
        return this;
    }

    @Override
    List<PersonNode> getPersonNodes() {
        return partners;
    }

    @Override
    PersonNode getMainPersonNode() {
        for (PersonNode partner : partners) {
            if (!partner.acquired)
                return partner;
        }
        return null;
    }

    @Override
    PersonNode getHusband() {
        if (!partners.isEmpty())
            return partners.get(0);
        return null;
    }

    @Override
    PersonNode getWife() {
        if (partners.size() > 1)
            return partners.get(1);
        else if (!partners.isEmpty())
            return partners.get(0);
        return null;
    }

    @Override
    PersonNode getPartner(int id) {
        if (partners.size() > id)
            return partners.get(id);
        return null;
    }

    @Override
    Match getMatch(Branch branch) {
        if (matches.size() > 1 && branch == Branch.PATER)
            return matches.get(1);
        else if (matches.size() > 0)
            return matches.get(0);
        return null;
    }

    // Add a spouse to this family
    void addPartner(PersonNode partner) {
        this.partners.add(partner);
        partner.familyNode = this;
        partner.spouseFamily = spouseFamily;
    }

    // Crate bond if there are no partners or many partners
    void createBond() {
        if (partners.size() == 1 && getMatch() != Match.MIDDLE && getMatch() != Match.FAR)
            return;
        bond = new Bond(this);
        if (!mini && partners.size() > 0) {
            // GEDCOM date of the marriage
            for (EventFact ef : spouseFamily.getEventsFacts()) {
                if (ef.getTag().equals("MARR"))
                    bond.marriageDate = ef.getDate();
            }
        }
    }

    // If this node has children
    boolean hasChildren() {
        if (mini) // Acquired mini ancestry don't have youth but they appear to have
            return true;
        return youth != null;
    }

    @Override
    public float centerRelX() {
        if (partners.isEmpty() || side == Side.RIGHT)
            return bond.width / 2;
        else if (partners.size() > 1 || side == Side.LEFT)
            return partners.get(0).width + getBondWidth() / 2;
        else
            return partners.get(0).width / 2;
    }

    @Override
    public float centerRelY() {
        return height / 2;
    }

    @Override
    float simpleCenterX() {
        if (bond != null) {
            return bond.x + bond.width / 2;
        } else {
            return x + width / 2;
        }
    }

    // Places partners and bond
    @Override
    void setX(float x) {
        force += x - this.x;
        this.x = x;
        if (partners.isEmpty()) { // Mini ancestry without partners
            bond.setX(x);
        } else if (side == Side.RIGHT) { // Next marriage with wife
            bond.x = x;
            partners.get(0).x = x + bond.width - bond.overlap;
        } else {
            for (int i = 0; i < partners.size(); i++) {
                PersonNode partner = partners.get(i);
                if (i == 0) {
                    partner.x = x;
                    x += partner.width;
                    if (bond != null) {
                        bond.setX(x);
                        x += getBondWidth();
                    }
                } else {
                    partner.x = x;
                }
            }
        }
    }

    @Override
    void setY(float y) {
        this.y = y;
        for (PersonNode partner : partners) {
            partner.setY(centerY() - partner.centerRelY());
        }
        if (bond != null)
            bond.y = y;
    }

    @Override
    float getLeftWidth(Branch branch) {
        if ((branch == Branch.MATER || partners.indexOf(getMainPersonNode()) > 0) && partners.size() > 1) { // Is wife
            return getPartner(0).width + getBondWidth() + getPartner(1).centerRelX();
        } else if (!partners.isEmpty()) { // Is husband
            return getPartner(0).centerRelX();
        }
        return 0;
    }

    @Override
    float getMainWidth(Position pos) {
        float size = 0;
        int index = partners.indexOf(getMainPersonNode());
        if (pos == Position.FIRST) {
            if (group.branch == Branch.MATER) {
                size = getWife().centerRelX();
            } else if (getMainPersonNode() != null) {
                size = getMainPersonNode().centerRelX();
                if (index == 0 && partners.size() > 1) // Is husband
                    size += getBondWidth() + getPartner(1).width;
            }
        } else if (pos == Position.LAST && getMainPersonNode() != null) {
            size = getMainPersonNode().centerRelX();
            if (index > 0) // Is wife
                size += getPartner(0).width + getBondWidth();
        } else { // The complete width
            size = width;
        }
        return size;
    }

    // Bond width excluding overlapping
    float getBondWidth() {
        if (bond != null)
            return mini ? MINI_BOND_WIDTH : bond.marriageDate != null ? MARRIAGE_INNER_WIDTH : BOND_WIDTH;
        return 0;
    }

    @Override
    public String toString() {
        String txt = "";
        // for (Match match : matches) txt += match + " ";
        txt += "{";
        for (PersonNode personNode : partners)
            txt += personNode + ", ";
        if (txt.lastIndexOf(", ") > 0)
            txt = txt.replaceAll(", $", "");
        // txt += " " + hashCode();
        // txt += " " + group.branch;
        txt += "}";
        return txt;
    }
}

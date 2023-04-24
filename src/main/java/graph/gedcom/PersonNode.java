package graph.gedcom;

import static graph.gedcom.Util.essence;

import java.util.ArrayList;
import java.util.List;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

import graph.gedcom.Util.Branch;
import graph.gedcom.Util.Card;
import graph.gedcom.Util.Match;
import graph.gedcom.Util.Position;

public class PersonNode extends Node {

    private Gedcom gedcom;
    public Person person;
    public Node origin; // The FamilyNode or PersonNode which this person was born from
    FamilyNode familyNode; // The FamilyNode in which this person is spouse. Null for singles.
    Card type; // Size and function
    public boolean acquired; // Is this person acquired spouse (not blood relative)?
    public boolean dead;
    public int amount; // Number to display in little ancestry or progeny
    CurveLine line; // Curve line connecting this person with the origin above

    // Creates a node rappresentig a person
    public PersonNode(Gedcom gedcom, Person person, Card type) {
        super();
        this.gedcom = gedcom;
        this.person = person;
        this.type = type;
        if (type == Card.FULCRUM || type == Card.REGULAR) {
            if (isDead())
                dead = true;
        } else if (type == Card.ANCESTRY) {
            amount = 1;
            countAncestors(person);
            mini = true;
        } else if (type == Card.PROGENY) {
            amount = 1;
            countDescendants(gedcom);
            mini = true;
        }
    }

    @Override
    Node getOrigin() {
        return origin;
    }

    @Override
    List<Node> getOrigins() {
        List<Node> origins = new ArrayList<>();
        if (origin != null)
            origins.add(origin);
        return origins;
    }

    @Override
    boolean hasOrigins() {
        return origin != null;
    }

    @Override
    FamilyNode getFamilyNode() {
        return familyNode;
    }

    @Override
    List<PersonNode> getPersonNodes() {
        List<PersonNode> persons = new ArrayList<>();
        persons.add(this);
        return persons;
    }

    @Override
    PersonNode getMainPersonNode() {
        return this;
    }

    @Override
    PersonNode getHusband() {
        if (familyNode != null)
            return familyNode.getHusband();
        return this;
    }

    @Override
    PersonNode getWife() {
        if (familyNode != null)
            return familyNode.getWife();
        return this;
    }

    @Override
    PersonNode getPartner(int id) {
        if (familyNode != null)
            return familyNode.getPartner(id);
        else if (id == 0) // Single person
            return this;
        return null;
    }

    @Override
    Match getMatch(Branch branch) {
        return matches.get(0);
    }

    // Recoursive count of direct ancestors
    private void countAncestors(Person ancestor) {
        if (amount <= 100) {
            for (Family family : ancestor.getParentFamilies(gedcom)) {
                for (Person father : family.getHusbands(gedcom)) {
                    amount++;
                    countAncestors(father);
                }
                for (Person mother : family.getWives(gedcom)) {
                    amount++;
                    countAncestors(mother);
                }
            }
        }
    }

    // Recoursive count of direct descendants
    void countDescendants(Gedcom gedcom) {
        this.gedcom = gedcom;
        recoursiveCountDescendants(person);
    }

    private void recoursiveCountDescendants(Person person) {
        if (amount <= 100) {
            for (Family family : person.getSpouseFamilies(gedcom))
                for (Person child : family.getChildren(gedcom)) {
                    amount++;
                    recoursiveCountDescendants(child);
                }
        }
    }

    // Check if this person is dead or buried
    private boolean isDead() {
        for (EventFact fact : person.getEventsFacts()) {
            if (fact.getTag().equals("DEAT") || fact.getTag().equals("BURI"))
                return true;
        }
        return false;
    }

    public boolean isFulcrumNode() {
        return type == Card.FULCRUM;
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
        force += x - this.x;
        this.x = x;
    }

    @Override
    void setY(float y) {
        this.y = y;
    }

    @Override
    float getMainWidth(Position pos) {
        if (pos == Position.MIDDLE)
            return width;
        else
            return centerRelX();
    }

    @Override
    float getLeftWidth(Branch branch) {
        return centerRelX();
    }

    @Override
    public String toString() {
        if (mini)
            return amount + " (" + essence(person) + ")";
        else {
            String txt = "";
            // txt += " " + Math.floor(force);
            // txt += " " + generation;
            // txt += " " + getMatch();
            txt += " " + essence(person);
            // txt += " *" + origin + "*";
            // txt += " " + person.getId();
            // txt += " " + (group != null ? group : "null"); // Produce stackoverflow
            // txt += " " + (group != null ? group.branch : "group=null");
            return txt.trim();
        }
    }
}

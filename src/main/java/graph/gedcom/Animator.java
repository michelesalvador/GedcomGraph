package graph.gedcom;

import static graph.gedcom.Util.BOND_WIDTH;
import static graph.gedcom.Util.MARRIAGE_INNER_WIDTH;
import static graph.gedcom.Util.MARRIAGE_WIDTH;
import static graph.gedcom.Util.MINI_BOND_WIDTH;
import static graph.gedcom.Util.PROGENY_DISTANCE;
import static graph.gedcom.Util.VERTICAL_SPACE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import graph.gedcom.Util.Match;
import graph.gedcom.Util.Side;

/**
 * Sets the vertical and horizontal position of nodes and lines.
 */
public class Animator {

    float width, height;
    PersonNode fulcrumNode;
    int maxAbove;
    List<Node> nodes; // All person and family nodes regular and mini
    List<PersonNode> personNodes;
    List<Bond> bonds;
    List<Line> lines; // All the continuous lines ordered from left to right
    List<LineRow> lineRows; // All the continuous lines divided in groups in a 2D array
    List<Set<Line>> lineGroups; // All the continuous lines distributed in groups by proximity
    List<Line> backLines; // All the back (dashed) lines ordered from left to right
    List<LineRow> backLineRows; // All the back (dashed) lines divided in groups in a 2D array
    List<Set<Line>> backLineGroups; // All the back (dashed) lines distributed in groups by proximity
    List<Group> groups; // Array of groups of PersonNodes and FamilyNodes (not mini)
    List<GroupRow> groupRows;
    List<UnionRow> unionRows; // Array of rows of Unions
    float maxBitmapSize;
    float biggestPathSize;

    Animator() {
        nodes = new ArrayList<>();
        personNodes = new ArrayList<>();
        bonds = new ArrayList<>();
        lines = new ArrayList<>();
        lineRows = new ArrayList<>();
        lineGroups = new ArrayList<>();
        backLines = new ArrayList<>();
        backLineRows = new ArrayList<>();
        backLineGroups = new ArrayList<>();
        groups = new ArrayList<>();
        groupRows = new ArrayList<>();
        unionRows = new ArrayList<>();
    }

    void addNode(Node newNode) {
        // Add node to nodes
        nodes.add(newNode);

        // Add partners to personNodes list
        personNodes.addAll(newNode.getPersonNodes());
    }

    // Preparing the nodes
    void initNodes(PersonNode fulcrumNode, int maxAbove, int maxBelow) {

        this.fulcrumNode = fulcrumNode;
        this.maxAbove = maxAbove;
        width = 0;
        height = 0;
        biggestPathSize = 0;

        // Array with max height of each row of nodes
        int totalRows = maxAbove + 1 + maxBelow;
        float[] rowMaxHeight = new float[totalRows];

        for (Node node : nodes) {
            // Calculate sizes of each family node
            if (node instanceof FamilyNode) {
                FamilyNode familyNode = (FamilyNode)node;
                for (Node partner : familyNode.partners) {
                    familyNode.width += partner.width;
                    familyNode.height = Math.max(familyNode.height, partner.height);
                }
                if (familyNode.height == 0) // Regular or mini ancestor without partners
                    familyNode.height = 20;
                // Bond sizes
                Bond bond = familyNode.bond;
                if (bond != null) {
                    float bondWidth = familyNode.mini ? MINI_BOND_WIDTH : bond.marriageDate != null ? MARRIAGE_WIDTH : BOND_WIDTH;
                    bond.width = bondWidth;
                    bond.height = familyNode.height;
                    if (bond.marriageDate != null) {
                        bond.overlap = (MARRIAGE_WIDTH - MARRIAGE_INNER_WIDTH) / 2;
                        if (familyNode.side == Side.LEFT || familyNode.side == Side.RIGHT)
                            familyNode.width += bond.overlap;
                    }
                    bonds.add(bond);
                    familyNode.width += familyNode.getBondWidth();
                }
            }
            // Discover the maximum height of rows
            if (!node.mini && node.height > rowMaxHeight[node.generation + maxAbove])
                rowMaxHeight[node.generation + maxAbove] = node.height;
        }

        // Vertical position of the generation rows
        unionRows.clear();
        groupRows.clear();
        float posY = rowMaxHeight[0] / 2;
        for (int gen = -maxAbove; gen < totalRows - maxAbove; gen++) {
            unionRows.add(new UnionRow(gen, posY));
            groupRows.add(new GroupRow(gen));
            if (gen + maxAbove < totalRows - 1)
                posY += rowMaxHeight[gen + maxAbove] / 2 + VERTICAL_SPACE + rowMaxHeight[gen + maxAbove + 1] / 2;
        }

        // Initialize the relation between groups and their origin
        for (Group group : groups) {
            group.setOrigin();
        }

        // Create the Lines
        lines.clear();
        backLines.clear();
        for (Node node : nodes) {
            // Curve lines
            for (PersonNode personNode : node.getPersonNodes()) {
                if (personNode.getOrigin() != null) {
                    lines.add(new CurveLine(personNode));
                }
            }
            // All other lines
            if (node instanceof FamilyNode) {
                FamilyNode familyNode = (FamilyNode)node;
                if (familyNode.partners.size() > 0 && (familyNode.getMatch() == Match.MIDDLE || familyNode.getMatch() == Match.FAR))
                    lines.add(new NextLine(familyNode));
                else if (familyNode.partners.size() > 1 && familyNode.bond.marriageDate == null)
                    lines.add(new HorizontalLine(familyNode));
                if (familyNode.getMatch() == Match.MIDDLE || familyNode.getMatch() == Match.FAR)
                    backLines.add(new BackLine(familyNode));
                if (familyNode.hasChildren() && familyNode.bond != null)
                    lines.add(new VerticalLine(familyNode));
            }
        }

        // Populate groupRows from groups
        for (Group group : groups) {
            if (!group.mini && !group.list.isEmpty()) {
                groupRows.get(group.generation + maxAbove).add(group);
            }
        }

        // Add prev and next to each node
        for (GroupRow row : groupRows) {
            Node previous = null;
            for (Group group : row) {
                for (int n = 0; n < group.list.size(); n++) {
                    Node node = group.list.get(n);
                    if (!(node.getPersonNodes().isEmpty() && node.isAncestor)) { // Empty ancestors are excluded
                        if (!node.equals(previous))
                            node.prev = previous;
                        if (node.prev != null)
                            node.prev.next = node;
                        previous = node;
                    }
                }
            }
        }

        // Populate unionRows from groups
        // Couples of ancestors groups with common ancestor node are joined in a single Union
        for (Group group : groups) {
            if (!group.mini && !group.list.isEmpty()) {
                UnionRow row = unionRows.get(group.generation + maxAbove);
                Union union = null;
                boolean joinExistingGroup = false;
                find: for (Node node : group.list) {
                    if (node.isAncestor) {
                        // Search an existing union to join
                        for (Union un : row) {
                            if (node.equals(un.ancestor)) {
                                union = un;
                                joinExistingGroup = true;
                                break find;
                            }
                        }
                        union = new Union();
                        union.ancestor = node;
                        break;
                    }
                }
                if (union == null) { // Union without ancestor
                    union = new Union();
                }
                // Add the group to the union
                if (joinExistingGroup) { // Already populated union
                    for (Node node : group.list) {
                        if (!node.equals(union.ancestor)) { // Avoid duplicated ancestor node
                            union.list.add(node); // Add always at the end because groups are well ordered
                        }
                    }
                } else { // Empty union
                    union.list.addAll(group.list);
                    row.addUnion(union);
                }
                for (Node node : union.list) { // TODO A ben guardare cicla 2 volte in metà della stessa union
                    node.union = union;
                    // Exceptionally also fulcrum family/person node will be putted in
                    // union.ancestor
                    PersonNode main = node.getMainPersonNode();
                    if (main != null && main.isFulcrumNode())
                        union.ancestor = node;
                }
            }
        }

        // Find the central node of each union row
        for (UnionRow row : unionRows) {
            row.findCentralNode();
        }
    }

    // At this point marriage bonds are added in the layout

    // Final position of all nodes, resolving all the overlaps
    void placeNodes() {

        /* Vertical positioning */

        // Vertically place each regular (not mini) node
        for (UnionRow row : unionRows) {
            for (Union union : row) {
                union.y = row.yAxe - union.centerRelY();
                for (Node node : union.list) {
                    node.setY(row.yAxe - node.centerRelY());
                }
            }
        }

        // Places vertically mini origins and mini youths
        for (Group group : groups) {
            if (!group.mini && group.isOriginMiniOrEmpty()) {
                group.y = unionRows.get(group.generation + maxAbove).yAxe - group.centerRelY();
                group.placeOriginY();
            }
            for (Node node : group.list) {
                node.placeAcquiredOriginY();
                // Set youth y position
                Group youth = node.youth;
                if (youth != null && youth.mini)
                    youth.setY(node.y + node.height + PROGENY_DISTANCE);
            }
        }

        /* Horizontal positioning */

        // The fulcrum family could be the only one and need to be placed
        if (maxAbove == 0 && fulcrumNode.familyNode != null) {
            fulcrumNode.familyNode.setX(0);
        }

        // Position the descendants starting from generation -1 (if existing) or from
        // fulcrum generation down
        int start = Math.max(0, maxAbove - 1);
        for (int r = start; r < unionRows.size(); r++) {
            UnionRow unionRow = unionRows.get(r);
            unionRow.resolveOverlap();
            unionRow.placeYouths();
        }

        // Ascend generations resolving overlaps and disposing ancestors and uncles
        // starting from generation -1 (if exists) or from fulcrum generation up
        for (int r = start; r >= 0; r--) {
            GroupRow groupRow = groupRows.get(r);
            groupRow.resolveOverlap();
            groupRow.placeAncestors();
        }

        // Outdistance ancestor unions and descendant nodes row by row, reducing lines
        // overlap
        float count = 20;
        float forces = Float.MAX_VALUE;
        while (count > 0 && Math.abs(forces) > 1) {
            for (Node node : nodes) {
                node.force = 0;
            }
            outdistanceDescendants();
            outdistanceAncestors();
            forces = 0;
            for (Node node : nodes) {
                forces += node.force;
            }
            count--;
        }

        // Horizontally place acquired mini ancestry and all mini progeny
        for (Node node : nodes) {
            node.placeAcquiredOriginX();
            node.placeMiniChildrenX();
        }
        // Place mini origins of the first row
        for (Group group : groupRows.get(0)) {
            if (group.origin != null)
                group.placeOriginX();
        }

        // Find the diagram margins to fit exactly around every node
        float minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        float maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Node node : nodes) {
            if (node.x < minX)
                minX = node.x;
            if (node.x + node.width > maxX)
                maxX = node.x + node.width;
            if (node.y < minY)
                minY = node.y;
            if (node.y + node.height > maxY)
                maxY = node.y + node.height;
        }
        width = maxX - minX;
        height = maxY - minY;

        // Correct the position of each node
        for (Node node : nodes) {
            node.setX(node.x - minX);
            node.setY(node.y - minY);
        }

        distributeLines(lines, lineRows, lineGroups);
        distributeLines(backLines, backLineRows, backLineGroups);
    }

    void outdistanceAncestors() {
        // Horizontaly align unions to youth, whether is enough space around
        for (int r = maxAbove; r >= 0; r--) { // Start from fulcrum generation up
            for (Union union : unionRows.get(r)) {
                if (union.ancestor != null // Cousins excluded
                        && union.ancestor.youth != null && !union.ancestor.youth.mini) {
                    union.updateX(); // Necessary
                    union.setX(union.x + union.spaceAround());
                }
            }
        }
        // Place unions below origins to reduce lines overlap
        for (int r = 0; r < maxAbove - 1; r++) { // From top down until generation -2 included
            UnionRow unionRow = unionRows.get(r);
            for (Union union : unionRow) {
                List<Node> origins = union.getOrigins();
                if (origins.size() > 1) {
                    float male = union.ancestor.getPartner(0).centerX();
                    float female = union.ancestor.getPartner(1).centerX();
                    float origin1 = origins.get(0).centerX();
                    float origin2 = origins.get(1).centerX();
                    if ((male < origin1) || (female > origin2)) {
                        union.updateX();
                        float shift = origin1 + (origin2 - origin1) / 2 - union.centerRelX();
                        union.setX(shift);
                    }
                }
            }
            unionRow.resolveOverlap();
        }
        // Align progenitors (those without ancestors, or with mini ancestors)
        for (int r = maxAbove - 1; r >= 0; r--) { // Starting from generation -1 and going up
            for (Union union : unionRows.get(r)) {
                // Strange, but there are crash reports of null youth
                if (!union.ancestor.hasOrigins() && union.ancestor.youth != null) {
                    union.updateX(); // Necessary
                    union.setX(union.x + union.spaceAround());
                }
            }
        }
    }

    void outdistanceDescendants() {
        // Horizontally align groups of this row (having enough space) below origin
        for (int r = maxAbove; r < groupRows.size(); r++) { // Start from fulcrum row down
            for (Group group : groupRows.get(r)) {
                if (group.origin != null) {
                    group.updateX();
                    group.setX(group.x + group.spaceAround());
                }
            }
        }
        // Align horizontally each node to its youth
        // From bottom up to fulcrum row or generation -1 if exists
        for (int r = unionRows.size() - 1; r >= (maxAbove > 0 ? maxAbove - 1 : 0); r--) {
            UnionRow unionRow = unionRows.get(r);
            for (Union union : unionRow) {
                for (Node node : union.list) {
                    Group youth = node.youth;
                    if (youth != null && !youth.mini) {
                        youth.updateX();
                        float distance = youth.centerX() - node.centerX();
                        if (distance != 0) {
                            node.setX(node.x + distance);
                        }
                    }
                }
                union.distributeSiblings();
            }
            unionRow.resolveOverlap();
        }
        // Horizontally align final (without youths) groups of each row (having enough
        // space) below origin
        for (int r = maxAbove; r < groupRows.size(); r++) { // From fulcrum row down
            for (Group group : groupRows.get(r)) {
                if (!group.hasChildren() && group.origin != null) {
                    group.updateX();
                    group.setX(group.x + group.spaceAround());
                }
            }
        }
    }

    private void distributeLines(List<Line> lines, List<LineRow> lineRows, List<Set<Line>> lineGroups) {

        // Max bitmap size is necessary
        if (maxBitmapSize == 0)
            return;

        // Update lines position
        for (Line line : lines)
            line.update();

        // Order lines from left to right
        Collections.sort(lines, new Comparator<Line>() {
            @Override
            public int compare(Line line1, Line line2) {
                return line1.compareTo(line2);
            }
        });

        // Clear lineRows
        for (LineRow row : lineRows) {
            row.reset();
        }

        // Distribute lines inside 'lineRows'
        for (Line line : lines) {
            int rowNum = (int)(line.y2 / maxBitmapSize);
            while (rowNum >= lineRows.size()) {
                lineRows.add(new LineRow());
            }
            LineRow row = lineRows.get(rowNum);
            float lineLeft = Math.min(line.x1, line.x2);
            if (row.size() == 0 || lineLeft > row.restartX + maxBitmapSize) {
                row.restartX = lineLeft;
                row.add(new HashSet<Line>());
            }
            row.get(row.size() - 1).add(line);
            // Store the wider path size
            float pathWidth = Math.max(line.x1, line.x2) - row.restartX;
            if (pathWidth > biggestPathSize)
                biggestPathSize = pathWidth;
        }

        // Populate linesGroups with existing groups of lines
        lineGroups.clear();
        for (LineRow row : lineRows) {
            for (Set<Line> group : row) {
                if (group.size() > 0)
                    lineGroups.add(group);
            }
        }
    }

    // One row of the 2D array 'lineRows'
    class LineRow extends ArrayList<Set<Line>> {
        float restartX; // (re)starting point of every group of lines inside this row

        void reset() {
            restartX = 0;
            for (Set<Line> group : this)
                group.clear();
        }
    }

    @Override
    public String toString() {
        String txt = "";
        for (Group group : groups) {
            txt += group.generation + ": ";
            txt += group + "\n";
        }
        txt += "- - - - - - - - - - -\n";
        for (UnionRow row : unionRows)
            txt += row + "\n";
        return txt;
    }
}

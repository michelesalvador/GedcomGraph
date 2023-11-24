package graph.gedcom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import static graph.gedcom.Util.p;

/**
 * Scaffolding for a graphical implementation of GedcomGraph.
 */
public class Diagram {

    public static void main(String[] args) throws Exception {
        new Diagram();
    }

    Diagram() throws Exception {

        // Parses a Gedcom file
        File file = new File("src/test/resources/tree.ged");
        Gedcom gedcom = new ModelParser().parseGedcom(file);
        gedcom.createIndexes();

        // Directly opens a Json file
        // String content = FileUtils.readFileToString(new File("src/test/resources/tree.json"), "UTF-8");
        // Gedcom gedcom = new JsonParser().fromJson(content);

        // Instantiates a graph
        Graph graph = new Graph();
        graph.setGedcom(gedcom).maxAncestors(5).maxGreatUncles(4).displaySpouses(true).maxDescendants(3).maxSiblingsNephews(2)
                .maxUnclesCousins(2).startFrom(gedcom.getPerson("I1"));

        p(graph);

        // This list represents the graphic layout
        List<GraphicMetric> graphicNodes = new ArrayList<>();

        // Puts all graphic nodes into the layout
        for (PersonNode personNode : graph.getPersonNodes()) {
            graphicNodes.add(new GraphicPerson(personNode));
        }

        // Gets the dimensions of each graphic node
        for (GraphicMetric graphicNode : graphicNodes) {
            graphicNode.metric.width = 100; // graphicNode.getWidth() or something
            graphicNode.metric.height = 100; // graphicNode.getHeight()
        }

        graph.initNodes(); // Prepares nodes

        // Adds bond nodes
        for (Bond bond : graph.getBonds()) {
            graphicNodes.add(new GraphicBond(bond));
        }

        graph.setMaxBitmapSize(1000); // So the lines can be distributed in groups

        graph.placeNodes(); // Lets the graph calculate final position of nodes and lines

        // Loops into the nodes to update their position on the cartesian plane of canvas
        for (GraphicMetric graphicNode : graphicNodes) {
            p(graphicNode, "\t", graphicNode.metric.x, "/", graphicNode.metric.y);
        }
        // And displays the lines
        for (Set<Line> linesGroup : graph.getLines()) {
            for (Line line : linesGroup) {
                // parent
                float x1 = line.x1;
                float y1 = line.y1;
                // child
                float x2 = line.x2;
                float y2 = line.y2;
                // ...
            }
        }
    }

    // The graphical representation of a node (person or bond)
    // In Android could extend a layout (RelativeLayout etc.)
    abstract class GraphicMetric {
        Metric metric;

        GraphicMetric(Metric metric) {
            this.metric = metric;
        }

        @Override
        public String toString() {
            return metric.toString();
        }
    }

    // The graphical representation of a person card
    // In Android could extend a layout (LinearLayout etc.)
    class GraphicPerson extends GraphicMetric {
        GraphicPerson(PersonNode personNode) {
            super(personNode);
            // TODO display a person card
        }
    }

    // The graphical representation of a bond between two persons of the same family
    class GraphicBond extends GraphicMetric {
        GraphicBond(Bond bond) {
            super(bond);
            // TODO display a family bond
        }
    }
}

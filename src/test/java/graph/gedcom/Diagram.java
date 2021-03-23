package graph.gedcom;

// Scaffolding for a graphical implementation of GedcomGraph

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.Graph;
import static graph.gedcom.Util.p;

public class Diagram {
	
	public static void main(String[] args) throws Exception {
		new Diagram();
	}
	
	Diagram() throws Exception {
		
		// Parse a Gedcom file
		File file = new File("src/test/resources/tree.ged");
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		/* Directly open a Json file
		String content = FileUtils.readFileToString(new File("src/test/resources/tree.json"), "UTF-8");
		Gedcom gedcom = new JsonParser().fromJson(content);*/
		
		// Instantiate a graph
		Graph graph = new Graph(gedcom);
		graph.maxAncestors(5)
			.maxGreatUncles(4)
			.displaySpouses(true)
			.maxDescendants(3)
			.maxSiblingsNephews(2)
			.maxUnclesCousins(2)
			.startFrom(gedcom.getPerson("I1"));

		p(graph);
	
		// This list represents the graphic layout
		List<GraphicNode> graphicNodes = new ArrayList<>();
		
		// Put all graphic nodes into the layout
		for (Node node : graph.getNodes()) {
			graphicNodes.add(new GraphicNode(node));
		}
		
		// Get the dimensions of each graphic node
		for (GraphicNode graphicNode : graphicNodes) {
			graphicNode.node.width = graphicNode.toString().length(); // graphicNode.getWidth() or something
			graphicNode.node.height = graphicNode.hashCode(); // graphicNode.getHeight()
		}
		
		graph.initNodes(); // Prepare nodes

		graph.placeNodes(); // First nodes displacement
		
		// Animate the nodes!
		for (int i=0; i < 5; i++) { // Maybe more than 5 frames...

			// Let the diagram calculate positions of Nodes and Lines
			graph.playNodes();
			
			// Loop into the nodes to update their position on the cartesian plane of canvas
			for (GraphicNode graphicNode : graphicNodes) {
				p(graphicNode, "\n\t", graphicNode.node.x, "/", graphicNode.node.y);
			}
			
			// Display the lines
			for (Line line : graph.getLines()) {
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
	
	// The graphical representation of a node
	// In Android could extend a layout (LinearLayout etc.)
	class GraphicNode {
		Node node;
		GraphicNode (Node node) {
			this.node = node;
			if (node instanceof PersonNode) {
				PersonNode personNode = (PersonNode) node;
				new GraphicPerson(personNode);
			} else if (node instanceof FamilyNode) {
				FamilyNode familyNode = (FamilyNode) node;
				new GraphicFamily(familyNode);
			}
		}
		@Override
		public String toString() {
			return node.toString();
		}
	}

	// The graphical representation of a person card
	// In Android could extend a layout (LinearLayout etc.)
	class GraphicPerson {
		PersonNode personNode;
		GraphicPerson (PersonNode personNode) {
			this.personNode = personNode;
			// TODO display a person card
		}
		@Override
		public String toString() {
			return personNode.toString();
		}
	}
	
	// The graphical representation of a family node
	class GraphicFamily {
		FamilyNode familyNode;
		GraphicFamily (FamilyNode familyNode) {
			this.familyNode = familyNode;
			// TODO display a family point
		}
		@Override
		public String toString() {
			return familyNode.toString();
		}
	}
}

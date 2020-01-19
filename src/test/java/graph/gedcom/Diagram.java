package graph.gedcom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.Graph;
import static graph.gedcom.Util.pr;

public class Diagram {

	public static void main(String[] args) throws Exception {
		new Diagram();
	}
	
	Diagram() throws Exception {
		
		// Parse a Gedcom file
		File file = new File("src/test/resources/tree.ged");	
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		// Directly open a Json file
		//String content = FileUtils.readFileToString(new File("src/test/resources/tree.json"), "UTF-8");
		//Gedcom gedcom = new JsonParser().fromJson(content);
		
		// Instantiate a graph
		Graph graph = new Graph(gedcom);
		graph.maxAncestors(5).maxUncles(4).displaySiblings(true).maxDescendants(3).startFrom("I1");
		//pr(graph);
	
		// This list represents the graphic layout
		List<GraphicNode> graphicNodes = new ArrayList<>();
		
		// Put all graphic cards into the layout
		for (Node node : graph.getNodes()) {
			graphicNodes.add(new GraphicNode(node));
		}
		
		// Get the dimensions of each graphic card and pass them back to each corresponding card
		for (GraphicNode graphicNode : graphicNodes) {
			for(GraphicCard graphicCard : graphicNode.graphicCards) {
				graphicCard.card.width = graphicNode.node.toString().length() * 6 + 20;
				graphicCard.card.height = (int) (25 + (50 * Math.random()));
			}
		}
		
		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();
		
		// Loop into the nodes so to update their position on the cartesian plane of canvas
		for (GraphicNode graphicNode : graphicNodes) {
			pr(graphicNode, "\n\t", graphicNode.node.x, "/", graphicNode.node.y);
		}
		
		// Add the lines
		for (Line line : graph.getLines()) {
			// ...
		}
	}
	
	// The graphical representation of a node
	// In Android could extend a layout (LinearLayout etc.)
	class GraphicNode {
		
		Node node;
		List<GraphicCard> graphicCards; // One or two cards
		
		GraphicNode (Node node) {
			this.node = node;
			graphicCards = new ArrayList<>();
			if(node instanceof UnitNode) {
				UnitNode unitNode = (UnitNode) node;
				if(unitNode.husband != null)
					graphicCards.add(new GraphicCard(unitNode.husband));
				if(unitNode.wife != null)
					graphicCards.add(new GraphicCard(unitNode.wife));
			}
		}
		
		@Override
		public String toString() {
			return node.toString();
		}
	}

	// The graphical representation of a card
	// In Android could extend a layout (LinearLayout etc.)
	class GraphicCard {
		
		Card card;
		
		GraphicCard (Card card) {
			this.card = card;
		}
		
		@Override
		public String toString() {
			return card.toString();
		}
	}
}

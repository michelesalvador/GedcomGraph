package graph.gedcom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import graph.gedcom.Card;
import graph.gedcom.Graph;
import static graph.gedcom.Util.pr;

public class Test {

	public static void main(String[] args) throws Exception {

		// Parse a Gedcom file
		File file = new File("src/test/resources/single.ged");	
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		// Directly open a Json file
		//String content = FileUtils.readFileToString(new File("src/test/resources/family.json"), "UTF-8");
		//Gedcom gedcom = new JsonParser().fromJson(content);
		
		// Instantiate a graph
		Graph graph = new Graph(gedcom);
		graph.maxAncestors(5).startFrom("I1");
		//pr(graph);
	
		List<ConcreteCard> concreteCards = new ArrayList<>();
		
		// Put all concrete cards into the layout
		for (Card card : graph.getCards()) {
			concreteCards.add(new ConcreteCard(card));
		}
		
		// Get the dimensions of each concrete card and pass them back to each corresponding card
		for (ConcreteCard concreteCard : concreteCards) {
			concreteCard.card.width = concreteCard.card.toString().length() * 6 + 20;
			concreteCard.card.height = (int) (25 + (50 * Math.random()));
		}
		
		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();
		
		// Loop into the cards so to update their position on the cartesian plane of canvas
		for(ConcreteCard concreteCard : concreteCards) {
			pr(concreteCard);
			pr("\t", concreteCard.card.x, concreteCard.card.y);
		}
	}
	
	// The graphical reppresentation of a card
	// In Android could extend a layout (LinearLayout etc.) 
	public static class ConcreteCard {
		
		Card card;
		
		public ConcreteCard (Card card) {
			this.card = card;
		}
		
		public String toString() {
			return card.toString();
		}
	}

}

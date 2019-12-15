package graph.gedcom;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;

import graph.gedcom.Card;
import graph.gedcom.Graph;
import graph.gedcom.Util;
import static graph.gedcom.Util.print;

public class Test {

	public static void main(String[] args) throws Exception {

		// Parse of a Gedcom file
		File file = new File("src/test/resources/single.ged");	
		Gedcom gedcom = new ModelParser().parseGedcom(file);
		gedcom.createIndexes();

		// Directly open a Json file
		//String content = FileUtils.readFileToString(new File("src/test/resources/family.json"), "UTF-8");
		//Gedcom gedcom = new JsonParser().fromJson(content);
		
		// Create the graph model
		Graph graph = new Graph(gedcom);
		graph.setCardClass(ConcreteCard.class).maxAncestors(2).startFrom("I1");
		// p(graph.toString1());
		
		// TODO in Android: put all concrete cards into the layout
		
		// Get the dimensions of each concrete card and pass them back
		for ( Card card : graph.getCards()) {
			card = (ConcreteCard)card;
			card.width = card.toString().length() * 6 + 20;
			card.height = (int) (25 + (50 * Math.random()));
		}
		
		// Let the diagram calculate positions of Nodes and Lines
		graph.arrange();
		
		// Loop into the cards so to update their position on the cartesian plane of canvas
		for(Card card : graph.getCards()) {
			ConcreteCard concreteCard = (ConcreteCard)card;
			print(concreteCard);
			print("\t" + concreteCard.x + " " + concreteCard.y);
		}
	}
	
	public static class ConcreteCard extends Card {
		public ConcreteCard (Person person) {
			super(person);
		}
		/*public String toString() {
			return Util.essence(getPerson());
		}*/
	}

}

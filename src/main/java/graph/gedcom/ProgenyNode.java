package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;
import static graph.gedcom.Util.pr;

public class ProgenyNode extends Node {

	public List<MiniCard> miniChildren;
	private Gedcom gedcom;
	public Node origin;
	private int people;

	ProgenyNode(Gedcom gedcom, Family family, Node origin) {
		this.gedcom = gedcom;
		this.origin = origin;
		miniChildren = new ArrayList<>();
		addFamily(family);
	}
	
	public void addFamily(Family family) {
		for (Person child : family.getChildren(gedcom)) {
			people = 1;
			countDescendants(child);
			MiniCard miniCard = new MiniCard(child, people);
			miniCard.origin = origin;
			miniChildren.add(miniCard);
		}
	}

	// Recoursive count of direct descendants
	private void countDescendants(Person p) {
		if (people < 500)
			for (Family family : p.getSpouseFamilies(gedcom))
				for (Person child : family.getChildren(gedcom)) {
					people++;
					countDescendants(child);
				}
	}

	@Override
	public float centerRelX() {
		return width / 2;
	}

	@Override
	public float centerX() {
		return x + centerRelX();
	}
	
	void positionCards() {
		float posX = x;
		for(MiniCard card : miniChildren) {
			card.x = posX;
			card.y = y;
			posX += card.width + Util.PLAY;
		}
	}
}

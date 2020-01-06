package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Person;

public class ProgenyNode extends Node {

	public List<MiniCard> miniChildren;
	private Gedcom gedcom;
	private int people;

	ProgenyNode(Gedcom gedcom, Family family, Node origin) {
		this.gedcom = gedcom;
		miniChildren = new ArrayList<>();
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
	public int centerX() {
		return x + centerXrel();
	}

	@Override
	public int centerXrel() {
		return width / 2;
	}
	
	void positionCards() {
		int posX = x;
		for(MiniCard card : miniChildren) {
			card.x = posX;
			card.y = y;
			posX += card.width + Util.PLAY;
		}
	}
}

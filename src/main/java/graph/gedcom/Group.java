package graph.gedcom;

import java.util.ArrayList;
import java.util.List;

public class Group {

	private Node guardian; // Node of parent(s), is null if there isn't any parent
	private List<CardNode> youths; // Children with their spouses

	public Group() {
		this.guardian = null;
		this.youths = new ArrayList<>();
	}
	
	public Node getGuardian() {
		return guardian;
	}
	
	void setGuardian(Node node) {
		guardian = node;
	}
	
	public CardNode getYouth(int index ) {
		return youths.get(index);
	}
	
	public List<CardNode> getYouths() {
		return youths;
	}

	void addYoung(CardNode node, boolean beginning) {
		if(beginning)
			youths.add(0, node);
		else
			youths.add(node);
	}

	int getYouthWidth() {
		int w = 0;
		for (CardNode node : youths) {
			w += node.width + Util.PADDING;
		}
		return w - Util.PADDING; // TODO e se non ci sono youths?
	}
	
	int getArcWidth(int branch) {
		if (branch == 1) // Husband ascendant group
			return youths.get(youths.size()-1).getCard(1).centerX() - youths.get(0).getMainCard().centerX();
		else if (branch == 2) // Wife ascendant group 
			return youths.get(youths.size()-1).getMainCard().centerX() - youths.get(0).getCard(2).centerX();
		else // Descendant group
			return youths.get(youths.size()-1).getMainCard().centerX() - youths.get(0).getMainCard().centerX();
	}
	
	public String toString() {
		String str = "...";
		if( guardian != null )
			str = guardian.toString();
		for (CardNode young : youths) {
			str += "\n\t" + young.toString();
		}
		return str;
	}
}

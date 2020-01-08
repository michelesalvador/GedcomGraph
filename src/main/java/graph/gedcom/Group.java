package graph.gedcom;

import java.util.ArrayList;
import java.util.List;
import static graph.gedcom.Util.pr;

public class Group {

	Node guardian; // Node of parent(s), is null if there isn't any parent
	List<UnitNode> youths; // Children with their spouses

	public Group(Node guardian) {
		this.guardian = guardian;
		guardian.guardGroup = this;
		this.youths = new ArrayList<>();
	}
	
	@Deprecated
	public Node getGuardian() {
		return guardian;
	}
	
	@Deprecated
	void setGuardian(Node node) {
		guardian = node;
	}
	
	public UnitNode getYouth(int index ) {
		return youths.size() > 0 ? youths.get(index) : null;
	}
	
	@Deprecated
	public List<UnitNode> getYouths() {
		return youths;
	}

	void addYoung(UnitNode node, boolean beginning) {
		if(beginning)
			youths.add(0, node);
		else
			youths.add(node);
	}

	// Center of youths excluding acquired spouses
	// TODO Non funziona se ci sono multimatrimoni sia maschili che femminili
	int centerX() {
		int leftX = getYouth(0).getMainCard().centerX();
		return leftX + ( getYouth(youths.size()-1).getMainCard().centerX() - leftX ) / 2;
	}
	
	// Branch 1 for husband, 2 for wife
	int centerX(int branch) {
		if (branch == 1)
			return centerX();
		else if (branch == 2) {
			int leftX = getYouth(0).getCard(2).centerX();
			if (youths.size() > 1)
				return leftX + ( getYouth(youths.size()-1).getMainCard().centerX() - leftX ) / 2;
			else
				return leftX;
		}
		return 0;
	}
	
	// Youths width excluding acquired spouses at the ends
	int getYouthWidth() {
		if (youths.size() > 1) {
			int w = 0;
			for (int i=0; i < youths.size(); i++) {
				UnitNode node = youths.get(i);			
				if(i == 0)
					w += node.getMainWidth(true);
				else if(i == youths.size() - 1)
					w += node.getMainWidth(false);
				else
					w += node.width;
				w += Util.PADDING;
			}
			return w - Util.PADDING;
		} if (youths.size() == 1) {
			return getYouth(0).getMainCard().width;
		}
		return 0;
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
		for (UnitNode young : youths) {
			str += "\n\t" + young.toString();
		}
		return str;
	}
}

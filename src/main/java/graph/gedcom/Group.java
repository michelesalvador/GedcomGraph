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
		youths = new ArrayList<>();
	}
	
	public UnitNode getYouth(int index) {
		return youths.size() > 0 ? youths.get(index) : null;
	}
	
	void addYoung(UnitNode node, boolean beginning) {
		if(beginning)
			youths.add(0, node);
		else
			youths.add(node);
	}

	// Center of youths excluding acquired spouses: branch 1 for husband, 2 for wife, 0 doesn't matter
	// Doesn't apply to multi marriages
	float centerX(int branch) {
		if (branch == 0 || branch == 1) {
			float leftX = getYouth(0).getMainCard().centerX();
			return leftX + ( getYouth(youths.size()-1).getMainCard().centerX() - leftX ) / 2;
		} else if (branch == 2) {
			float leftX = getYouth(0).getCard(2).centerX();
			if (youths.size() > 1)
				return leftX + ( getYouth(youths.size()-1).getMainCard().centerX() - leftX ) / 2;
			else
				return leftX;
		}
		return 0;
	}
	
	// Youths width excluding acquired spouses at the ends
	float getYouthWidth() {
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
	
	public String toString() {
		String str = "";
		if( guardian != null )
			str = guardian.toString();
		for (UnitNode youth : youths) {
			str += "\n\t" + youth;
		}
		return str;
	}
}

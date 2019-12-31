package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;

public final class ParentNode extends CardNode {

	public ParentNode(Gedcom gedcom, Family family) {
		init(gedcom, family);
		if (husband != null)
			new AncestryNode(gedcom, husband);
		if (wife != null)
			new AncestryNode(gedcom, wife);
	}

	@Override
	public Card getMainCard() {
		return null;
	}

	@Override
	public Card getSpouseCard() {
		return null;
	}
}

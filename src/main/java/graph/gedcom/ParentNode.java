package graph.gedcom;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;

public final class ParentNode extends UnitNode {

	public ParentNode(Gedcom gedcom, Family family) {
		init(gedcom, family);
	}

	@Override
	public IndiCard getMainCard() {
		return null;
	}

	@Override
	public IndiCard getSpouseCard() {
		return null;
	}
}

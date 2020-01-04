package graph.gedcom;

// di una Person restituisce una lista di figli col numero di rispettivi discendenti

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
			contaDiscendenti( child );
			MiniCard miniCard = new MiniCard(child, people);
			miniCard.origin = origin;
			miniChildren.add(miniCard);
		}
	}
	
	@Override
	public int centerX() {
		return 0; // TODO
	}

	@Override
	public int centerXrel() {
		return 0; // TODO
	}

	// conta ricorsivamente i discendenti
	private void contaDiscendenti( Person p ) {
		if( people < 500 )
			for( Family fam : p.getSpouseFamilies(gedcom) )
				for( Person figlio : fam.getChildren(gedcom) ) {
					people++;
					contaDiscendenti( figlio );
				}
	}
}

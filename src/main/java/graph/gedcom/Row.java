// List of unions of the same generation, ordered left to right

package graph.gedcom;

import java.util.ArrayList;

public class Row extends ArrayList<Union> {

	int generation;
	float yAxe;

	Row(int generation, float yAxe) {
		this.generation = generation;
		this.yAxe = yAxe;
	}

	void addUnion(Union union) {
		add(union);
	}

	@Override
	public String toString() {
		String txt = generation + ": <";
		for( Union union : this )
			txt += union + ", ";
		txt = txt.replaceAll(", $", "");
		txt += "> " + yAxe;
		return txt;
	}
}

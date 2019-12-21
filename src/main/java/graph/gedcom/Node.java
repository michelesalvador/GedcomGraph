package graph.gedcom;

import org.folg.gedcom.model.Person;

public abstract class Node {

	public int x, y, width, height;
	Group husbandGroup; // The group to which the husband of this node belongs as child
	Group wifeGroup; // The group to which the wife of this node belongs as child
	public Group guardGroup; // The group to which this node belongs as guardian

	/**
	 * Retrieve the {#Card} of the blood relative (that is, not the spouse).
	 * 
	 * @return A Card
	 */
	public abstract Card getMainCard();
	
	/**
	 * 
	 * @param branch 1 for the husband, 2 for the wife
	 * @return
	 */
	abstract Card getCard(int branch);

	public boolean isSingle() {
		return this instanceof Single;
	}

	public boolean isCouple() {
		return this instanceof Couple;
	}
	
	public abstract int centerX();
	
	public int centerY() {
		return y + height / 2;
	}

	public abstract int centerXrel();
	
	public int centerYrel() {
		return height / 2;
	}

	Person getPerson(int branch) {
		if (isCouple()) {
			if (branch == 1)
				return ((Couple) this).husband.getPerson();
			else if (branch == 2)
				return ((Couple) this).wife.getPerson();
		} else if (isSingle()) {
			return ((Single) this).one.getPerson();
		}
		return null;
	}
	
	@Override
	public String toString() {
		String str = "";
		//str += this.hashCode() + "\t";
		if (isCouple()) {
			str += ((Couple) this).husband.toString() + " - " + ((Couple) this).wife.toString();
		} else if (isSingle())
			str += ((Single) this).one.toString();
		return str;
	}
}

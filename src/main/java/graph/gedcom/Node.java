package graph.gedcom;

import org.folg.gedcom.model.Person;

public abstract class Node {

	public int x, y, width, height;
	Group husbandGroup; // The group to which the husband of this node belongs as child
	Group wifeGroup; // The group to which the wife of this node belongs as child
	Group guardGroup; // The group to which this node belongs as guardian
	int[] descendants;
	Class<? extends Card> genericCard;

	/**
	 * Retrieve the {#Card} of the blood relative (that is, not the spouse).
	 * 
	 * @return A Card
	 */
	abstract Card getMainCard();
	
	/**
	 * 
	 * @param branch 1 for the husband e 2 for the wife
	 * @return
	 */
	abstract Card getCard(int branch);

	public boolean isSingle() {
		return this instanceof Single;
	}

	public boolean isCouple() {
		return this instanceof Couple;
	}
	
	abstract int centerX();

	// centrare gli youths a partire dai guardian
	/*@Deprecated
	void setX(int moveX) {
		if( this.equals(group.getYouth(0)) )
			this.x = group.getGuardian().centerX() - group.getArcWidth()/2 - this.width / 2;  // <---- Sbagliato
		else
			this.x = moveX;
	}*/

	boolean identical(Node other) {
		if (isCouple() && other.isCouple()
				&& ((Couple) this).husband.getPerson().equals(((Couple) other).husband.getPerson())
				&& ((Couple) this).wife.getPerson().equals(((Couple) other).wife.getPerson())
				&& ((Couple) this).marriage.equals(((Couple) other).marriage))
			return true;
		if (isSingle() && other.isSingle() && ((Single) this).one.getPerson().equals(((Single) other).one.getPerson()))
			return true;
		return false;
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
		str += this.hashCode() + "\t";
		if (isCouple()) {
			str += ((Couple) this).husband.toString() + " - " + ((Couple) this).wife.toString();
		} else if (isSingle())
			str += ((Single) this).one.toString();
		return str;
	}
}

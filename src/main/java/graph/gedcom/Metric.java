/** Basic geometry to position an element in the diagram on x and y, and with width and height.
 * Extended by Node (PersonNode, FamilyNode), Bond, Group, Union.
 */

package graph.gedcom;

public abstract class Metric {

	public float x, y; // Absolute coordinates of the top left corner
	public float width, height; // Dimensions in dip

	abstract public float centerRelX();
	abstract public float centerRelY();

	public float centerX() {
		return x + centerRelX();
	}

	public float centerY() {
		return y + centerRelY();
	}

	abstract void setX(float x);
	abstract void setY(float y);

	abstract void setForce(float power);
}

package graph.gedcom;

import static graph.gedcom.Util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Animator {
	
	float width, height;
	PersonNode fulcrumNode;
	int maxAbove;
	Set<Node> nodes;
	List<Line> lines;
	Map<Integer, Float> genCenterPos; // Number of the generation and vertical center of the rows
	
	private double ATTRACTION_CONSTANT = 0.01;	// spring constant
	private double REPULSION_CONSTANT = 20000; 	// charge constant
	private double DEFAULT_DAMPING = .5; // Value between 0 and 1 that slows the motion of the nodes during layout.
	private int DEFAULT_SPRING_LENGTH = 150; // Value in pixels representing the length of the imaginary springs that run along the connectors.
	
	Animator() {
		nodes = new HashSet<>();
		lines = new ArrayList<>();
	}

	void addNode(Node newNode) {
		nodes.add(newNode);
		/*for (Node oldNode : nodes) { TODO put into node the next sibling
			if (newNode.generation == oldNode.generation && oldNode.next == null) {
				oldNode.next = newNode;
				newNode.prev = oldNode;
				break;
			}
		}*/
	}
	
	// Preparing the nodes for dance
	void arrangeNodes(PersonNode fulcrumNode, int maxAbove, int maxBelow) {

		this.fulcrumNode = fulcrumNode;
		this.maxAbove = maxAbove;
		width = 0;
		height = 0;

		// Array with max height of each row of nodes
		int totalRows = maxAbove + 1 + maxBelow;
		float[] rowMaxHeight = new float[totalRows];

		for (Node node : nodes) {
			node.velocity = new Vector();
			node.nextPosition = new Point();

			// Discover the maximum height of rows
			if (node.height > rowMaxHeight[node.generation+maxAbove])
				rowMaxHeight[node.generation+maxAbove] = node.height;
		}
		
		// Vertical position of the generation rows
		genCenterPos = new HashMap<>();
		float posY = rowMaxHeight[0] / 2;
		for (int gen = -maxAbove; gen < totalRows-maxAbove; gen++) {
			genCenterPos.put(gen, posY);
			posY += rowMaxHeight[gen + maxAbove] + Util.SPACE;

		}
		
		// Set the coordinates of the center of each node
		int rank = 0;
		for (Node node : nodes) {
			node.pos = new Point(node.centerRelX() + rank * 150, genCenterPos.get(node.generation));
			rank++;
		}
	}
	
	// Generate the next position of each node and line
	public void playNodes() {
		for (Node current : nodes) {
			
			// express the node's current position as a vector, relative to the origin
			Vector currentPosition = new Vector(
					calcDistance(new Point(), current.pos),
					getBearingAngle(new Point(), current.pos)
			);
			Vector netForce = new Vector(0, 0);

			// determine repulsion between all nodes
			for (Node other : nodes) {
				if (other != current)
					netForce.sum(calcRepulsionForce(current, other));
			}

			// Attraction between all nodes
			for (Node other : nodes) {
				if (other != current)
					netForce.sum(calcAttractionForce(current, other, DEFAULT_SPRING_LENGTH));
			}

			// Attraction inside a family
			if(current instanceof FamilyNode) {
				for(PersonNode partner : ((FamilyNode)current).partners) {
					netForce.sum(calcAttractionForce(current, partner, DEFAULT_SPRING_LENGTH));
				}
			}
			for (Node child : current.children) {
				netForce.sum(calcAttractionForce(current, child, DEFAULT_SPRING_LENGTH));
			}
			/*for (Node parent : nodes) {
				if (parent.children.contains(current))
					netForce.sum(calcAttractionForce(current, parent, DEFAULT_SPRING_LENGTH));
			}*/

			// Force attractive to generation row
			netForce.sum(calcGenerationAttraction(current));

			// apply net force to node velocity
			current.velocity.sum(netForce);
			current.velocity.magnitude *= DEFAULT_DAMPING;

			// apply velocity to node position
			current.nextPosition = currentPosition.sum(current.velocity).toPoint();
			
			// Constraint to the generation row
	//		current.nextPosition.y = genCenterPos.get(current.generation);
		}

		// Set the resultant positions
		for (Node current : nodes) {
			current.pos = current.nextPosition;
		}

		// Find the diagram margins
		Rectangle logicalBounds = getDiagramBounds();
		width = logicalBounds.width;
		height = logicalBounds.height;

		// Correct the position and prepare cards for lines
		for (Node node : nodes) {
			node.pos.x -= logicalBounds.x;
			node.pos.y -= logicalBounds.y;
			node.x = node.pos.x - node.centerRelX(); // From here we use the top lef corner x y coordiantes
			node.y = node.pos.y - node.centerRelY();
		}
		
		// Create the Lines
		lines.clear();
		for (Node node : nodes) {
			if (node instanceof PersonNode && node.getOrigin() != null)
				lines.add(new Line((PersonNode)node));
			if (node instanceof PersonNode && node.getFamilyNode() != null)
				lines.add(new Line((PersonNode)node, true));
		}
		
		// Order lines from left to right
		Collections.sort(lines, new Comparator<Line>() {
			@Override
			public int compare(Line line1, Line line2) {
				return line1.compareTo(line2);
			}
		});
	}

	// Calculates the distance between two points
	private int calcDistance(Point a, Point b) {
		double xDist = (a.x - b.x);
		double yDist = (a.y - b.y);
		int dist = (int)Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
		return dist;
	}

	// Calculates the bearing angle from one point to another
	private double getBearingAngle(Point start, Point end) {
		Point half = new Point(start.x + ((end.x - start.x) / 2), start.y + ((end.y - start.y) / 2));

		double diffX = (double)(half.x - start.x);
		double diffY = (double)(half.y - start.y);

		if (diffX == 0) diffX = 0.001;
		if (diffY == 0) diffY = 0.001;

		double angle;
		if (Math.abs(diffX) > Math.abs(diffY)) {
			angle = Math.tanh(diffY / diffX) * (180.0 / Math.PI);
			if (((diffX < 0) && (diffY > 0)) || ((diffX < 0) && (diffY < 0))) angle += 180;
		}
		else {
			angle = Math.tanh(diffX / diffY) * (180.0 / Math.PI);
			if (((diffY < 0) && (diffX > 0)) || ((diffY < 0) && (diffX < 0))) angle += 180;
			angle = (180 - (angle + 90));
		}
		return angle;
	}

	// Calculates the repulsion force between any two nodes in the diagram space
	private Vector calcRepulsionForce(Node x, Node y) {
		int proximity = Math.max(calcDistance(x.pos, y.pos), 1);

		double force = -(REPULSION_CONSTANT / Math.pow(proximity, 2));
		double angle = getBearingAngle(x.pos, y.pos);

		return new Vector(force, angle);
	}

	/** Calculates the attraction force between two connected nodes, using the specified spring length.
	 * @param a The node that the force is acting on.
	 * @param b The node creating the force.
	 * @param springLength The length of the spring, in pixels.
	 * @return A Vector representing the attraction force.
	 */
	private Vector calcAttractionForce(Node a, Node b, double springLength) {
		int proximity = Math.max(calcDistance(a.pos, b.pos), 1);
		//float proximity = Math.max(calcMonoDistance(a.pos.x, b.pos.x), 1);

		// Hooke's Law: F = -kx
		double force = ATTRACTION_CONSTANT * Math.max(proximity - springLength, 0);
		double angle = getBearingAngle(a.pos, b.pos);

		return new Vector(force, angle);
	}

	// Attraction towards a generation row
	private Vector calcGenerationAttraction(Node node) {
		Point generation = new Point(node.pos.x, genCenterPos.get(node.generation));
		int proximity = Math.max(calcDistance(node.pos, generation), 1);
		int SHORT_SPRING = 1;
		double force = ATTRACTION_CONSTANT * Math.max(proximity - SHORT_SPRING, 0);
		double angle = getBearingAngle(node.pos, generation);
		return new Vector(force, angle);
	}

	// Determines the logical bounds of the diagram.
	// @return A Rectangle that fits exactly around every node in the diagram.
	private Rectangle getDiagramBounds() {
		float minX = 999999, minY = 999999;
		float maxX = -999999, maxY = -999999;
		for (Node node : nodes) {
			if (node.pos.x - node.centerRelX() < minX) minX = node.pos.x - node.centerRelX();
			if (node.pos.x + node.width - node.centerRelX() > maxX) maxX = node.pos.x + node.width - node.centerRelX();
			if (node.pos.y - node.height/2 < minY) minY = node.pos.y - node.height/2;
			if (node.pos.y + node.height/2 > maxY) maxY = node.pos.y + node.height/2;
		}
		return new Rectangle(minX, minY, maxX, maxY);
	}

	class Rectangle {
		float x, y, width, height;
		Rectangle(float x, float y, float width, float height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}
}

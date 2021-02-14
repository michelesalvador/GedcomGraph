package graph.gedcom;

public class Vector {

	double direction;
	double magnitude;
	
	public Vector() {}
	
	public Vector(double magnitude, double direction) {
		this.magnitude = magnitude;
		this.direction = direction;

		// resolve negative magnitude by reversing direction
		if (magnitude < 0) {
			magnitude = -magnitude;
			direction = (180.0 + direction) % 360;
		}
		// resolve negative direction
		if (direction < 0) direction = (360.0 + direction);
	}
	
	// Converts the vector into an X-Y coordinate representation.
	public Point toPoint() {
		float aX = (float) (magnitude * Math.cos((Math.PI / 180.0) * direction));
		float aY = (float) (magnitude * Math.sin((Math.PI / 180.0) * direction));

		return new Point(aX, aY);
	}
	
	/// Calculates the resultant sum of two vectors.
	public Vector sum(Vector b) {
		// break into x-y components
		double aX = this.magnitude * Math.cos((Math.PI / 180.0) * this.direction);
		double aY = this.magnitude * Math.sin((Math.PI / 180.0) * this.direction);

		double bX = b.magnitude * Math.cos((Math.PI / 180.0) * b.direction);
		double bY = b.magnitude * Math.sin((Math.PI / 180.0) * b.direction);

		// add x-y components
		aX += bX;
		aY += bY;

		// pythagorus' theorem to get resultant magnitude
		magnitude = Math.sqrt(Math.pow(aX, 2) + Math.pow(aY, 2));

		// calculate direction using inverse tangent
        if (magnitude == 0)
            direction = 0;
        else
            direction = (180.0 / Math.PI) * Math.atan2(aY, aX);

		return this;
	}
	
	@Override
	public String toString() {
		return "Vector dir: " + direction + " magn: " + magnitude;
	}
}

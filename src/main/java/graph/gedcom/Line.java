package graph.gedcom;

public class Line {

	public int x1, y1, x2, y2;

	public Line( Card end ) {
		if(end != null) {
			Node start = end.getOrigin();
			// this.end = end;
			if( start != null ) {
				if(start.isCouple()) {
					x1 = ((Couple)start).centerX();
					y1 = start.y + ((Couple)start).height;
				} else {
					x1 = start.x + ((Single)start).one.width / 2;
					y1 = start.y + ((Single)start).one.height;
				}
				x2 = end.centerX();
				y2 = end.y;
			} else {
				try {
					this.finalize();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
}

package traffic.routing;

import common.Settings;
import traffic.road.Edge;

import java.awt.geom.Point2D;

/**
 * A leg on the route.
 */
public class RouteLeg {
	/**
	 * Edge of the leg.
	 */
	public Edge edge;
	/**
	 * Time length that vehicle needs to wait on the edge.
	 */
	public double stopover = 0;

	public int lane;

	private double speed;
	private double previousEndTarget = -1;
	private double nextStartTarget = -1;
	private double nextEndTarget = -1;
	private boolean scheduled = false;
	private Edge next;
	private Point2D p0 = null;
	private Point2D p1 = null;
	private Point2D p2 = null;
	private Point2D p3 = null;

	private double startH = -1;
	private double endH = -1;

	/**
	 * @param edge
	 * @param stopover
	 */
	public RouteLeg(final Edge edge, final double stopover) {
		super();
		this.edge = edge;
		this.stopover = stopover;
		this.speed = edge.freeFlowSpeed;
	}

	public int getLane() {
		return lane;
	}

	public void setLane(int lane) {
		this.lane = lane;
	}

	public void setTargetTimes(double previousEndTarget, double nextStartTarget, double nextEndTarget, Edge next) {
		scheduled = true;
		this.previousEndTarget = previousEndTarget;
		this.nextStartTarget = nextStartTarget;
		this.nextEndTarget = nextEndTarget;
		this.next = next;
	}

	public void setPoints(Point2D p0, Point2D p1, Point2D p2, Point2D p3){
		scheduled = true;
		this.p0 = p0;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}

	public double getCurve(double timeNow){
		double t = (timeNow - p0.getY())/(p3.getY() - p0.getY());
		double y = 3 * (1-t)*(1-t)*(p1.getY() - p0.getY())+ 6*(1-t)*t*(p2.getY()-p1.getY()) +3*t*t*(p3.getY()-p2.getY());
		return 1/y;
	}

	public void setStartH(double startH) {
		this.startH = startH;
	}

	public void setEndH(double endH) {
		this.endH = endH;
	}

	public double getTargetPosition(double time){
		return (time-p0.getY())*(p3.getX()-p0.getX())/(p3.getY() - p0.getY()) + p0.getX();
	}

	public double getHeadwayMultiplier(double pos, double defaultVal){
		if(startH > 0 && endH > 0) {
			return startH + (pos / edge.length) * (endH - startH);
		}
		return defaultVal;
	}
}

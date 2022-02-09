package traffic.routing;

import traffic.road.Edge;

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

	/**
	 * @param edge
	 * @param stopover
	 */
	public RouteLeg(final Edge edge, final double stopover) {
		super();
		this.edge = edge;
		this.stopover = stopover;
	}
}

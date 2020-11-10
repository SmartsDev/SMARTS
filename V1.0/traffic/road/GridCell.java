package traffic.road;

import java.util.ArrayList;

/**
 * A rectangular cell on a grid.
 *
 */
public class GridCell {
	/**
	 * Row of the cell.
	 */
	public int row;
	/**
	 * Column of the cell.
	 */
	public int col;
	/**
	 * Collection of nodes that are contained in the cell.
	 */
	public ArrayList<Node> nodes = new ArrayList<>(500);
	/**
	 * Total length of lanes on the edges that start from within the cell. This
	 * length can be used for balancing the work load between workers.
	 */
	public int laneLength;
}

package traffic.road;

import java.util.ArrayList;

/**
 * Node is a basic element in road network. A network consists of a number of
 * nodes connected by edges.
 *
 */
public class Node {
	/**
	 * Latitude of node.
	 */
	public double lat;
	/**
	 * Longitude of node.
	 */
	public double lon;
	/**
	 * Original node id in OpenStreetMap data.
	 */
	public long osmId;
	/**
	 * The grid cell that contains this node.
	 */
	public GridCell gridCell;
	/**
	 * Whether there is traffic light at this node.
	 */
	public boolean light;
	/**
	 * Whether there is tram stop at this node.
	 */
	public boolean tramStop;
	/**
	 * Whether there is bus stop at this node.
	 */
	public boolean busStop;
	/**
	 * Edges that start from this node.
	 */
	public ArrayList<Edge> outwardEdges = new ArrayList<>();
	/**
	 * Edges that end at this node.
	 */
	public ArrayList<Edge> inwardEdges = new ArrayList<>();
	/**
	 * Nodes that are connected with this node
	 */
	public ArrayList<Node> connectedNodes = new ArrayList<>();
	/**
	 * Name in OpenStreetMap data.
	 */
	public String name;
	/**
	 * Index of the node in the list of all nodes.
	 */
	public int index;
	/**
	 * Names of the streets that cross this node.
	 */
	public String streetNames = "";
	/**
	 * Group of nodes with traffic lights. This node belongs to this group if it
	 * has traffic light.
	 */
	public long idLightNodeGroup = 0;

	public Node(final long osmId, final String name, final double lat, final double lon, final boolean light,
			final boolean tram_stop, final boolean bus_stop) {
		super();
		this.osmId = osmId;
		this.name = name;
		this.lat = lat;
		this.lon = lon;
		this.light = light;
		tramStop = tram_stop;
		busStop = bus_stop;
	}

}

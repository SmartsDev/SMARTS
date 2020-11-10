package traffic.road;

import java.util.ArrayList;
import java.util.List;

import traffic.light.LightColor;
import traffic.vehicle.Vehicle;

/**
 * Edge is a basic element in road network. A network consists of a number of
 * nodes connected by edges.
 *
 */
public class Edge {
	int importedStartNodeIndex;
	int importedEndNodeIndex;
	public Node startNode;
	public Node endNode;
	public RoadType type;
	public String name;
	/**
	 * Free flow speed of vehicles
	 */
	public double freeFlowSpeed;

	/**
	 * Number of vehicles that can be filled into this edge based on average
	 * vehicle speed.
	 */
	public double capacity = 0;
	/**
	 * Whether this edge is in a roundabout.
	 */
	public boolean isRoundabout;
	/**
	 * Index of this edge in the whole network.
	 */
	public int index;
	/**
	 * Length of this edge in meters.
	 */
	public double length;
	/**
	 * Collection of the lanes belonging to this edge.
	 */
	public ArrayList<Lane> lanes = new ArrayList<>();
	/**
	 * Remaining time that the tram stops at the station on this edge.
	 */
	public double timeTramStopping = 0;
	/**
	 * Remaining time that the tram stop should be ignored. This can be used for
	 * preventing vehicles to wait at tram station for consecutive periods.
	 */
	public double timeNoTramStopping = 0;
	/**
	 * Color of traffic light at the end of this edge.
	 */
	public LightColor lightColor = LightColor.GYR_G;
	/**
	 * Whether this edge is on one-way road. If it is on a two-way road, there
	 * is another edge in the opposite direction.
	 */
	public boolean onOneWayRoad;
	/**
	 * Tram edge parallel to this edge and is with tram stop.
	 */
	public Edge parallelTramEdgeWithTramStop = null;
	/**
	 * Distance between the start node of this edge to the end node of the
	 * parallel tram edge with tram stop. The tram stop is at the end node of
	 * the edge.
	 */
	public double distToTramStop;
	/**
	 * Collection of vehicles that are parked along this edge.
	 */
	public ArrayList<Vehicle> parkedVehicles = new ArrayList<>(100);
	/**
	 * List of tram routes passing this edge.
	 */
	public ArrayList<String> tramRoutesRef = new ArrayList<>();
	/**
	 * List of bus routes passing this edge.
	 */
	public ArrayList<String> busRoutesRef = new ArrayList<>();
	/**
	 * Angle of edge
	 */
	public double angleOutward, angleInward;
	/**
	 * Whether there is one or more vehicle within the detection range of
	 * traffic light
	 */
	public boolean isDetectedVehicleForLight = false;
	/**
	 * Delay in seconds. This value can be used for calibration.
	 */
	public double delay;
	/**
	 * Number of lanes for right-turn
	 */
	public int numRightLanes;
	public int numRightOnlyLanes;
	/**
	 * Number of lanes for left-turn
	 */
	public int numLeftLanes;
	public int numLeftOnlyLanes;

	public Edge(final int importedStartNodeIndex, final int importedEndNodeIndex, final String type, final String name,
			final double maxspeed, final boolean roundabout, final List<String> tramRoutesRef,
			final List<String> busRoutesRef, final int numRightLanes, final int numLeftLanes,
			final int numRightOnlyLanes, final int numLeftOnlyLanes) {
		super();
		this.importedStartNodeIndex = importedStartNodeIndex;
		this.importedEndNodeIndex = importedEndNodeIndex;
		this.type = RoadType.valueOf(type);
		this.name = name;
		freeFlowSpeed = maxspeed;
		isRoundabout = roundabout;
		this.tramRoutesRef.addAll(tramRoutesRef);
		this.busRoutesRef.addAll(busRoutesRef);
		this.numLeftLanes = numLeftLanes;
		this.numLeftOnlyLanes = numLeftOnlyLanes;
		this.numRightLanes = numRightLanes;
		this.numRightOnlyLanes = numRightOnlyLanes;
	}
}

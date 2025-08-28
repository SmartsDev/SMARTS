package traffic.road;

import common.Settings;
import traffic.vehicle.Vehicle;
import traffic.vehicle.carfollow.SimpleCurve;

import javax.sound.sampled.Line;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

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
	private List<Point2D> intersectionPolygon = new ArrayList<>();
	private Map<Node, Line2D> stopLines = new HashMap<>();
	private Map<Node, Double> stopLineDists = new HashMap<>();
	private Map<Node, Line2D> leftPavement = new HashMap<>();
	private Map<Node, Line2D> rightPavement = new HashMap<>();
	private Map<String, SimpleCurve> curveMap = new HashMap<>();
	private Settings settings;

	public Node(Settings settings, final long osmId, final String name, final double lat, final double lon, final boolean light,
			final boolean tram_stop, final boolean bus_stop) {
		super();
		this.settings = settings;
		this.osmId = osmId;
		this.name = name;
		this.lat = lat;
		this.lon = lon;
		this.light = light;
		tramStop = tram_stop;
		busStop = bus_stop;
	}

	public void findIntersectionPolygon(){
		//TODO right driving implementation is not considered, For right driving just reverse the connected nodes order
		if(connectedNodes.size() < 3){
			return;
		}
		List<Node> nodesClockwise = getConnectedNodesInBearingOrder();
		for (int i = 0; i < nodesClockwise.size(); i++) {
			Node n = nodesClockwise.get(i);
			Node n1;
			if(i == 0){
				n1 = nodesClockwise.get(nodesClockwise.size()-1);
			}else {
				n1 = nodesClockwise.get((i - 1));
			}
			Node n2;
			if(i== nodesClockwise.size()-1){
				n2 = nodesClockwise.get(0);
			}else {
				n2 = nodesClockwise.get(i + 1);
			}
			Line2D l_n1 = getLeftPavement(n1);
			Line2D l_n_n1 = getRightPavement(n);
			Line2D l_n2 = getRightPavement(n2);
			Line2D l_n_n2 = getLeftPavement(n);
			Point2D rightCorner = getValidCorner(l_n1, l_n_n1);
			Point2D leftCorner = getValidCorner(l_n2, l_n_n2);

			Line2D stopLine = getStopLine(leftCorner, l_n_n2, rightCorner, l_n_n1);
			if(stopLine != null) {
				stopLines.put(n, stopLine);
				stopLineDists.put(n, getStopLineDist(n, stopLine));
				intersectionPolygon.add(RoadUtil.getIntersectionPoint(stopLine, l_n_n1));
				intersectionPolygon.add(RoadUtil.getIntersectionPoint(stopLine, l_n_n2));
			}
		}
	}

	private double getStopLineDist(Node n, Line2D stopLine){
		Line2D centreLine = new Line2D.Double(new Point2D.Double(lon, lat), new Point2D.Double(n.lon,n.lat));
		Point2D ip = RoadUtil.getIntersectionPoint(centreLine, stopLine);
		return RoadUtil.getDistInMeters(lat,lon, ip.getY(), ip.getX());
	}

	public double getIntersectionSize(Node n){
		if(stopLineDists.containsKey(n)) {
			return stopLineDists.get(n);
		}
		return 0;
	}

	public List<Node> getConnectedNodesInBearingOrder(){
		Map<Double, Node> connectedNodeMap = new HashMap<>();
		List<Node> connectedNodes = new ArrayList<>();
		for (int i = this.connectedNodes.size() - 1; i >= 0; i--) {
			Node con = this.connectedNodes.get(i);
			connectedNodeMap.put(RoadUtil.getClockwiseBearing(new Point2D.Double(lon, lat), new Point2D.Double(con.lon, con.lat)), con);
		}
		List<Double> keys = new ArrayList<>(connectedNodeMap.keySet());
		Collections.sort(keys);
		for (Double key : keys) {
			connectedNodes.add(connectedNodeMap.get(key));
		}
		return connectedNodes;
	}

	public Line2D getStopLine(Point2D leftCorner, Line2D leftPavement, Point2D rightCorner, Line2D rightPavement){
		double ratio = 0;
		boolean isLeft = true;
		if(leftCorner != null) {
			ratio = (leftCorner.getX() - leftPavement.getX1())/(leftPavement.getX2() - leftPavement.getX1());
		}
		if(rightCorner != null){
			double ratio2 = (rightCorner.getX() - rightPavement.getX1())/(rightPavement.getX2() - rightPavement.getX1());
			if(ratio2 > ratio){
				ratio = ratio2;
				isLeft = false;
			}
		}
		Point2D corner = null;
		Point2D otherCorner = null;
		if(isLeft){
			corner = leftCorner;
			otherCorner = new Point2D.Double(rightPavement.getX1() + ratio * (rightPavement.getX2()-rightPavement.getX1()),
					rightPavement.getY1() + ratio * (rightPavement.getY2()-rightPavement.getY1()));
		}else{
			corner = rightCorner;
			otherCorner = new Point2D.Double(leftPavement.getX1() + ratio * (leftPavement.getX2()-leftPavement.getX1()),
					leftPavement.getY1() + ratio * (leftPavement.getY2()-leftPavement.getY1()));
		}
		if(corner == null || otherCorner == null){
			return null;
		}
		return new Line2D.Double(corner, otherCorner);
	}

	public Line2D getLeftPavement(Node node){
		if(!leftPavement.containsKey(node)) {
			Edge inwardEdge = getInwardEdge(node);
			Edge outwardEdge = getOutwardEdge(node);
			Line2D pavement = null;
			Line2D directionCorrected = null;
			if (inwardEdge != null) {
				pavement = RoadUtil.getPavementGPS(inwardEdge.getLane(0), settings.isDriveOnLeft,
						settings.laneWidthInMeters, settings.lonVsLat, settings.pavementLineRatio);
				directionCorrected = new Line2D.Double(pavement.getP2(), pavement.getP1());
			} else {
				int l = outwardEdge.getLaneCount() - 1;
				pavement = RoadUtil.getPavementGPS(outwardEdge.getLane(outwardEdge.getLaneCount() - 1),
						settings.isDriveOnLeft, settings.laneWidthInMeters, settings.lonVsLat, settings.pavementLineRatio);
				directionCorrected = pavement;
			}
			leftPavement.put(node, directionCorrected);
		}
		return leftPavement.get(node);
	}

	public Line2D getRightPavement(Node node){
		if(!rightPavement.containsKey(node)) {
			Edge inwardEdge = getInwardEdge(node);
			Edge outwardEdge = getOutwardEdge(node);
			Line2D pavement = null;
			Line2D directionCorrected = null;
			if (outwardEdge != null) {
				pavement = RoadUtil.getPavementGPS(outwardEdge.getLane(0),
						settings.isDriveOnLeft, settings.laneWidthInMeters, settings.lonVsLat, settings.pavementLineRatio);
				directionCorrected = pavement;
			} else {
				pavement = RoadUtil.getPavementGPS(inwardEdge.getLane(inwardEdge.getLaneCount() - 1),
						settings.isDriveOnLeft, settings.laneWidthInMeters, settings.lonVsLat, settings.pavementLineRatio);
				directionCorrected = new Line2D.Double(pavement.getP2(), pavement.getP1());
			}
			rightPavement.put(node, directionCorrected);
		}
		return rightPavement.get(node);
	}

	public Point2D getValidCorner(Line2D pavement1, Line2D pavement2){
		Point2D corner = RoadUtil.getIntersectionPoint(pavement1, pavement2);
		if(corner == null){
			return null;
		}
		double check1 = (corner.getX() - pavement1.getX1())/(pavement1.getX2()-corner.getX());
		double check2 = (corner.getX() - pavement2.getX1())/(pavement2.getX2()-corner.getX());
		if(check1 < 0 || check2 < 0){
			return null;
		}
		return corner;
	}

	public List<Point2D> getIntersectionPolygon() {
		return intersectionPolygon;
	}

	public Edge getOutwardEdge(Node node){
		for (Edge outwardEdge : outwardEdges) {
			if(outwardEdge.endNode.index == node.index){
				return outwardEdge;
			}
		}
		return null;
	}

	public Edge getInwardEdge(Node node){
		for (Edge inwardEdge : inwardEdges) {
			if(inwardEdge.startNode.index == node.index){
				return inwardEdge;
			}
		}
		return null;
	}

	public Line2D getStopLine(Node n) {
		return stopLines.get(n);
	}

	public SimpleCurve getCurve(Vehicle v){
		Vehicle.IntersectionDecision decision = v.getDecision();
		if(decision != null){
			Lane curL = decision.getStartLane();
			Lane nextL = decision.getEndLane();
			String curveKey = getCurveKey(curL.edge, nextL.edge, curL, nextL);
			if(SimpleCurve.hasCurve(curL.edge, nextL.edge, curL.laneNumber, nextL.laneNumber)) {
				if (!curveMap.containsKey(curveKey)) {
					curveMap.put(curveKey, new SimpleCurve(curL, nextL));
				}
				return curveMap.get(curveKey);
			}
		}
		return null;
	}

	private String getCurveKey(Edge cur, Edge next, Lane curL, Lane nextL){
		return cur.index+"-"+next.index+"-"+curL.laneNumber+"-"+nextL.laneNumber;
	}
}

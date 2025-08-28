package traffic.road;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.Movement;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

public class RoadUtil {

	static class NodeAngleComparator implements Comparator<Node> {
		Node node;
		double lonVsLat;

		public NodeAngleComparator(final Node node, double lonVsLat) {
			super();
			this.node = node;
			this.lonVsLat = lonVsLat;
		}

		@Override
		public int compare(final Node n1, final Node n2) {

			final double n1Angle = Math.atan2((node.lat - n1.lat) * lonVsLat, node.lon - n1.lon);
			final double n2Angle = Math.atan2((node.lat - n2.lat) * lonVsLat, node.lon - n2.lon);

			return n1Angle < n2Angle ? -1 : n1Angle == n2Angle ? 0 : 1;
		}
	}

	/**
	 * Find the inward edges on the left of two links e1 and e2. e1's end node
	 * is e2's start node.
	 */
	static ArrayList<Edge> findInwardEdgesOnLeft(final Edge e1, final Edge e2) {
		final Node pivot = e1.endNode;
		final ArrayList<Edge> edgesOnLeftSide = new ArrayList<>();
		final int e2Index = pivot.connectedNodes.indexOf(e2.endNode);
		// Note: exclude e2's end node
		for (int i = 1; i < pivot.connectedNodes.size(); i++) {
			final Node connectedNode = pivot.connectedNodes.get((e2Index + i) % pivot.connectedNodes.size());
			// Break as soon as reaching e1's start node
			if (connectedNode == e1.startNode) {
				break;
			}
			for (final Edge e : pivot.inwardEdges) {
				if (e.startNode == connectedNode) {
					edgesOnLeftSide.add(e);
					break;
				}
			}
		}
		return edgesOnLeftSide;
	}

	/**
	 * Find the inward edges on the right of two links e1 and e2. e1's end node
	 * is e2's start node.
	 */
	static ArrayList<Edge> findInwardEdgesOnRight(final Edge e1, final Edge e2) {
		final Node pivot = e1.endNode;
		final ArrayList<Edge> edgesOnRightSide = new ArrayList<>();
		final int e1Index = pivot.connectedNodes.indexOf(e1.startNode);
		// Note: exclude e1's start node
		for (int i = 1; i < pivot.connectedNodes.size(); i++) {
			final Node connectedNode = pivot.connectedNodes.get((e1Index + i) % pivot.connectedNodes.size());
			// Stop when e2's end node is reached
			if (connectedNode == e2.endNode) {
				break;
			}
			for (final Edge e : pivot.inwardEdges) {
				if (e.startNode == connectedNode) {
					edgesOnRightSide.add(e);
					break;
				}
			}
		}
		return edgesOnRightSide;
	}

	/**
	 * Find the outward edges on the left of two links e1 and e2. e1's end node
	 * is e2's start node.
	 */
	public static ArrayList<Edge> findOutwardEdgesOnLeft(final Edge e1, final Edge e2) {
		final Node pivot = e1.endNode;
		final ArrayList<Edge> edgesOnLeftSide = new ArrayList<>();
		final int e2Index = pivot.connectedNodes.indexOf(e2.endNode);
		// Note: exclude e2's end node
		for (int i = 1; i < pivot.connectedNodes.size(); i++) {
			final Node connectedNode = pivot.connectedNodes.get((e2Index + i) % pivot.connectedNodes.size());
			// Stop when e1's start node is reached
			if (connectedNode == e1.startNode) {
				break;
			}
			for (final Edge e : pivot.outwardEdges) {
				if (e.endNode == connectedNode) {
					edgesOnLeftSide.add(e);
					break;
				}
			}
		}
		return edgesOnLeftSide;
	}

	/**
	 * Find the outward edges on the right of two links e1 and e2. e1's end node
	 * is e2's start node.
	 */
	public static ArrayList<Edge> findOutwardEdgesOnRight(final Edge e1, final Edge e2) {
		final Node pivot = e1.endNode;
		final ArrayList<Edge> edgesOnRightSide = new ArrayList<>();
		final int e1Index = pivot.connectedNodes.indexOf(e1.startNode);
		// Note: exclude e1's start node
		for (int i = 1; i < pivot.connectedNodes.size(); i++) {
			final Node connectedNode = pivot.connectedNodes.get((e1Index + i) % pivot.connectedNodes.size());
			// Stop when e2's end node is reached
			if (connectedNode == e2.endNode) {
				break;
			}
			for (final Edge e : pivot.outwardEdges) {
				if (e.endNode == connectedNode) {
					edgesOnRightSide.add(e);
					break;
				}
			}
		}
		return edgesOnRightSide;
	}

	/**
	 * Get the edges with potential conflicting traffic when a vehicle needs to
	 * turn onto e2 from e1
	 */
	public static ArrayList<Edge> getConflictingEdges(final Edge e1, final Edge e2, boolean isDriveOnLeft) {
		final ArrayList<Edge> conflictEdges = new ArrayList<>();
		final ArrayList<Edge> inwardEdgesR = findInwardEdgesOnRight(e1, e2);
		final ArrayList<Edge> inwardEdgesL = findInwardEdgesOnLeft(e1, e2);
		final ArrayList<Edge> outwardEdgesL = findOutwardEdgesOnLeft(e1, e2);
		final ArrayList<Edge> outwardEdgesR = findOutwardEdgesOnRight(e1, e2);
		Edge e1Adjacent = e1.endNode.getOutwardEdge(e1.startNode);

		if (isDriveOnLeft) {
			if (e1Adjacent != null) { //null check added
				// Drive on LEFT
				for (final Edge eR : inwardEdgesR) {
					List<Edge> eROutList = findOutwardEdgesOnRight(eR, e1Adjacent);
					if (isCrossingMovementsRed(eR, eROutList)) {
						continue;
					}
					if (eR.isRoundabout) {
						conflictEdges.add(eR);
					} else if (eR.type.priority > e1.type.priority) {
						conflictEdges.add(eR);
					} else if (eR.type.priority == e1.type.priority) {
					/*if (eR.name.length() > 0) {
						for (final Edge e : outwardEdgesL) {
							if (e.name.equals(eR.name)) {
								// Cross eR's road
								conflictEdges.add(eR);
								break;
							}
						}
					}*/
						if (!eR.name.equals(e1.name)) {
							// Cross eR's road
							conflictEdges.add(eR);
						}
					}
				}

				for (final Edge eL : inwardEdgesL) {
					List<Edge> eLOutList = findOutwardEdgesOnRight(eL, e2);
					if (e1.isRoundabout || isCrossingMovementsRed(eL, eLOutList)
						/*|| (eL.startNode == e2.endNode)*/) {
						continue;
					}
					if (eL.type.priority > e1.type.priority) {
						conflictEdges.add(eL);
					} else if (eL.type.priority == e1.type.priority && eL.name.equals(e1.name)
							&& isIncomingTrafficCrossingGreen(e1, e2)) {
						// Consider vehicle from opposite direction on same road when turning right under green light
						conflictEdges.add(eL);
					}
				}
			}
		}
		else {
			// Drive on RIGHT
			for (final Edge eL : inwardEdgesL) {
				List<Edge> eLOutList = findOutwardEdgesOnLeft(eL, e1Adjacent);
				if (isCrossingMovementsRed(eL, eLOutList)) {
					continue;
				}
				if (eL.isRoundabout) {
					conflictEdges.add(eL);
				} else if (eL.type.priority > e1.type.priority) {
					conflictEdges.add(eL);
				} else if (eL.type.priority == e1.type.priority) {
					/*if (eL.name.length() > 0) {
						for (final Edge e : outwardEdgesR) {
							if (e.name.equals(eL.name)) {
								// Cross eL's road
								conflictEdges.add(eL);
								break;
							}
						}
					}*/
					if(!eL.name.equals(e1.name)){
						conflictEdges.add(eL);
					}
				}
			}

			for (final Edge eR : inwardEdgesR) {
				List<Edge> eROutList = findOutwardEdgesOnRight(eR, e2);
				if (e1.isRoundabout || isCrossingMovementsRed(eR, eROutList)
						/*|| (eR.startNode == e2.endNode)*/) {
					continue;
				}
				if (eR.type.priority > e1.type.priority) {
					conflictEdges.add(eR);
				}else if (eR.type.priority == e1.type.priority && eR.name.equals(e1.name)
						&& isIncomingTrafficCrossingGreen(e1,e2)) {
					// Consider vehicle from opposite direction on same road when turning left under green light
					conflictEdges.add(eR);

				}
			}
		}

		return conflictEdges;
	}

	public static boolean isIncomingTrafficCrossingGreen(Edge inwardEdge, Edge outwardEdge){
		Movement movement = new Movement(Arrays.asList(new Edge[]{inwardEdge, outwardEdge}));
		LightColor color = inwardEdge.getMovementLight(movement);
		if(color == LightColor.GYR_G){
				return true;
		}
		return false;
	}

	public static boolean isCrossingMovementsRed(Edge inwardEdge, List<Edge> outwardEdges){
		for (Edge outwardEdge : outwardEdges) {
			Movement movement = new Movement(Arrays.asList(new Edge[]{inwardEdge, outwardEdge}));
			LightColor color = inwardEdge.getMovementLight(movement);
			if(!(color == LightColor.GYR_R || color == LightColor.KEEP_RED)){
				return false;
			}
		}
		return true;
	}

	/**
	 * Decides which lane in the target edge will be used. This method keeps the
	 * gap between a given lane and the fastest lane, e.g., the right-most lane
	 * in a keep-left system.
	 */
	public static int getLaneNumberForTargetEdge(Edge targetEdge, Edge currentEdge, int currentLaneNumber) {
		int currentLaneNumberFromOppositeSide = currentEdge.getLaneCount() - 1 - currentLaneNumber;
		int nextLaneNumberFromOppositeSide = currentLaneNumberFromOppositeSide;
		if (nextLaneNumberFromOppositeSide >= targetEdge.getLaneCount())
			nextLaneNumberFromOppositeSide = targetEdge.getLaneCount() - 1;
		int nextLaneNumber = targetEdge.getLaneCount() - 1 - nextLaneNumberFromOppositeSide;
		return nextLaneNumber;
	}

	/**
	 * Calculate the distance in meters between two coordinates.
	 */
	public static double getDistInMeters(final double lat1, final double lon1, final double lat2, final double lon2) {
		final double R = 6371000; // m
		final double dLat = Math.toRadians(lat2 - lat1);
		final double dLon = Math.toRadians(lon2 - lon1);

		final double a = (Math.sin(dLat / 2) * Math.sin(dLat / 2)) + (Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2));
		final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		final double d = R * c;

		return d;
	}

	/**
	 * Calculate GPS coordinates of the start point and the end point of a lane.
	 */
	public static double[] getLaneGPS(final Lane lane, boolean isDriveOnLeft, double laneWidthInMeters, double lonVsLat) {
		final Edge edge = lane.edge;
		final double[] points = new double[4];
		double rotateDegree = 90;
		if (!isDriveOnLeft) {
			rotateDegree = -90;
		}

		// GPS of start point of lane
		double extendedEdgeStartToEdgeRatio = 1;
		if (edge.isOnTwoWayRoad()) {
			extendedEdgeStartToEdgeRatio = ((-0.5 + (lane.edge.getLaneCount() - lane.laneNumber))
					* laneWidthInMeters) / edge.length;
		} else {
			extendedEdgeStartToEdgeRatio = ((-0.5 + ((lane.edge.getLaneCount() / 2.0) - lane.laneNumber))
					* laneWidthInMeters) / edge.length;
		}
		final double lonExtendedEdgeStart = edge.startNode.lon
				+ ((edge.endNode.lon - edge.startNode.lon) * extendedEdgeStartToEdgeRatio);
		final double latExtendedEdgeStart = (edge.startNode.lat
				+ ((edge.endNode.lat - edge.startNode.lat) * extendedEdgeStartToEdgeRatio)) * lonVsLat;
		final double[] ptStart = { lonExtendedEdgeStart, latExtendedEdgeStart };
		AffineTransform.getRotateInstance(Math.toRadians(rotateDegree), edge.startNode.lon,
				edge.startNode.lat * lonVsLat).transform(ptStart, 0, ptStart, 0, 1);
		ptStart[1] = ptStart[1] / lonVsLat;
		points[0] = ptStart[0];
		points[1] = ptStart[1];

		// GPS of end point of lane
		double extendedEdgeEndToEdgeRatio = 1;
		if (edge.isOnTwoWayRoad()) {
			extendedEdgeEndToEdgeRatio = 1
					+ (((-0.5 + (lane.edge.getLaneCount() - lane.laneNumber)) * laneWidthInMeters)
							/ edge.length);
		} else {
			extendedEdgeEndToEdgeRatio = 1
					+ (((-0.5 + ((lane.edge.getLaneCount() / 2.0) - lane.laneNumber)) * laneWidthInMeters)
							/ edge.length);
		}

		final Double lonExtendedEdgeEnd = edge.startNode.lon
				+ ((edge.endNode.lon - edge.startNode.lon) * extendedEdgeEndToEdgeRatio);
		final double latExtendedEdgeEnd = (edge.startNode.lat
				+ ((edge.endNode.lat - edge.startNode.lat) * extendedEdgeEndToEdgeRatio)) * lonVsLat;
		final double[] ptEnd = { lonExtendedEdgeEnd, latExtendedEdgeEnd };
		AffineTransform
				.getRotateInstance(Math.toRadians(rotateDegree), edge.endNode.lon, edge.endNode.lat * lonVsLat)
				.transform(ptEnd, 0, ptEnd, 0, 1);
		ptEnd[1] = ptEnd[1] / lonVsLat;
		points[2] = ptEnd[0];
		points[3] = ptEnd[1];

		return points;
	}

	/**
	 * Calculate GPS coordinates of the start point and the end point of a lane.
	 */
	public static Line2D getPavementGPS(final Lane lane, boolean isDriveOnLeft, double laneWidthInMeters,
										double lonVsLat, double pavementLineRatio) {
		final Edge edge = lane.edge;
		final double[] points = new double[4];
		double rotateDegree = 90;
		if (!isDriveOnLeft) {
			rotateDegree = -90;
		}
		double pavementLaneNumber;
		if(lane.laneNumber == 0) {
			pavementLaneNumber = lane.laneNumber - pavementLineRatio;
		}else{
			pavementLaneNumber = lane.laneNumber + pavementLineRatio;
		}

		// GPS of start point of lane
		double extendedEdgeStartToEdgeRatio = 1;
		if (edge.isOnTwoWayRoad()) {
			extendedEdgeStartToEdgeRatio = ((-0.5 + (lane.edge.getLaneCount() - pavementLaneNumber))
					* laneWidthInMeters) / edge.length;
		} else {
			extendedEdgeStartToEdgeRatio = ((-0.5 + ((lane.edge.getLaneCount() / 2.0) - pavementLaneNumber))
					* laneWidthInMeters) / edge.length;
		}
		final double lonExtendedEdgeStart = edge.startNode.lon
				+ ((edge.endNode.lon - edge.startNode.lon) * extendedEdgeStartToEdgeRatio);
		final double latExtendedEdgeStart = (edge.startNode.lat
				+ ((edge.endNode.lat - edge.startNode.lat) * extendedEdgeStartToEdgeRatio)) * lonVsLat;
		final double[] ptStart = { lonExtendedEdgeStart, latExtendedEdgeStart };
		AffineTransform.getRotateInstance(Math.toRadians(rotateDegree), edge.startNode.lon,
				edge.startNode.lat * lonVsLat).transform(ptStart, 0, ptStart, 0, 1);
		ptStart[1] = ptStart[1] / lonVsLat;
		points[0] = ptStart[0];
		points[1] = ptStart[1];

		// GPS of end point of lane
		double extendedEdgeEndToEdgeRatio = 1;
		if (edge.isOnTwoWayRoad()) {
			extendedEdgeEndToEdgeRatio = 1
					+ (((-0.5 + (lane.edge.getLaneCount() - pavementLaneNumber)) * laneWidthInMeters)
					/ edge.length);
		} else {
			extendedEdgeEndToEdgeRatio = 1
					+ (((-0.5 + ((lane.edge.getLaneCount() / 2.0) - pavementLaneNumber)) * laneWidthInMeters)
					/ edge.length);
		}

		final Double lonExtendedEdgeEnd = edge.startNode.lon
				+ ((edge.endNode.lon - edge.startNode.lon) * extendedEdgeEndToEdgeRatio);
		final double latExtendedEdgeEnd = (edge.startNode.lat
				+ ((edge.endNode.lat - edge.startNode.lat) * extendedEdgeEndToEdgeRatio)) * lonVsLat;
		final double[] ptEnd = { lonExtendedEdgeEnd, latExtendedEdgeEnd };
		AffineTransform
				.getRotateInstance(Math.toRadians(rotateDegree), edge.endNode.lon, edge.endNode.lat * lonVsLat)
				.transform(ptEnd, 0, ptEnd, 0, 1);
		ptEnd[1] = ptEnd[1] / lonVsLat;
		points[2] = ptEnd[0];
		points[3] = ptEnd[1];
		Line2D l = new Line2D.Double(new Point2D.Double(points[0], points[1]), new Point2D.Double(points[2], points[3]));
		return l;
	}

	public static double getLatitudeDegreePerMeter(final double latitude) {
		final double dist = getDistInMeters(latitude - 0.0005, 0, latitude + 0.0005, 0);
		return 0.001 / dist;
	}

	public static double getLongitudeDegreePerMeter(final double latitude) {
		final double dist = getDistInMeters(latitude, -0.0005, latitude, 0.0005);
		return 0.001 / dist;
	}

	public static double getMetersPerLongitudeDegree(final double longitude) {
		return 1.0 / getLongitudeDegreePerMeter(longitude);
	}

	public static boolean hasIntersectionAtEdgeStart(final Edge edge) {
		for (final Edge e : edge.startNode.inwardEdges) {
			if (e.name.equals(edge.name) && (e.type == edge.type)) {
				// Ignore the edge on the same road
				continue;
			}
			return true;
		}
		return false;
	}

	public static String importBuiltinRoadGraphFile(String inputBuiltinRoadGraph) {
		try {
			final InputStream inputStream = RoadUtil.class.getResourceAsStream(inputBuiltinRoadGraph);
			final Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			final StringBuilder sb = new StringBuilder();
			int numChars = -1;
			final char[] chars = new char[1000];
			do {
				numChars = reader.read(chars, 0, chars.length);
				if (numChars > 0) {
					sb.append(chars, 0, numChars);
				}
			} while (numChars > 0);
			return sb.toString();
		} catch (final UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


	public static ArrayList<Node> sortEdgesBasedOnAngle(final Node node, double lonVsLat) {
		final NodeAngleComparator nodeAngleComparator = new NodeAngleComparator(node, lonVsLat);
		final ArrayList<Node> connectedNodes = new ArrayList<>();
		for (final Edge e : node.outwardEdges) {
			connectedNodes.add(e.endNode);
		}
		for (final Edge e : node.inwardEdges) {
			if (!connectedNodes.contains(e.startNode)) {
				connectedNodes.add(e.startNode);
			}
		}
		Collections.sort(connectedNodes, nodeAngleComparator);

		return connectedNodes;
	}

	//TODO Need to consider Geo coordinate system, These are valid only for small distances

	public static Point2D getDividingPoint(Point2D p1, Point2D p2, double l, double m){
		return new Point2D.Double((p1.getX()*m + p2.getX()*l)/(l+m), (p1.getY()*m+p2.getY()*l)/(l+m));
	}

	public static Point2D getIntersectionPoint(Line2D l1, Line2D l2){
		double m1 = (l1.getY2() - l1.getY1())/(l1.getX2() - l1.getX1());
		double c1 = (l1.getY1()*l1.getX2() - l1.getY2()*l1.getX1())/(l1.getX2()-l1.getX1());
		double m2 = (l2.getY2() - l2.getY1())/(l2.getX2() - l2.getX1());
		double c2 = (l2.getY1()*l2.getX2() - l2.getY2()*l2.getX1())/(l2.getX2()-l2.getX1());

		if(m1 != m2){
			if(Double.isInfinite(m1) && Double.isInfinite(m2)){
				return null;
			}else if(Double.isInfinite(m1)){
				double x = l1.getX1();
				return new Point2D.Double(x, m2*x + c2);
			}else if(Double.isInfinite(m2)){
				double x = l2.getX1();
				return new Point2D.Double(x, m1*x + c1);
			}else {
				double x = (c2 - c1) / (m1 - m2);
				return new Point2D.Double(x, m1*x + c1);
			}
		}
		return null;
	}

	public static boolean isParalell(Line2D l1, Line2D l2, double delta){
		double diff =  getAngleDiff(l1, l2);
		if (diff < delta) {
			return true;
		}else if( diff > Math.PI - delta && diff < Math.PI + delta){
			return true;
		}else if( diff > 2 * Math.PI - delta){
			return true;
		}else{
			return false;
		}
	}

	public static double getSmallestDiff(Line2D l1, Line2D l2, double delta){
		double diff =  getAngleDiff(l1, l2);
		if (diff < delta) {
			return 0;
		}else if( diff > Math.PI - delta && diff < Math.PI + delta){
			return 0;
		}else if( diff > 2 * Math.PI - delta){
			return 0;
		}else if(diff > Math.PI){
			return diff - Math.PI;
		}else{
			return diff;
		}
	}

	public static double getClockwiseBearing(Point2D co1, Point2D co2){
		double arcTan = Math.atan2(co2.getY() - co1.getY(), co2.getX() - co1.getX());
		double bearing = ((5 *Math.PI / 2) - arcTan)/(2 * Math.PI);
		int intPart = (int)bearing;
		return (bearing - intPart) * 2 * Math.PI;
	}

	public static double getAngleDiff(Line2D l1, Line2D l2){
		double arcTan1 = Math.atan2(l1.getY2() - l1.getY1(), l1.getX2() - l1.getX1());
		double arcTan2 = Math.atan2(l2.getY2() - l2.getY1(), l2.getX2() - l2.getX1());
		return Math.abs(arcTan1 - arcTan2);
	}

	public static double getAntiClockwiseBearing(Point2D co1, Point2D co2){
		double arcTan = Math.atan2(co2.getY() - co1.getY(), co2.getX() - co1.getX());
		double bearing = ( arcTan + (3 * Math.PI / 2))/(2 * Math.PI);
		int intPart = (int) bearing;
		return (bearing - intPart) * 2 * Math.PI;
	}

	public static double getBackBearing(double bearing){
		if(bearing >= Math.PI *2){
			bearing -= Math.PI *2;
		}
		if(bearing >= Math.PI){
			return bearing - Math.PI;
		}else{
			return bearing + Math.PI;
		}
	}

	public static double[] findLineEquation(double latStart, double latEnd, double lonStart, double lonEnd){
		double [] line = {0,0,0}; // represent a, b, c

		line[0] = latStart - latEnd;
		line[1] = lonStart - lonEnd;
		line[2] = line[0]*lonStart+line[1]*latEnd;

		double sqrt = Math.sqrt(Math.pow(line[0],2) + Math.pow(line[0],2) + Math.pow(line[0],2));

		for (int i =0; i < 3; i++){
			line[i] /= sqrt;
		}
		return line;
	}
}

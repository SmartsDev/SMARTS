package traffic.road;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import common.Settings;
import traffic.light.LightColor;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

public class RoadUtil {

	static class NodeAngleComparator implements Comparator<Node> {
		Node node;

		public NodeAngleComparator(final Node node) {
			super();
			this.node = node;
		}

		@Override
		public int compare(final Node n1, final Node n2) {

			final double n1Angle = Math.atan2((node.lat - n1.lat) * Settings.lonVsLat, node.lon - n1.lon);
			final double n2Angle = Math.atan2((node.lat - n2.lat) * Settings.lonVsLat, node.lon - n2.lon);

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
	public static ArrayList<Edge> getConflictingEdges(final Edge e1, final Edge e2) {
		final ArrayList<Edge> conflictEdges = new ArrayList<>();
		final ArrayList<Edge> inwardEdgesR = findInwardEdgesOnRight(e1, e2);
		final ArrayList<Edge> inwardEdgesL = findInwardEdgesOnLeft(e1, e2);

		if (Settings.isDriveOnLeft) {
			// Drive on LEFT
			for (final Edge eInR : inwardEdgesR) {
				if ((eInR.lightColor == LightColor.GYR_R) || (eInR.lightColor == LightColor.KEEP_RED)) {
					continue;
				}
				if (eInR.isRoundabout) {
					conflictEdges.add(eInR);
				} else if (eInR.type.priority > e1.type.priority) {
					conflictEdges.add(eInR);
				} else if (eInR.type.priority == e1.type.priority && !eInR.name.equals(e1.name)) {
					conflictEdges.add(eInR);
				}
			}

			for (final Edge eInL : inwardEdgesL) {
				if (e1.isRoundabout || (eInL.lightColor == LightColor.GYR_R)
						|| (eInL.lightColor == LightColor.KEEP_RED)) {
					continue;
				}
				if (eInL.type.priority > e1.type.priority) {
					conflictEdges.add(eInL);
				} else if (eInL.type.priority == e1.type.priority && eInL.name.equals(e1.name)
						&& e1.lightColor == LightColor.GYR_G) {
					// Consider vehicle from opposite direction on same road when turning right under green light
					conflictEdges.add(eInL);
				}
			}
		} else {
			// Drive on RIGHT
			for (final Edge eInL : inwardEdgesL) {
				if ((eInL.lightColor == LightColor.GYR_R) || (eInL.lightColor == LightColor.KEEP_RED)) {
					continue;
				}
				if (eInL.isRoundabout) {
					conflictEdges.add(eInL);
				} else if (eInL.type.priority > e1.type.priority) {
					conflictEdges.add(eInL);
				} else if (eInL.type.priority == e1.type.priority && !eInL.name.equals(e1.name)) {
					conflictEdges.add(eInL);
				}
			}

			for (final Edge eInR : inwardEdgesR) {
				if (e1.isRoundabout || (eInR.lightColor == LightColor.GYR_R)
						|| (eInR.lightColor == LightColor.KEEP_RED)) {
					continue;
				}
				if (eInR.type.priority > e1.type.priority) {
					conflictEdges.add(eInR);
				} else if (eInR.type.priority == e1.type.priority && eInR.name.equals(e1.name)
						&& e1.lightColor == LightColor.GYR_G) {
					// Consider vehicle from opposite direction on same road when turning left under green light
					conflictEdges.add(eInR);
				}
			}
		}

		return conflictEdges;
	}

	/**
	 * Decides which lane in the target edge will be used. This method keeps the
	 * gap between a given lane and the fastest lane, e.g., the right-most lane
	 * in a keep-left system.
	 */
	public static int getLaneNumberForTargetEdge(Edge targetEdge, Edge currentEdge, int currentLaneNumber) {
		int currentLaneNumberFromOppositeSide = currentEdge.lanes.size() - 1 - currentLaneNumber;
		int nextLaneNumberFromOppositeSide = currentLaneNumberFromOppositeSide;
		if (nextLaneNumberFromOppositeSide >= targetEdge.lanes.size())
			nextLaneNumberFromOppositeSide = targetEdge.lanes.size() - 1;
		int nextLaneNumber = targetEdge.lanes.size() - 1 - nextLaneNumberFromOppositeSide;
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
	public static double[] getLaneGPS(final Lane lane) {
		final Edge edge = lane.edge;
		final double[] points = new double[4];
		double rotateDegree = 90;
		if (!Settings.isDriveOnLeft) {
			rotateDegree = -90;
		}

		// GPS of start point of lane
		double extendedEdgeStartToEdgeRatio = 1;
		if (RoadUtil.isOnTwoWayRoad(edge)) {
			extendedEdgeStartToEdgeRatio = ((-0.5 + (lane.edge.lanes.size() - lane.laneNumber))
					* Settings.laneWidthInMeters) / edge.length;
		} else {
			extendedEdgeStartToEdgeRatio = ((-0.5 + ((lane.edge.lanes.size() / 2.0) - lane.laneNumber))
					* Settings.laneWidthInMeters) / edge.length;
		}
		final double lonExtendedEdgeStart = edge.startNode.lon
				+ ((edge.endNode.lon - edge.startNode.lon) * extendedEdgeStartToEdgeRatio);
		final double latExtendedEdgeStart = (edge.startNode.lat
				+ ((edge.endNode.lat - edge.startNode.lat) * extendedEdgeStartToEdgeRatio)) * Settings.lonVsLat;
		final double[] ptStart = { lonExtendedEdgeStart, latExtendedEdgeStart };
		AffineTransform.getRotateInstance(Math.toRadians(rotateDegree), edge.startNode.lon,
				edge.startNode.lat * Settings.lonVsLat).transform(ptStart, 0, ptStart, 0, 1);
		ptStart[1] = ptStart[1] / Settings.lonVsLat;
		points[0] = ptStart[0];
		points[1] = ptStart[1];

		// GPS of end point of lane
		double extendedEdgeEndToEdgeRatio = 1;
		if (RoadUtil.isOnTwoWayRoad(edge)) {
			extendedEdgeEndToEdgeRatio = 1
					+ (((-0.5 + (lane.edge.lanes.size() - lane.laneNumber)) * Settings.laneWidthInMeters)
							/ edge.length);
		} else {
			extendedEdgeEndToEdgeRatio = 1
					+ (((-0.5 + ((lane.edge.lanes.size() / 2.0) - lane.laneNumber)) * Settings.laneWidthInMeters)
							/ edge.length);
		}

		final Double lonExtendedEdgeEnd = edge.startNode.lon
				+ ((edge.endNode.lon - edge.startNode.lon) * extendedEdgeEndToEdgeRatio);
		final double latExtendedEdgeEnd = (edge.startNode.lat
				+ ((edge.endNode.lat - edge.startNode.lat) * extendedEdgeEndToEdgeRatio)) * Settings.lonVsLat;
		final double[] ptEnd = { lonExtendedEdgeEnd, latExtendedEdgeEnd };
		AffineTransform
				.getRotateInstance(Math.toRadians(rotateDegree), edge.endNode.lon, edge.endNode.lat * Settings.lonVsLat)
				.transform(ptEnd, 0, ptEnd, 0, 1);
		ptEnd[1] = ptEnd[1] / Settings.lonVsLat;
		points[2] = ptEnd[0];
		points[3] = ptEnd[1];

		return points;
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

	public static String importBuiltinRoadGraphFile() {
		try {
			final InputStream inputStream = RoadUtil.class.getResourceAsStream(Settings.inputBuiltinRoadGraph);
			final Reader reader = new InputStreamReader(inputStream, "UTF-8");
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

	public static boolean isEdgeBlocked(final Edge edge) {
		for (final Lane l : edge.lanes) {
			if (!l.isBlocked) {
				return false;
			}
		}
		return true;
	}

	public static boolean isEdgeOnPathOfPriorityVehicle(final Edge edge) {
		for (final Lane l : edge.lanes) {
			if (l.isPriority) {
				return true;
			}
		}
		return false;
	}

	public static boolean isEdgeContainsPriorityVehicle(final Edge edge) {
		if (isEdgeOnPathOfPriorityVehicle(edge)) {
			for (final Lane l : edge.lanes) {
				for (Vehicle v : l.vehicles) {
					if (v.type == VehicleType.PRIORITY) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isOnTwoWayRoad(final Edge edge) {
		final Node startNode = edge.startNode;
		for (final Edge edgeToCheck : edge.endNode.outwardEdges) {
			if (edgeToCheck.endNode == startNode) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Node> sortEdgesBasedOnAngle(final Node node) {
		final NodeAngleComparator nodeAngleComparator = new NodeAngleComparator(node);
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
}

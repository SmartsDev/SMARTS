package traffic.routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeMap;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

/**
 * This class computes route based on a light-weight algorithm for generating
 * random traffic. Vehicles tend to stay on roads for a certain distance. The
 * minimum distance is higher for higher-priority roads. When it is possible to
 * change road, vehicles turn to the road link whose end point is closer to the
 * destination than the end points of other road links. This class extends the
 * default Routing class but it does not aim to find an exact route to the given
 * destination. The given destination could be reached with a low probability.
 *
 */
public class Simple extends Routing {

	public Simple(TrafficNetwork trafficNetwork) {
		super(trafficNetwork);
		// TODO Auto-generated constructor stub
	}

	Random random = new Random();
	double maxRouteLengthFactor = 2;// Maximum ratio of route length over
									// Euclidean distance between source and
									// destination
	double probabilityOfRandomDirection = 0.5;// Chance of picking a random
												// direction at intersection
	double distanceOnCurrentRoad;

	public ArrayList<RouteLeg> createCompleteRoute(Edge startEdge,
			Edge endEdge, VehicleType type) {
		Node currentNode = startEdge.startNode;
		Node destinationNode = endEdge.endNode;

		// Compute maximum possible route length
		double EuclideanDistSD = RoadUtil.getDistInMeters(currentNode.lat,
				currentNode.lon, destinationNode.lat, destinationNode.lon);
		double maximumRouteLength = EuclideanDistSD * maxRouteLengthFactor;

		ArrayList<RouteLeg> legsOnRoute = new ArrayList<RouteLeg>();
		legsOnRoute.add(new RouteLeg(startEdge, 0));
		distanceOnCurrentRoad = startEdge.length;
		currentNode = startEdge.endNode;
		HashSet<Edge> existingEdgesOnRoute = new HashSet<Edge>();

		while (currentNode != destinationNode
				&& RouteUtil.getTotalDistanceOfRoute(legsOnRoute) < maximumRouteLength) {

			// Last edge on existing route
			Edge lastEdge = null;
			if (legsOnRoute.size() > 0) {
				lastEdge = legsOnRoute.get(legsOnRoute.size() - 1).edge;
			}

			// List of candidate edges
			ArrayList<Edge> candidateEdgesForNextLeg = new ArrayList<Edge>();
			for (Edge e : currentNode.outwardEdges) {

				if (VehicleUtil.canGoThrough(e.startNode, e.endNode, type)
						&& !existingEdgesOnRoute.contains(e)) {
					candidateEdgesForNextLeg.add(e);
				}
			}

			// Find next edge from candidate edges
			Edge nextEdge = findNextEdge(candidateEdgesForNextLeg, lastEdge);
			if (nextEdge == null) {
				break;
			}

			// Append edge to existing route
			RouteLeg newLeg = new RouteLeg(nextEdge, 0);
			legsOnRoute.add(newLeg);
			existingEdgesOnRoute.add(nextEdge);

			// Proceed to next leg
			currentNode = newLeg.edge.endNode;
		}

		if (legsOnRoute.size() == 0) {
			return null;
		}

		// Remove repeated sections
		legsOnRoute = RouteUtil.removeRepeatSections(legsOnRoute);

		return legsOnRoute;
	}

	boolean isBeyondMinimumDistanceOnSameRoad(Edge lastEdge) {
		switch (lastEdge.type) {
		case motorway:
			if (distanceOnCurrentRoad > 2000)
				return true;
		case trunk:
			if (distanceOnCurrentRoad > 1000)
				return true;
		case primary:
			if (distanceOnCurrentRoad > 800)
				return true;
		case secondary:
			if (distanceOnCurrentRoad > 600)
				return true;
		case tertiary:
			if (distanceOnCurrentRoad > 400)
				return true;
		case residential:
			if (distanceOnCurrentRoad > 200)
				return true;
		case unclassified:
			if (distanceOnCurrentRoad > 100)
				return true;
		case service:
			if (distanceOnCurrentRoad > 20)
				return true;
		default:
			break;
		}
		return false;
	}

	Edge findNextEdge(ArrayList<Edge> candidateEdges, Edge lastEdge) {
		Edge nextEdge = null;

		Edge nextEdgeOnSameRoad = null;
		for (Edge edge : candidateEdges) {
			if (edge.name.equals(lastEdge.name) && edge.type == lastEdge.type) {
				nextEdgeOnSameRoad = edge;
				break;
			}
		}
		if (nextEdgeOnSameRoad == null
				|| isBeyondMinimumDistanceOnSameRoad(lastEdge)) {

			if (random.nextBoolean()) {
				double shortestDist = Double.MAX_VALUE;
				for (Edge edge : candidateEdges) {
					double dist = RoadUtil.getDistInMeters(edge.endNode.lat,
							edge.endNode.lon, lastEdge.endNode.lat,
							lastEdge.endNode.lon);
					if (dist < shortestDist) {
						shortestDist = dist;
						nextEdge = edge;
					}
				}
			}

		} else {
			nextEdge = nextEdgeOnSameRoad;

		}

		// Update distance value
		if (nextEdge != null) {
			if (nextEdge != nextEdgeOnSameRoad) {
				distanceOnCurrentRoad = nextEdge.length;
			} else {
				distanceOnCurrentRoad += nextEdge.length;
			}
		}

		return nextEdge;
	}
}

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
 * This class creates route based on a light-weight algorithm for generating
 * random traffic. This algorithm is not for creating exact routes from source
 * to destination. In this algorithm, vehicles tend to stay on roads for a
 * certain distance. The minimum distance is higher for higher-priority roads.
 * When it is possible to make a turn, vehicles tend to turn to the road with
 * the same or higher priority, e.g., leaving a secondary road and entering a
 * trunk road.
 *
 */
public class Simple extends Routing {

	public Simple(TrafficNetwork trafficNetwork) {
		super(trafficNetwork);
		// TODO Auto-generated constructor stub
	}

	Random random = new Random();
	double maxNumLegs = 40;
	double probAllowTurnToLowerPriorityRoad = 0.5;
	double distanceOnCurrentRoad;

	boolean isEdgeEndInRoute(Edge edgeToTest, HashSet<Edge> existingEdgesOnRoute) {
		for (Edge edge : existingEdgesOnRoute) {
			if (edgeToTest.endNode == edge.startNode)
				return true;
		}
		return false;
	}

	boolean isNextEdgeWithProperPriority(Edge lastEdge, Edge nextEdge) {
		if (nextEdge.type.priority >= lastEdge.type.priority) {
			return true;
		} else {
			if (random.nextDouble() < probAllowTurnToLowerPriorityRoad) {
				return true;
			} else {
				return false;
			}
		}

	}

	public ArrayList<RouteLeg> createCompleteRoute(Edge startEdge,
			Edge endEdge, VehicleType type) {
		Node currentNode = startEdge.startNode;
		Node destinationNode = endEdge.endNode;

		ArrayList<RouteLeg> legsOnRoute = new ArrayList<RouteLeg>();
		legsOnRoute.add(new RouteLeg(startEdge, 0));
		distanceOnCurrentRoad = startEdge.length;
		currentNode = startEdge.endNode;
		HashSet<Edge> existingEdgesOnRoute = new HashSet<Edge>();

		while (currentNode != destinationNode
				&& legsOnRoute.size() < maxNumLegs) {

			// Last edge on existing route
			Edge lastEdge = null;
			if (legsOnRoute.size() > 0) {
				lastEdge = legsOnRoute.get(legsOnRoute.size() - 1).edge;
			}

			// List of candidate edges
			ArrayList<Edge> candidateEdgesForNextLeg = new ArrayList<Edge>();
			for (Edge e : currentNode.outwardEdges) {
				if (VehicleUtil.canGoThrough(e.startNode, e.endNode, type)
						&& !isEdgeEndInRoute(e, existingEdgesOnRoute)
						&& isNextEdgeWithProperPriority(lastEdge, e)) {
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
			if (distanceOnCurrentRoad > 50)
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
		if (candidateEdges.size() == 0) {
			return null;
		}

		Edge nextEdgeOnSameRoad = null;
		for (Edge edge : candidateEdges) {
			if (edge.name.equals(lastEdge.name) && edge.type == lastEdge.type) {
				nextEdgeOnSameRoad = edge;
				break;
			}
		}

		Edge nextEdgeOnRoute = null;

		if (isBeyondMinimumDistanceOnSameRoad(lastEdge)
				|| nextEdgeOnSameRoad == null) {
			nextEdgeOnRoute = candidateEdges.get(random.nextInt(candidateEdges
					.size()));
		} else {
			nextEdgeOnRoute = nextEdgeOnSameRoad;
		}

		// Update distance value
		if (nextEdgeOnRoute != null) {
			if (nextEdgeOnRoute != nextEdgeOnSameRoad) {
				distanceOnCurrentRoad = nextEdgeOnRoute.length;
			} else {
				distanceOnCurrentRoad += nextEdgeOnRoute.length;
			}
		}

		return nextEdgeOnRoute;
	}
}

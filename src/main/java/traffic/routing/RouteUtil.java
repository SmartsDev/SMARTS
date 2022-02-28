package traffic.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import common.Settings;
import processor.communication.message.SerializableRouteLeg;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadType;
import traffic.vehicle.VehicleType;

public class RouteUtil {

	public static RouteLeg createRouteLeg(final int edgeIndex,
			final double stopDuration, final ArrayList<Edge> edges) {
		return new RouteLeg(edges.get(edgeIndex), stopDuration);
	}

	public static double getTotalDistanceOfRoute(
			final ArrayList<RouteLeg> routeLegs) {
		double distance = 0;
		for (int i = 1; i < routeLegs.size(); i++) {
			final RouteLeg leg = routeLegs.get(i);
			distance += leg.edge.length;
		}
		return distance;
	}

	static Node findRepeatedNode(final ArrayList<RouteLeg> routeLegs) {
		final HashMap<Integer, Integer> indexList = new HashMap<>();
		indexList.put(routeLegs.get(0).edge.startNode.index,
				routeLegs.get(0).edge.startNode.index);
		for (int i = 1; i < routeLegs.size(); i++) {
			final RouteLeg leg = routeLegs.get(i);
			if (indexList.containsKey(leg.edge.endNode.index)) {
				return leg.edge.endNode;
			} else {
				indexList.put(leg.edge.endNode.index, leg.edge.endNode.index);
			}
		}
		return null;
	}

	public static Routing.Algorithm getRoutingAlgorithmFromString(
			final String selected) {
		for (final Routing.Algorithm algorithm : Routing.Algorithm.values()) {
			if (selected.equalsIgnoreCase(algorithm.name())) {
				return algorithm;
			}
		}
		return Routing.Algorithm.DIJKSTRA;
	}

	public static ArrayList<RouteLeg> parseReceivedRoute(
			final ArrayList<SerializableRouteLeg> routeLegs,
			final ArrayList<Edge> edges) {
		final ArrayList<RouteLeg> route = new ArrayList<>();
		for (final SerializableRouteLeg routeLeg : routeLegs) {
			route.add(createRouteLeg(routeLeg.edgeIndex, routeLeg.stopover,
					edges));
		}
		return route;
	}

	public static ArrayList<RouteLeg> removeRepeatSections(
			final ArrayList<RouteLeg> routeLegs) {
		Node repeatedNode = findRepeatedNode(routeLegs);
		while (repeatedNode != null) {
			final Iterator<RouteLeg> iterator = routeLegs.iterator();
			boolean isToRemove = false;
			while (iterator.hasNext()) {
				final RouteLeg leg = iterator.next();
				if (repeatedNode == leg.edge.startNode) {
					isToRemove = true;
				}
				if (isToRemove) {
					iterator.remove();
					if (leg.edge.endNode == repeatedNode) {
						break;
					}
				}
			}
			if (routeLegs.size() == 0) {
				return null;
			}
			repeatedNode = findRepeatedNode(routeLegs);
		}

		return routeLegs;

	}
	
	
	
}

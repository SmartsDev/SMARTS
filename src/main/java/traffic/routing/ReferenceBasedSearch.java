package traffic.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.vehicle.VehicleType;

public class ReferenceBasedSearch {
	static Random random = new Random();

	static ArrayList<RouteLeg> createOneRoute(final VehicleType transport, final String ref,
			final ArrayList<Edge> startEdgesOfRandomRoute, final ArrayList<Edge> endEdgesOfRandomRoute) {
		final Edge startEdge = startEdgesOfRandomRoute.get(random.nextInt(startEdgesOfRandomRoute.size())); // 
		Edge endEdge = endEdgesOfRandomRoute.get(random.nextInt(endEdgesOfRandomRoute.size()));
		final ArrayList<RouteLeg> routeLegs = new ArrayList<>();
		final HashMap<Integer, Integer> nodesOnRoute = new HashMap<>();
		routeLegs.add(new RouteLeg(startEdge, 0));
		if (startEdge == endEdge) {
			return routeLegs;
		}
		nodesOnRoute.put(startEdge.startNode.index, null);
		nodesOnRoute.put(startEdge.endNode.index, null);
		Edge nextEdge = pickNextEdgeOnPublicTransportRoute(startEdge.endNode, ref, transport, nodesOnRoute);

		while (nextEdge != null) {
			routeLegs.add(new RouteLeg(nextEdge, 0));
			nodesOnRoute.put(nextEdge.endNode.index, null);
			if (nextEdge == endEdge) {
				break;
			}
			nextEdge = pickNextEdgeOnPublicTransportRoute(nextEdge.endNode, ref, transport, nodesOnRoute);
		}

		if (routeLegs.get(routeLegs.size() - 1).edge == endEdge) {
			return routeLegs;
		} else {
			return null;
		}
	}

	public static ArrayList<RouteLeg> createRoute(final VehicleType transport, final String ref,
			final ArrayList<Edge> startEdgesOfRandomRoute, final ArrayList<Edge> endEdgesOfRandomRoute) {
		ArrayList<RouteLeg> routeLegs = null;
		for (int i = 0; i < 3; i++) {
			routeLegs = createOneRoute(transport, ref, startEdgesOfRandomRoute, endEdgesOfRandomRoute);
			if (routeLegs != null) {
				break;
			}
		}
		return routeLegs;
	}

	/**
	 * Pick the next edge on pre-defined public transport route. To prevent
	 * vehicle circles between two adjacent nodes, the next edge should not: 1)
	 * lead to the previous node; 2) already exist on the route.
	 */
	static Edge pickNextEdgeOnPublicTransportRoute(final Node startNode, final String ref, final VehicleType transport,
			final HashMap<Integer, Integer> nodesOnRoute) {
		final ArrayList<Edge> options = new ArrayList<>();
		for (final Edge e : startNode.outwardEdges) {

			if (transport == VehicleType.TRAM) {
				if (e.tramRoutesRef.contains(ref) && !nodesOnRoute.containsKey(e.endNode.index)) {
					options.add(e);
				}
			}
			if (transport == VehicleType.BUS) {
				if (e.busRoutesRef.contains(ref) && !nodesOnRoute.containsKey(e.endNode.index)) {
					options.add(e);
				}
			}
		}
		if (options.size() > 0) {
			return options.get(random.nextInt(options.size()));
		}
		return null;
	}

}

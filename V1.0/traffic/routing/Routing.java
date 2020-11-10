package traffic.routing;

import java.util.ArrayList;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

public abstract class Routing {
	public enum Algorithm {
		DIJKSTRA, RANDOM_A_STAR
	}

	TrafficNetwork trafficNetwork;;

	public Routing(final TrafficNetwork trafficNetwork) {
		this.trafficNetwork = trafficNetwork;
	}

	public abstract ArrayList<RouteLeg> createCompleteRoute(Edge startEdge, Edge endEdge, VehicleType type);

	/**
	 * Create a new route from a given vehicle's current edge to its
	 * destination. The new route must be different to the old route.
	 */
	public void reRoute(Vehicle vehicle) {

		ArrayList<RouteLeg> oldRoute = vehicle.routeLegs;
		int currentIndexOnOldRoute = vehicle.indexLegOnRoute;

		// No re-route if vehicle is on last leg
		if (currentIndexOnOldRoute >= oldRoute.size() - 1) {
			return;
		}

		// Copy earlier parts of old route to new route
		ArrayList<RouteLeg> newRoute = new ArrayList<RouteLeg>();
		for (int i = 0; i <= currentIndexOnOldRoute; i++) {
			newRoute.add(oldRoute.get(i));
		}

		// Try a few times for computing new route. 
		for (int i = 0; i < 3; i++) {
			ArrayList<RouteLeg> partialRoute = createCompleteRoute(oldRoute.get(currentIndexOnOldRoute + 1).edge,
					oldRoute.get(oldRoute.size() - 1).edge, vehicle.type);
			// The next leg on the old route cannot be the next leg on the new route!
			if (partialRoute != null && partialRoute.get(0).edge != oldRoute.get(currentIndexOnOldRoute + 1).edge) {
				newRoute.addAll(partialRoute);
				vehicle.routeLegs = newRoute;
				break;
			}
		}

	}

}

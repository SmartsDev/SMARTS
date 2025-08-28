package traffic.routing;

import java.util.ArrayList;
import java.util.List;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

public abstract class Routing {
	public enum Algorithm {
		DIJKSTRA_LPF, RANDOM_A_STAR, DIJKSTRA
	}

	TrafficNetwork trafficNetwork;
	protected Settings settings;

    public Routing(final TrafficNetwork trafficNetwork) {
		this.trafficNetwork = trafficNetwork;
		this.settings = trafficNetwork.getSettings();
	}

	public abstract ArrayList<RouteLeg> createCompleteRoute(Node start, Node end, VehicleType type);



}

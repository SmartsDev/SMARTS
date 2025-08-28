package traffic.light;

import java.util.*;

import common.Settings;
import processor.SimulationListener;
import processor.communication.message.SerializableInt;
import traffic.TrafficNetwork;
import traffic.light.manager.TLManager;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.vehicle.lanechange.MOBIL;

/**
 * This class handles the initialization of traffic lights and the switch of
 * light colors. A light controls the traffic of a specific inward edge.
 *
 */
public class LightCoordinator {

	List<List<Node>> nodeGroups = new ArrayList<>();

	/**
	 * Groups of traffic lights. Lights in the same group are within a certain
	 * distance to each other.
	 */
	public Map<Node, TrafficLightCluster> clusterMap = new HashMap<>();
	public List<TrafficLightCluster> lightClusters = new ArrayList<>();
	private TLManager tlManager;

	public void addRemoveLights(List<Node> nodes, List<SerializableInt> lightNodes) {
		List<Integer> lightNodeIndices = new ArrayList<>();
		for (SerializableInt lightNode : lightNodes) {
			lightNodeIndices.add(lightNode.value);
		}
		for (Node node : nodes) {
			if(lightNodeIndices.contains(node.index)){
				node.light = true;
			}else{
				node.light = false;
			}
		}
	}

	/**
	 * Group nodes with traffic signals. Nodes in the same group are within
	 * certain distance to each other.
	 */
	void groupAdjacentNodes(final List<Node> lightNodes, TrafficLightTiming trafficLightTiming, double maxLightGroupRadius) {
		nodeGroups.clear();

		for (final Node node : lightNodes) {
			node.idLightNodeGroup = 0;
		}
		if (trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}
		/*
		 * For each node with traffic light, find other nodes with lights within
		 * a certain distance.
		 */
		final HashSet<Node> checkedNodes = new HashSet<>();
		for (final Node node : lightNodes) {
			if (checkedNodes.contains(node)) {
				continue;
			}
			final List<Node> nodeGroup = new ArrayList<>();
			final long idNodeGroup = node.osmId;

			nodeGroup.add(node);
			node.idLightNodeGroup = idNodeGroup;

			checkedNodes.add(node);

			for (final Node otherNode : lightNodes) {
				if (checkedNodes.contains(otherNode)) {
					continue;
				}
				if (RoadUtil.getDistInMeters(node.lat, node.lon, otherNode.lat,
						otherNode.lon) < maxLightGroupRadius) {
					nodeGroup.add(otherNode);
					otherNode.idLightNodeGroup = idNodeGroup;

					checkedNodes.add(otherNode);
				}
			}
			nodeGroups.add(nodeGroup);
		}
	}

	/**
	 * Group inward edges based on the street names in node groups.
	 */
	void groupInwardEdges(TrafficLightTiming trafficLightTiming) {
		lightClusters.clear();
		if (trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}

		/*
		 * Identify the lights on the same street based on the name of the
		 * links.
		 */
		for (List<Node> nodeGroup : nodeGroups) {

			List<Movement> moveList = findMovementsInsideNodeGroup(nodeGroup);

			TrafficLightCluster cluster = new TrafficLightCluster(moveList);
			for (Node node : nodeGroup) {
				clusterMap.put(node, cluster);
			}
			lightClusters.add(cluster);

		}
	}

	private List<Movement> findMovementsInsideNodeGroup(List<Node> nodeGroup){
		List<Movement> fullMovements = new ArrayList<>();
		List<Movement> partialMovements = new ArrayList<>();
		for (Node node : nodeGroup) {
			for (Edge inEdge : node.inwardEdges) {
				if(!nodeGroup.contains(inEdge.startNode)){
					Movement movement = new Movement();
					movement.addEdge(inEdge);
					partialMovements.add(movement);
				}
			}
		}
		/** while(!partialMovements.isEmpty()){
			Movement partial = partialMovements.remove(0);
			Node end = partial.getEndNode();
			if(nodeGroup.contains(end)){
				for (Edge outwardEdge : end.outwardEdges) {
					if (outwardEdge.endNode != end) {
						Movement newMove = partial.clone();
						newMove.addEdge(outwardEdge);
						partialMovements.add(newMove);
					}
				}
			}else{
				fullMovements.add(partial);
			}
		} **/

		for (Movement movement : partialMovements){

			for (Edge nextRoad : movement.getEdges()){
				List<Edge> visited = new ArrayList<>();
				Deque<Edge> stack = new ArrayDeque<Edge>();
				stack.push(nextRoad);

				while(!stack.isEmpty()){

					Edge edge = stack.peek();

					if ( !nodeGroup.contains(edge.endNode)){
						//Create movement list
						ArrayList<Edge> movementList = new ArrayList<>();
						for (Edge element : stack){
							movementList.add(0,element);
						}
						Movement fullmovement = new Movement(movementList);
						fullMovements.add(fullmovement);
						stack.pop();
					}
					else {
						boolean isAllvisited = true;
						for (Edge otherEdge : edge.endNode.outwardEdges) {
							if (!visited.contains(otherEdge) && (nextRoad.endNode != otherEdge.endNode)) {
								stack.push(otherEdge);
								visited.add(otherEdge);
								isAllvisited = false;
								break;
							}
						}

						if (isAllvisited) {
							stack.pop();
						}
					}
				}
			}
		}

		return fullMovements;
	}

	/**
	 * Initialize traffic lights. Group inward edges based on street names.
	 * There can be multiple streets at the same intersection. Only one street
	 * can get green light in a group at any time. During initialization, the
	 * first street gets green light and other streets get red lights.
	 *
	 */
	public void init(TrafficNetwork trafficNetwork, List<Node> mapNodes, List<SerializableInt> lightNodes) {
		// Add or remove lights
		addRemoveLights(mapNodes, lightNodes);

		// Groups adjacent nodes with traffic lights based on distance.
		groupAdjacentNodes(mapNodes, trafficNetwork.getSettings().trafficLightTiming, trafficNetwork.getSettings().maxLightGroupRadius);
		System.out.println("Grouped adjacent traffic lights.");

		// Groups inward edges of the grouped nodes.
		groupInwardEdges(trafficNetwork.getSettings().trafficLightTiming);
		System.out.println("Divided lights in each light group based on street names.");

		tlManager = trafficNetwork.getSettings().getLightScheduler();
		if(tlManager != null){
			tlManager.setClusters(lightClusters);
			tlManager.init(trafficNetwork);
		}
	}

	public void scheduleLights(double timeNow){
		if(tlManager != null){
			tlManager.schedule(timeNow);
		}
	}

	/**
	 * Compute how long the active streets have been given their current color.
	 * Change the color and/or change active streets depending on the situation.
	 */
	public void updateLights(double timeNow) {
		for (TrafficLightCluster lightCluster : lightClusters) {
			lightCluster.updateLights(timeNow);
		}
	}

	public TLManager getTlManager() {
		return tlManager;
	}
}

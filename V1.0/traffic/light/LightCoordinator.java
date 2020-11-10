package traffic.light;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import common.Settings;
import processor.communication.message.SerializableInt;
import processor.worker.Workarea;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadUtil;

/**
 * This class handles the initialization of traffic lights and the switch of
 * light colors. A light controls the traffic of a specific inward edge.
 *
 */
public class LightCoordinator {
	/**
	 * A group of lights consists of one or more sub-groups. The sub-groups are
	 * organized based on street name. At any time, only one of the sub-groups
	 * is in a green-yellow-red cycle. This sub-group is called the active
	 * sub-group (or active street). All other sub-groups remain in red color.
	 */
	public class LightGroup {
		public ArrayList<ArrayList<Edge>> edgeGroups = new ArrayList<>();
		/**
		 * This identifies the active street, i.e., a street in green-yellow-red
		 * cycle. Non-active streets always get red lights.
		 */
		public int inwardEdgeGroupIndexForGYR;
		double trafficSignalTimerGYR;
		double trafficSignalAccumulatedGYRTime;

		public LightGroup(final ArrayList<ArrayList<Edge>> groups) {
			edgeGroups = groups;
		}
	}

	ArrayList<ArrayList<Node>> nodeGroups = new ArrayList<>();

	/**
	 * Groups of traffic lights. Lights in the same group are within a certain
	 * distance to each other.
	 */
	public ArrayList<LightGroup> lightGroups = new ArrayList<>();

	public void addRemoveLights(final ArrayList<Node> nodes, final ArrayList<SerializableInt> indexNodesToAddLight,
			final ArrayList<SerializableInt> indexNodesToRemoveLight) {
		for (final SerializableInt si : indexNodesToAddLight) {
			nodes.get(si.value).light = true;
		}
		for (final SerializableInt si : indexNodesToRemoveLight) {
			nodes.get(si.value).light = false;
		}
	}

	/**
	 * Group nodes with traffic signals. Nodes in the same group are within
	 * certain distance to each other.
	 */
	void groupAdjacentNodes(final ArrayList<Node> mapNodes, final Workarea workarea) {
		nodeGroups.clear();

		for (final Node node : mapNodes) {
			node.idLightNodeGroup = 0;
		}

		if (Settings.trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}

		/*
		 * Collects the nodes with traffic lights in the current work area
		 */
		final ArrayList<Node> lightNodes = new ArrayList<>();
		for (final Node node : mapNodes) {
			if (workarea.workCells.contains(node.gridCell) && node.light && (node.inwardEdges.size() > 0)) {
				lightNodes.add(node);
			}
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
			final ArrayList<Node> nodeGroup = new ArrayList<>();
			final long idNodeGroup = node.osmId;

			nodeGroup.add(node);
			node.idLightNodeGroup = idNodeGroup;

			checkedNodes.add(node);

			for (final Node otherNode : lightNodes) {
				if (checkedNodes.contains(otherNode)) {
					continue;
				}
				if (RoadUtil.getDistInMeters(node.lat, node.lon, otherNode.lat,
						otherNode.lon) < Settings.maxLightGroupRadius) {
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
	void groupInwardEdges() {
		lightGroups.clear();
		if (Settings.trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}

		/*
		 * Identify the lights on the same street based on the name of the
		 * links.
		 */
		for (final ArrayList<Node> nodeGroup : nodeGroups) {
			final HashMap<String, ArrayList<Edge>> inwardEdgeGroupsHashMap = new HashMap<>();

			for (final Node node : nodeGroup) {
				for (final Edge edge : node.inwardEdges) {
					if (!inwardEdgeGroupsHashMap.containsKey(edge.name)) {
						inwardEdgeGroupsHashMap.put(edge.name, new ArrayList<Edge>());
					}
					inwardEdgeGroupsHashMap.get(edge.name).add(edge);
					edge.isDetectedVehicleForLight = false;
				}
			}

			final ArrayList<ArrayList<Edge>> inwardEdgeGroupsArrayList = new ArrayList<>();
			inwardEdgeGroupsArrayList.addAll(inwardEdgeGroupsHashMap.values());

			lightGroups.add(new LightGroup(inwardEdgeGroupsArrayList));

		}
	}

	/**
	 * Initialize traffic lights. Group inward edges based on street names.
	 * There can be multiple streets at the same intersection. Only one street
	 * can get green light in a group at any time. During initialization, the
	 * first street gets green light and other streets get red lights.
	 *
	 */
	public void init(final ArrayList<Node> mapNodes, final ArrayList<SerializableInt> indexNodesToAddLight,
			final ArrayList<SerializableInt> indexNodesToRemoveLight, final Workarea workarea) {
		// Add or remove lights
		addRemoveLights(mapNodes, indexNodesToAddLight, indexNodesToRemoveLight);

		// Reset timer of light groups
		for (final LightGroup egbn : lightGroups) {
			resetGYR(egbn);
		}

		// Groups adjacent nodes with traffic lights based on distance.
		groupAdjacentNodes(mapNodes, workarea);
		System.out.println("Grouped adjacent traffic lights.");

		// Groups inward edges of the grouped nodes.
		groupInwardEdges();
		System.out.println("Divided lights in each light group based on street names.");

		// Set green color to the first street at any light group
		for (final LightGroup egbn : lightGroups) {
			egbn.inwardEdgeGroupIndexForGYR = 0;
			setGYR(egbn, LightColor.GYR_G);
		}

	}

	/**
	 * Check whether the current active approach has a priority vehicle.
	 */
	boolean isPriorityVehicleInActiveApproach(final LightGroup egbn) {
		for (final Edge e : egbn.edgeGroups.get(egbn.inwardEdgeGroupIndexForGYR)) {
			if (RoadUtil.isEdgeContainsPriorityVehicle(e)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether inactive approaches have a priority vehicle.
	 */
	boolean isPriorityVehicleInInactiveApproach(final LightGroup egbn) {
		for (int i = 0; i < egbn.edgeGroups.size(); i++) {
			for (final Edge e : egbn.edgeGroups.get(i)) {
				if (RoadUtil.isEdgeContainsPriorityVehicle(e)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether there is vehicle coming to the current approach under
	 * active control.
	 */
	boolean isTrafficExistAtActiveStreet(final LightGroup egbn) {
		for (final Edge e : egbn.edgeGroups.get(egbn.inwardEdgeGroupIndexForGYR)) {
			if (e.isDetectedVehicleForLight) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether there is incoming vehicle at conflicting approaches.
	 */
	boolean isTrafficExistAtNonActiveStreet(final LightGroup egbn) {
		for (int i = 0; i < egbn.edgeGroups.size(); i++) {
			if (i == egbn.inwardEdgeGroupIndexForGYR) {
				continue;
			}
			for (final Edge e : egbn.edgeGroups.get(i)) {
				if (e.isDetectedVehicleForLight) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Reset timer of all light groups.
	 *
	 */
	public void resetGYR(final LightGroup edgeGroupsAtNode) {
		for (final ArrayList<Edge> edgeGraoup : edgeGroupsAtNode.edgeGroups) {
			for (final Edge edge : edgeGraoup) {
				edge.lightColor = LightColor.GYR_G;
			}
		}
		edgeGroupsAtNode.trafficSignalTimerGYR = LightColor.GYR_G.minDynamicTime;
		edgeGroupsAtNode.trafficSignalAccumulatedGYRTime = 0;
	}

	/**
	 * Set the color of an active street and initialize the timer for the color.
	 * Non-active streets get red lights.
	 */
	public void setGYR(final LightGroup edgeGroupsAtNode, final LightColor type) {
		for (int i = 0; i < edgeGroupsAtNode.edgeGroups.size(); i++) {
			if (i == edgeGroupsAtNode.inwardEdgeGroupIndexForGYR) {
				for (final Edge edge : edgeGroupsAtNode.edgeGroups.get(i)) {
					edge.lightColor = type;
				}
			} else {
				for (final Edge edge : edgeGroupsAtNode.edgeGroups.get(i)) {
					edge.lightColor = LightColor.KEEP_RED;
				}
			}
		}
		edgeGroupsAtNode.trafficSignalAccumulatedGYRTime = 0;
		if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
			edgeGroupsAtNode.trafficSignalTimerGYR = type.minDynamicTime;
		} else if (Settings.trafficLightTiming == TrafficLightTiming.FIXED) {
			edgeGroupsAtNode.trafficSignalTimerGYR = type.fixedTime;
		}
	}

	/**
	 * Compute how long the active streets have been given their current color.
	 * Change the color and/or change active streets depending on the situation.
	 */
	public void updateLights() {
		final double secEachStep = 1 / Settings.numStepsPerSecond;
		for (int i = 0; i < lightGroups.size(); i++) {
			final LightGroup egbn = lightGroups.get(i);
			egbn.trafficSignalAccumulatedGYRTime += secEachStep;

			final Edge anEdgeInActiveApproach = egbn.edgeGroups.get(egbn.inwardEdgeGroupIndexForGYR).get(0);
			if (isPriorityVehicleInInactiveApproach(egbn) && !isPriorityVehicleInActiveApproach(egbn)) {
				// Grant green light to an inactive approach it has priority vehicle and the current active approach does not have one
				egbn.inwardEdgeGroupIndexForGYR = getEdgeGroupIndexOfPriorityInactiveApproach(egbn);
				setGYR(egbn, LightColor.GYR_G);
			}
			if (!isPriorityVehicleInInactiveApproach(egbn) && isPriorityVehicleInActiveApproach(egbn)) {
				// Grant green light to current active approach if it has a priority vehicle and inactive approaches do not have priority vehicle
				setGYR(egbn, LightColor.GYR_G);
			}

			if (anEdgeInActiveApproach.lightColor == LightColor.GYR_G) {
				if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
					if (!isTrafficExistAtNonActiveStreet(egbn)) {
						continue;
					} else if (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime) {
						// Switch to yellow if traffic waiting at conflicting approach
						if (!isTrafficExistAtActiveStreet(egbn)) {
							setGYR(egbn, LightColor.GYR_Y);
						} else {
							// Without conflicting traffic: increment green light time if possible; change to yellow immediately if max green time passed
							if (egbn.trafficSignalAccumulatedGYRTime < LightColor.GYR_G.maxDynamicTime) {
								egbn.trafficSignalTimerGYR += secEachStep;
							} else {
								setGYR(egbn, LightColor.GYR_Y);
							}
						}
					}
				} else if ((Settings.trafficLightTiming == TrafficLightTiming.FIXED)
						&& (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime)) {
					setGYR(egbn, LightColor.GYR_Y);
				}
			} else if ((anEdgeInActiveApproach.lightColor == LightColor.GYR_Y)
					&& (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime)) {
				setGYR(egbn, LightColor.GYR_R);
			} else if ((anEdgeInActiveApproach.lightColor == LightColor.GYR_R
					|| anEdgeInActiveApproach.lightColor == LightColor.KEEP_RED)
					&& (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime)) {
				// Starts GYR cycle for next group of edges			
				egbn.inwardEdgeGroupIndexForGYR = (egbn.inwardEdgeGroupIndexForGYR + 1) % egbn.edgeGroups.size();
				setGYR(egbn, LightColor.GYR_G);
			}

			// Reset vehicle detection flag at all edges
			for (final ArrayList<Edge> edgeGroup : egbn.edgeGroups) {
				for (final Edge edge : edgeGroup) {
					edge.isDetectedVehicleForLight = false;
				}
			}
		}

	}

	int getEdgeGroupIndexOfPriorityInactiveApproach(LightGroup egbn) {
		for (int i = 0; i < egbn.edgeGroups.size(); i++) {
			if (i == egbn.inwardEdgeGroupIndexForGYR) {
				continue;
			}
			for (Edge e : egbn.edgeGroups.get(i)) {
				if (RoadUtil.isEdgeContainsPriorityVehicle(e)) {
					return i;
				}
			}
		}
		return -1;
	}

}

package processor.communication.message;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.light.*;
import traffic.road.Edge;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

/**
 * Worker-to-server message that is sent by worker after simulating one step.
 * This message is only used if synchronization involves server. Information
 * contained in this message can be used for updating GUI.
 *
 */
public class Message_WS_TrafficReport {
	public String workerName;
	public ArrayList<Serializable_GUI_Vehicle> vehicleList = new ArrayList<>();
	public ArrayList<Serializable_GUI_Light> lightList = new ArrayList<>();
	public ArrayList<SerializableRouteDump> randomRoutes = new ArrayList<>();
	public ArrayList<Serializable_Finished_Vehicle> finishedList = new ArrayList<>();
	public int step;
	public int numInternalNonPubVehicles;
	public int numInternalTrams;
	public int numInternalBuses;
	public ArrayList<SerializableLaneIndex> laneIndexes = new ArrayList<>();
	public ArrayList<SerializableLaneIndex> edgesToUpdate = new ArrayList<>();

	public Message_WS_TrafficReport() {

	}

	public Message_WS_TrafficReport(Settings settings, final String workerName, final ArrayList<Vehicle> vehiclesOnRoad,
									final LightCoordinator lightCoordinator, final ArrayList<Vehicle> newVehiclesSinceLastReport,
									List<Vehicle> vehiclesFinished,final int step,
									final int numInternalNonPubVehicles, final int numInternalTrams, final int numInternalBuses,
									final ArrayList<Integer> laneIndexes, final  ArrayList<Integer> edgesToUpdate) {
		this.workerName = workerName;
		if (settings.isVisualize || settings.isOutputTrajectory) {
			vehicleList = getDetailOfActiveVehiclesOnRoad(vehiclesOnRoad);
		}
		if (settings.isVisualize && settings.trafficLightTiming != TrafficLightTiming.NONE) {
			lightList = getDetailOfLights(lightCoordinator);
		}
		if (settings.isOutputInitialRoutes) {
			randomRoutes = getInitialRouteList(newVehiclesSinceLastReport);
		}
		this.finishedList = new ArrayList<>(getFinishedListFromVehicles(vehiclesFinished, step/ settings.numStepsPerSecond));
		this.step = step;
		this.numInternalNonPubVehicles = numInternalNonPubVehicles;
		this.numInternalTrams = numInternalTrams;
		this.numInternalBuses = numInternalBuses;
		this.laneIndexes = addLaneIndexes(laneIndexes);
		this.edgesToUpdate = addLaneIndexes(edgesToUpdate);
	}

	public Message_WS_TrafficReport(Settings settings, final String workerName, final int step, final TrafficNetwork trafficNetwork, final ArrayList<Integer> edgesToUpdate){
		this(settings, workerName, trafficNetwork.vehicles, trafficNetwork.lightCoordinator,
				trafficNetwork.newVehiclesSinceLastReport, trafficNetwork.getFinishedVehicles(), step,
				trafficNetwork.numInternalNonPublicVehicle,
				trafficNetwork.numInternalTram, trafficNetwork.numInternalBus, trafficNetwork.laneIndexOfChangeDir, edgesToUpdate);
	}

	ArrayList<SerializableLaneIndex> addLaneIndexes(final ArrayList<Integer> lanes){
		ArrayList<SerializableLaneIndex> l = new ArrayList<>();
		for (final Integer i : lanes){
			l.add(new SerializableLaneIndex(i.intValue()));
		}
		return l;
	}

	ArrayList<Serializable_GUI_Vehicle> getDetailOfActiveVehiclesOnRoad(final ArrayList<Vehicle> vehicles) {
		final ArrayList<Serializable_GUI_Vehicle> list = new ArrayList<>();
		for (final Vehicle v : vehicles) {
			if (v.active && (v.lane != null)) {
				final Serializable_GUI_Vehicle sVehicle = new Serializable_GUI_Vehicle();
				if ((v.type == VehicleType.TRAM) && (v.lane.edge.timeTramStopping > 0)) {
					sVehicle.type = v.type.name() + "@Stop";
				} else {
					sVehicle.type = v.type.name();
				}
				sVehicle.speed = v.speed;
				sVehicle.acceleration = v.acceleration;
				final double[] coordinates = VehicleUtil.calculateCoordinates(v);
				sVehicle.lonHead = coordinates[0];
				sVehicle.latHead = coordinates[1];
				sVehicle.lonTail = coordinates[2];
				sVehicle.latTail = coordinates[3];
				sVehicle.length = v.type.length;
				sVehicle.width = v.type.width;
				sVehicle.numLinksToGo = v.getRouteLegCount() - 1 - v.indexLegOnRoute;
				sVehicle.id = v.id;
				sVehicle.vid = v.vid;
				sVehicle.worker = workerName;
				sVehicle.driverProfile = v.driverProfile.name();
				sVehicle.slowDownFactor = v.getRecentSlowDownFactor();
				sVehicle.headwayMultiplier = v.getHeadWayMultiplier();
				sVehicle.edgeIndex = v.lane.edge.index;
				sVehicle.laneIndex = v.lane.index;
				sVehicle.originalEdgeMaxSpeed = v.lane.edge.freeFlowSpeed;
				sVehicle.isAffectedByPriorityVehicle = v.isAffectedByPriorityVehicle;
				sVehicle.displacement = v.getDisplacement();
				list.add(sVehicle);
			}
		}

		return list;
	}

	ArrayList<Serializable_GUI_Light> getDetailOfLights(final LightCoordinator lightCoordinator) {
		final ArrayList<Serializable_GUI_Light> list = new ArrayList<>();
		for (final TrafficLightCluster cluster : lightCoordinator.lightClusters) {
			Map<Edge, Integer> lightCount = new HashMap<>();
			for (int i = 0; i < cluster.getMovements().size(); i++) {
				Movement movement = cluster.getMovements().get(i);
				Edge e = movement.getControlEdge();
				int count = 0;
				if(lightCount.containsKey(e)){
					count = lightCount.get(e) + 1;
				}
				final double lightPositionToEdgeRatio = (e.length - e.getEndIntersectionSize() + 0.5 + count * 0.3 ) / e.length;

				Point2D start = e.getEdgeStartMidlle();
				Point2D end = e.getEdgeEndMidlle();

				final double latitude = (start.getY() + ((end.getY() - start.getY()) * lightPositionToEdgeRatio));
				final double longitude = (start.getX() + ((end.getX() - start.getX()) * lightPositionToEdgeRatio));
				list.add(new Serializable_GUI_Light(longitude, latitude, e.getMovementLight(movement).color));
				lightCount.put(e, count);
			}
		}

		return list;
	}

	ArrayList<SerializableRouteDump> getInitialRouteList(final ArrayList<Vehicle> vehicles) {
		final ArrayList<SerializableRouteDump> list = new ArrayList<>();

		for (final Vehicle vehicle : vehicles) {
			final ArrayList<SerializableRouteDumpPoint> routeDumpPoints = new ArrayList<>();
			final SerializableRouteDumpPoint startPoint = new SerializableRouteDumpPoint(
					vehicle.getRouteLegEdge(0).startNode.osmId, vehicle.getRouteLeg(0).stopover);
			routeDumpPoints.add(startPoint);
			for (final RouteLeg routeLeg : vehicle.getRouteLegs()) {
				final SerializableRouteDumpPoint point = new SerializableRouteDumpPoint(routeLeg.edge.endNode.osmId,
						routeLeg.stopover);
				routeDumpPoints.add(point);
			}
			list.add(new SerializableRouteDump(vehicle.id, vehicle.vid, vehicle.type.name(), vehicle.timeRouteStart,
					vehicle.getRouteLeg(0).edge.startNode.index,
					vehicle.getRouteLeg(vehicle.getRouteLegCount()-1).edge.endNode.index,
					vehicle.getBestTravelTime(), vehicle.getRouteLength(), vehicle.getRouteString(), routeDumpPoints,
					vehicle.driverProfile.name()));
		}

		return list;
	}

	ArrayList<SerializableVehicleSpeed> getVehicleSpeedList(final ArrayList<Double> vehicleSpeedOnCalibratedEdges) {
		final ArrayList<SerializableVehicleSpeed> list = new ArrayList<>();
		for (final double speed : vehicleSpeedOnCalibratedEdges) {
			list.add(new SerializableVehicleSpeed(speed));
		}
		return list;
	}

	public List<Serializable_Finished_Vehicle> getFinishedListFromVehicles(List<Vehicle> vehiclesFinished, double timeNow){
		List<Serializable_Finished_Vehicle> finishedVehicles = new ArrayList<>();
		for (Vehicle vehicle : vehiclesFinished) {
			Serializable_Finished_Vehicle finishedVehicle = new Serializable_Finished_Vehicle(vehicle.id, vehicle.vid,
					vehicle.getRouteLeg(0).edge.startNode.index,
					vehicle.getRouteLeg(vehicle.getRouteLegCount()-1).edge.endNode.index,
					vehicle.getBestTravelTime(), timeNow - vehicle.timeRouteStart, vehicle.getRouteLength(), vehicle.getRouteString(),
					vehicle.getTimeOnDirectionalTraffic(), vehicle.getTimeOnDirectionalTraffic_speed());
			finishedVehicles.add(finishedVehicle);
		}
		return finishedVehicles;
	}



	public ArrayList<Serializable_Finished_Vehicle> getFinishedList() {
		return finishedList;
	}

}

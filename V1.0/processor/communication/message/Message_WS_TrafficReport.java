package processor.communication.message;

import java.util.ArrayList;

import common.Settings;
import processor.server.DataOutputScope;
import traffic.light.LightCoordinator;
import traffic.light.LightCoordinator.LightGroup;
import traffic.light.TrafficLightTiming;
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
	public ArrayList<Serializable_GUI_Vehicle> vehicles = new ArrayList<>();
	public ArrayList<Serializable_GUI_Light> trafficLights = new ArrayList<>();
	public ArrayList<SerializableRoute> newRoutesSinceLastReport = new ArrayList<>();
	public ArrayList<SerializableTravelTime> travelTimes = new ArrayList<>();
	public int step;
	public int numInternalNonPubVehicles;
	public int numInternalTrams;
	public int numInternalBuses;
	public int totalNumVehicles;
	public double aggregatedTravelSpeedValues;

	public Message_WS_TrafficReport() {

	}

	public Message_WS_TrafficReport(final String workerName, final ArrayList<Vehicle> vehiclesOnRoad,
			final LightCoordinator lightCoordinator, final ArrayList<Vehicle> newVehiclesSinceLastReport,
			final int step, final int numInternalNonPubVehicles, final int numInternalTrams,
			final int numInternalBuses) {
		this.workerName = workerName;
		vehicles = getDetailOfActiveVehiclesOnRoad(Settings.isVisualize, vehiclesOnRoad,
				Settings.outputTrajectoryScope);
		aggregatedTravelSpeedValues = getAggregatedTravelSpeedValues(vehiclesOnRoad);
		trafficLights = getDetailOfLights(lightCoordinator);
		newRoutesSinceLastReport = getInitialRouteList(newVehiclesSinceLastReport, Settings.outputRouteScope);
		travelTimes = getTravelTimes(vehiclesOnRoad, Settings.outputTravelTimeScope);

		this.step = step;
		this.numInternalNonPubVehicles = numInternalNonPubVehicles;
		this.numInternalTrams = numInternalTrams;
		this.numInternalBuses = numInternalBuses;
		this.totalNumVehicles = vehiclesOnRoad.size();
	}

	double getAggregatedTravelSpeedValues(final ArrayList<Vehicle> vehicles) {
		double aggregated = 0;
		if (Settings.isOutputSimulationLog) {
			for (Vehicle vehicle : vehicles) {
				aggregated += vehicle.speed;
			}
		}
		return aggregated;
	}

	ArrayList<SerializableTravelTime> getTravelTimes(final ArrayList<Vehicle> vehicles,
			DataOutputScope outputTravelTimeScope) {
		final ArrayList<SerializableTravelTime> list = new ArrayList<>();
		for (final Vehicle v : vehicles) {
			if (outputTravelTimeScope == DataOutputScope.ALL
					|| (v.isForeground && outputTravelTimeScope == DataOutputScope.FOREGROUND)
					|| (!v.isForeground && outputTravelTimeScope == DataOutputScope.BACKGROUND)) {
				list.add(new SerializableTravelTime(v.id, v.timeTravel));
			}
		}
		return list;
	}

	ArrayList<Serializable_GUI_Vehicle> getDetailOfActiveVehiclesOnRoad(boolean isVisualize,
			final ArrayList<Vehicle> vehicles, DataOutputScope outputTrajectoryScope) {
		final ArrayList<Serializable_GUI_Vehicle> list = new ArrayList<>();
		if (isVisualize || outputTrajectoryScope != DataOutputScope.NONE) {
			for (final Vehicle v : vehicles) {
				if (Settings.isVisualize || outputTrajectoryScope == DataOutputScope.ALL
						|| (v.isForeground && outputTrajectoryScope == DataOutputScope.FOREGROUND)
						|| (!v.isForeground && outputTrajectoryScope == DataOutputScope.BACKGROUND)) {
					if (v.active && (v.lane != null)) {
						final Serializable_GUI_Vehicle sVehicle = new Serializable_GUI_Vehicle();
						sVehicle.type = v.type.name();						
						sVehicle.speed = v.speed;
						final double[] coordinates = VehicleUtil.calculateCoordinates(v);
						sVehicle.lonHead = coordinates[0];
						sVehicle.latHead = coordinates[1];
						sVehicle.lonTail = coordinates[2];
						sVehicle.latTail = coordinates[3];
						sVehicle.numLinksToGo = v.routeLegs.size() - 1 - v.indexLegOnRoute;
						sVehicle.id = v.id;
						sVehicle.worker = workerName;
						sVehicle.driverProfile = v.driverProfile.name();
						sVehicle.edgeIndex = v.lane.edge.index;
						sVehicle.originalEdgeMaxSpeed = v.lane.edge.freeFlowSpeed;
						sVehicle.isAffectedByPriorityVehicle = v.isAffectedByPriorityVehicle;
						sVehicle.isForeground = v.isForeground;
						list.add(sVehicle);
					}
				}
			}
		}

		return list;
	}

	ArrayList<Serializable_GUI_Light> getDetailOfLights(final LightCoordinator lightCoordinator) {
		final ArrayList<Serializable_GUI_Light> list = new ArrayList<>();
		if (Settings.isVisualize && Settings.trafficLightTiming != TrafficLightTiming.NONE) {
			for (final LightGroup edgeGroups : lightCoordinator.lightGroups) {
				for (final ArrayList<Edge> edgeGroup : edgeGroups.edgeGroups) {
					for (final Edge e : edgeGroup) {
						final double lightPositionToEdgeRatio = (e.length - 1) / e.length;
						final double latitude = (e.startNode.lat
								+ ((e.endNode.lat - e.startNode.lat) * lightPositionToEdgeRatio));
						final double longitude = (e.startNode.lon
								+ ((e.endNode.lon - e.startNode.lon) * lightPositionToEdgeRatio));
						list.add(new Serializable_GUI_Light(longitude, latitude, e.lightColor.color));
					}
				}
			}
		}

		return list;
	}

	ArrayList<SerializableRoute> getInitialRouteList(final ArrayList<Vehicle> vehicles,
			DataOutputScope outputRouteScope) {
		final ArrayList<SerializableRoute> list = new ArrayList<>();
		if (outputRouteScope != DataOutputScope.NONE) {
			for (final Vehicle vehicle : vehicles) {
				if (outputRouteScope == DataOutputScope.ALL
						|| (vehicle.isForeground && outputRouteScope == DataOutputScope.FOREGROUND)
						|| (!vehicle.isForeground && outputRouteScope == DataOutputScope.BACKGROUND)) {
					final ArrayList<SerializableRouteDumpPoint> routeDumpPoints = new ArrayList<>();
					final SerializableRouteDumpPoint startPoint = new SerializableRouteDumpPoint(
							vehicle.routeLegs.get(0).edge.startNode.osmId, vehicle.routeLegs.get(0).stopover);
					routeDumpPoints.add(startPoint);
					for (final RouteLeg routeLeg : vehicle.routeLegs) {
						final SerializableRouteDumpPoint point = new SerializableRouteDumpPoint(
								routeLeg.edge.endNode.osmId, routeLeg.stopover);
						routeDumpPoints.add(point);
					}
					list.add(new SerializableRoute(vehicle.id, vehicle.type.name(), vehicle.timeRouteStart,
							routeDumpPoints, vehicle.driverProfile.name(), vehicle.isForeground));
				}
			}
		}

		return list;
	}

}

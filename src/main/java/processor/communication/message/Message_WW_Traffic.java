package processor.communication.message;

import java.util.ArrayList;

import processor.worker.Fellow;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleUtil;

/**
 * Worker-to-worker message that contains information of traffic near the
 * boundary of worker's responsible area.
 *
 */
public class Message_WW_Traffic {

	public String senderName;
	public int stepAtSender;
	public ArrayList<SerializableVehicle> vehiclesEnteringReceiver = new ArrayList<>(1000);
	//Latest vehicles that left the 'receiving' side at earlier steps, i.e., these vehicles are running at the worker who is sending this message.
	public ArrayList<SerializableFrontVehicleOnBorder> lastVehiclesLeftReceiver = new ArrayList<>(1000);

	public Message_WW_Traffic() {

	}

	public Message_WW_Traffic(final String senderName, final Fellow receiverFellow, final int stepCurrent) {
		this.senderName = senderName;
		stepAtSender = stepCurrent;
		vehiclesEnteringReceiver = getVehiclesEnteringReceiver(receiverFellow);
		lastVehiclesLeftReceiver = getLastVehiclesLeftReceiver(receiverFellow);
	}

	ArrayList<SerializableVehicle> getVehiclesEnteringReceiver(final Fellow receiver) {
		final ArrayList<SerializableVehicle> serializableVehicle = new ArrayList<>();
		for (final Vehicle vehicle : receiver.vehiclesToCreateAtBorder) {
			serializableVehicle.add(getSerializableVehicle(vehicle));
		}
		return serializableVehicle;
	}

	ArrayList<SerializableFrontVehicleOnBorder> getLastVehiclesLeftReceiver(final Fellow receiver) {
		final ArrayList<SerializableFrontVehicleOnBorder> serializableInfo = new ArrayList<>();
		for (final Edge edge : receiver.outwardEdgesAcrossBorder) {
			for (final Lane lane : edge.lanes) {
				final SerializableFrontVehicleOnBorder oneV = new SerializableFrontVehicleOnBorder();
				oneV.laneIndex = lane.index;
				final Vehicle lastV = VehicleUtil.getLastVehicle(lane);
				if (lastV == null) {
					oneV.endPosition = edge.length + 1;
					oneV.speed = 10000;
				} else {
					oneV.endPosition = lastV.headPosition - lastV.length;
					oneV.speed = lastV.speed;
				}
				serializableInfo.add(oneV);
			}
		}
		return serializableInfo;
	}

	ArrayList<SerializableRouteLeg> getSerializableRoute(final Vehicle vehicle) {
		final ArrayList<SerializableRouteLeg> legs = new ArrayList<>();
		for (final RouteLeg leg : vehicle.routeLegs) {
			legs.add(new SerializableRouteLeg(leg.edge.index, leg.stopover));
		}
		return legs;
	}

	SerializableVehicle getSerializableVehicle(final Vehicle vehicle) {
		final SerializableVehicle serializableVehicle = new SerializableVehicle();
		serializableVehicle.type = vehicle.type.name();
		serializableVehicle.routeLegs = getSerializableRoute(vehicle);
		serializableVehicle.indexRouteLeg = vehicle.indexLegOnRoute;
		serializableVehicle.laneIndex = vehicle.lane.index;
		serializableVehicle.headPosition = vehicle.headPosition;
		serializableVehicle.speed = vehicle.speed;
		serializableVehicle.timeRouteStart = vehicle.timeRouteStart;
		serializableVehicle.id = vehicle.id;
		serializableVehicle.isExternal = vehicle.isExternal;
		serializableVehicle.isForeground = vehicle.isForeground;
		serializableVehicle.idLightGroupPassed = vehicle.idLightGroupPassed;
		serializableVehicle.driverProfile = vehicle.driverProfile.name();
		return serializableVehicle;
	}

}

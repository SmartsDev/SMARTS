package processor.communication.message;

import traffic.TrafficNetwork;
import traffic.routing.RouteUtil;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

import java.util.ArrayList;

public class SerializableVehicle {
	public String type;
	public ArrayList<SerializableRouteLeg> routeLegs = new ArrayList<>();
	public int indexRouteLeg;
	public int laneIndex;
	public double headPosition;
	public double speed;
	public double timeRouteStart;
	public String id;
	public boolean isExternal;
	public boolean isForeground;
	public long idLightGroupPassed = -1;
	public String driverProfile;

	public SerializableVehicle() {

	}

	public Vehicle createVehicle(TrafficNetwork trafficNetwork){
		final Vehicle vehicle = new Vehicle(trafficNetwork.getSettings());
		vehicle.type = VehicleType.getVehicleTypeFromName(type);
		vehicle.length = vehicle.type.length;
		vehicle.setRouteLegs(RouteUtil.parseReceivedRoute(routeLegs, trafficNetwork.edges));
		vehicle.indexLegOnRoute = indexRouteLeg;
		vehicle.lane = trafficNetwork.lanes.get(laneIndex);
		vehicle.headPosition = headPosition;
		vehicle.speed = speed;
		vehicle.timeRouteStart = timeRouteStart;
		vehicle.id = id;
		vehicle.isExternal = isExternal;
		vehicle.isForeground = isForeground;
		vehicle.idLightGroupPassed = idLightGroupPassed;
		vehicle.driverProfile = DriverProfile.valueOf(driverProfile);
		return vehicle;
	}
}

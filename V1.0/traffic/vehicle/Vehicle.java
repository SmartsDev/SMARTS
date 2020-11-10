package traffic.vehicle;

import java.util.ArrayList;

import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;

public class Vehicle {
	public String id = "";
	public ArrayList<RouteLeg> routeLegs = new ArrayList<>(1000);
	public VehicleType type = null;
	public double headPosition = 0;
	public Lane lane = null;
	public double speed = 0;
	public double length = 0;
	public double acceleration = 0;
	public int indexLegOnRoute = 0;
	public boolean active = false;
	public double timeRouteStart = 0;
	public double earliestTimeToLeaveParking = 0;
	public double timeTravel = 0;
	public double timeJamStart = 0;
	public int numReRoute = 0;
	public boolean isExternal = false;
	public boolean isForeground = false;
	public double distToImpedingObject = 10000;
	public double spdOfImpedingObject = 0;
	public double timeOfLastLaneChange = 0;
	public boolean isRoadBlockedAhead = false;
	/**
	 * The ID of the latest light group. This vehicle will ignore other traffic
	 * lights in the same group if it passes one of the lights in the group.
	 */
	public long idLightGroupPassed = -1;
	public DriverProfile driverProfile = DriverProfile.NORMAL;
	/**
	 * This is for highlighting vehicles affected by emergency vehicles on GUI
	 */
	public boolean isAffectedByPriorityVehicle = false;
	/**
	 * The edge where vehicle will make a right turn
	 */
	public Edge edgeBeforeTurnRight = null;
	/**
	 * The edge where vehicle will make a left turn
	 */
	public Edge edgeBeforeTurnLeft = null;
}

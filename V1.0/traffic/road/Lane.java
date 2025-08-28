package traffic.road;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

/**
 * Lane is a basic element in road network. An edge contains one or more lanes.
 *
 */
public class Lane {
	/**
	 * The edge that this lane belongs to.
	 */
	public Edge edge;
	/**
	 * Lane number. The lane closest to road side is number 0.
	 */
	public int laneNumber;
	/**
	 * Index of this lane in the whole network.
	 */
	public int index;
	/**
	 * Collection of the vehicles traveling on this lane.
	 */
	private ArrayList<Vehicle> vehicles = new ArrayList<>(50);
	/**
	 * Whether this lane is manually blocked by user.
	 */
	public boolean isBlocked = false;
	/**
	 * Speed of the last vehicle if this lane crosses border between workers.
	 */
	public double speedOfLatestVehicleLeftThisWorker = 100;
	/**
	 * Head position of the last vehicle if this lane crosses border between
	 * workers.
	 */
	public double endPositionOfLatestVehicleLeftThisWorker = 1000000000;
	/**
	 * Whether this lane is being used by a priority vehicle, such as ambulance.
	 */
	public boolean isPriority = false;

	/**
	 * GPS coordinates of start/end points
	 *
	 */
	public double latStart, lonStart, latEnd, lonEnd, latLength, lonLength;

	/**
	 * Lane changes the direction and still there are vehicles in road
	 */
	public boolean isDirectionChanging = false;

	public VehiclePositionComparator vehiclePositionComparator = new VehiclePositionComparator();

	public Lane(final Edge edge) {
		this.edge = edge;
	}

	public void addVehicleToLane(Vehicle v){
		vehicles.add(v);
		Collections.sort(vehicles, vehiclePositionComparator);
	}

	public void clearVehicles(){
		vehicles.clear();
	}

	public int getVehicleCount(){
		return vehicles.size();
	}

	public Vehicle getFrontVehicleInLane(){
		if(!vehicles.isEmpty()) {
			return vehicles.get(0);
		}else{
			return null;
		}
	}

	/**
	 * Get the last vehicle in a given lane. The vehicle is the closest vehicle
	 * to the start node of this lane's edge. In other words, this vehicle will
	 * be the last vehicle to leave the lane.
	 *
	 */
	public Vehicle getLastVehicleInLane(){
		if(!vehicles.isEmpty()) {
			return vehicles.get(vehicles.size() - 1);
		}else{
			return null;
		}
	}

	public void removeVehicle(Vehicle vehicle){
		vehicles.remove(vehicle);
	}

	public boolean hasPriorityVehicles(){
		for (Vehicle v : vehicles) {
			if (v.type == VehicleType.PRIORITY) {
				return true;
			}
		}
		return false;
	}

	public List<Vehicle> getVehicles(){
		return Collections.unmodifiableList(vehicles);
	}

	/**
	 * Get the closest vehicle whose head position is behind the head position
	 * of a given vehicle. The two vehicles may not be in the same lane.
	 *
	 */
	public Vehicle getClosestBackVehicleInLane(final Vehicle vehicle) {
		Vehicle backVehicle = null;
		for (int i = 0; i < vehicles.size(); i++) {
			if (vehicles.get(i).headPosition < vehicle.headPosition) {
				backVehicle = vehicles.get(i);
				break;
			}
		}
		return backVehicle;
	}

	/**
	 * Get the closest vehicle whose head position is ahead of the head position
	 * of a given vehicle. The two vehicles may not be in the same lane.
	 *
	 */
	public Vehicle getClosestFrontVehicleInLane(final Vehicle vehicle, final double gapToTargetLane) {
		Vehicle frontVehicle = null;

		for (int i = vehicles.size() - 1; i >= 0; i--) {
			if ((vehicles.get(i).headPosition + gapToTargetLane) > vehicle.headPosition) {
				frontVehicle = vehicles.get(i);
				break;
			}
		}
		return frontVehicle;
	}

	public Line2D getLaneLine(){
	    return new Line2D.Double(lonStart, latStart, lonEnd, latEnd);
    }

    public List<Vehicle> getVehiclesBelowHeadPosition(double headPos){
		List<Vehicle> list = new ArrayList<>();
		for (int i = vehicles.size() - 1; i >= 0; i--) {
			Vehicle v = vehicles.get(i);
			if(v.headPosition < headPos){
				list.add(v);
			}else{
				break;
			}
		}
		return list;
	}

	public List<Vehicle> getVehiclesAboveHeadPosition(double headPos){
		List<Vehicle> list = new ArrayList<>();
		for (int i = 0; i < vehicles.size(); i++) {
			Vehicle v = vehicles.get(i);
			if(v.headPosition > headPos){
				list.add(v);
			}else{
				break;
			}
		}
		return list;
	}

	public List<Vehicle> vehiclesStartedMovingTowards(Vehicle v){
		List<Vehicle> towardsVehicles = new ArrayList<>();
		Node junc = edge.startNode;
		for (Edge inwardEdge : junc.inwardEdges) {
			for (Lane lane : inwardEdge.getLanes()) {
				double aboveHeadPos = inwardEdge.length - inwardEdge.getEndIntersectionSize();
				if(v.lane == lane){
					aboveHeadPos = v.headPosition;
				}
				List<Vehicle> vehiclesInsideIntersection = lane.getVehiclesAboveHeadPosition(aboveHeadPos);
				for (Vehicle vehicle : vehiclesInsideIntersection) {
					Vehicle.IntersectionDecision decision = vehicle.getDecision();
					if (decision != null && decision.getEndLane() == this) {//decision can be null for ending vehicles
						towardsVehicles.add(vehicle);
					}
				}
			}
		}
		return towardsVehicles;
	}

	/**
	 * Comparator of vehicles based on their positions in a lane. The vehicle
	 * closest to the end of the lane, will be the first element in the sorted
	 * list.
	 *
	 */
	public class VehiclePositionComparator implements Comparator<Vehicle> {

		@Override
		public int compare(final Vehicle v1, final Vehicle v2) {
			return v1.headPosition > v2.headPosition ? -1 : v1.headPosition == v2.headPosition ? 0 : 1;
		}
	}

	public void changeLaneCoordinates(){
		double temp = latStart;
		latStart = latEnd;
		latEnd = temp;

		temp = lonStart;
		lonStart = lonEnd;
		lonEnd = temp;

		lonLength = lonEnd - lonStart;
		latLength = latEnd - latStart;
	}

	public boolean updateDirection(){
		if (isDirectionChanging){
			if (vehicles.size() == 0)	{
				// Vehicles cleared
				isDirectionChanging = false;
				//Remove lane from previous direction
				moveLaneToOppositeEdge();
				return true;
			}
			return false;
		}
		return false;
	}

	public void moveLaneToOppositeEdge(){
		edge.removeLastLane();
		if (edge.numRightLanes > 1){
			edge.numRightLanes--;
		}
		if (edge.numLeftLanes > 1){
			edge.numLeftLanes--;
		}
		// Find the opposite direction edge
		Edge opposedEdge = edge.getOppositeEdge();
		// add to new direction
		edge = opposedEdge;
		edge.numRightLanes++;
		edge.numLeftLanes++;
		// Get the highest lane number of opposite dir. and update changed lane
		laneNumber = edge.getLaneCount();
		changeLaneCoordinates();
		edge.addLane(this);
	}

	public void markLaneToChangeDir(){
		if ( isDirectionChanging == false){
			Edge oppositeEdge = edge.getOppositeEdge();
			if (oppositeEdge.getLastLane().isDirectionChanging == false)
				isDirectionChanging = true;
			else {
				System.out.println("WARNING: attempts to change lane direction while opposite direction lane direction change already in operation."
				+ " Road already in changing dir: " + edge.getOppositeEdge().index + " Request denied");
			}
		}
	}

	/**
	 * This methods finds number of vehicles at the end of the lane
	 */

	public int spaceLeftAtEnd(){
		int vehicleCount = 0;
		for(Vehicle v : vehicles){
			if (v.headPosition < this.edge.getStartIntersectionSize()){
				vehicleCount++;
			}
		}

		return vehicleCount;
	}
}

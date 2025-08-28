package traffic.road;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.Movement;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

/**
 * Edge is a basic element in road network. A network consists of a number of
 * nodes connected by edges.
 *
 */
public class Edge {
	int importedStartNodeIndex;
	int importedEndNodeIndex;
	public Node startNode;
	public Node endNode;
	public RoadType type;
	public String name;
	public Settings settings;
	/**
	 * Free flow speed of vehicles
	 */
	public double freeFlowSpeed, maxFreeFlowSpeed, minFreeFlowSpeed;

	/**
	 * Number of vehicles that can be filled into this edge based on average
	 * vehicle speed.
	 */
	public double capacity = 0;
	/**
	 * Whether this edge is in a roundabout.
	 */
	public boolean isRoundabout;
	/**
	 * Index of this edge in the whole network.
	 */
	public int index;
	/**
	 * Length of this edge in meters.
	 */
	public double length;
	/**
	 * Collection of the lanes belonging to this edge.
	 */
	private ArrayList<Lane> lanes = new ArrayList<>();
	/**
	 * Remaining time that the tram stops at the station on this edge.
	 */
	public double timeTramStopping = 0;
	/**
	 * Remaining time that the tram stop should be ignored. This can be used for
	 * preventing vehicles to wait at tram station for consecutive periods.
	 */
	public double timeNoTramStopping = 0;
	/**
	 * Color of traffic light at the end of this edge.
	 */
	private Map<Movement, LightColor> lightColorMap;

	/**
	 * Tram edge parallel to this edge and is with tram stop.
	 */
	public Edge parallelTramEdgeWithTramStop = null;
	/**
	 * Distance between the start node of this edge to the end node of the
	 * parallel tram edge with tram stop. The tram stop is at the end node of
	 * the edge.
	 */
	public double distToTramStop;
	/**
	 * Collection of vehicles that are parked along this edge.
	 */
	private PriorityQueue<Vehicle> parkedVehicles = new PriorityQueue<>(getParkedVehicleComparator());
	private Vehicle vehicleToGetIntoTheLane = null;
	/**
	 * List of tram routes passing this edge.
	 */
	public ArrayList<String> tramRoutesRef = new ArrayList<>();
	/**
	 * List of bus routes passing this edge.
	 */
	public ArrayList<String> busRoutesRef = new ArrayList<>();
	/**
	 * Angle of edge
	 */
	public double angleOutward, angleInward;
	/**
	 * Vehicle speed extracted from external traffic data
	 */
	public double currentSpeed;
	public double mvgCurrentSpeed;
	/**
	 * Whether there is one or more vehicle within the detection range of
	 * traffic light
	 */
	public boolean isDetectedVehicleForLight = false;
	/**
	 * Delay in seconds. This value can be used for calibration.
	 */
	public double delay;
	/**
	 * Number of lanes for right-turn
	 */
	public int numRightLanes;
	public int numRightOnlyLanes;
	/**
	 * Number of lanes for left-turn
	 */
	public int numLeftLanes;
	public int numLeftOnlyLanes;

	/**
	 * Maximum vehicles at the end of lane can handle:
	 * this is used when lane numbers are different in two roads
	 */
	public int maxVehiclesAtTheEnd = 5;
	private double slowDownFactor = 0.2;


	/**
	 * Traffic flow statistics
	 */
	private double inflow = 0;
	private double outflow = 0;

	private double inflow_unnorm = 0;
	private double outflow_unnorm = 0;

	private int inflowPerStep = 0;
	private int outflowPerStep = 0;

	private double flowComputeWindow = 0.8;
	private int numVehicleOutPerLane = 12;

	private Vehicle currentVehicleInBeforeTurnLaneChangePos = null;
	private Map<Vehicle, Double> laneChangePositions = new HashMap<>();
	private List<Vehicle> chanceGivingVehicles = new ArrayList<>();
	private LinkedHashMap<Edge, Integer> edgeLaneMap;

	public int projectVehicles = 0;

	public double getInflow() {
		/*computeRoadCapacity();
		if (capacity < numVehicles){
			return  outflow*2;
		}*/
		return inflow;
	}


	public double getOutflow() {
		return outflow;
	}

	public void setOutflow(double outflow) {
		this.outflow = outflow;
	}

	public int getInflowPerStep() {
		int inflowCurrentStep = inflowPerStep;
		inflowPerStep = 0;
		return inflowCurrentStep;
	}

	public void setInflowPerStep() {
		this.inflowPerStep++;
	}

	public int getOutflowPerStep() {
		int outflowCurrentStep = outflowPerStep;
		outflowPerStep = 0;
		return outflowCurrentStep;
	}


	void computeRoadCapacity(){
		capacity = ( (length -  getEndIntersectionSize()) / (VehicleType.CAR.length + DriverProfile.HIGHLY_POLITE.IDM_s0 )) * getLaneCount();
	}

	public void setOutflowPerStep() {
		this.outflowPerStep++;
	}

	public double getNumVehicles() {
		return numVehicles;
	}

	public double getNumVehiclesRight() {
		return numVehiclesRight;
	}

	public double getNumVehiclesStraight() {
		return numVehiclesStraight;
	}

	public double getNumVehiclesLeft() {
		return numVehiclesLeft;
	}

	private double numVehicles = 0;
	private double numVehiclesRight = 0;
	private double numVehiclesStraight = 0;
	private double numVehiclesLeft = 0;

	public Edge(final int importedStartNodeIndex, final int importedEndNodeIndex, final String type, final String name,
			final double maxspeed, final boolean roundabout, final List<String> tramRoutesRef,
			final List<String> busRoutesRef, final int numRightLanes, final int numLeftLanes,
			final int numRightOnlyLanes, final int numLeftOnlyLanes, Settings settings) {
		super();
		this.importedStartNodeIndex = importedStartNodeIndex;
		this.importedEndNodeIndex = importedEndNodeIndex;
		this.type = RoadType.valueOf(type);
		this.name = name;
		freeFlowSpeed = maxspeed;
		currentSpeed = freeFlowSpeed;
		maxFreeFlowSpeed = freeFlowSpeed + 2.78*2; //20kmh from freeFlowSpeed: 2.78ms -> 10kmh.
		minFreeFlowSpeed = freeFlowSpeed - 2.78*2; //50kmh from freeFlowSpeed: 2.78ms -> 10kmh.
		isRoundabout = roundabout;
		this.tramRoutesRef.addAll(tramRoutesRef);
		this.busRoutesRef.addAll(busRoutesRef);
		this.numLeftLanes = numLeftLanes;
		this.numLeftOnlyLanes = numLeftOnlyLanes;
		this.numRightLanes = numRightLanes;
		this.numRightOnlyLanes = numRightOnlyLanes;
		this.lightColorMap = new HashMap<>();
		this.settings = settings;
	}

	public void clearVehicles(){
		for (Lane lane : lanes) {
			lane.clearVehicles();
		}
	}

	public void changeFreeFlowSpeed(int speedChange){ // calculated in 10ms
		freeFlowSpeed = Math.min(maxFreeFlowSpeed, minFreeFlowSpeed + speedChange*2.78);
	}

	public boolean allBlocked(){
		boolean allBlocked = true;
		for ( Lane lane : getLanes()){
			if ( !lane.isBlocked ){
				allBlocked = false;
			}
		}
		return allBlocked;
	}

	public void updateVehicleNumbers(int numVehicles, int numVehiclesStraight, int numVehiclesRight, int numVehiclesLeft){
		double alpha = 0.8;
		this.numVehicles = alpha*this.numVehicles + (1-alpha)*numVehicles;
		this.numVehiclesRight = alpha*this.numVehiclesRight + (1-alpha)*numVehiclesRight;
		this.numVehiclesStraight = alpha*this.numVehiclesStraight + (1-alpha)*numVehiclesStraight;
		this.numVehiclesLeft = alpha*this.numVehiclesLeft + (1-alpha)*numVehiclesLeft;
	}

	public int getLaneCount(){
		return lanes.size();
	}

	public int getNonBlockedLaneCount(){
		int numLanes = 0;
		for (Lane lane : lanes){
			if (lane.isBlocked == false) numLanes++;
		}

		return numLanes;
	}

	public Lane getFirstNonBlockedLane(){
		for (Lane lane : lanes){
			if (lane.isBlocked == false) return lane;
		}
		return null;
	}

	public void addLane(Lane lane){
		lanes.add(lane);
	}

	public void removeLastLane(){
		lanes.remove(lanes.size()-1);
	}

	public Lane getFirstLane(){
		if(!lanes.isEmpty()){
			return lanes.get(0);
		}
		return null;
	}


	public Lane getLastLane(){
		if(!lanes.isEmpty()){
			return lanes.get(lanes.size()-1);
		}
		return null;
	}

	public List<Lane> getLanes(){
		return Collections.unmodifiableList(lanes);
	}

	public Lane getLaneAwayFromRoadside(Lane currentLane){
		int nextLaneNumber = currentLane.laneNumber + 1;
		if(nextLaneNumber > -1 && nextLaneNumber < lanes.size()){
			return lanes.get(nextLaneNumber);
		}
		return null;
	}

	public Lane getLaneTowardsRoadside(Lane currentLane){
		int nextLaneNumber = currentLane.laneNumber - 1;
		if(nextLaneNumber > -1 && nextLaneNumber < lanes.size()){
			return lanes.get(nextLaneNumber);
		}
		return null;
	}

	public boolean isAllLanesOnLeftBlocked(int laneNumber, boolean isDriveOnLeft){
		if (isDriveOnLeft) {
			for (int num = laneNumber - 1; num >= 0; num--) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		} else {
			for (int num = laneNumber + 1; num < lanes.size(); num++) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isAllLanesOnRightBlocked(int laneNumber, boolean isDriveOnLeft) {
		if (isDriveOnLeft) {
			for (int num = laneNumber + 1; num < lanes.size(); num++) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		} else {
			for (int num = laneNumber - 1; num >= 0; num--) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isAllLanesOnRoadSideBlocked(int laneNumber){
		for (int num = laneNumber - 1; num >= 0; num--) {
			if (!lanes.get(num).isBlocked) {
				return false;
			}
		}
		return true;
	}

	public boolean isAllLanesAwayRoadSideBlocked(int laneNumber){
		for (int num = laneNumber + 1; num < lanes.size(); num++) {
			if (!lanes.get(num).isBlocked) {
				return false;
			}
		}
		return true;
	}

	public boolean isBlocked() {
		for (final Lane lane : lanes) {
			if (!lane.isBlocked) {
				return false;
			}
		}
		return true;
	}

	public Lane getLane(int laneNumber){
		if(laneNumber > -1 && laneNumber < lanes.size()) {
			return lanes.get(laneNumber);
		}
		return null;
	}

	public boolean isSuitableForRouteEndOfInternalVehicle(double minLengthOfRouteStartEndEdge) {
		return  !(length < minLengthOfRouteStartEndEdge) && !isRoundabout;
	}

	public boolean isSuitableForRouteStartOfInternalVehicle(List<GridCell> workareaCells, double minLengthOfRouteStartEndEdge) {
		// Note: route cannot start from cross-border edge at the starting side of the edge. This is to prevent problem in transferring of vehicle.
		return  !(length < minLengthOfRouteStartEndEdge) && !isRoundabout && workareaCells.contains(endNode.gridCell);
	}

	public boolean isOnTwoWayRoad() {
		for (final Edge edgeToCheck : endNode.outwardEdges) {
			if (edgeToCheck.endNode == startNode) {
				return true;
			}
		}
		return false;
	}

	public boolean isEdgeOnPathOfPriorityVehicle() {
		for (final Lane l : lanes) {
			if (l.isPriority) {
				return true;
			}
		}
		return false;
	}

	public boolean isEdgeContainsPriorityVehicle() {
		if (isEdgeOnPathOfPriorityVehicle()) {
			for (final Lane l : lanes) {
				if(l.hasPriorityVehicles()){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether an edge is on one-way road, i.e., there is no edge that
	 * links the same two end points but is in the opposite direction.
	 */
	boolean isOnOneWayRoad() {
		for (final Edge e : startNode.inwardEdges) {
			if (e.startNode == endNode) {
				return false;
			}
		}
		return true;
	}

	public void addParkedVehicle(Vehicle v){
		parkedVehicles.add(v);
	}

	public void clearParkedVehicles(){
		parkedVehicles.clear();
	}

	public Comparator<Vehicle> getParkedVehicleComparator(){
		return new Comparator<Vehicle>() {
			@Override
			public int compare(Vehicle v1, Vehicle v2) {
				if(v1.earliestTimeToLeaveParking < v2.earliestTimeToLeaveParking){
					return -1;
				}else if(v1.earliestTimeToLeaveParking > v2.earliestTimeToLeaveParking){
					return 1;
				}
				return 0;
			}
		};
	}

	public Edge getOppositeEdge(){
		for (Edge e : startNode.inwardEdges){
			if (e.startNode == endNode) {
				return e;
			}
		}
		return null;
	}

	public Vehicle getNextParkedVehicle(double timeNow){
		Vehicle v =  parkedVehicles.peek();
		if(v != null && v.isStartFromParking(timeNow)){
			return parkedVehicles.poll();
		}
		return null;
	}

    public Vehicle getVehicleToGetIntoTheLane() {
        return vehicleToGetIntoTheLane;
    }

    public void setNextVehicleToGetIntoTheLane(Vehicle vehicle){
	    vehicleToGetIntoTheLane = vehicle;
    }

	public double getStartIntersectionSize(){
		return startNode.getIntersectionSize(endNode);
	}

	public double getEndIntersectionSize(){
		return endNode.getIntersectionSize(startNode);
	}

	public Point2D getStartPoint(){
		return new Point2D.Double(startNode.lon, startNode.lat);
	}

	public Point2D getEndPoint(){
		return new Point2D.Double(endNode.lon, endNode.lat);
	}

	public double getEndIntersectionLaneChangeProhibitedPos(){
		return length - getEndIntersectionSize() - 1;
	}

	public double getStartIntersectionLaneChangeProhibitedPos(Vehicle vehicle){
		return getStartIntersectionSize() + vehicle.length;
	}



	public double getLaneChangeWaitingPos(){
		return getEndIntersectionLaneChangeProhibitedPos();
	}

	public double getLaneChangeGiveChancePos(){
		Vehicle v = currentVehicleInBeforeTurnLaneChangePos;
		if (v == null)
			return getLaneChangeWaitingPos();
		else
			return getLaneChangeWaitingPos() - v.driverProfile.IDM_s0 - v.length - v.driverProfile.IDM_s0 - 0.0001;
	}

	public double getBeforeTurnLaneChangePos(Vehicle vehicle, boolean isDriveOnLeft){
	    if(!laneChangePositions.containsKey(vehicle) && vehicle.lane.edge == this){
			updateLaneChangeConflicts(isDriveOnLeft);
		}
		Double val = laneChangePositions.get(vehicle);
	    if(val != null){
	    	return val;
		}else{
	    	//If the next edge in lookahead distance ask for position
			return getLaneChangeWaitingPos();
		}
    }

	public List<Vehicle> laneChangeNeedVehicles(boolean isDriveOnLeft){
		List<Vehicle> vehicles = new ArrayList<>();
		for (Lane lane : lanes) {
			for (Vehicle v: lane.getVehicles()) {
				if(!v.isWithinLaneChangeProhibitedArea() && VehicleUtil.isNeedLaneChangeForTurn(this, v, isDriveOnLeft)){
					vehicles.add(v);
				}
			}
		}
		Collections.sort(vehicles, new Comparator<Vehicle>() {
			@Override
			public int compare(Vehicle o1, Vehicle o2) {
				if(o1.headPosition > o2.headPosition){
					return -1;
				}else if(o1.headPosition < o2.headPosition){
					return 1;
				}else {
					return 0;
				}
			}
		});
		return vehicles;
	}

	public void updateLaneChangeConflicts(boolean isDriveOnLeft){
		laneChangePositions.clear();
		currentVehicleInBeforeTurnLaneChangePos = null;
		chanceGivingVehicles.clear();
		List<Vehicle> vehicles = laneChangeNeedVehicles(isDriveOnLeft);
		for (int i = 0; i < vehicles.size(); i++) {
			Vehicle current = vehicles.get(i);
			if(i == 0){
				currentVehicleInBeforeTurnLaneChangePos = current;
				laneChangePositions.put(current, getLaneChangeWaitingPos());
				current.setLaneBeforeChange(current.lane);
			}else{
				laneChangePositions.put(vehicles.get(i), getLaneChangeGiveChancePos());
			}
		}
	}

	public void findChanceGivingVehicle(boolean isDriveOnLeft){
		for (Lane lane : lanes) {
			for (Vehicle vehicle : lane.getVehicles()) {
				Vehicle v = currentVehicleInBeforeTurnLaneChangePos;
				if(vehicle != v && vehicle.headPosition < getLaneChangeGiveChancePos() && VehicleUtil.isNeedLaneChangeForTurn(lane.edge, vehicle,isDriveOnLeft)){
					chanceGivingVehicles.add(vehicle);
				} else if(vehicle.headPosition < getLaneChangeGiveChancePos() - vehicle.speed * (vehicle.driverProfile.IDM_T*vehicle.getHeadWayMultiplier())) {
					chanceGivingVehicles.add(vehicle);
				}
			}
		}
	}

	public boolean hasAWaitingVehicle(Vehicle vehicle){
		Vehicle v = currentVehicleInBeforeTurnLaneChangePos;
		if(v != null && chanceGivingVehicles.contains(vehicle)){
			return true;
		}
		return false;
	}

	public boolean hasSpaceInEndOfAllLane(Lane lane, Vehicle vehicle){
		int vehicleCount = 0;
		for (Vehicle v : lane.getVehicles()) {
			if (v.headPosition < lane.edge.getStartIntersectionSize()) {
				vehicleCount++;
			}
		}

		double maxVehiclecount = maxVehiclesAtTheEnd*getLaneCount();

		if (!vehicle.isCAV()){
			maxVehiclecount *= slowDownFactor; //Human-driven vehicles cannot merge as fast as autonomous vehicles
		}

		if ( vehicleCount < maxVehiclecount){
			return true;
		}
		else {
			//System.out.println("Blocked by Queue spill-back");
			return false;
		}
	}

	public boolean hasSpaceInEndOfAllLane(){
		int vehicleCount = 0;
		for (Lane lane : lanes) {
			for (Vehicle v : lane.getVehicles()) {
				if (v.headPosition < lane.edge.getStartIntersectionSize() && v.speed < 1) {
					vehicleCount++;
				}
			}
		}

		if ( vehicleCount < getLaneCount()){
			return true;
		}
		else {
			//System.out.println("Blocked by Queue spill-back");
			return false;
		}
	}

	public boolean hasSpaceForAvehicleInBack(Lane lane, Vehicle vehicle, double minTimeSafeToCrossIntersection){
		Vehicle last = lane.getLastVehicleInLane();
		double startPos = lane.edge.length - lane.edge.getEndIntersectionSize();
		if(last != null){
			startPos = last.headPosition - last.length + last.speed * minTimeSafeToCrossIntersection + Math.pow(last.acceleration, 2)*0.5;
		}
		double expectedFill = 0;
		for (Vehicle v : lane.vehiclesStartedMovingTowards(vehicle)) {
			expectedFill += v.length + v.driverProfile.IDM_s0;
		}
		double freeSpace = startPos - expectedFill;// - lane.edge.getStartIntersectionSize();

		//When merging from higher lane to lower lane in a different road segment
		if ((vehicle.lane.edge.getLaneCount() > lane.edge.getLaneCount()) && (vehicle.lane.laneNumber >= lane.edge.getLaneCount()-1)){
			return hasSpaceInEndOfAllLane(lane, vehicle);
		}

		return freeSpace >= (vehicle.length + vehicle.driverProfile.IDM_s0);
	}

	public Point2D getEdgeStartMidlle(){
		Line2D l1 = lanes.get(0).getLaneLine();
		Line2D l2 = lanes.get(lanes.size()-1).getLaneLine();
		return RoadUtil.getDividingPoint(l1.getP1(), l2.getP1(), 1, 1);
	}

	public Point2D getEdgeEndMidlle(){
		Line2D l1 = lanes.get(0).getLaneLine();
		Line2D l2 = lanes.get(lanes.size()-1).getLaneLine();
		return RoadUtil.getDividingPoint(l1.getP2(), l2.getP2(), 1, 1);
	}

	public LightColor getMovementLight(Movement movement){
		return lightColorMap.get(movement);
	}

	public void setMovementLight(Movement movement, LightColor light){
		lightColorMap.put(movement, light);
	}

	public Integer getLaneForNextEdge(Edge next) {
		return edgeLaneMap.get(next);
	}

	public void setEdgeLaneMap(LinkedHashMap<Edge, Integer> edgeLaneMap) {
		this.edgeLaneMap = edgeLaneMap;
	}

	private int iterator = 0;

	public void updateTrafficStatistics(){
		updateFlow();
		iterator++;
	}

	public void updateFlow(){
		//Inflow and Outflow is normalized to per lane
		inflow_unnorm = flowComputeWindow*inflow_unnorm + (1 - flowComputeWindow)*getInflowPerStep();
		inflow = inflow_unnorm/(1 - Math.pow(flowComputeWindow, iterator+1));

		computeOutFlow();
	}

	public void  computeOutFlow(){
		int fullSignalCycle = endNode.outwardEdges.size() * 45;
		outflow = ( numVehicleOutPerLane * getLaneCount() * settings.mvgFlow) / (fullSignalCycle*settings.numStepsPerSecond);
	}

	public Vehicle getLastVehicleInEdge(){
		Vehicle lastVehicle = null;
		for (Lane lane : lanes){
			Vehicle v = lane.getLastVehicleInLane();

			if (v != null){
				if (lastVehicle == null){
					lastVehicle = v;
				}

				if (lastVehicle.headPosition > v.headPosition){
					lastVehicle = v;
				}
			}
		}
		return lastVehicle;
	}

	public Boolean isEdgeFull(){
		Vehicle v = getLastVehicleInEdge();

		if (v != null){
			if (v.headPosition < 1 && v.speed < 0.1){
				return true;
			}
		}
		return false;
	}
}

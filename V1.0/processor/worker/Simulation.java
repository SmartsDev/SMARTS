package processor.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import common.Settings;
import processor.SimulationListener;
import processor.communication.externalMessage.ExternalSimulationListener;
import processor.communication.externalMessage.RoadControl;
import processor.communication.externalMessage.RoadIndex;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableInt;
import processor.communication.message.SerializableWorkerMetadata;
import traffic.TrafficNetwork;
import traffic.road.*;
import traffic.vehicle.Vehicle;

/**
 * This class performs simulation at worker. The simulation includes a sequence
 * of tasks, such as moving vehicles forward based on their speed and
 * surrounding environment, making lane changes, updating traffic lights, etc.
 *
 */
public class Simulation {

	private int numLocalRandomPrivateVehicles = 0;
	private int numLocalRandomTrams = 0;
	private int numLocalRandomBuses = 0;
	private List<Edge> pspBorderEdges = new ArrayList<>();// For PSP (server-less)
	private List<Edge> pspNonBorderEdges = new ArrayList<>();// For PSP (server-less)
	private int step = 0;
	private double timeNow;
	private ArrayList<Integer> laneChangedEdgeIndex;
	TrafficNetwork trafficNetwork;
	ArrayList<Vehicle> oneStepData_vehiclesReachedFellowWorker = new ArrayList<>();
	ArrayList<Vehicle> oneStepData_allVehiclesReachedDestination = new ArrayList<>();
	SimulationListener simulationListener = null;
	ExternalSimulationListener extListner = null;
	boolean extListnerInitCalled = false;
	Settings settings;


	public Simulation(Settings settings,int startStep, String roadGraph,
					  int numLocalRandomPrivateVehicles, int numLocalRandomTrams, int numLocalRandomBuses,
					  String workAreaName,
					  List<SerializableWorkerMetadata> workerMetadatas,
					  List<SerializableInt> lightNodes) {
		if (roadGraph.equals("builtin")) {
			settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile(settings.inputBuiltinRoadGraph);
		} else {
			settings.roadGraph = roadGraph;
		}
		this.trafficNetwork = new TrafficNetwork(settings, workAreaName, workerMetadatas);
		resetSimulation(settings, startStep, numLocalRandomPrivateVehicles, numLocalRandomTrams, numLocalRandomBuses, lightNodes);
		if (settings.isExternalListenerUsed) {
			extListner = settings.getExternalSimulationListener();
			extListner.setSettings(settings);
		}
		laneChangedEdgeIndex = new ArrayList<>();
	}

	public void resetSimulation(Settings settings,int startStep,
								int numLocalRandomPrivateVehicles, int numLocalRandomTrams, int numLocalRandomBuses,
								List<SerializableInt> lightNodes){
		this.settings = settings;
		setStep(startStep);
		this.numLocalRandomPrivateVehicles = numLocalRandomPrivateVehicles;
		this.numLocalRandomTrams = numLocalRandomTrams;
		this.numLocalRandomBuses = numLocalRandomBuses;
		trafficNetwork.lightCoordinator.init(trafficNetwork, trafficNetwork.nodes, lightNodes);
		trafficNetwork.buildEnvironment();
		if(settings.getSimulationListener() != null) {
			settings.getSimulationListener().onStart(trafficNetwork, settings.maxNumSteps, (int) settings.numStepsPerSecond);
		}
		resetExistingNetwork();
		resetTraffic();
		simulationListener = settings.getSimulationListener();

		if (settings.isExternalListenerUsed){
			// send road network to TMS_MQ
		}
	}



	public void setStep(int step) {
		this.step = step;
		this.timeNow = step / settings.numStepsPerSecond;
	}

	void clearOneStepData() {
		oneStepData_vehiclesReachedFellowWorker.clear();
		oneStepData_allVehiclesReachedDestination.clear();
	}



	ArrayList<Vehicle> moveVehicleForward(final double timeNow, final List<Edge> edges) {
		final ArrayList<Vehicle> vehicles = new ArrayList<>();
		for (final Edge edge : edges) {
			double accumulatedVehicleSpeed = 0;
			int numVehiclesOnEdge = 0;
			for (final Lane lane : edge.getLanes()) {
				for (final Vehicle vehicle : lane.getVehicles()) {

					if (!vehicle.active) {
						continue;
					}

					vehicles.add(vehicle);
					numVehiclesOnEdge++;

					vehicle.moveForward(timeNow);

					// Update accumulated vehicle speed
					accumulatedVehicleSpeed += vehicle.speed;

					//

					if (vehicle.isCAV() || vehicle.isConnectedV()) {
						vehicle.reRoute(timeNow, trafficNetwork.routingAlgorithm);
						vehicle.dynamicReRoute(timeNow, trafficNetwork.routingAlgorithm);
					}

					if(vehicle.isFinished()){
						oneStepData_allVehiclesReachedDestination.add(vehicle);
					}
					// Set priority lanes
					vehicle.setPriorityLanes(true);
				}

			}
			// Update average vehicle speed for this lane
			if (numVehiclesOnEdge > 0) {
				edge.currentSpeed = accumulatedVehicleSpeed / numVehiclesOnEdge;
			} else {
				edge.currentSpeed = edge.freeFlowSpeed;
			}
			edge.mvgCurrentSpeed = 0.01*edge.mvgCurrentSpeed + 0.99*edge.currentSpeed;
		}
		return vehicles;
	}

	void moveVehicleToNextLink(List<Fellow> connectedFellows, final double timeNow, final ArrayList<Vehicle> vehiclesToCheck) {
		for (final Vehicle vehicle : vehiclesToCheck) {

			if (!vehicle.active) {
				continue;
			}
			if(!vehicle.isFinished()){
				vehicle.moveToNextLink(timeNow, connectedFellows);
			}
			if(vehicle.isReachedFellow()){
				oneStepData_vehiclesReachedFellowWorker.add(vehicle);
			}
		}

	}

	void updateLaneDirections(){
		for (Lane lane : trafficNetwork.lanes){
			if(lane.updateDirection()){
				trafficNetwork.laneIndexOfChangeDir.add(lane.index);
			}
		}
	}

	/**
	 * Pause thread. This can be useful for observing simulation on GUI.
	 */
	void pause() {
		if (settings.pauseTimeBetweenStepsInMilliseconds > 0) {
			try {
				Thread.sleep(settings.pauseTimeBetweenStepsInMilliseconds);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check whether a vehicle reaches the work area of a fellow worker.
	 */
	public static boolean reachFellow(List<Fellow> connectedFellows, final Vehicle vehicle) {
		if (vehicle.lane == null) {
			return false;
		}
		for (final Fellow fellowWorker : connectedFellows) {
			for (final Edge edge : fellowWorker.inwardEdgesAcrossBorder) {
				if (edge == vehicle.lane.edge) {
					vehicle.active = false;
					fellowWorker.vehiclesToCreateAtBorder.add(vehicle);
					return true;
				}
			}
		}
		return false;
	}



	synchronized public void simulateOneStep(Worker worker, double timeNow, int step, List<Edge> pspBorderEdges,
											 List<Edge> pspNonBorderEdges, int numLocalRandomPrivateVehicles, int numLocalRandomTrams,
											 int numLocalRandomBuses, boolean isNewNonPubVehiclesAllowed,
			boolean isNewTramsAllowed, boolean isNewBusesAllowed) {
		pause();
		waitForInit();
		updateLaneDirections();

		moveVehiclesAroundBorder(worker.connectedFellows, timeNow, pspBorderEdges);
		transferDataTofellow(worker);
		moveVehiclesNotAroundBorder(worker.connectedFellows, timeNow, pspNonBorderEdges);
		onVehicleMove(step);

		removeTripFinishedVehicles();
		onVehicleRemove(oneStepData_allVehiclesReachedDestination, step);
		trafficNetwork.changeLaneOfVehicles(timeNow);
		trafficNetwork.updateTrafficLights(timeNow);
		trafficNetwork.updateTramStopTimers();
		trafficNetwork.releaseTripMakingVehicles(timeNow, simulationListener);
		trafficNetwork.releaseVehicleFromParking(timeNow, simulationListener);
		trafficNetwork.blockTramAtTramStop();
		trafficNetwork.removeActiveVehicles(oneStepData_vehiclesReachedFellowWorker);
		trafficNetwork.createInternalVehicles(numLocalRandomPrivateVehicles, numLocalRandomTrams,
				numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed,
				timeNow);
		trafficNetwork.repeatExternalVehicles(step, timeNow);
		trafficNetwork.finishRemoveCheck(timeNow);

		trafficNetwork.updateStatistics(step);
		//sendTrafficDataToExternal();

		// Wait for External agent for send instructions after number of time sreps
		sendTrafficData();
		waitForActionsFromExternalClient();

		// Clear one-step data
		clearOneStepData();

		//if (step % 18000 == 0){
		//	resetTraffic();
		//}

	}
	public void waitForInit(){
		if (settings.isExternalListenerUsed){
			if (extListnerInitCalled == false) {
				System.out.println("Initializing...");
				extListner.waitForAction();
				extListner.getRoadDirChange();

				extListner.sendTrafficData(trafficNetwork);
				extListnerInitCalled = true;

				System.out.println("Initialize complete");
			}
		}
	}

	public void waitForActionsFromExternalClient(){
		if (settings.isExternalListenerUsed){
			if (step % settings.extListenerUpdateInterval == 1) {
				System.out.println("Waiting...");
				extListner.waitForAction();
			}
			getActionsFromExtListner();
		}
	}

	public void sendTrafficData(){
		if (settings.isExternalListenerUsed){
			if (step % settings.extListenerUpdateInterval == 0 & step > 0) {
				extListner.sendTrafficData(trafficNetwork);
			}
		}
	}




	public void getActionsFromExtListner(){
		if (settings.isExternalListenerUsed){
			RoadIndex roadIndex = extListner.getRoadDirChange();
			if (roadIndex != null) {
				if (roadIndex.edges != null) {
					for (RoadControl edge : roadIndex.edges) {
						if (edge.laneChange) {
							int oppositeEdgeIndex = trafficNetwork.edges.get(edge.index).getOppositeEdge().index;
							//if  (oppositeEdgeIndex== 145 || oppositeEdgeIndex == 118) {
								changeLaneDirection(oppositeEdgeIndex);
								laneChangedEdgeIndex.add(oppositeEdgeIndex);
							//}
						}

						if (edge.speed > 0){
							trafficNetwork.edges.get(edge.index).changeFreeFlowSpeed(edge.speed);
						}
					}
				}
			}
		}
	}




	public ArrayList<Integer> getLaneChanges(){
		ArrayList<Integer> indexes = new ArrayList<>();

		for (Integer index : laneChangedEdgeIndex){
			indexes.add(index);
		}
		laneChangedEdgeIndex.clear();
		return indexes;
	}

	public  void markLanesToChange(){
		for (final Edge edge : trafficNetwork.edges){
			if (edge.index == 292){
				edge.getLastLane().isDirectionChanging = true;
			}
		}
	}

	void moveVehiclesAroundBorder(List<Fellow> connectedFellows, double timeNow, List<Edge> pspBorderEdges){
		final ArrayList<Vehicle> vehiclesAroundBorder = moveVehicleForward(timeNow, pspBorderEdges);
		moveVehicleToNextLink(connectedFellows, timeNow, vehiclesAroundBorder);
	}

	void transferDataTofellow(final Worker worker){
		if (!settings.isServerBased) {
			worker.transferVehicleDataToFellow();
		}
	}

	void moveVehiclesNotAroundBorder(List<Fellow> connectedFellows, double timeNow, List<Edge> pspNonBorderEdges){
		final ArrayList<Vehicle> vehiclesNotAroundBorder = moveVehicleForward(timeNow, pspNonBorderEdges);
		moveVehicleToNextLink(connectedFellows, timeNow, vehiclesNotAroundBorder);
	}

	void removeTripFinishedVehicles(){
		trafficNetwork.addTripFinishedVehicles(oneStepData_allVehiclesReachedDestination);
		trafficNetwork.removeActiveVehicles(oneStepData_allVehiclesReachedDestination);
	}

	public void onVehicleMove(int step){
		if(simulationListener != null && trafficNetwork.isPublishTime(step)){
			List<Vehicle> allmovedVehicles = trafficNetwork.vehicles.stream().filter(vehicle -> vehicle.active)
					.collect(Collectors.toList());
			simulationListener.onVehicleMove(allmovedVehicles, step, trafficNetwork);
		}
	}

	public void onVehicleRemove(List<Vehicle> vehicles, int step){
		if(simulationListener != null){
			simulationListener.onVehicleRemove(vehicles, step, trafficNetwork);
		}
	}

	synchronized public void simulateOneStepSingle(double timeNow, int step, List<Edge> pspBorderEdges,
											 List<Edge> pspNonBorderEdges, int numLocalRandomPrivateVehicles, int numLocalRandomTrams,
											 int numLocalRandomBuses, boolean isNewNonPubVehiclesAllowed,
											 boolean isNewTramsAllowed, boolean isNewBusesAllowed) {
		pause();
		moveVehiclesAroundBorder(new ArrayList<>(), timeNow, pspBorderEdges);
		moveVehiclesNotAroundBorder(new ArrayList<>(), timeNow, pspNonBorderEdges);
		onVehicleMove(step);
		removeTripFinishedVehicles();
		onVehicleRemove(oneStepData_allVehiclesReachedDestination, step);
		trafficNetwork.changeLaneOfVehicles(timeNow);
		trafficNetwork.updateTrafficLights(timeNow);
		trafficNetwork.updateTramStopTimers();
		trafficNetwork.releaseTripMakingVehicles(timeNow, simulationListener);
		trafficNetwork.releaseVehicleFromParking(timeNow, simulationListener);
		trafficNetwork.blockTramAtTramStop();
		trafficNetwork.removeActiveVehicles(oneStepData_vehiclesReachedFellowWorker);
		trafficNetwork.createInternalVehicles(numLocalRandomPrivateVehicles, numLocalRandomTrams,
				numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed,
				timeNow);
		trafficNetwork.repeatExternalVehicles(step, timeNow);
		trafficNetwork.finishRemoveCheck(timeNow);
		// Clear one-step data
		clearOneStepData();
	}

	/////////////////////////////////////////

	public void setPspEdges(Map<String, List<Edge>> pspEdges){
		this.pspBorderEdges = pspEdges.get("Border");
		this.pspNonBorderEdges = pspEdges.get("NonBorder");
	}

	public TrafficNetwork getTrafficNetwork() {
		return trafficNetwork;
	}

	public void createVehicles(ArrayList<SerializableExternalVehicle> externalRoutes){
		trafficNetwork.createExternalVehicles(externalRoutes, timeNow);
		trafficNetwork.createInternalVehicles(numLocalRandomPrivateVehicles, numLocalRandomTrams,
				numLocalRandomBuses, true, true, true, timeNow);
	}

	public void changeLaneBlock(int laneIndex, boolean isBlocked) {
		trafficNetwork.lanes.get(laneIndex).isBlocked = isBlocked;
	}

	public void changeLaneDirection(int edgeIndex){

		if (trafficNetwork.edges.get(edgeIndex).getLaneCount() + trafficNetwork.edges.get(edgeIndex).getOppositeEdge().getLaneCount() <= 4){
			if (trafficNetwork.edges.get(edgeIndex).getLaneCount() > 1) {
				trafficNetwork.edges.get(edgeIndex).getLastLane().markLaneToChangeDir();
			} else {
				System.out.println("Warning : Cannot change the direction because this is the only lane in Road: " + edgeIndex);
			}
		}
		else {
			if (trafficNetwork.edges.get(edgeIndex).getLaneCount() > 2) {
				trafficNetwork.edges.get(edgeIndex).getLastLane().markLaneToChangeDir();
			} else {
				System.out.println("Warning : Cannot change the direction because this is the only lane in Road: " + edgeIndex);
			}
		}
	}

	public void addTransferredVehicle(Vehicle vehicle){
		trafficNetwork.addOneTransferredVehicle(vehicle, timeNow);
	}

	public void resetExistingNetwork(){
		// Reset existing network
		for (final Edge edge : trafficNetwork.edges) {
			edge.currentSpeed = edge.freeFlowSpeed;
		}
	}

	public void resetTraffic(){
		for (final Edge edge : pspBorderEdges) {
			edge.clearVehicles();
		}
		for (final Edge edge : pspNonBorderEdges) {
			edge.clearVehicles();
		}
		trafficNetwork.resetTraffic();
	}

	public void updateTrafficAtOutgoingEdgesToFellows(int laneIndex, double position, double speed){
		trafficNetwork.lanes.get(laneIndex).endPositionOfLatestVehicleLeftThisWorker = position;
		trafficNetwork.lanes.get(laneIndex).speedOfLatestVehicleLeftThisWorker = speed;
	}

	public void clearReportedTrafficData(){
		trafficNetwork.clearReportedData();
	}

	public void simulateOneStep(Worker worker, boolean isNewNonPubVehiclesAllowed,
								boolean isNewTramsAllowed, boolean isNewBusesAllowed){
		simulateOneStep(worker, timeNow, step, pspBorderEdges, pspNonBorderEdges, numLocalRandomPrivateVehicles,
				numLocalRandomTrams, numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed);
	}

	public int getStep() {
		return step;
	}

	public double getTimeNow() {
		return timeNow;
	}
}

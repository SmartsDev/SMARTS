package processor.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.UUID;

import common.Settings;
import osm.OSM;
import processor.communication.IncomingConnectionBuilder;
import processor.communication.MessageHandler;
import processor.communication.message.Message_SW_BlockLane;
import processor.communication.message.Message_SW_ChangeSpeed;
import processor.communication.message.Message_SW_KillWorker;
import processor.communication.message.Message_SW_ServerBased_ShareTraffic;
import processor.communication.message.Message_SW_ServerBased_Simulate;
import processor.communication.message.Message_SW_Serverless_Pause;
import processor.communication.message.Message_SW_Serverless_Resume;
import processor.communication.message.Message_SW_Serverless_Start;
import processor.communication.message.Message_SW_Serverless_Stop;
import processor.communication.message.Message_SW_Setup;
import processor.communication.message.Message_WS_Join;
import processor.communication.message.Message_WS_TrafficReport;
import processor.communication.message.SerializableRoute;
import processor.communication.message.SerializableTravelTime;
import processor.communication.message.Message_WS_ServerBased_SharedMyTrafficWithNeighbor;
import processor.communication.message.Message_WS_Serverless_Complete;
import processor.communication.message.Message_WS_SetupCreatingVehicles;
import processor.communication.message.Message_WS_SetupDone;
import processor.communication.message.Serializable_GPS_Rectangle;
import processor.communication.message.Serializable_GUI_Vehicle;
import processor.server.gui.GUI;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.road.RoadUtil;

/**
 * This class can do: 1) loading and distributing simulation configuration; 2)
 * balancing workload between workers; 3) instructing workers to perform tasks
 * (if server-based synchronization is enabled); 4) visualizing simulation; 5)
 * collecting results from workers; 6) writing results to files.
 * 
 * This class can be run as Java application.
 */
public class Server implements MessageHandler, Runnable {
	public RoadNetwork roadNetwork;
	ArrayList<WorkerMeta> workerMetas = new ArrayList<>();
	int step = 0;// Time step in the current simulation
	GUI gui;
	FileOutput fileOutput = new FileOutput();
	public boolean isSimulating = false;// Whether simulation is running, i.e., it is not paused or stopped
	int numInternalNonPubVehiclesAtAllWorkers = 0;
	int numInternalTramsAtAllWorkers = 0;
	int numInternalBusesAtAllWorkers = 0;
	long timeStamp = 0;
	double simulationWallTime = 0;// Total time length spent on simulation
	ScriptLoader scriptLoader = new ScriptLoader();
	int totalNumWwCommChannels = 0;// Total number of communication channels between workers. A worker has two
									// channels with a neighbor worker, one for sending and one for receiving.
	ArrayList<Node> nodesToAddLight = new ArrayList<>();
	ArrayList<Node> nodesToRemoveLight = new ArrayList<>();
	int numTrajectoriesReceived = 0;// Number of complete trajectories received from workers
	int numVehiclesCreatedDuringSetup = 0;// For updating setup progress on GUI
	int numVehiclesNeededAtStart = 0;// For updating setup progress on GUI
	double aggregatedVehicleCountInOneSimulation = 0.0;
	double aggregatedVehicleTravelSpeedInOneSimulation = 0.0;
	Server server;
	Scanner sc = new Scanner(System.in);
	boolean isOpenForNewWorkers = true;
	ArrayList<Message_WS_TrafficReport> receivedTrafficReportCache = new ArrayList<>();
	ArrayList<SerializableRoute> routesForOutput = new ArrayList<SerializableRoute>();
	HashMap<String, TreeMap<Double, double[]>> trajectoriesForOutput = new HashMap<String, TreeMap<Double, double[]>>();
	HashMap<String, Double> travelTimesForOutput = new HashMap<String, Double>();

	public static void main(final String[] args) {
		if (processCommandLineArguments(args)) {
			new Server().run();
		} else {
			System.out.println("There is an error in command line parameter. Program exits.");
		}
	}

	static boolean processCommandLineArguments(final String[] args) {
		try {
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
				case "-gui":
					Settings.isVisualize = Boolean.parseBoolean(args[i + 1]);
					break;
				}
			}
		} catch (final Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Adds a new worker unless simulation is running or the required number of
	 * workers is reached.
	 */
	void addWorker(final Message_WS_Join received) {
		final WorkerMeta worker = new WorkerMeta(received.workerName, received.workerAddress, received.workerPort);
		if (isAllWorkersAtState(WorkerState.NEW) && Settings.numWorkers > workerMetas.size()) {
			workerMetas.add(worker);
			System.out.println(workerMetas.size() + "/" + Settings.numWorkers + " workers connected.");
			if (Settings.isVisualize) {
				gui.updateNumConnectedWorkers(workerMetas.size());
				if (workerMetas.size() == Settings.numWorkers) {
					gui.getReadyToSetup();
				}
			} else {
				if (workerMetas.size() == Settings.numWorkers) {
					isOpenForNewWorkers = false;// No need for more workers
					acceptSimScriptFromConsole();// Let user input simulation script path
				}

			}
		} else {
			final Message_SW_KillWorker msd = new Message_SW_KillWorker(Settings.isSharedJVM);
			worker.send(msd);
		}
	}

	/*
	 * Start a server-less simulation.
	 */
	void askWorkersProceedWithoutServer() {
		timeStamp = System.nanoTime();
		final Message_SW_Serverless_Start message = new Message_SW_Serverless_Start(step);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
			worker.setState(WorkerState.SERVERLESS_WORKING);
		}
	}

	/*
	 * Ask workers to transfer vehicles information to fellow workers in
	 * server-based synchronization mode.
	 */
	void askWorkersShareTrafficDataWithFellowWorkers() {

		// Increment step
		step++;
		timeStamp = System.nanoTime();

		System.out.println("Doing step " + step);

		// Ask all workers to share data with fellow
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.SHARING_STARTED);
		}
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_ServerBased_ShareTraffic(step));
		}
	}

	/*
	 * Ask workers to update traffic in their corresponding work areas for one time
	 * step. This is called after workers exchange traffic information with their
	 * neighbors in server-based simulation.
	 */
	synchronized void askWorkersSimulateOneStep() {
		boolean isNewNonPubVehiclesAllowed = numInternalNonPubVehiclesAtAllWorkers < Settings.numGlobalRandomBackgroundPrivateVehicles
				? true
				: false;
		boolean isNewTramsAllowed = numInternalTramsAtAllWorkers < Settings.numGlobalBackgroundRandomTrams ? true
				: false;
		boolean isNewBusesAllowed = numInternalBusesAtAllWorkers < Settings.numGlobalBackgroundRandomBuses ? true
				: false;

		// Clear one-step vehicle counts from last step
		numInternalNonPubVehiclesAtAllWorkers = 0;
		numInternalTramsAtAllWorkers = 0;
		numInternalBusesAtAllWorkers = 0;

		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.SIMULATING);
		}
		final Message_SW_ServerBased_Simulate message = new Message_SW_ServerBased_Simulate(isNewNonPubVehiclesAllowed,
				isNewTramsAllowed, isNewBusesAllowed, UUID.randomUUID().toString());
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
		}
	}

	void buildGui() {
		if (gui != null) {
			gui.dispose();
		}
		final GUI newGUI = new GUI(this);
		gui = newGUI;
		gui.setVisible(true);
	}

	public void changeMap() {
		Settings.listRouteSourceWindowForInternalVehicle.clear();
		Settings.listRouteDestinationWindowForInternalVehicle.clear();
		Settings.listRouteSourceDestinationWindowForInternalVehicle.clear();
		Settings.isNewEnvironment = true;

		if (Settings.inputOpenStreetMapFile.length() > 0) {
			final OSM osm = new OSM();
			osm.processOSM(Settings.inputOpenStreetMapFile, true);
			Settings.isBuiltinRoadGraph = false;
			// Revert to built-in map if there was an error when converting new map
			if (Settings.roadGraph.length() == 0) {
				Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
				Settings.isBuiltinRoadGraph = true;
			}
		} else {
			Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
			Settings.isBuiltinRoadGraph = true;
		}

		roadNetwork = new RoadNetwork();// Build road network based on new road graph
	}

	/**
	 * Change simulation speed by setting pause time after doing a step at all
	 * workers. Note that this will affect simulation time. By default there is no
	 * pause time between steps.
	 */
	public void changeSpeed(final int pauseTimeEachStep) {
		Settings.pauseTimeBetweenStepsInMilliseconds = pauseTimeEachStep;
		final Message_SW_ChangeSpeed message = new Message_SW_ChangeSpeed(Settings.pauseTimeBetweenStepsInMilliseconds);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
		}
	}

	public WorkerMeta getWorkerAtRouteStart(final Node routeStartNode) {
		for (final WorkerMeta worker : workerMetas) {
			if (worker.workarea.workCells.contains(routeStartNode.gridCell)) {
				return worker;
			}
		}
		return null;
	}

	boolean isAllWorkersAtState(final WorkerState state) {
		int count = 0;
		for (final WorkerMeta w : workerMetas) {
			if (w.state == state) {
				count++;
			}
		}

		if (count == workerMetas.size()) {
			return true;
		} else {
			return false;
		}

	}

	public void killConnectedWorkers() {
		final Message_SW_KillWorker msd = new Message_SW_KillWorker(Settings.isSharedJVM);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(msd);
		}
		workerMetas.clear();
		Settings.isNewEnvironment = true;
	}

	public void pauseSim() {
		isSimulating = false;
		if (!Settings.isServerBased) {
			final Message_SW_Serverless_Pause message = new Message_SW_Serverless_Pause(step);
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
			}
		}
	}

	/**
	 * Process received message sent from worker. Based on the received message,
	 * server can update GUI, decide whether to do next time step or finish
	 * simulation and instruct the workers what to do next.
	 */
	@Override
	synchronized public void processReceivedMsg(final Object message) {
		if (message instanceof Message_WS_Join) {
			if (isOpenForNewWorkers)
				addWorker((Message_WS_Join) message);
		} else if (message instanceof Message_WS_SetupCreatingVehicles) {
			numVehiclesCreatedDuringSetup += ((Message_WS_SetupCreatingVehicles) message).numVehicles;

			double createdVehicleRatio = (double) numVehiclesCreatedDuringSetup / numVehiclesNeededAtStart;
			if (createdVehicleRatio > 1) {
				createdVehicleRatio = 1;
			}

			if (Settings.isVisualize) {
				gui.updateSetupProgress(createdVehicleRatio);
			}
		} else if (message instanceof Message_WS_SetupDone) {
			updateWorkerState(((Message_WS_SetupDone) message).workerName, WorkerState.READY);
			totalNumWwCommChannels += (((Message_WS_SetupDone) message).numFellowWorkers);
			if (isAllWorkersAtState(WorkerState.READY)) {
				if (Settings.isVisualize) {
					gui.stepToDraw = 0;
				}
				startSimulation();
			}
		} else if (message instanceof Message_WS_ServerBased_SharedMyTrafficWithNeighbor) {
			if (!isSimulating) {
				// No need to process the message if simulation was stopped
				return;
			}

			Message_WS_ServerBased_SharedMyTrafficWithNeighbor messageToProcess = (Message_WS_ServerBased_SharedMyTrafficWithNeighbor) message;

			for (final WorkerMeta w : workerMetas) {
				if (w.name.equals(messageToProcess.workerName) && (w.state == WorkerState.SHARING_STARTED)) {
					updateWorkerState(messageToProcess.workerName, WorkerState.SHARED);
					if (isAllWorkersAtState(WorkerState.SHARED)) {
						askWorkersSimulateOneStep();
					}
					break;
				}
			}

		} else if (message instanceof Message_WS_TrafficReport) {
			if (!isSimulating) {
				// No need to process the message if simulation was stopped
				return;
			}

			final Message_WS_TrafficReport received = (Message_WS_TrafficReport) message;

			// Cache received reports
			receivedTrafficReportCache.add(received);

			// Output data from the reports
			processCachedReceivedTrafficReports();

			// Stop if max number of steps is reached in server-based mode
			if (Settings.isServerBased) {
				updateWorkerState(received.workerName, WorkerState.FINISHED_ONE_STEP);
				if (isAllWorkersAtState(WorkerState.FINISHED_ONE_STEP)) {
					updateSimulationTime();
					if (step >= Settings.maxNumSteps) {
						stopSim();
					} else if (isSimulating) {
						askWorkersShareTrafficDataWithFellowWorkers();
					}
				}
			} else {
				// Update time step in server-less mode
				if (received.step > step) {
					step = received.step;
				}
			}
		} else if (message instanceof Message_WS_Serverless_Complete) {
			if (!isSimulating) {
				// No need to process the message if simulation was stopped
				return;
			}
			final Message_WS_Serverless_Complete received = (Message_WS_Serverless_Complete) message;
			if (received.step > step) {
				step = received.step;
			}

			updateWorkerState(received.workerName, WorkerState.NEW);

			if (isAllWorkersAtState(WorkerState.NEW)) {
				updateSimulationTime();
				stopSim();
			}
		}
	}

	synchronized void processCachedReceivedTrafficReports() {
		final Iterator<Message_WS_TrafficReport> iMessage = receivedTrafficReportCache.iterator();
		while (iMessage.hasNext()) {
			final Message_WS_TrafficReport message = iMessage.next();
			// Update GUI
			if (Settings.isVisualize) {
				gui.updateObjectData(message.vehicles, message.trafficLights, message.workerName, workerMetas.size(),
						message.step);
			}
			// Build trajectories of vehicles based on the vehicle list
			if (Settings.outputTrajectoryScope != DataOutputScope.NONE) {
				double timeStamp = message.step / Settings.numStepsPerSecond;
				for (Serializable_GUI_Vehicle vehicle : message.vehicles) {
					if (Settings.outputTrajectoryScope == DataOutputScope.ALL
							|| (vehicle.isForeground && Settings.outputTrajectoryScope == DataOutputScope.FOREGROUND)
							|| (!vehicle.isForeground
									&& Settings.outputTrajectoryScope == DataOutputScope.BACKGROUND)) {
						String key=vehicle.id+"_"+vehicle.type;
						if (!trajectoriesForOutput.containsKey(key)) {
							trajectoriesForOutput.put(key, new TreeMap<Double, double[]>());
						}
						trajectoriesForOutput.get(key).put(timeStamp,
								new double[] { vehicle.latHead, vehicle.lonHead });
					}
				}
			}
			// Aggregate vehicle count and travel speed
			aggregatedVehicleCountInOneSimulation += message.totalNumVehicles;
			aggregatedVehicleTravelSpeedInOneSimulation += message.aggregatedTravelSpeedValues;
			// Store routes of new vehicles created since last report
			routesForOutput.addAll(message.newRoutesSinceLastReport);
			// Update for travel time output
			for (SerializableTravelTime record : message.travelTimes) {
				travelTimesForOutput.put(record.ID, record.travelTime);
			}
			// Increment vehicle counts
			numInternalNonPubVehiclesAtAllWorkers += message.numInternalNonPubVehicles;
			numInternalTramsAtAllWorkers += message.numInternalTrams;
			numInternalBusesAtAllWorkers += message.numInternalBuses;
			// Remove processed message
			iMessage.remove();
		}
	}

	@Override
	public void run() {
		// Load default road network
		Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
		roadNetwork = new RoadNetwork();

		// Start GUI or load simulation configuration without GUI
		if (Settings.isVisualize) {
			buildGui();
		} else {
			acceptInitialConfigFromConsole();
		}

		// Prepare to receive connection request from workers
		new IncomingConnectionBuilder(Settings.serverListeningPortForWorkers, this).start();
	}

	void acceptInitialConfigFromConsole() {
		// Let user input number of workers
		System.out.println("Please specify the number of workers.");
		Settings.numWorkers = Integer.parseInt(sc.nextLine());
		while (Settings.numWorkers <= 0) {
			System.out.println("Please specify the number of workers.");
			Settings.numWorkers = Integer.parseInt(sc.nextLine());
		}
		System.out.println("Please specify the partitioning type [GridGraph, Space-grid].");
		Settings.partitionType = sc.nextLine();
		while (!(Settings.partitionType.equals("GridGraph") || Settings.partitionType.equals("Space-grid")) ) {
			System.out.println("Please specify the partitioning type [GridGraph, Space-grid].");
			Settings.partitionType = sc.nextLine();
		}
		// Kill all connected workers
		killConnectedWorkers();
		// Inform user next step
		System.out.println("Please launch workers now.");
	}

	void acceptSimStartCommandFromConsole() {
		System.out.println("Ready to simulate. Start (y/n)?");
		String choice = sc.nextLine();
		if (choice.equals("y") || choice.equals("Y")) {
			startSimulationFromLoadedScript();
		} else if (choice.equals("n") || choice.equals("N")) {
			System.out.println("Quit system.");
			killConnectedWorkers();
			System.exit(0);
		} else {
			System.out.println("Ready to simulate. Start (y/n)?");
		}
	}

	void acceptConsoleCommandAtSimEnd() {
		System.out.println("Simulations are completed. Exit (y/n)?");
		String choice = sc.nextLine();
		if (choice.equals("y") || choice.equals("Y")) {
			// Kill all connected workers
			System.out.println("Quit system.");
			killConnectedWorkers();
			System.exit(0);
		} else if (choice.equals("n") || choice.equals("N")) {
			acceptSimScriptFromConsole();
		} else {
			System.out.println("Simulations are completed. Exit (y/n)?");
		}
	}

	void acceptSimScriptFromConsole() {
		System.out.println("Please specify the simulation script path.");
		Settings.inputSimulationScript = sc.nextLine();
		while (!scriptLoader.loadScriptFile()) {
			System.out.println("Please specify the simulation script path.");
			Settings.inputSimulationScript = sc.nextLine();
		}
		if (workerMetas.size() == Settings.numWorkers) {
			acceptSimStartCommandFromConsole();
		}
	}

	public void resumeSim() {
		isSimulating = true;
		if (Settings.isServerBased) {
			System.out.println("Resuming server-based simulation...");
			askWorkersShareTrafficDataWithFellowWorkers();
		} else {
			System.out.println("Resuming server-less simulation...");
			final Message_SW_Serverless_Resume message = new Message_SW_Serverless_Resume(step);
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
			}
		}
	}

	/**
	 * Update node lists for nodes where traffic light needs to be added or removed.
	 * The lists will be sent to worker during setup.
	 */
	public void setLightChangeNode(final Node node) {
		node.light = !node.light;
		nodesToAddLight.remove(node);
		nodesToRemoveLight.remove(node);
		if (node.light) {
			nodesToAddLight.add(node);
		} else {
			nodesToRemoveLight.add(node);
		}
	}

	ArrayList<double[]> setRouteSourceDestinationWindow(final ArrayList<Serializable_GPS_Rectangle> sList) {
		final ArrayList<double[]> list = new ArrayList<>();
		for (final Serializable_GPS_Rectangle sgr : sList) {
			// Skip the zero item when the list does not have meaningful items
			if ((sgr.minLon == 0) && (sgr.maxLat == 0) && (sgr.maxLon == 0) && (sgr.minLat == 0)) {
				continue;
			}
			list.add(new double[] { sgr.minLon, sgr.maxLat, sgr.maxLon, sgr.minLat });
		}
		return list;
	}

	/**
	 * Resets dynamic fields and sends simulation configuration to workers. The
	 * workers will set up simulation environment upon receiving the configuration.
	 */
	public void setupNewSim() {
		// Reset temporary variables
		simulationWallTime = 0;
		step = 0;
		totalNumWwCommChannels = 0;
		numTrajectoriesReceived = 0;
		numVehiclesCreatedDuringSetup = 0;
		numVehiclesNeededAtStart = 0;
		receivedTrafficReportCache.clear();
		aggregatedVehicleCountInOneSimulation = 0.0;
		aggregatedVehicleTravelSpeedInOneSimulation = 0.0;

		// Reset worker status
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.NEW);
		}

		// In a new environment (map), determine the work areas for all workers
		if (Settings.isNewEnvironment) {
			roadNetwork.buildGrid();
			WorkloadBalancer.partitionGridCells(workerMetas, roadNetwork);
		}

		// Determine the number of internal vehicles at all workers
		WorkloadBalancer.assignNumInternalVehiclesToWorkers(workerMetas, roadNetwork);

		// Assign vehicle routes from external file to workers
		final RouteLoader routeLoader = new RouteLoader(this, workerMetas);
		routeLoader.loadRoutes();

		// Get number of vehicles needed
		numVehiclesNeededAtStart = routeLoader.vehicles.size() + Settings.numGlobalRandomBackgroundPrivateVehicles
				+ Settings.numGlobalBackgroundRandomTrams + Settings.numGlobalBackgroundRandomBuses;

		// Send simulation configuration to workers
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_Setup(workerMetas, worker, roadNetwork.edges, step, nodesToAddLight,
					nodesToRemoveLight));
		}

		// Initialize output
		fileOutput.init();

		Settings.isNewEnvironment = false;

		System.out.println("Sent simulation configuration to all workers.");

	}

	public void askWorkersChangeLaneBlock(int laneIndex, boolean isBlocked) {
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_BlockLane(laneIndex, isBlocked));
		}
	}

	void startSimulation() {
		if (step < Settings.maxNumSteps) {
			System.out.println("All workers are ready to do simulation.");
			isSimulating = true;
			if (Settings.isVisualize) {
				gui.startSimulation();
			}
			if (Settings.isServerBased) {
				System.out.println("Starting server-based simulation...");
				askWorkersShareTrafficDataWithFellowWorkers();
			} else {
				System.out.println("Starting server-less simulation...");
				askWorkersProceedWithoutServer();
			}
		}

	}

	void startSimulationFromLoadedScript() {
		if (scriptLoader.retrieveOneSimulationSetup()) {
			changeMap();
		}
		setupNewSim();
	}

	public void stopSim() {
		isSimulating = false;
		numInternalNonPubVehiclesAtAllWorkers = 0;
		numInternalTramsAtAllWorkers = 0;
		numInternalBusesAtAllWorkers = 0;
		nodesToAddLight.clear();
		nodesToRemoveLight.clear();
		processCachedReceivedTrafficReports();
		fileOutput.outputSimLog(step, simulationWallTime, totalNumWwCommChannels, aggregatedVehicleCountInOneSimulation,
				aggregatedVehicleTravelSpeedInOneSimulation);
		aggregatedVehicleCountInOneSimulation = 0.0;
		aggregatedVehicleTravelSpeedInOneSimulation = 0.0;
		fileOutput.outputRoutes(routesForOutput);
		routesForOutput.clear();
		fileOutput.outputTrajectories(trajectoriesForOutput);
		trajectoriesForOutput.clear();
		fileOutput.outputTravelTime(travelTimesForOutput);
		travelTimesForOutput.clear();
		fileOutput.close();

		// Ask workers stop in server-less mode. Note that workers may already stopped
		// before receiving the message.
		if (!Settings.isServerBased) {
			final Message_SW_Serverless_Stop message = new Message_SW_Serverless_Stop(step);
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
				worker.setState(WorkerState.NEW);
			}
		}

		System.out.println("Simulation stopped.\n");

		if ((Settings.isVisualize)) {
			if (workerMetas.size() == Settings.numWorkers) {
				gui.getReadyToSetup();
			}
		} else {
			if (scriptLoader.isEmpty()) {
				acceptConsoleCommandAtSimEnd();
			} else {
				System.out.println("Loading configuration of new simulation...");
				startSimulationFromLoadedScript();
			}
		}
	}

	/**
	 * Updates wall time spent on simulation.
	 */
	synchronized void updateSimulationTime() {
		simulationWallTime += (double) (System.nanoTime() - timeStamp) / 1000000000;
	}

	synchronized void updateWorkerState(final String workerName, final WorkerState state) {
		for (final WorkerMeta worker : workerMetas) {
			if (worker.name.equals(workerName)) {
				worker.state = state;
				break;
			}
		}
	}
}

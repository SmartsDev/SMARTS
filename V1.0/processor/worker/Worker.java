package processor.worker;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import common.Settings;
import common.SysUtil;
import processor.communication.IncomingConnectionBuilder;
import processor.communication.MessageHandler;
import processor.communication.MessageSender;
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
import processor.communication.message.Message_WS_ServerBased_SharedMyTrafficWithNeighbor;
import processor.communication.message.Message_WS_Serverless_Complete;
import processor.communication.message.Message_WS_SetupCreatingVehicles;
import processor.communication.message.Message_WS_SetupDone;
import processor.communication.message.Message_WW_Traffic;
import processor.communication.message.SerializableDouble;
import processor.communication.message.SerializableFrontVehicleOnBorder;
import processor.communication.message.SerializableGridCell;
import processor.communication.message.SerializableInt;
import processor.communication.message.SerializableLaneIndex;
import processor.communication.message.SerializableVehicle;
import processor.communication.message.SerializableWorkerMetadata;
import processor.communication.message.Serializable_GPS_Rectangle;
import processor.server.DataOutputScope;
import traffic.TrafficNetwork;
import traffic.light.LightUtil;
import traffic.road.Edge;
import traffic.road.GridCell;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.routing.RouteUtil;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

/**
 * Worker receives simulation configuration from server and simulates traffic in
 * a specific area. Operations include:
 * <ul>
 * <li>Initialize work area
 * <li>Generate vehicles
 * <li>Update status of vehicles and traffic lights
 * <li>Exchange traffic information with neighbor workers
 * <li>Report result back to server
 * </ul>
 *
 * Note: IP/port of worker should be directly accessible as the information will
 * be used by server to build TCP connection with the worker.
 *
 * Simulation runs in server-based mode (BSP) or server-less mode (PSP). In BSP,
 * a worker needs to wait two messages from server at each time step: one asking
 * the worker to share traffic information with its fellow workers, another
 * asking the worker to simulate (computing models for vehicles and traffic
 * lights). In PSP, worker synchronize simulation with its fellow workers
 * automatically. Worker can send information about vehicles and traffic lights
 * to server in both modes, if server asks the worker to do so during setup. The
 * information can be used to update GUI at server or a remote controller.
 */
public class Worker implements MessageHandler, Runnable {
	class ConnectionBuilderTerminationTask extends TimerTask {
		@Override
		public void run() {
			connectionBuilder.terminate();
		}
	}

	/**
	 * Starts worker and tries to connect with server
	 *
	 * @param args server address (optional)
	 */
	public static void main(final String[] args) {

		if (args.length > 0) {
			Settings.serverAddress = args[0];
		}

		new Worker().run();
	}

	Worker me = this;
	IncomingConnectionBuilder connectionBuilder;
	TrafficNetwork trafficNetwork;
	int step = 0;
	double timeNow;
	ArrayList<Fellow> fellowWorkers = new ArrayList<>();
	String name = "";
	MessageSender senderForServer;
	String address = "localhost";
	int listeningPort;
	Workarea workarea;
	ArrayList<Fellow> connectedFellows = new ArrayList<>();// Fellow workers that share at least one edge with this
															// worker
	ArrayList<Message_WW_Traffic> receivedTrafficCache = new ArrayList<>();
	Simulation simulation;
	boolean isDuringServerlessSim;// Once server-less simulation begins, this will true until the simulation ends
	boolean isPausingServerlessSim;
	boolean isSimulatingOneStep;
	ArrayList<Edge> pspBorderEdges = new ArrayList<>();// For PSP (server-less)
	ArrayList<Edge> pspNonBorderEdges = new ArrayList<>();// For PSP (server-less)
	Thread singleWorkerServerlessThread = new Thread();// Used when this worker is the only worker in server-less mode
	int numVehicleCreatedSinceLastSetupProgressReport = 0;
	int numLocalRandomPrivateVehicles = 0;
	int numLocalRandomTrams = 0;
	int numLocalRandomBuses = 0;

	void changeLaneBlock(int laneIndex, boolean isBlocked) {
		trafficNetwork.lanes.get(laneIndex).isBlocked = isBlocked;
	}

	void buildThreadForSingleWorkerServerlessSimulation() {
		singleWorkerServerlessThread = new Thread() {
			public void run() {

				while (step < Settings.maxNumSteps) {

					if (!isDuringServerlessSim) {
						break;
					}

					while (isPausingServerlessSim) {
						try {
							this.sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					timeNow = step / Settings.numStepsPerSecond;
					simulation.simulateOneStep(me, true, true, true);

					sendTrafficReportInServerlessMode();

					step++;
				}
				// Finish simulation
				senderForServer.send(new Message_WS_Serverless_Complete(name, step, trafficNetwork.vehicles.size()));

			}
		};
	}

	void sendTrafficReportInServerlessMode() {
		if ((step + 1) % Settings.trafficReportStepGapInServerlessMode == 0) {
			senderForServer
					.send(new Message_WS_TrafficReport(name, trafficNetwork.vehicles, trafficNetwork.lightCoordinator,
							trafficNetwork.newVehiclesSinceLastReport, step, trafficNetwork.numInternalNonPublicVehicle,
							trafficNetwork.numInternalTram, trafficNetwork.numInternalBus));
			trafficNetwork.clearReportedData();
		}
	}

	Vehicle createReceivedVehicle(final SerializableVehicle serializableVehicle) {
		final Vehicle vehicle = new Vehicle();
		vehicle.type = VehicleType.getVehicleTypeFromName(serializableVehicle.type);
		vehicle.length = vehicle.type.length;
		vehicle.routeLegs = RouteUtil.parseReceivedRoute(serializableVehicle.routeLegs, trafficNetwork.edges);
		vehicle.indexLegOnRoute = serializableVehicle.indexRouteLeg;
		vehicle.lane = trafficNetwork.lanes.get(serializableVehicle.laneIndex);
		vehicle.headPosition = serializableVehicle.headPosition;
		vehicle.speed = serializableVehicle.speed;
		vehicle.timeRouteStart = serializableVehicle.timeRouteStart;
		vehicle.id = serializableVehicle.id;
		vehicle.isExternal = serializableVehicle.isExternal;
		vehicle.isForeground = serializableVehicle.isForeground;
		vehicle.idLightGroupPassed = serializableVehicle.idLightGroupPassed;
		vehicle.driverProfile = DriverProfile.valueOf(serializableVehicle.driverProfile);
		return vehicle;
	}

	void createVehiclesFromFellow(final Message_WW_Traffic messageToProcess) {
		for (final SerializableVehicle serializableVehicle : messageToProcess.vehiclesEnteringReceiver) {
			final Vehicle vehicle = createReceivedVehicle(serializableVehicle);
			trafficNetwork.addOneTransferredVehicle(vehicle, timeNow);
		}
	}

	/**
	 * Divide edges overlapping with the responsible area of this worker into two
	 * sets based on their closeness to the border of the responsible area.
	 */
	void divideLaneSetForServerlessSim() {
		final HashSet<Edge> edgeSet = new HashSet<>();
		for (final Fellow fellow : fellowWorkers) {
			for (final Edge e : fellow.inwardEdgesAcrossBorder) {
				edgeSet.add(e);
				edgeSet.addAll(findInwardEdgesWithinCertainDistance(e.startNode, 0, 56.0 / Settings.numStepsPerSecond,
						edgeSet));
			}
		}
		pspBorderEdges.addAll(edgeSet);
		for (final Edge e : trafficNetwork.edges) {
			if (!edgeSet.contains(e)) {
				pspNonBorderEdges.add(e);
			}
		}
	}

	/**
	 * Find the fellow workers that need to communicate with this worker. Each of
	 * these fellow workers shares at least one edge with this worker.
	 */
	void findConnectedFellows() {
		connectedFellows.clear();
		for (final Fellow fellowWorker : fellowWorkers) {
			if ((fellowWorker.inwardEdgesAcrossBorder.size() > 0)
					|| (fellowWorker.outwardEdgesAcrossBorder.size() > 0)) {
				connectedFellows.add(fellowWorker);
			}
		}
	}

	HashSet<Edge> findInwardEdgesWithinCertainDistance(final Node node, final double accumulatedDistance,
			final double maxDistance, final HashSet<Edge> edgesToSkip) {
		for (final Edge e : node.inwardEdges) {
			if (edgesToSkip.contains(e)) {
				continue;
			} else {
				edgesToSkip.add(e);
				final double updatedAccumulatedDistance = accumulatedDistance + e.length;
				if (updatedAccumulatedDistance < maxDistance) {
					findInwardEdgesWithinCertainDistance(e.startNode, accumulatedDistance, maxDistance, edgesToSkip);
				}
			}
		}
		return edgesToSkip;
	}

	boolean isAllFellowsAtState(final FellowState state) {
		int count = 0;
		for (final Fellow w : connectedFellows) {
			if (w.state == state) {
				count++;
			}
		}

		if (count == connectedFellows.size()) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Create a new worker. Set the worker's name, address and listening port. The
	 * worker comes with a new receiver for receiving messages, e.g., connection
	 * requests, from other entities such as workers.
	 */
	void join() {
		// Get IP address
		address = SysUtil.getMyIpV4Addres();

		// Find an available port
		listeningPort = Settings.serverListeningPortForWorkers + 1
				+ (new Random()).nextInt(65535 - Settings.serverListeningPortForWorkers);
		while (true) {
			try {
				final ServerSocket ss = new ServerSocket(listeningPort);
				ss.close();
				break;
			} catch (final IOException e) {
				listeningPort = 60000 + (new Random()).nextInt(65535 - 60000);
				continue;
			}
		}

		connectionBuilder = new IncomingConnectionBuilder(listeningPort, this);
		connectionBuilder.start();

		senderForServer = new MessageSender(Settings.serverAddress, Settings.serverListeningPortForWorkers);

		name = SysUtil.getRandomID(4);

		workarea = new Workarea(name, null);

		senderForServer.send(new Message_WS_Join(name, address, listeningPort));
	}

	void proceedBasedOnSyncMethod() {
		// In case neighbor already sent traffic for this step
		processCachedReceivedTraffic();

		if (isAllFellowsAtState(FellowState.SHARED)) {
			if (Settings.isServerBased) {
				senderForServer.send(new Message_WS_ServerBased_SharedMyTrafficWithNeighbor(name));
			} else if (isDuringServerlessSim) {
				sendTrafficReportInServerlessMode();

				// Proceed to next step or finish
				if (step >= Settings.maxNumSteps) {
					senderForServer
							.send(new Message_WS_Serverless_Complete(name, step, trafficNetwork.vehicles.size()));
					resetTraffic();
				} else if (!isPausingServerlessSim) {
					step++;
					timeNow = step / Settings.numStepsPerSecond;
					simulation.simulateOneStep(this, true, true, true);
					proceedBasedOnSyncMethod();
				}
			}
		}
	}

	void processCachedReceivedTraffic() {
		final Iterator<Message_WW_Traffic> iMessage = receivedTrafficCache.iterator();

		while (iMessage.hasNext()) {
			final Message_WW_Traffic message = iMessage.next();
			if (message.stepAtSender == step) {
				processReceivedTraffic(message);
				iMessage.remove();
			}
		}
	}

	ArrayList<GridCell> processReceivedGridCells(final ArrayList<SerializableGridCell> received,
			final GridCell[][] grid) {
		final ArrayList<GridCell> cellsInWorkarea = new ArrayList<>();
		for (final SerializableGridCell receivedCell : received) {
			cellsInWorkarea.add(grid[receivedCell.row][receivedCell.column]);
		}
		return cellsInWorkarea;
	}

	void processReceivedMetadataOfWorkers(final ArrayList<SerializableWorkerMetadata> metadataWorkers) {
		// Set work area of all workers
		for (final SerializableWorkerMetadata metadata : metadataWorkers) {
			final ArrayList<GridCell> cellsInWorkarea = processReceivedGridCells(metadata.gridCells,
					trafficNetwork.grid);
			if (metadata.name.equals(name)) {
				workarea.setWorkCells(cellsInWorkarea);
			} else {
				final Fellow fellow = new Fellow(metadata.name, metadata.address, metadata.port, cellsInWorkarea);
				fellowWorkers.add(fellow);
			}
		}

		// Identify edges shared with fellow workers
		for (final Fellow fellowWorker : fellowWorkers) {
			fellowWorker.getEdgesFromAnotherArea(workarea);
			fellowWorker.getEdgesToAnotherArea(workarea);
		}

		// Identify fellow workers that share edges with this worker
		findConnectedFellows();

		// Prepare communication with the fellow workers that share edges with
		// this worker
		for (final Fellow fellowWorker : connectedFellows) {
			fellowWorker.prepareCommunication();
		}
	}

	@Override
	public synchronized void processReceivedMsg(final Object message) {
		if (message instanceof Message_SW_Setup) {
			final Message_SW_Setup messageToProcess = (Message_SW_Setup) message;
			processReceivedSimulationConfiguration(messageToProcess);
			trafficNetwork.buildEnvironment(workarea.workCells, workarea.workerName, step);
			resetTraffic();
			isSimulatingOneStep = false;

			// Pause a bit so other workers can reset before starting simulation
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if ((messageToProcess.indexNodesToAddLight.size() > 0)
					|| (messageToProcess.indexNodesToRemoveLight.size() > 0)) {
				trafficNetwork.lightCoordinator.init(trafficNetwork.nodes, messageToProcess.indexNodesToAddLight,
						messageToProcess.indexNodesToRemoveLight, workarea);
			}

			// Reset fellow state
			for (final Fellow connectedFellow : connectedFellows) {
				connectedFellow.state = FellowState.SHARED;
			}

			// Create vehicles
			numVehicleCreatedSinceLastSetupProgressReport = 0;
			final TimerTask progressTimerTask = new TimerTask() {
				@Override
				public void run() {
					senderForServer.send(new Message_WS_SetupCreatingVehicles(
							trafficNetwork.vehicles.size() - numVehicleCreatedSinceLastSetupProgressReport));
					numVehicleCreatedSinceLastSetupProgressReport = trafficNetwork.vehicles.size();
				}
			};
			final Timer progressTimer = new Timer();
			final Random random = new Random();
			if (Settings.isVisualize) {
				progressTimer.scheduleAtFixedRate(progressTimerTask, 500, random.nextInt(1000) + 1);
			}

			trafficNetwork.createExternalVehicles(messageToProcess.externalRoutes, timeNow);

			trafficNetwork.createInternalVehicles(numLocalRandomPrivateVehicles, numLocalRandomTrams,
					numLocalRandomBuses, true, true, true, timeNow);

			progressTimerTask.cancel();
			progressTimer.cancel();

			// Let server know that setup is done
			senderForServer.send(new Message_WS_SetupDone(name, connectedFellows.size()));
		} else if (message instanceof Message_SW_ServerBased_ShareTraffic) {
			final Message_SW_ServerBased_ShareTraffic messageToProcess = (Message_SW_ServerBased_ShareTraffic) message;

			step = messageToProcess.currentStep;
			timeNow = step / Settings.numStepsPerSecond;
			transferVehicleDataToFellow();
			proceedBasedOnSyncMethod();
		} else if (message instanceof Message_WW_Traffic) {
			final Message_WW_Traffic messageToProcess = (Message_WW_Traffic) message;

			receivedTrafficCache.add(messageToProcess);
			proceedBasedOnSyncMethod();
		} else if (message instanceof Message_SW_ServerBased_Simulate) {
			final Message_SW_ServerBased_Simulate messageToProcess = (Message_SW_ServerBased_Simulate) message;

			simulation.simulateOneStep(this, messageToProcess.isNewNonPubVehiclesAllowed,
					messageToProcess.isNewTramsAllowed, messageToProcess.isNewBusesAllowed);
			senderForServer
					.send(new Message_WS_TrafficReport(name, trafficNetwork.vehicles, trafficNetwork.lightCoordinator,
							trafficNetwork.newVehiclesSinceLastReport, step, trafficNetwork.numInternalNonPublicVehicle,
							trafficNetwork.numInternalTram, trafficNetwork.numInternalBus));
			trafficNetwork.clearReportedData();
		} else if (message instanceof Message_SW_Serverless_Start) {
			final Message_SW_Serverless_Start messageToProcess = (Message_SW_Serverless_Start) message;
			step = messageToProcess.startStep;
			isDuringServerlessSim = true;
			isPausingServerlessSim = false;

			if (connectedFellows.size() == 0) {
				buildThreadForSingleWorkerServerlessSimulation();
				singleWorkerServerlessThread.start();
			} else {
				timeNow = step / Settings.numStepsPerSecond;
				simulation.simulateOneStep(this, true, true, true);
				proceedBasedOnSyncMethod();
			}
		} else if (message instanceof Message_SW_KillWorker) {
			final Message_SW_KillWorker messageToProcess = (Message_SW_KillWorker) message;
			// Quit depending on how the worker was started
			if (messageToProcess.isSharedJVM) {
				final ConnectionBuilderTerminationTask task = new ConnectionBuilderTerminationTask();
				new Timer().schedule(task, 1);
			} else {
				System.exit(0);
			}
		} else if (message instanceof Message_SW_Serverless_Stop) {
			isDuringServerlessSim = false;
			isPausingServerlessSim = false;
			singleWorkerServerlessThread.stop();
			resetTraffic();
		} else if (message instanceof Message_SW_Serverless_Pause) {
			isPausingServerlessSim = true;
		} else if (message instanceof Message_SW_Serverless_Resume) {
			isPausingServerlessSim = false;
			// When it is not single worker environment, explicitly resume the routine tasks
			if (connectedFellows.size() > 0) {
				proceedBasedOnSyncMethod();
			}
		} else if (message instanceof Message_SW_ChangeSpeed) {
			final Message_SW_ChangeSpeed messageToProcess = (Message_SW_ChangeSpeed) message;
			Settings.pauseTimeBetweenStepsInMilliseconds = messageToProcess.pauseTimeBetweenStepsInMilliseconds;
		} else if (message instanceof Message_SW_BlockLane) {
			final Message_SW_BlockLane messageToProcess = (Message_SW_BlockLane) message;
			changeLaneBlock(messageToProcess.laneIndex, messageToProcess.isBlocked);
		}
	}

	void processReceivedSimulationConfiguration(final Message_SW_Setup received) {

		Settings.numWorkers = received.numWorkers;
		Settings.maxNumSteps = received.maxNumSteps;
		Settings.numStepsPerSecond = received.numStepsPerSecond;
		step = received.startStep;
		timeNow = step / Settings.numStepsPerSecond;
		Settings.trafficReportStepGapInServerlessMode = received.workerToServerReportStepGapInServerlessMode;
		Settings.periodOfTrafficWaitForTramAtStop = received.periodOfTrafficWaitForTramAtStop;
		Settings.driverProfileDistribution = setDriverProfileDistribution(received.driverProfileDistribution);
		Settings.lookAheadDistance = received.lookAheadDistance;
		Settings.trafficLightTiming = LightUtil.getLightTypeFromString(received.trafficLightTiming);
		Settings.isVisualize = received.isVisualize;
		numLocalRandomPrivateVehicles = received.numRandomPrivateVehicles;
		numLocalRandomTrams = received.numRandomTrams;
		numLocalRandomBuses = received.numRandomBuses;
		Settings.isServerBased = received.isServerBased;
		Settings.routingAlgorithm = RouteUtil.getRoutingAlgorithmFromString(received.routingAlgorithm);
		Settings.isAllowPriorityVehicleUseTramTrack = received.isAllowPriorityVehicleUseTramTrack;
		Settings.outputRouteScope = DataOutputScope.valueOf(received.outputRouteScope);
		Settings.outputTrajectoryScope = DataOutputScope.valueOf(received.outputTrajectoryScope);
		Settings.outputTravelTimeScope = DataOutputScope.valueOf(received.outputTravelTimeScope);
		Settings.isOutputSimulationLog = received.isOutputSimulationLog;
		Settings.listRouteSourceWindowForInternalVehicle = setRouteSourceDestinationWindow(
				received.listRouteSourceWindowForInternalVehicle);
		Settings.listRouteDestinationWindowForInternalVehicle = setRouteSourceDestinationWindow(
				received.listRouteDestinationWindowForInternalVehicle);
		Settings.listRouteSourceDestinationWindowForInternalVehicle = setRouteSourceDestinationWindow(
				received.listRouteSourceDestinationWindowForInternalVehicle);
		Settings.isAllowReroute = received.isAllowReroute;
		Settings.isAllowTramRule = received.isAllowTramRule;
		Settings.isDriveOnLeft = received.isDriveOnLeft;

		if (received.isNewEnvironment) {
			if (received.roadGraph.equals("builtin")) {
				Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
			} else {
				Settings.roadGraph = SysUtil.decompressString(received.roadGraph);
			}
			trafficNetwork = new TrafficNetwork();
			processReceivedMetadataOfWorkers(received.metadataWorkers);

			simulation = new Simulation(trafficNetwork, connectedFellows, workarea);
			divideLaneSetForServerlessSim();
		}

		// Reset lights
		trafficNetwork.lightCoordinator.init(trafficNetwork.nodes, new ArrayList<SerializableInt>(),
				new ArrayList<SerializableInt>(), workarea);
	}

	void processReceivedTraffic(final Message_WW_Traffic messageToProcess) {
		updateFellowState(messageToProcess.senderName, FellowState.SHARING_DATA_RECEIVED);
		createVehiclesFromFellow(messageToProcess);
		updateTrafficAtOutgoingEdgesToFellows(messageToProcess);
	}

	void resetTraffic() {

		for (final Fellow fellow : fellowWorkers) {
			fellow.vehiclesToCreateAtBorder.clear();
			fellow.state = FellowState.SHARED;
		}

		receivedTrafficCache.clear();
		for (final Edge edge : pspBorderEdges) {
			for (final Lane lane : edge.lanes) {
				lane.vehicles.clear();
			}
		}
		for (final Edge edge : pspNonBorderEdges) {
			for (final Lane lane : edge.lanes) {
				lane.vehicles.clear();
			}
		}

		trafficNetwork.resetTraffic();
	}

	@Override
	public void run() {
		// Join system by connecting with server
		join();
	}

	/**
	 * Set the percentage of drivers with different profiles, from highly aggressive
	 * to highly polite.
	 */
	ArrayList<Double> setDriverProfileDistribution(final ArrayList<SerializableDouble> sList) {
		final ArrayList<Double> list = new ArrayList<>();
		for (final SerializableDouble sd : sList) {
			list.add(sd.value);
		}
		return list;
	}

	ArrayList<double[]> setRouteSourceDestinationWindow(final ArrayList<Serializable_GPS_Rectangle> sList) {
		final ArrayList<double[]> list = new ArrayList<>();
		for (final Serializable_GPS_Rectangle sgr : sList) {
			list.add(new double[] { sgr.minLon, sgr.maxLat, sgr.maxLon, sgr.minLat });
		}
		return list;
	}

	/**
	 * Send vehicle position on cross-border edges to fellow workers.
	 */
	void transferVehicleDataToFellow() {
		for (final Fellow fellowWorker : connectedFellows) {
			fellowWorker.send(new Message_WW_Traffic(name, fellowWorker, step));
			updateFellowState(fellowWorker.name, FellowState.SHARING_DATA_SENT);
			fellowWorker.vehiclesToCreateAtBorder.clear();
		}
	}

	void unblockLanes(final ArrayList<SerializableLaneIndex> unblockedLaneIndex) {
		for (final SerializableLaneIndex item : unblockedLaneIndex) {
			trafficNetwork.lanes.get(item.index).isBlocked = false;
		}
	}

	void updateFellowState(final String workerName, final FellowState newState) {
		for (final Fellow fellow : connectedFellows) {
			if (fellow.name.equals(workerName)) {
				if ((fellow.state == FellowState.SHARING_DATA_RECEIVED)
						&& (newState == FellowState.SHARING_DATA_SENT)) {
					fellow.state = FellowState.SHARED;
				} else if ((fellow.state == FellowState.SHARING_DATA_SENT)
						&& (newState == FellowState.SHARING_DATA_RECEIVED)) {
					fellow.state = FellowState.SHARED;
				} else {
					fellow.state = newState;
				}
				break;
			}
		}
	}

	void updateTrafficAtOutgoingEdgesToFellows(final Message_WW_Traffic received) {
		for (final SerializableFrontVehicleOnBorder info : received.lastVehiclesLeftReceiver) {
			final int laneIndex = info.laneIndex;
			final double position = info.endPosition;
			final double speed = info.speed;
			trafficNetwork.lanes.get(laneIndex).endPositionOfLatestVehicleLeftThisWorker = position;
			trafficNetwork.lanes.get(laneIndex).speedOfLatestVehicleLeftThisWorker = speed;
		}
	}
}

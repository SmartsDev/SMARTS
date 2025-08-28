package processor.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import common.Settings;
import processor.SimServerData;
import processor.SimulationProcessor;
import processor.communication.IncomingConnectionBuilder;
import processor.communication.MessageHandler;
import processor.communication.externalMessage.DemandBasedLaneManager;
import processor.communication.externalMessage.ExternalSimulationListener;
import processor.communication.externalMessage.LaneManager;
import processor.communication.message.*;
import traffic.network.ODDistributor;
import traffic.road.Node;
import traffic.road.RoadNetwork;

/**
 * This class can do: 1) loading and distributing simulation configuration; 2)
 * balancing workload between workers; 3) instructing workers to perform tasks
 * (if server-based synchronization is enabled); 4) visualizing simulation; 5)
 * collecting results from workers; 6) writing results to files.
 * 
 * This class can be run as Java application.
 */
public class Server implements MessageHandler, Runnable, SimulationProcessor {
	ArrayList<WorkerMeta> workerMetas = new ArrayList<>();
	public boolean isSimulating = false;//Whether simulation is running, i.e., it is not paused or stopped
	private boolean isOpenForNewWorkers = true;
	private ArrayList<Message_WS_TrafficReport> receivedTrafficReportCache = new ArrayList<>();
	private SimServerData data;
	private Settings settings;
	private ExternalSimulationListener extListner;
	private int writeOutputStepInServerlessMode = 1;

	public Server(boolean isVisualize){
		this.settings = new Settings();
		this.settings.isVisualize = isVisualize;
		data = new SimServerData(settings);
		extListner = settings.getExternalSimulationListener();
		//extListner.init();
	}

	public static void main(final String[] args) {
		boolean isVisualize = true;
		boolean error = false;
		try {
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
					case "-gui":
						isVisualize =  Boolean.parseBoolean(args[i + 1]);
				}
			}
		} catch (final Exception e) {
			error = true;
		}
		if (!error) {
			new Server(isVisualize).run();
		} else {
			System.out.println("There is an error in command line parameter. Program exits.");
		}
	}

	/**
	 * Adds a new worker unless simulation is running or the required number of
	 * workers is reached.
	 */
	void addWorker(final Message_WS_Join received) {
		final WorkerMeta worker = new WorkerMeta(received.workerName, received.workerAddress, received.workerPort);
		if (isAllWorkersAtState(WorkerState.NEW) && settings.numWorkers > workerMetas.size()) {
			workerMetas.add(worker);
			data.showNumberOfConnectedWorkers(workerMetas.size());
			if (workerMetas.size() == settings.numWorkers) {
				isOpenForNewWorkers = false;// No need for more workers
				data.prepareForSetup();
			}
		} else {
			final Message_SW_KillWorker msd = new Message_SW_KillWorker(settings.isSharedJVM);
			worker.send(msd);
		}
	}

	/*
	 * Start a server-less simulation.
	 */
	void askWorkersProceedWithoutServer() {
		data.takeTimeStamp();
		final Message_SW_Serverless_Start message = new Message_SW_Serverless_Start(data.getStep());
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
		data.setStep(data.getStep() + 1);
		data.takeTimeStamp();

		System.out.println("Doing step " + data.getStep());

		// Ask all workers to share data with fellow
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.SHARING_STARTED);
		}
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_ServerBased_ShareTraffic(data.getStep()));
		}
	}

	/*
	 * Ask workers to update traffic in their corresponding work areas for one
	 * time step. This is called after workers exchange traffic information with
	 * their neighbors in server-based simulation.
	 */
	synchronized void askWorkersSimulateOneStep() {
		boolean[] req = data.updateVehicleCounts();
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.SIMULATING);
		}
		final Message_SW_ServerBased_Simulate message = new Message_SW_ServerBased_Simulate(req[0],
				req[1], req[2], UUID.randomUUID().toString());
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
		}
	}

	public void changeMap() {
		data.changeMap();
		//if (settings.isExternalListenerUsed) {
		//	extListner.getRoadGraph(data.getRoadNetwork());
		//}
	}

	@Override
	public void onClose() {
		killConnectedWorkers();
	}

	/**
	 * Change simulation speed by setting pause time after doing a step at all
	 * workers. Note that this will affect simulation time. By default there is
	 * no pause time between steps.
	 */
	public void changeSpeed(final int pauseTimeEachStep) {
		settings.pauseTimeBetweenStepsInMilliseconds = pauseTimeEachStep;
		final Message_SW_ChangeSpeed message = new Message_SW_ChangeSpeed(settings.pauseTimeBetweenStepsInMilliseconds);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
		}
	}

	boolean isAllWorkersAtState(final WorkerState state) {
		int count = 0;
		for (final WorkerMeta w : workerMetas) {
			if (w.state == state) {
				count++;
			}
		}

		return count == workerMetas.size();

	}

	public void killConnectedWorkers() {
		final Message_SW_KillWorker msd = new Message_SW_KillWorker(settings.isSharedJVM);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(msd);
		}
		workerMetas.clear();
		settings.isNewEnvironment = true;
	}

	public void pauseSim() {
		isSimulating = false;
		if (!settings.isServerBased) {
			final Message_SW_Serverless_Pause message = new Message_SW_Serverless_Pause(data.getStep());
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
			onWSJoinMessage((Message_WS_Join) message);
		} else if (message instanceof Message_WS_SetupCreatingVehicles) {
			onWSSetupCreatingVehiclesMsg((Message_WS_SetupCreatingVehicles) message);
		} else if (message instanceof Message_WS_SetupDone) {
			onWSSetupDoneMsg((Message_WS_SetupDone) message);
		} else if (message instanceof Message_WS_ServerBased_SharedMyTrafficWithNeighbor) {
			onWSServerBasedShareMyTrafficWithNeighbourMsg((Message_WS_ServerBased_SharedMyTrafficWithNeighbor) message);
			onWSServerBasedShareMyTrafficWithNeighbourMsg((Message_WS_ServerBased_SharedMyTrafficWithNeighbor) message);
		} else if (message instanceof Message_WS_TrafficReport) {
			onWSTrafficReportMsg((Message_WS_TrafficReport) message);
		} else if (message instanceof Message_WS_Serverless_Complete) {
			onWSServerlessCompleteMsg((Message_WS_Serverless_Complete) message);
		}
	}

	synchronized void processCachedReceivedTrafficReports() {
		final Iterator<Message_WS_TrafficReport> iMessage = receivedTrafficReportCache.iterator();
		while (iMessage.hasNext()) {
			final Message_WS_TrafficReport message = iMessage.next();
			data.updateFromReport(message.vehicleList, message.lightList, message.workerName, workerMetas.size(),
					message.step, message.randomRoutes, message.finishedList,message.numInternalNonPubVehicles, message.numInternalTrams,
					message.numInternalBuses, message.laneIndexes, message.edgesToUpdate);
			// Remove processed message
			iMessage.remove();
		}
	}

	@Override
	public void run() {
		data.initRoadNetwork();
		changeMap();
		data.initUIs(this);
		// Prepare to receive connection request from workers
		new IncomingConnectionBuilder(settings.serverListeningPortForWorkers, this).start();
	}

	public void resumeSim() {
		isSimulating = true;
		if (settings.isServerBased) {
			System.out.println("Resuming server-based simulation...");
			askWorkersShareTrafficDataWithFellowWorkers();
		} else {
			System.out.println("Resuming server-less simulation...");
			final Message_SW_Serverless_Resume message = new Message_SW_Serverless_Resume(data.getStep());
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
			}
		}
	}

	/**
	 * Update node lists for nodes where traffic light needs to be added or
	 * removed. The lists will be sent to worker during setup.
	 */
	public void setLightChangeNode(final Node node) {
		data.setLightChangeNode(node);
	}

	/**
	 * Resets dynamic fields and sends simulation configuration to workers. The
	 * workers will set up simulation environment upon receiving the
	 * configuration.
	 */
	public void setupNewSim() {
		data.resetVariablesForSetup();
		receivedTrafficReportCache.clear();

		if (settings.isVisualize == false) data.loadSettingsFromScript();
		//data.initRoadNetwork();
		//data.updateSettings();
		// In a new environment (map), determine the work areas for all workers
		if (settings.isNewEnvironment) {
				changeMap();
		}
		data.initFileOutput();
		assignODWindows();
		final RouteLoader routeLoader = new RouteLoader(data.getRoadNetwork());
		List<SerializableExternalVehicle> vehicleList = routeLoader.loadRoutes(settings.inputForegroundVehicleFile, settings.inputBackgroundVehicleFile);
		data.updateNoOfVehiclesNeededAtStart(routeLoader.vehicles.size());


		WorkloadBalancer workloadBalancer = new WorkloadBalancer(workerMetas, data.getRoadNetwork());
		workloadBalancer.balanceLoad(settings, data.getStep(), vehicleList, data.getNodesToAddLight(), data.getNodesToRemoveLight());


		settings.isNewEnvironment = false;
	}

	private void assignODWindows(){
		ODDistributor odDistributor = settings.getODDistributor();
		settings.listRouteSourceWindowForInternalVehicle = odDistributor.getSourceWidows(getRoadNetwork());
		settings.listRouteDestinationWindowForInternalVehicle = odDistributor.getDestinationWidows(getRoadNetwork());
		settings.listRouteSourceDestinationWindowForInternalVehicle = odDistributor.getSourceDestinationWidows(getRoadNetwork());
	}

	@Override
	public void setupMultipleSim() {
		data.startMultipleExperimentRunning();
	}

	@Override
	public boolean loadScript() {
		return data.loadScript();
	}

	@Override
	public Settings getSettings() {
		return settings;
	}

	public void askWorkersChangeLaneBlock(int laneIndex, boolean isBlocked) {
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_BlockLane(laneIndex, isBlocked));
		}
	}

	public void askWorkersChangeLaneDirection(int edgeIndex) {

		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_ChangeLaneDirection(edgeIndex));
		}
	}

	void startSimulation() {
		if (data.getStep() < settings.maxNumSteps) {
			System.out.println("All workers are ready to do simulation.");
			isSimulating = true;
			data.startSimulationInUI();
			if (settings.isServerBased) {
				System.out.println("Starting server-based simulation...");
				askWorkersShareTrafficDataWithFellowWorkers();
			} else {
				System.out.println("Starting server-less simulation...");
				askWorkersProceedWithoutServer();
			}
		}

	}

	@Override
	public RoadNetwork getRoadNetwork() {
		return data.getRoadNetwork();
	}

	public void stopSim() {
		isSimulating = false;
		data.reserVariablesAtStop();
		processCachedReceivedTrafficReports();
		data.writeOutputFiles(data.getStep());

		// Ask workers stop in server-less mode. Note that workers may already stopped before receiving the message.
		if (!settings.isServerBased) {
			final Message_SW_Serverless_Stop message = new Message_SW_Serverless_Stop(data.getStep());
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
				worker.setState(WorkerState.NEW);
			}
		}

		System.out.println("Simulation stopped.\n");
		//if (workerMetas.size() == settings.numWorkers) {
		//	data.prepareForSetupAgain();
		//}
	}



	synchronized void updateWorkerState(final String workerName, final WorkerState state) {
		for (final WorkerMeta worker : workerMetas) {
			if (worker.name.equals(workerName)) {
				worker.state = state;
				break;
			}
		}
	}

	private void onWSJoinMessage(Message_WS_Join msg){
		if (isOpenForNewWorkers)
			addWorker(msg);
	}

	private void onWSSetupCreatingVehiclesMsg(Message_WS_SetupCreatingVehicles msg){
		data.updateSetupprogressInUI((msg).numVehicles);
	}

	private void onWSSetupDoneMsg(Message_WS_SetupDone msg){
		updateWorkerState((msg).workerName, WorkerState.READY);
		data.updateChannelCount((msg).numFellowWorkers);
		if (isAllWorkersAtState(WorkerState.READY)) {
			data.resetStepInUI();
			startSimulation();
		}
	}

	private void onWSServerBasedShareMyTrafficWithNeighbourMsg(Message_WS_ServerBased_SharedMyTrafficWithNeighbor msg){
		if (!isSimulating) {
			// No need to process the message if simulation was stopped
			return;
		}
		for (final WorkerMeta w : workerMetas) {
			if (w.name.equals(msg.workerName) && (w.state == WorkerState.SHARING_STARTED)) {
				updateWorkerState(msg.workerName, WorkerState.SHARED);
				if (isAllWorkersAtState(WorkerState.SHARED)) {

					//if (data.getStep()%18000 == 0 & data.getStep() > 0){
					//	data.startReLogging();
					//}
					askWorkersSimulateOneStep();
				}
				break;
			}
		}
	}

	private void onWSTrafficReportMsg(Message_WS_TrafficReport msg){
		if (!isSimulating) {
			// No need to process the message if simulation was stopped
			return;
		}

		// process and send the data to external


		// Cache received reports
		receivedTrafficReportCache.add(msg);
		// Output data from the reports
		processCachedReceivedTrafficReports();
		// Stop if max number of steps is reached in server-based mode
		if (settings.isServerBased) {
			updateWorkerState(msg.workerName, WorkerState.FINISHED_ONE_STEP);
			if (isAllWorkersAtState(WorkerState.FINISHED_ONE_STEP)) {
				data.updateSimulationTime();
				if (data.isSimulationStopReached()) {
					stopSim();
				} else if (isSimulating) {
					askWorkersShareTrafficDataWithFellowWorkers();
				}
			}
		} else {
			// Update time step in server-less mode
			if (msg.step > data.getStep()) {
				data.setStep(msg.step);
				//if ( data.getStep()/18000 > writeOutputStepInServerlessMode ){
				//	System.out.println("Start re-write logs.. ");
				//	writeOutputStepInServerlessMode++;
				//	data.startReLogging();
				//	System.out.println("Logging done");
				//}
			}
		}
	}

	private void onWSServerlessCompleteMsg(Message_WS_Serverless_Complete msg){
		if (!isSimulating) {
			// No need to process the message if simulation was stopped
			return;
		}
		if (msg.step > data.getStep()) {
			data.setStep(msg.step);
		}
		updateWorkerState(msg.workerName, WorkerState.NEW);
		if (isAllWorkersAtState(WorkerState.NEW)) {
			data.updateSimulationTime();
			stopSim();
		}
	}

	private void sendTrafficReportToListner(Message_WS_TrafficReport trafficReport){
		extListner.getMessage(trafficReport);
	}


}

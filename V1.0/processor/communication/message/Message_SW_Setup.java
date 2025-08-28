package processor.communication.message;

import java.util.ArrayList;
import java.util.List;

import common.Settings;
import processor.server.WorkerMeta;
import traffic.light.LightUtil;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.routing.RouteUtil;

/**
 * Server-to-worker message containing the simulation configuration. Worker will
 * set up simulation environment upon receiving this message.
 */
public class Message_SW_Setup {
	public boolean isNewEnvironment;
	public int numWorkers;
	public int startStep;
	public int maxNumSteps;
	public double numStepsPerSecond;
	public int workerToServerReportStepGapInServerlessMode;
	public double periodOfTrafficWaitForTramAtStop;
	public ArrayList<SerializableDouble> driverProfileDistribution = new ArrayList<>();
	public double lookAheadDistance;
	public String trafficLightTiming;
	public boolean isVisualize;
	public int numRandomPrivateVehicles;
	public int numRandomTrams;
	public int numRandomBuses;
	public ArrayList<SerializableExternalVehicle> externalRoutes = new ArrayList<>();
	/**
	 * Metadata of all workers
	 */
	public ArrayList<SerializableWorkerMetadata> metadataWorkers = new ArrayList<>();
	public boolean isServerBased;
	public String roadGraph;
	public String routingAlgorithm;
	public ArrayList<SerializableInt> lightNodes = new ArrayList<>();
	public boolean isAllowPriorityVehicleUseTramTrack;
	public boolean isOutputForegroundTrajectory = false;
	public boolean isOutputInternalBackgroundRoutePlan = false;
	public ArrayList<Serializable_GPS_Rectangle> listRouteSourceWindowForInternalVehicle = new ArrayList<>();
	public ArrayList<Serializable_GPS_Rectangle> listRouteDestinationWindowForInternalVehicle = new ArrayList<>();
	public ArrayList<Serializable_GPS_Rectangle> listRouteSourceDestinationWindowForInternalVehicle = new ArrayList<>();
	public boolean isAllowReroute = false;
	public boolean isAllowTramRule = true;
	public boolean isDriveOnLeft;

	public Message_SW_Setup() {

	}

	public Message_SW_Setup(Settings settings,
							List<WorkerMeta> workers,
							WorkerMeta workerToReceiveMessage,
							List<Edge> edges,
							int step) {
		isNewEnvironment = settings.isNewEnvironment;
		numWorkers = settings.numWorkers;
		startStep = step;
		maxNumSteps = settings.maxNumSteps;
		numStepsPerSecond = settings.numStepsPerSecond;
		workerToServerReportStepGapInServerlessMode = settings.trafficReportStepGapInServerlessMode;
		periodOfTrafficWaitForTramAtStop = settings.periodOfTrafficWaitForTramAtStop;
		driverProfileDistribution = getDriverProfilePercentage(settings.driverProfileDistribution);
		lookAheadDistance = settings.lookAheadDistance;
		trafficLightTiming = settings.trafficLightTiming.name();
		isVisualize = settings.isVisualize;
		metadataWorkers = appendMetadataOfWorkers(workers);
		numRandomPrivateVehicles = workerToReceiveMessage.numRandomPrivateVehicles;
		numRandomTrams = workerToReceiveMessage.numRandomTrams;
		numRandomBuses = workerToReceiveMessage.numRandomBuses;
		externalRoutes = workerToReceiveMessage.externalRoutes;
		isServerBased = settings.isServerBased;
		if (settings.isNewEnvironment) {
			if (settings.isBuiltinRoadGraph) {
				roadGraph = "builtin";
			} else {
				roadGraph = settings.roadGraph;
			}
		} else {
			roadGraph = "";
		}
		routingAlgorithm = settings.routingAlgorithm.name();
		isAllowPriorityVehicleUseTramTrack = settings.isAllowPriorityVehicleUseTramTrack;
		this.lightNodes = getLightNodeIndex(workerToReceiveMessage.lightNodes);
		isOutputForegroundTrajectory = settings.isOutputTrajectory;
		isOutputInternalBackgroundRoutePlan = settings.isOutputInitialRoutes;
		listRouteSourceWindowForInternalVehicle = getListRouteWindow(settings.listRouteSourceWindowForInternalVehicle);
		listRouteDestinationWindowForInternalVehicle = getListRouteWindow(
				settings.listRouteDestinationWindowForInternalVehicle);
		listRouteSourceDestinationWindowForInternalVehicle = getListRouteWindow(
				settings.listRouteSourceDestinationWindowForInternalVehicle);
		isAllowReroute = settings.isAllowReroute;
		isAllowTramRule = settings.isAllowTramRule;
		isDriveOnLeft = settings.isDriveOnLeft;
	}

	ArrayList<SerializableWorkerMetadata> appendMetadataOfWorkers(List<WorkerMeta> workers) {
		final ArrayList<SerializableWorkerMetadata> listSerializableWorkerMetadata = new ArrayList<>();
		for (final WorkerMeta worker : workers) {
			listSerializableWorkerMetadata.add(new SerializableWorkerMetadata(worker));
		}
		return listSerializableWorkerMetadata;
	}

	ArrayList<SerializableDouble> getDriverProfilePercentage(List<Double> percentages) {
		final ArrayList<SerializableDouble> list = new ArrayList<>();
		for (final Double percentage : percentages) {
			list.add(new SerializableDouble(percentage));
		}
		return list;
	}

	ArrayList<SerializableInt> getLightNodeIndex(List<Node> nodes) {
		final ArrayList<SerializableInt> list = new ArrayList<>();
		for (final Node node : nodes) {
			list.add(new SerializableInt(node.index));
		}
		return list;
	}

	ArrayList<Serializable_GPS_Rectangle> getListRouteWindow(List<double[]> windows) {
		final ArrayList<Serializable_GPS_Rectangle> list = new ArrayList<>();
		for (final double[] window : windows) {
			list.add(new Serializable_GPS_Rectangle(window[0], window[1], window[2], window[3]));
		}
		return list;
	}

	public void setupFromMessage(Settings settings){
		settings.isNewEnvironment = isNewEnvironment;
		settings.numWorkers = numWorkers;
		settings.maxNumSteps = maxNumSteps;
		settings.numStepsPerSecond = numStepsPerSecond;
		settings.trafficReportStepGapInServerlessMode = workerToServerReportStepGapInServerlessMode;
		settings.periodOfTrafficWaitForTramAtStop = periodOfTrafficWaitForTramAtStop;
		settings.driverProfileDistribution = setDriverProfileDistribution(driverProfileDistribution);
		settings.lookAheadDistance = lookAheadDistance;
		settings.trafficLightTiming = LightUtil.getLightTypeFromString(trafficLightTiming);
		settings.isVisualize = isVisualize;
		settings.isServerBased = isServerBased;
		settings.routingAlgorithm = RouteUtil.getRoutingAlgorithmFromString(routingAlgorithm);
		settings.isAllowPriorityVehicleUseTramTrack = isAllowPriorityVehicleUseTramTrack;
		settings.isOutputInitialRoutes = isOutputInternalBackgroundRoutePlan;
		settings.isOutputTrajectory = isOutputForegroundTrajectory;
		settings.listRouteSourceWindowForInternalVehicle = setRouteSourceDestinationWindow(listRouteSourceWindowForInternalVehicle);
		settings.listRouteDestinationWindowForInternalVehicle = setRouteSourceDestinationWindow(listRouteDestinationWindowForInternalVehicle);
		settings.listRouteSourceDestinationWindowForInternalVehicle = setRouteSourceDestinationWindow(listRouteSourceDestinationWindowForInternalVehicle);
		settings.isAllowReroute = isAllowReroute;
		settings.isAllowTramRule = isAllowTramRule;
		settings.isDriveOnLeft = isDriveOnLeft;
	}

	/**
	 * Set the percentage of drivers with different profiles, from highly
	 * aggressive to highly polite.
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
}

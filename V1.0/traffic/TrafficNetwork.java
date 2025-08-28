package traffic;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.*;

import common.Settings;
import processor.SimulationListener;
import processor.communication.externalMessage.VehiclePathExternal;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableRouteLeg;
import processor.communication.message.SerializableWorkerMetadata;
import processor.worker.Workarea;
import traffic.light.LightCoordinator;
import traffic.light.TrafficLightTiming;
import traffic.network.ODDemand;
import traffic.road.*;
import traffic.routing.*;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

/**
 * A trafficNetwork contains vehicles and traffic lights on top of a road
 * network. The main functionalities of this class include initializing traffic
 * environment, creating vehicles, releasing vehicles from parking area and
 * removing vehicles. It also does miscellaneous utilities related to
 * interaction with trams and server-less simulation. Check classes in the
 * sub-folders for performing other tasks related to vehicles and traffic
 * lights.
 */
public class TrafficNetwork extends RoadNetwork {
	/**
	 * Comparator of edges based on their distance to a node.
	 *
	 */
	public class NearbyEdgeComparator implements Comparator<Edge> {
		Node node;

		public NearbyEdgeComparator(final Node node) {
			super();
			this.node = node;
		}

		@Override
		public int compare(final Edge edge1, final Edge edge2) {
			final Line2D.Double line1 = new Line2D.Double(edge1.startNode.lon, edge1.startNode.lat * settings.lonVsLat,
					edge1.endNode.lon, edge1.endNode.lat * settings.lonVsLat);
			final double dist1 = line1.ptSegDist(node.lon, node.lat * settings.lonVsLat);
			final Line2D.Double line2 = new Line2D.Double(edge2.startNode.lon, edge2.startNode.lat * settings.lonVsLat,
					edge2.endNode.lon, edge2.endNode.lat * settings.lonVsLat);
			final double dist2 = line2.ptSegDist(node.lon, node.lat * settings.lonVsLat);
			return dist1 < dist2 ? -1 : dist1 == dist2 ? 0 : 1;
		}
	}



	public Workarea workarea;
	public ArrayList<Vehicle> vehicles = new ArrayList<>();

	// For report data
	public ArrayList<Vehicle> newVehiclesSinceLastReport = new ArrayList<>();
	public ArrayList<Vehicle> newVehiclesforDemandEstimation = new ArrayList<>();
	public HashMap<SerializableExternalVehicle, Double> externalVehicleRepeatPerStep = new HashMap<>();
	ArrayList<Double> driverProfilePercAccumulated = new ArrayList<>();
	ArrayList<Edge> internalTramStopEdges = new ArrayList<>();
	ArrayList<Edge> internalNonPublicVehicleStartEdges = new ArrayList<>();
	ArrayList<Edge> internalNonPublicVehicleEndEdges = new ArrayList<>();
	ArrayList<Edge> internalBusStartEdges = new ArrayList<>();
	ArrayList<Edge> internalBusEndEdges = new ArrayList<>();
	ArrayList<Edge> internalTramStartEdges = new ArrayList<>();
	ArrayList<Edge> internalTramEndEdges = new ArrayList<>();
	public ArrayList<Integer> laneIndexOfChangeDir = new ArrayList<>();
	public Routing routingAlgorithm;
	public RoutingAlgoFactory routingAlgoFactory;


	Random random = new Random();
	int numInternalVehicleAllTime = 0;
	public int numInternalNonPublicVehicle = 0;
	public int numInternalTram = 0;
	public int numInternalBus = 0;
	public LightCoordinator lightCoordinator = new LightCoordinator();
	String internalVehiclePrefix = "";
	double timeLastPublicVehicleCreated = 0;
	ArrayList<String> internalTramRefInSdWindow = new ArrayList<>();

	HashMap<String, ArrayList<Edge>> internalTramStartEdgesInSourceWindow = new HashMap<>();
	HashMap<String, ArrayList<Edge>> internalTramEndEdgesInDestinationWindow = new HashMap<>();
	ArrayList<String> internalBusRefInSourceDestinationWindow = new ArrayList<>();

	HashMap<String, ArrayList<Edge>> internalBusStartEdgesInSourceWindow = new HashMap<>();

	HashMap<String, ArrayList<Edge>> internalBusEndEdgesInDestinationWindow = new HashMap<>();
	List<Vehicle> finishedVehicles = new ArrayList<>();

	private PriorityQueue<Vehicle> tripMakingVehicles;

	private ArrayList<VehiclePathExternal> paths = new ArrayList<>();

	/**
	 * Initialize traffic network.
	 */
	public TrafficNetwork(Settings settings, String name, List<SerializableWorkerMetadata> metadataWorkers) {
		super(settings);
		this.internalVehiclePrefix = name;
		setupWorkArea(name, metadataWorkers);
		identifyInternalTramStopEdges();
		addTramStopsToParallelNonTramEdges();
		tripMakingVehicles = new PriorityQueue<>(getTripMakingVehicleComparator());
		setCrossingIncreasingOrders();
		routingAlgoFactory = new RoutingAlgoFactory();

	}

	private void setupWorkArea(String name, List<SerializableWorkerMetadata> metadataWorkers){
		workarea = new Workarea(name, null);
		for (final SerializableWorkerMetadata metadata : metadataWorkers) {
			final ArrayList<GridCell> cellsInWorkarea = metadata.processReceivedGridCells(grid);
			if (metadata.name.equals(name)) {
				workarea.setWorkCells(cellsInWorkarea);
				break;
			}
		}
	}

	public void clearReportedData() {
		newVehiclesSinceLastReport.clear();
		finishedVehicles.clear();
		laneIndexOfChangeDir.clear();
	}

	/**
	 * Create a new vehicle object and add it to the pool of vehicles.
	 *
	 */
	void addNewVehicle(final VehicleType type, final boolean isExternal, final boolean foreground, Node start, Node end,
			List<RouteLeg> routeLegs, final String idPrefix, final double timeRouteStart,
			final String externalId, final int vid, final DriverProfile dP) {
		// Do not proceed if the route is empty
		/*if (routeLegs.size() < 1) {
			return;
		}*///TODO if the start and end is not reachable it should be handled
		// Create new vehicle
		final Vehicle vehicle = new Vehicle(settings);
		// Creation time
		vehicle.timeRouteStart = timeRouteStart;
		// Jam start time
		vehicle.timeJamStart = vehicle.timeRouteStart;
		// Type
		vehicle.type = type;
		// Is vehicle of particular interest?
		vehicle.isForeground = foreground;
		// External or internal?
		vehicle.isExternal = isExternal;
		// Length of vehicle
		vehicle.length = type.length;
		vehicle.setStart(start);
		vehicle.setEnd(end);
		// Legs of route
		vehicle.setRouteLegs(routeLegs);
		// Driver profile
		vehicle.driverProfile = dP;
		// Set as active
		vehicle.active = true;
		// Add vehicle to system

		if (Math.random() <= settings.cavPercentage)
			vehicle.setCAV(true);
		else {
			vehicle.setConnectedV(true);
			vehicle.setCAV(false);
		}

		vehicle.setHeadWayMultiplier(settings.safetyHeadwayMultiplier);
		if (!vehicle.isExternal) {
			// Update vehicle counters				
			numInternalVehicleAllTime++;
			if (type == VehicleType.TRAM) {
				numInternalTram++;
			} else if (type == VehicleType.BUS) {
				numInternalBus++;
			} else {
				numInternalNonPublicVehicle++;
			}
			// Assign vehicle ID
			vehicle.id = idPrefix + Long.toString(numInternalVehicleAllTime);
			vehicle.vid = numInternalVehicleAllTime;
			// Add vehicle to system
			vehicles.add(vehicle);
			tripMakingVehicles.add(vehicle);
			//vehicle.park(true, timeRouteStart);
		} else {
			// Add external vehicle to system
			vehicle.id = externalId;
			vehicle.vid = vid;
			vehicles.add(vehicle);
			tripMakingVehicles.add(vehicle);
			//vehicle.park(true, timeRouteStart);
		}
	}

	/**
	 * Add an existing vehicle object to traffic network. The vehicle object is
	 * transferred from a neighbor worker.
	 */
	public void addOneTransferredVehicle(final Vehicle vehicle, final double timeNow) {
		vehicle.active = true;
		vehicles.add(vehicle);
		vehicle.lane.addVehicleToLane(vehicle);
		if (!vehicle.isExternal) {
			if (vehicle.type == VehicleType.TRAM) {
				numInternalTram++;
			} else if (vehicle.type == VehicleType.BUS) {
				numInternalBus++;
			} else {
				numInternalNonPublicVehicle++;
			}
		}
		if (vehicle.getCurrentLeg().stopover > 0) {
			vehicle.park(false, timeNow);
		}
	}

	/**
	 * Add tram stop information to roads that are parallel to tram tracks. This
	 * should be done if the tram tracks in map data are separated from normal
	 * roads.
	 */
	void addTramStopsToParallelNonTramEdges() {
		ArrayList<Edge> candidateEdges = new ArrayList<>();
		for (final Edge tramEdge : internalTramStopEdges) {
			candidateEdges = getEdgesParallelToEdge(tramEdge.startNode.lat, tramEdge.startNode.lon,
					tramEdge.endNode.lat, tramEdge.endNode.lon);
			final NearbyEdgeComparator parallelEdgeComparator = new NearbyEdgeComparator(tramEdge.endNode);
			Collections.sort(candidateEdges, parallelEdgeComparator);
			// Foot of perpendicular from tram edge must be within the candidate edge
			for (final Edge candidateEdge : candidateEdges) {
				if (candidateEdge.type == RoadType.tram) {
					continue;
				}

				final Line2D.Double line = new Line2D.Double(candidateEdge.startNode.lon,
						candidateEdge.startNode.lat * settings.lonVsLat, candidateEdge.endNode.lon,
						candidateEdge.endNode.lat * settings.lonVsLat);
				final double distSq_StopToLine = line.ptSegDistSq(tramEdge.endNode.lon,
						tramEdge.endNode.lat * settings.lonVsLat);
				final double distSq_StopToLineStart = Point2D.distanceSq(tramEdge.endNode.lon,
						tramEdge.endNode.lat * settings.lonVsLat, candidateEdge.startNode.lon,
						candidateEdge.startNode.lat * settings.lonVsLat);
				final double dist_StartToClosestPoint = Math.sqrt(distSq_StopToLineStart - distSq_StopToLine);
				final double dist_StartToEnd = Point2D.distance(candidateEdge.startNode.lon,
						candidateEdge.startNode.lat * settings.lonVsLat, candidateEdge.endNode.lon,
						candidateEdge.endNode.lat * settings.lonVsLat);
				final double ratio = dist_StartToClosestPoint / dist_StartToEnd;
				if ((ratio >= 0) && (ratio <= 1)) {
					candidateEdge.distToTramStop = candidateEdge.length * ratio;
					candidateEdge.parallelTramEdgeWithTramStop = tramEdge;
					break;
				}
			}

		}
	}

	/**
	 * Initialize traffic network in work area and certain global settings.
	 */
	public void buildEnvironment() {
		identifyInternalVehicleRouteStartEndEdges();
		identifyReferencesOfAllPublicTransportTypesInSourceDestinationWindow();
		computeAccumulatedDriverProfileDistribution();

		routingAlgorithm = routingAlgoFactory.getRoutingAlgo(settings.routingAlgorithm, this);

	}

	void computeAccumulatedDriverProfileDistribution() {
		driverProfilePercAccumulated = new ArrayList<>();
		double total = 0;
		for (final Double d : settings.driverProfileDistribution) {
			total += d;
		}
		double accumulated = 0;
		for (final Double d : settings.driverProfileDistribution) {
			accumulated += d;
			driverProfilePercAccumulated.add(accumulated / total);
		}
	}

	/**
	 * Create vehicles based on external routes imported during setup.
	 *
	 */
	public void createExternalVehicles(final ArrayList<SerializableExternalVehicle> externalRoutes,
			final double timeNow) {
		for (final SerializableExternalVehicle vehicle : externalRoutes) {
			final VehicleType type = VehicleType.getVehicleTypeFromName(vehicle.vehicleType);
			if (vehicle.numberRepeatPerSecond <= 0) {
				List<RouteLeg> routeLegs = createOneRouteFromSerializedData(vehicle.route, type);
				addNewVehicle(type, true, vehicle.foreground, routeLegs.get(0).edge.startNode,
						routeLegs.get(routeLegs.size()-1).edge.endNode, routeLegs , "",
						vehicle.startTime, vehicle.id, vehicle.vid, DriverProfile.valueOf(vehicle.driverProfile));
			} else {
				// This is a simple way to get number of vehicles per step. The result number may be inaccurate.
				final double numRepeatPerStep = vehicle.numberRepeatPerSecond / settings.numStepsPerSecond;
				externalVehicleRepeatPerStep.put(vehicle, numRepeatPerStep);
			}
		}
	}

	/**
	 * Create non-public vehicle.
	 * 
	 * @param isNewNonPubVehiclesAllowed
	 */
	void createInternalNonPublicVehicles(int numLocalRandomPrivateVehicles, final double timeNow,
			boolean isNewNonPubVehiclesAllowed) {
		if (isNewNonPubVehiclesAllowed) {

			ArrayList<ODDemand> traffic = null;
			try {
				traffic = settings.getTrafficGenerator().getGeneratedTraffic(this,
						internalNonPublicVehicleStartEdges, internalNonPublicVehicleEndEdges, (int) (timeNow*settings.numStepsPerSecond));
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (ODDemand odpair : traffic){
				final int numVehiclesNeeded = odpair.getNumVehicles();
				for (int i = 0; i < numVehiclesNeeded; i++) { ;

					addNewVehicle(odpair.getVehicleType(), false, false, odpair.getOrigin(), odpair.getDestination(), null, internalVehiclePrefix, timeNow, "", -1,
							DriverProfile.NORMAL);
				}
				}
			}

	}

	/**
	 * Create public transport vehicle.
	 * 
	 * @param numLocalRandomBuses
	 * @param numLocalRandomTrams
	 * 
	 * @param isNewBusesAllowed
	 * @param isNewTramsAllowed
	 */
	void createInternalPublicVehicles(int numLocalRandomTrams, int numLocalRandomBuses, boolean isNewTramsAllowed,
			boolean isNewBusesAllowed, final double timeNow) {
		if (isNewTramsAllowed) {
			int numTramsNeeded = numLocalRandomTrams - numInternalTram;
			for (int i = 0; i < numTramsNeeded; i++) {
				createOneInternalPublicVehicle(VehicleType.TRAM, timeNow);
			}
		}
		if (isNewBusesAllowed) {
			int numBusesNeeded = numLocalRandomBuses - numInternalBus;
			for (int i = 0; i < numBusesNeeded; i++) {
				createOneInternalPublicVehicle(VehicleType.BUS, timeNow);
			}
		}

	}

	/**
	 * Create random vehicles.
	 * 
	 * @param numLocalRandomBuses
	 * @param numLocalRandomTrams
	 * 
	 * @param isNewBusesAllowed
	 * @param isNewTramsAllowed
	 * @param isNewNonPubVehiclesAllowed
	 */
	public void createInternalVehicles(int numLocalRandomPrivateVehicles, int numLocalRandomTrams,
			int numLocalRandomBuses, boolean isNewNonPubVehiclesAllowed, boolean isNewTramsAllowed,
			boolean isNewBusesAllowed, final double timeNow) {
		if ((internalNonPublicVehicleStartEdges.size() > 0) && (internalNonPublicVehicleEndEdges.size() > 0) && ( timeNow%settings.demandGenerationTimeInterval == 0 )) { //TODO: shift to 90
			createInternalNonPublicVehicles(numLocalRandomPrivateVehicles, timeNow, isNewNonPubVehiclesAllowed);
		}
		//createInternalPublicVehicles(numLocalRandomTrams, numLocalRandomBuses, isNewTramsAllowed, isNewBusesAllowed,
		//		timeNow);
	}

	void createOneInternalPublicVehicle(final VehicleType type, final double timeNow) {
		VehicleType transport = null;
		String randomRef = null;
		ArrayList<Edge> startEdgesOfRandomRoute = null;
		ArrayList<Edge> endEdgesOfRandomRoute = null;
		if ((type == VehicleType.TRAM) && (internalTramRefInSdWindow.size() > 0)) {
			transport = VehicleType.TRAM;
			randomRef = internalTramRefInSdWindow.get(random.nextInt(internalTramRefInSdWindow.size()));
			startEdgesOfRandomRoute = internalTramStartEdgesInSourceWindow.get(randomRef);
			endEdgesOfRandomRoute = internalTramEndEdgesInDestinationWindow.get(randomRef);
		} else if ((type == VehicleType.BUS) && (internalBusRefInSourceDestinationWindow.size() > 0)) {
			transport = VehicleType.BUS;
			randomRef = internalBusRefInSourceDestinationWindow
					.get(random.nextInt(internalBusRefInSourceDestinationWindow.size()));
			startEdgesOfRandomRoute = internalBusStartEdgesInSourceWindow.get(randomRef);
			endEdgesOfRandomRoute = internalBusEndEdgesInDestinationWindow.get(randomRef);
		}

		if ((startEdgesOfRandomRoute == null) || (endEdgesOfRandomRoute == null)) {
			return;
		}

		ArrayList<RouteLeg> route = ReferenceBasedSearch.createRoute(transport, randomRef, startEdgesOfRandomRoute,
				endEdgesOfRandomRoute);
		for (int numTry = 0; numTry < 10; numTry++) {
			if (route == null) {
				route = ReferenceBasedSearch.createRoute(transport, randomRef, startEdgesOfRandomRoute,
						endEdgesOfRandomRoute);
			} else {
				break;
			}
		}
		Node start = route.get(0).edge.startNode;
		Node end = route.get(route.size()-1).edge.endNode;
		addNewVehicle(type, false, false, start, end, route, internalVehiclePrefix, timeNow, "", -1, getRandomDriverProfile());
	}

	/**
	 * Generate a route.
	 * @param type
	 * @return
	 */
	/*ArrayList<RouteLeg> createOneRandomInternalRoute(final VehicleType type) {


		final ArrayList<RouteLeg> route = routingAlgorithm.createCompleteRoute(edges[0], edges[1], type);

		if ((route == null) || (route.size() == 0)) {
			return null;
		} else {
			return route;
		}
	}*/

	ArrayList<RouteLeg> createOneRouteFromSerializedData(final ArrayList<SerializableRouteLeg> serializedData, VehicleType type) {
		if(settings.inputOnlyODPairsOfForegroundVehicleFile){
			SerializableRouteLeg legStart = serializedData.get(0);
			SerializableRouteLeg legEnd = serializedData.get(serializedData.size()-1);
			return routingAlgorithm.createCompleteRoute(edges.get(legStart.edgeIndex).startNode, edges.get(legEnd.edgeIndex).endNode, type);
		}else {
			final ArrayList<RouteLeg> route = new ArrayList<>(1000);
			for (final SerializableRouteLeg sLeg : serializedData) {
				final RouteLeg leg = new RouteLeg(edges.get(sLeg.edgeIndex), sLeg.stopover);
				route.add(leg);
			}
			return route;
		}
	}

	DriverProfile getRandomDriverProfile() {
		final double r = random.nextDouble();
		for (int i = 0; i < DriverProfile.values().length; i++) {
			if (r < driverProfilePercAccumulated.get(i)) {
				return DriverProfile.values()[i];
			}
		}
		return DriverProfile.NORMAL;
	}



	/**
	 * Collect the edges that are used by tram and whose end points are tram
	 * stops.
	 */
	void identifyInternalTramStopEdges() {
		internalTramStopEdges = new ArrayList<>(3000);
		for (final Edge edge : edges) {
			if (edge.endNode.tramStop) {
				internalTramStopEdges.add(edge);
			}
		}
	}

	/**
	 * Collect the edges that can be used as the start/end leg of non-public
	 * vehicle routes.
	 * 
	 */
	public void identifyInternalVehicleRouteStartEndEdges() {
		internalNonPublicVehicleStartEdges.clear();
		internalNonPublicVehicleEndEdges.clear();
		internalBusStartEdges.clear();
		internalBusEndEdges.clear();
		internalTramStartEdges.clear();
		internalTramEndEdges.clear();

		// Start edges within workarea
		for (final GridCell cell : workarea.workCells) {
			for (final Node node : cell.nodes) {
				if (((settings.listRouteSourceWindowForInternalVehicle.size() == 0)
						&& (settings.listRouteSourceDestinationWindowForInternalVehicle.size() == 0))
						|| ((settings.listRouteSourceWindowForInternalVehicle.size() > 0)
								&& isNodeInsideRectangle(node, settings.listRouteSourceWindowForInternalVehicle))
						|| ((settings.listRouteSourceDestinationWindowForInternalVehicle.size() > 0)
								&& isNodeInsideRectangle(node,
										settings.listRouteSourceDestinationWindowForInternalVehicle))) {
					for (final Edge edge : node.outwardEdges) {
						if (edge.isSuitableForRouteStartOfInternalVehicle(workarea.workCells, settings.minLengthOfRouteStartEndEdge)) {
							if (edge.type != RoadType.tram) {
								internalNonPublicVehicleStartEdges.add(edge);
								if (edge.busRoutesRef.size() > 0) {
									internalBusStartEdges.add(edge);
								}
							} else {
								if (edge.tramRoutesRef.size() > 0) {
									internalTramStartEdges.add(edge);
								}
							}
						}
					}
				}
			}
		}

		// End edges (can be anywhere in road network)
		for (final Edge edge : edges) {
			if (((settings.listRouteDestinationWindowForInternalVehicle.size() == 0)
					&& (settings.listRouteSourceDestinationWindowForInternalVehicle.size() == 0))
					|| ((settings.listRouteDestinationWindowForInternalVehicle.size() > 0) && isNodeInsideRectangle(
							edge.endNode, settings.listRouteDestinationWindowForInternalVehicle))
					|| ((settings.listRouteSourceDestinationWindowForInternalVehicle.size() > 0)
							&& isNodeInsideRectangle(edge.endNode,
									settings.listRouteSourceDestinationWindowForInternalVehicle))) {
				if (edge.isSuitableForRouteEndOfInternalVehicle(settings.minLengthOfRouteStartEndEdge)) {
					if (edge.type != RoadType.tram) {
						internalNonPublicVehicleEndEdges.add(edge);
						if (edge.busRoutesRef.size() > 0) {
							internalBusEndEdges.add(edge);
						}
					} else {
						if (edge.tramRoutesRef.size() > 0) {
							internalTramEndEdges.add(edge);
						}
					}
				}
			}
		}

	}

	/**
	 * Identifying edges for public transport.
	 */
	void identifyReferencesOfAllPublicTransportTypesInSourceDestinationWindow() {
		identifyReferencesOfOnePublicTransportTypeInSourceDestinationWindow(internalTramRefInSdWindow, tramRoutes,
				internalTramStartEdges, internalTramEndEdges, internalTramStartEdgesInSourceWindow,
				internalTramEndEdgesInDestinationWindow);
		identifyReferencesOfOnePublicTransportTypeInSourceDestinationWindow(internalBusRefInSourceDestinationWindow,
				busRoutes, internalBusStartEdges, internalBusEndEdges, internalBusStartEdgesInSourceWindow,
				internalBusEndEdgesInDestinationWindow);
	}

	/**
	 * Collect the edges on tram routes that overlap with this work area.
	 */
	void identifyReferencesOfOnePublicTransportTypeInSourceDestinationWindow(final ArrayList<String> refsInSdWindow,
			final HashMap<String, ArrayList<Edge>> referencedRoutes, final ArrayList<Edge> routeStartEdges,
			final ArrayList<Edge> routeEndEdges, final HashMap<String, ArrayList<Edge>> routeStartEdgesInSourceWindow,
			final HashMap<String, ArrayList<Edge>> routeEndEdgesInDestinationWindow) {
		refsInSdWindow.clear();
		final Iterator it = referencedRoutes.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry pairs = (Map.Entry) it.next();
			final String ref = (String) pairs.getKey();
			final ArrayList<Edge> edgesOnOneRoute = (ArrayList<Edge>) pairs.getValue();
			final ArrayList<Edge> startEdgesOnOneRoute = new ArrayList<>();
			final ArrayList<Edge> endEdgesOnOneRoute = new ArrayList<>();
			boolean isStartCovered = false;
			for (final Edge edge : edgesOnOneRoute) {
				if (routeStartEdges.contains(edge)) {
					isStartCovered = true;
					startEdgesOnOneRoute.add(edge);
				}
			}
			boolean isEndCovered = false;
			for (final Edge edge : edgesOnOneRoute) {
				if (routeEndEdges.contains(edge)) {
					isEndCovered = true;
					endEdgesOnOneRoute.add(edge);
				}
			}
			if (isStartCovered && (startEdgesOnOneRoute.size() > 0) && isEndCovered
					&& (endEdgesOnOneRoute.size() > 0)) {
				refsInSdWindow.add(ref);
				routeStartEdgesInSourceWindow.put(ref, startEdgesOnOneRoute);
				routeEndEdgesInDestinationWindow.put(ref, endEdgesOnOneRoute);
			}
		}
	}





	/**
	 * Remove vehicles from their lanes and the whole traffic network.
	 */
	public void removeActiveVehicles(final ArrayList<Vehicle> vehiclesToBeRemoved) {

		for (final Vehicle v : vehiclesToBeRemoved) {
			// Cancel priority lanes
			v.setPriorityLanes(false);

			// Makes vehicle inactive
			v.active = false;
			/*
			 * Remove vehicles from old lanes
			 */
			if (v.lane != null) {
				v.lane.removeVehicle(v);
			}
			/*
			 * Remove vehicle from the traffic network on this worker
			 */
			vehicles.remove(v);
			/*
			 * Update count of internally generated vehicles
			 */
			if (!v.isExternal) {
				if (v.type == VehicleType.TRAM) {
					numInternalTram--;
				} else if (v.type == VehicleType.BUS) {
					numInternalBus--;
				} else {
					numInternalNonPublicVehicle--;
				}
			}
		}
	}

	public void repeatExternalVehicles(final int step, final double timeNow) {
		for (final SerializableExternalVehicle vehicle : externalVehicleRepeatPerStep.keySet()) {
			double numRepeatThisStep = externalVehicleRepeatPerStep.get(vehicle);
			if (externalVehicleRepeatPerStep.get(vehicle) < 1.0) {
				final int numStepsPerRepeat = (int) (1.0 / numRepeatThisStep);
				if ((step % numStepsPerRepeat) == 0) {
					numRepeatThisStep = 1;
				} else {
					numRepeatThisStep = 0;
				}
			}

			for (int i = 0; i < (int) numRepeatThisStep; i++) {
				final VehicleType type = VehicleType.getVehicleTypeFromName(vehicle.vehicleType);
				final DriverProfile profile = DriverProfile.valueOf(vehicle.driverProfile);
				List<RouteLeg> route = createOneRouteFromSerializedData(vehicle.route, type);
				Node start = route.get(0).edge.startNode;
				Node end = route.get(route.size()-1).edge.endNode;
				addNewVehicle(type, true, vehicle.foreground,start, end, route , "",
						timeNow, vehicle.id + "_time_" + timeNow + "_" + i, vehicle.vid,  profile);
			}
		}
	}

	public void resetTraffic() {
		// Clear vehicles from network
		vehicles.clear();
		externalVehicleRepeatPerStep.clear();
		// Reset temp values for lanes
		for (final Lane lane : lanes) {
			lane.clearVehicles();
			lane.isBlocked = false;
			lane.speedOfLatestVehicleLeftThisWorker = 100;
			lane.endPositionOfLatestVehicleLeftThisWorker = 1000000000;
			lane.isPriority = false;
		}

		// Clear parked vehicles from edges
		for (final Edge edge : edges) {
			edge.clearParkedVehicles();
		}

		// Reset temporary values
		numInternalNonPublicVehicle = 0;
		numInternalTram = 0;
		numInternalBus = 0;
		numInternalVehicleAllTime = 0;
		timeLastPublicVehicleCreated = 0;
	}



	/**
	 * Update the timers related to tram stops.
	 */
	public void updateTramStopTimers() {
		for (int i = 0; i < internalTramStopEdges.size(); i++) {
			final Edge edge = internalTramStopEdges.get(i);
			if (edge.timeTramStopping > 0) {
				edge.timeTramStopping -= 1 / settings.numStepsPerSecond;

				/*
				 * Ensure the tram stop CAN NOT block for some time after the
				 * block timer is gone: the first tram moves on and other
				 * vehicles waiting for it in parallel edges can also move on
				 * without stopping again.
				 */
				if (edge.timeTramStopping <= 0) {
					edge.timeNoTramStopping = settings.minGapBetweenTramStopTimerCountDowns;
				}
			} else if (edge.timeNoTramStopping > 0) {
				edge.timeNoTramStopping -= 1 / settings.numStepsPerSecond;
			}

		}
	}

	public void blockTramAtTramStop() {
		for (int i = 0; i < vehicles.size(); i++) {
			final Vehicle vehicle = vehicles.get(i);
			vehicle.blockAtTramStop();
		}
	}

	public void changeLaneOfVehicles(final double timeNow) {
		for (int i = 0; i < vehicles.size(); i++) {
			final Vehicle vehicle = vehicles.get(i);
			vehicle.changeLane(timeNow);
		}
	}

	/**
	 * Try to move vehicle from parking area onto roads. A vehicle can only be
	 * released from parking if the current time has passed the earliest start
	 * time of the vehicle.
	 *
	 */
	public void releaseVehicleFromParking(final double timeNow,SimulationListener listener) {
		for (Edge edge : edges) {
			Vehicle next = edge.getVehicleToGetIntoTheLane();
			if(next == null) {
				next = edge.getNextParkedVehicle(timeNow);
			}
			edge.setNextVehicleToGetIntoTheLane(next);
			if(next != null && next.startFromParking()){
				if(listener != null){
					listener.onVehicleStartMoving(vehicles, (int) (timeNow*settings.numStepsPerSecond), this);
				}
			}
		}
	}

	public void releaseTripMakingVehicles(final double timeNow, SimulationListener listener) {
		Vehicle next = getNextTripMakingVehicle(timeNow);
		while (next != null){

			if (!(next.isCAV() || next.isConnectedV())){
				next.setRouteLegs(routingAlgoFactory.getRoutingAlgo(Routing.Algorithm.DIJKSTRA, this).createCompleteRoute(next.getStart(), next.getEnd(), next.type));
			}
			else {
				next.setRouteLegs(routingAlgorithm.createCompleteRoute(next.getStart(), next.getEnd(), next.type));
			}
			if(listener != null){
				listener.onVehicleAdd(Arrays.asList(next),(int)(timeNow/settings.numStepsPerSecond), this);
			}
			newVehiclesSinceLastReport.add(next);
			newVehiclesforDemandEstimation.add(next);
			next.park(true, timeNow);
			next = getNextTripMakingVehicle(timeNow);
		}
	}

	public Vehicle getNextTripMakingVehicle(double timeNow){
		Vehicle v =  tripMakingVehicles.peek();
		if(v != null && v.isStartTripMaking(timeNow)){
			return tripMakingVehicles.poll();
		}
		return null;
	}

	public void updateTrafficLights(double timeNow){
		if (settings.trafficLightTiming != TrafficLightTiming.NONE) {
			lightCoordinator.scheduleLights(timeNow);
			lightCoordinator.updateLights(timeNow);
		}
	}

	public int getVehicleCount(){
		return vehicles.size();
	}

	public void finishRemoveCheck(final double timeNow){
		for (Vehicle vehicle : vehicles) {
			if(!vehicle.isFinished()){
				if(vehicle.lane != null && vehicle.lane.getFrontVehicleInLane().id == vehicle.id) {
					if(timeNow - vehicle.getLastSpeedChangeTime() > settings.numStepsPerSecond * 300) {
						System.out.println("Vehicle Stucked " + vehicle.id + " at: " + vehicle.lane.edge.index);
					}
				}
			}else{
				System.out.println("Finished Vehicle " + vehicle.id);
			}
		}
	}

	public void addTripFinishedVehicles(List<Vehicle> vehicles){
		finishedVehicles.addAll(vehicles);
	}

	public List<Vehicle> getFinishedVehicles() {
		return finishedVehicles;
	}

	public boolean isPublishTime(int step){
		if(settings.updateStepInterval == 1){
			return true;
		}else {
			return (step % settings.updateStepInterval) == 0;
		}
	}

	public Comparator<Vehicle> getTripMakingVehicleComparator(){
		return new Comparator<Vehicle>() {
			@Override
			public int compare(Vehicle v1, Vehicle v2) {
				if(v1.timeRouteStart < v2.timeRouteStart){
					return -1;
				}else if(v1.timeRouteStart > v2.timeRouteStart){
					return 1;
				}
				return 0;
			}
		};
	}

	public void setCrossingIncreasingOrders(){
		for (Node node : nodes) {
			for (Edge inwardEdge : node.inwardEdges) {
				List<Edge> outEdges = new ArrayList<>();
				outEdges.addAll(node.outwardEdges);
				if(settings.isDriveOnLeft) {
					Collections.sort(outEdges, new Comparator<Edge>() {
						@Override
						public int compare(Edge o1, Edge o2) {
							int o1Left = RoadUtil.findOutwardEdgesOnLeft(inwardEdge, o1).size();
							int o2Left = RoadUtil.findOutwardEdgesOnLeft(inwardEdge, o2).size();
							if (o1Left < o2Left) {
								return -1;
							} else if (o1Left > o2Left) {
								return 1;
							} else {
								return 0;
							}
						}
					});
				}else{
					Collections.sort(outEdges, new Comparator<Edge>() {
						@Override
						public int compare(Edge o1, Edge o2) {
							int o1Right = RoadUtil.findOutwardEdgesOnRight(inwardEdge, o1).size();
							int o2Right = RoadUtil.findOutwardEdgesOnRight(inwardEdge, o2).size();
							if(o1Right < o2Right){
								return -1;
							}else if(o1Right > o2Right){
								return 1;
							}else {
								return 0;
							}
						}
					});
				}
				LinkedHashMap<Edge, Integer> edgeLaneMap = new LinkedHashMap<>();

				if(inwardEdge.getLaneCount() == 1){
					for (Edge outEdge : outEdges) {
						edgeLaneMap.put(outEdge, 0);
					}
				}else if(inwardEdge.getLaneCount() == outEdges.size()){
					for (int i = 0; i < outEdges.size() ; i++) {
						edgeLaneMap.put(outEdges.get(i), i);
					}
				}else{
					//throw new UnsupportedOperationException("The condition is not supported at this moment");
				}
				inwardEdge.setEdgeLaneMap(edgeLaneMap);
			}
		}
	}

	public void clearPaths(){
		paths.clear();
	}

	public ArrayList<VehiclePathExternal> getVehiclePaths(){
		return paths;
	}

	public void addVehiclePaths(Vehicle v){
		if (v.getNextRouteLegs().size() > 1 && (v.isCAV() || v.isConnectedV())) {
			ArrayList<Integer> pathInInt = new ArrayList<>();
			for (RouteLeg routeLeg : v.getNextRouteLegs()) {
				pathInInt.add(routeLeg.edge.startNode.index);
			}
			if (v.getNextRouteLegs().size() > 1) {
				Edge lastEdge = v.getNextRouteLegs().get(v.getNextRouteLegs().size() - 1).edge;
				pathInInt.add(lastEdge.endNode.index);
			}

			double vehicleFraction = settings.mvgVehicleCount/settings.extListenerUpdateInterval;

			paths.add(new VehiclePathExternal(pathInInt, 1));
		}
	}


	public void updateStatistics(int step){

		/**
		 * Add stat collectors here
		 */
		updateFlow(step);
		updateVehicleNumbers(step);

	}

	/**
	 * Stat collectors
	 */

	public  void updateFlow(int step){
		if (step%settings.mvgFlow == 0) {
			for (Edge edge : edges) {
				edge.updateTrafficStatistics();
			}
		}
	}


	public void updateVehicleNumbers(int step){
		if (step%settings.mvgVehicleCount == 0) {
			for (Edge edge : edges) {
				int numVehicles = 0;
				int numVehiclesRight = 0;
				int numVehiclesStraight = 0;
				int numVehiclesLeft = 0;
				for (Lane lane : edge.getLanes()) {
					//numVehicles += lane.getVehicles().size();
					for (Vehicle v : lane.getVehicles()) {
						if(v.isCAV() || v.isConnectedV()) {
							numVehicles += 2;
							if (v.edgeBeforeTurnRight == edge) {
								numVehiclesRight++;
							} else if (v.edgeBeforeTurnLeft == edge) {
								numVehiclesLeft++;
							} else {
								numVehiclesStraight++;
							}

							addVehiclePaths(v);
						}

					}

				}
				edge.updateVehicleNumbers(numVehicles, numVehiclesStraight, numVehiclesRight, numVehiclesLeft);
			}
		}
	}


}

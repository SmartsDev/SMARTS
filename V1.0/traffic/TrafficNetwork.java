package traffic;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import common.Settings;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableRouteLeg;
import processor.worker.Fellow;
import traffic.light.LightCoordinator;
import traffic.road.Edge;
import traffic.road.GridCell;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.road.RoadType;
import traffic.road.RoadUtil;
import traffic.routing.Dijkstra;
import traffic.routing.RandomAStar;
import traffic.routing.ReferenceBasedSearch;
import traffic.routing.RouteLeg;
import traffic.routing.Routing;
import traffic.routing.Simple;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.SlowdownFactor;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

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
			final Line2D.Double line1 = new Line2D.Double(edge1.startNode.lon,
					edge1.startNode.lat * Settings.lonVsLat, edge1.endNode.lon,
					edge1.endNode.lat * Settings.lonVsLat);
			final double dist1 = line1.ptSegDist(node.lon, node.lat
					* Settings.lonVsLat);
			final Line2D.Double line2 = new Line2D.Double(edge2.startNode.lon,
					edge2.startNode.lat * Settings.lonVsLat, edge2.endNode.lon,
					edge2.endNode.lat * Settings.lonVsLat);
			final double dist2 = line2.ptSegDist(node.lon, node.lat
					* Settings.lonVsLat);
			return dist1 < dist2 ? -1 : dist1 == dist2 ? 0 : 1;
		}
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
			return v1.headPosition > v2.headPosition ? -1
					: v1.headPosition == v2.headPosition ? 0 : 1;
		}
	}

	public ArrayList<Vehicle> vehicles = new ArrayList<>();

	// For report data
	public ArrayList<Vehicle> newVehiclesSinceLastReport = new ArrayList<>();
	public HashMap<SerializableExternalVehicle, Double> externalVehicleRepeatPerStep = new HashMap<>();
	ArrayList<Double> driverProfilePercAccumulated = new ArrayList<>();
	ArrayList<Edge> internalTramStopEdges = new ArrayList<>();
	ArrayList<Edge> internalNonPublicVehicleStartEdges = new ArrayList<>();
	ArrayList<Edge> internalNonPublicVehicleEndEdges = new ArrayList<>();
	ArrayList<Edge> internalBusStartEdges = new ArrayList<>();
	ArrayList<Edge> internalBusEndEdges = new ArrayList<>();
	ArrayList<Edge> internalTramStartEdges = new ArrayList<>();
	ArrayList<Edge> internalTramEndEdges = new ArrayList<>();
	public Routing routingAlgorithm;
	Random random = new Random();
	int numInternalVehicleAllTime = 0;
	public int numInternalNonPublicVehicle = 0;
	public int numInternalTram = 0;
	public int numInternalBus = 0;
	public LightCoordinator lightCoordinator = new LightCoordinator();
	ArrayList<GridCell> workareaCells;
	String internalVehiclePrefix = "";
	VehicleUtil vehicleUtil = new VehicleUtil();
	public VehiclePositionComparator vehiclePositionComparator = new VehiclePositionComparator();
	double timeLastPublicVehicleCreated = 0;
	ArrayList<String> internalTramRefInSdWindow = new ArrayList<>();

	HashMap<String, ArrayList<Edge>> internalTramStartEdgesInSourceWindow = new HashMap<>();
	HashMap<String, ArrayList<Edge>> internalTramEndEdgesInDestinationWindow = new HashMap<>();
	ArrayList<String> internalBusRefInSourceDestinationWindow = new ArrayList<>();

	HashMap<String, ArrayList<Edge>> internalBusStartEdgesInSourceWindow = new HashMap<>();

	HashMap<String, ArrayList<Edge>> internalBusEndEdgesInDestinationWindow = new HashMap<>();

	/**
	 * Initialize traffic network.
	 */
	public TrafficNetwork() {
		super();
		identifyInternalTramStopEdges();
		addTramStopsToParallelNonTramEdges();
	}

	public void clearReportedData() {
		newVehiclesSinceLastReport.clear();
	}

	/**
	 * Create a new vehicle object and add it to the pool of vehicles.
	 *
	 */
	void addNewVehicle(final VehicleType type, final boolean isExternal,
			final boolean foreground, final ArrayList<RouteLeg> routeLegs,
			final String idPrefix, final double timeRouteStart,
			final String externalId, final DriverProfile dP) {
		// Do not proceed if the route is empty
		if (routeLegs.size() < 1) {
			return;
		}
		// Create new vehicle
		final Vehicle vehicle = new Vehicle();
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
		// Legs of route
		vehicle.routeLegs = routeLegs;
		// Driver profile
		vehicle.driverProfile = dP;
		// Set as active
		vehicle.active = true;
		// Add vehicle to system
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
			// Add vehicle to system
			vehicles.add(vehicle);

			parkOneVehicle(vehicle, true, timeRouteStart);
		} else {
			// Add external vehicle to system
			vehicle.id = externalId;
			vehicles.add(vehicle);
			parkOneVehicle(vehicle, true, timeRouteStart);
		}

		// Certain information about this vehicle will be reported to server
		newVehiclesSinceLastReport.add(vehicle);
	}

	/**
	 * Add an existing vehicle object to traffic network. The vehicle object is
	 * transferred from a neighbor worker.
	 */
	public void addOneTransferredVehicle(final Vehicle vehicle,
			final double timeNow) {
		vehicle.active = true;
		vehicles.add(vehicle);
		vehicle.lane.vehicles.add(vehicle);
		if (!vehicle.isExternal) {
			if (vehicle.type == VehicleType.TRAM) {
				numInternalTram++;
			} else if (vehicle.type == VehicleType.BUS) {
				numInternalBus++;
			} else {
				numInternalNonPublicVehicle++;
			}
		}
		if (vehicle.routeLegs.get(vehicle.indexLegOnRoute).stopover > 0) {
			parkOneVehicle(vehicle, false, timeNow);
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
			candidateEdges = getEdgesParallelToEdge(tramEdge.startNode.lat,
					tramEdge.startNode.lon, tramEdge.endNode.lat,
					tramEdge.endNode.lon);
			final NearbyEdgeComparator parallelEdgeComparator = new NearbyEdgeComparator(
					tramEdge.endNode);
			Collections.sort(candidateEdges, parallelEdgeComparator);
			// Foot of perpendicular from tram edge must be within the candidate
			// edge
			for (final Edge candidateEdge : candidateEdges) {
				if (candidateEdge.type == RoadType.tram) {
					continue;
				}

				final Line2D.Double line = new Line2D.Double(
						candidateEdge.startNode.lon,
						candidateEdge.startNode.lat * Settings.lonVsLat,
						candidateEdge.endNode.lon, candidateEdge.endNode.lat
								* Settings.lonVsLat);
				final double distSq_StopToLine = line.ptSegDistSq(
						tramEdge.endNode.lon, tramEdge.endNode.lat
								* Settings.lonVsLat);
				final double distSq_StopToLineStart = Point2D.distanceSq(
						tramEdge.endNode.lon, tramEdge.endNode.lat
								* Settings.lonVsLat,
						candidateEdge.startNode.lon,
						candidateEdge.startNode.lat * Settings.lonVsLat);
				final double dist_StartToClosestPoint = Math
						.sqrt(distSq_StopToLineStart - distSq_StopToLine);
				final double dist_StartToEnd = Point2D.distance(
						candidateEdge.startNode.lon,
						candidateEdge.startNode.lat * Settings.lonVsLat,
						candidateEdge.endNode.lon, candidateEdge.endNode.lat
								* Settings.lonVsLat);
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
	public void buildEnvironment(final ArrayList<GridCell> cells,
			final String internalVehiclePrefix, final int stepCurrent) {

		workareaCells = cells;
		this.internalVehiclePrefix = internalVehiclePrefix;

		identifyInternalVehicleRouteStartEndEdges();
		identifyReferencesOfAllPublicTransportTypesInSourceDestinationWindow();
		computeAccumulatedDriverProfileDistribution();

		if (Settings.routingAlgorithm == Routing.Algorithm.DIJKSTRA) {
			routingAlgorithm = new Dijkstra(this);
		} else if (Settings.routingAlgorithm == Routing.Algorithm.RANDOM_A_STAR) {
			routingAlgorithm = new RandomAStar(this);
		} else if (Settings.routingAlgorithm == Routing.Algorithm.SIMPLE) {
			routingAlgorithm = new Simple(this);
		}

	}

	void computeAccumulatedDriverProfileDistribution() {
		driverProfilePercAccumulated = new ArrayList<>();
		double total = 0;
		for (final Double d : Settings.driverProfileDistribution) {
			total += d;
		}
		double accumulated = 0;
		for (final Double d : Settings.driverProfileDistribution) {
			accumulated += d;
			driverProfilePercAccumulated.add(accumulated / total);
		}
	}

	/**
	 * Create vehicles based on external routes imported during setup.
	 *
	 */
	public void createExternalVehicles(
			final ArrayList<SerializableExternalVehicle> externalRoutes,
			final double timeNow) {
		for (final SerializableExternalVehicle vehicle : externalRoutes) {
			final VehicleType type = VehicleType
					.getVehicleTypeFromName(vehicle.vehicleType);
			if (vehicle.numberRepeatPerSecond <= 0) {
				addNewVehicle(type, true, vehicle.foreground,
						createOneRouteFromSerializedData(vehicle.route), "",
						vehicle.startTime, vehicle.id,
						DriverProfile.valueOf(vehicle.driverProfile));
			} else {
				// This is a simple way to get number of vehicles per step. The
				// result number
				// may be inaccurate.
				final double numRepeatPerStep = vehicle.numberRepeatPerSecond
						/ Settings.numStepsPerSecond;
				externalVehicleRepeatPerStep.put(vehicle, numRepeatPerStep);
			}
		}
	}

	/**
	 * Create non-public vehicle.
	 * 
	 * @param isNewNonPubVehiclesAllowed
	 */
	void createInternalNonPublicVehicles(int numLocalRandomPrivateVehicles,
			final double timeNow, boolean isNewNonPubVehiclesAllowed) {
		if (isNewNonPubVehiclesAllowed) {
			final int numVehiclesNeeded = numLocalRandomPrivateVehicles
					- numInternalNonPublicVehicle;
			for (int i = 0; i < numVehiclesNeeded; i++) {
				final double typeDecider = random.nextDouble();
				VehicleType type = null;
				if (typeDecider < 0.05) {
					type = VehicleType.BIKE;
				} else if ((0.05 <= typeDecider) && (typeDecider < 0.1)) {
					type = VehicleType.TRUCK;
				} else {
					type = VehicleType.CAR;
				}
				ArrayList<RouteLeg> route = createOneRandomInternalRoute(type);
				if (route != null) {
					addNewVehicle(type, false, false, route,
							internalVehiclePrefix, timeNow, "",
							getRandomDriverProfile());
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
	void createInternalPublicVehicles(int numLocalRandomTrams,
			int numLocalRandomBuses, boolean isNewTramsAllowed,
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
	public void createInternalVehicles(int numLocalRandomPrivateVehicles,
			int numLocalRandomTrams, int numLocalRandomBuses,
			boolean isNewNonPubVehiclesAllowed, boolean isNewTramsAllowed,
			boolean isNewBusesAllowed, final double timeNow) {
		if ((internalNonPublicVehicleStartEdges.size() > 0)
				&& (internalNonPublicVehicleEndEdges.size() > 0)) {
			createInternalNonPublicVehicles(numLocalRandomPrivateVehicles,
					timeNow, isNewNonPubVehiclesAllowed);
		}
		createInternalPublicVehicles(numLocalRandomTrams, numLocalRandomBuses,
				isNewTramsAllowed, isNewBusesAllowed, timeNow);
	}

	void createOneInternalPublicVehicle(final VehicleType type,
			final double timeNow) {
		VehicleType transport = null;
		String randomRef = null;
		ArrayList<Edge> startEdgesOfRandomRoute = null;
		ArrayList<Edge> endEdgesOfRandomRoute = null;
		if ((type == VehicleType.TRAM)
				&& (internalTramRefInSdWindow.size() > 0)) {
			transport = VehicleType.TRAM;
			randomRef = internalTramRefInSdWindow.get(random
					.nextInt(internalTramRefInSdWindow.size()));
			startEdgesOfRandomRoute = internalTramStartEdgesInSourceWindow
					.get(randomRef);
			endEdgesOfRandomRoute = internalTramEndEdgesInDestinationWindow
					.get(randomRef);
		} else if ((type == VehicleType.BUS)
				&& (internalBusRefInSourceDestinationWindow.size() > 0)) {
			transport = VehicleType.BUS;
			randomRef = internalBusRefInSourceDestinationWindow.get(random
					.nextInt(internalBusRefInSourceDestinationWindow.size()));
			startEdgesOfRandomRoute = internalBusStartEdgesInSourceWindow
					.get(randomRef);
			endEdgesOfRandomRoute = internalBusEndEdgesInDestinationWindow
					.get(randomRef);
		}

		if ((startEdgesOfRandomRoute == null)
				|| (endEdgesOfRandomRoute == null)) {
			return;
		}

		ArrayList<RouteLeg> route = ReferenceBasedSearch.createRoute(transport,
				randomRef, startEdgesOfRandomRoute, endEdgesOfRandomRoute);
		for (int numTry = 0; numTry < 10; numTry++) {
			if (route == null) {
				route = ReferenceBasedSearch.createRoute(transport, randomRef,
						startEdgesOfRandomRoute, endEdgesOfRandomRoute);
			} else {
				break;
			}
		}
		if (route != null) {
			addNewVehicle(type, false, false, route, internalVehiclePrefix,
					timeNow, "", getRandomDriverProfile());
		}
	}

	

	/**
	 * Generate a route.
	 *
	 * @param workarea
	 * @param strategy
	 */
	ArrayList<RouteLeg> createOneRandomInternalRoute(final VehicleType type) {
		final Edge edgeStart = internalNonPublicVehicleStartEdges.get(random
				.nextInt(internalNonPublicVehicleStartEdges.size()));

		final Edge edgeEnd = internalNonPublicVehicleEndEdges.get(random
				.nextInt(internalNonPublicVehicleEndEdges.size()));

		final ArrayList<RouteLeg> route = routingAlgorithm.createCompleteRoute(
				edgeStart, edgeEnd, type);

		if ((route == null) || (route.size() == 0)) {
			return null;
		} else {
			return route;
		}
	}

	ArrayList<RouteLeg> createOneRouteFromSerializedData(
			final ArrayList<SerializableRouteLeg> serializedData) {
		final ArrayList<RouteLeg> route = new ArrayList<>(1000);
		for (final SerializableRouteLeg sLeg : serializedData) {
			final RouteLeg leg = new RouteLeg(edges.get(sLeg.edgeIndex),
					sLeg.stopover);
			route.add(leg);
		}
		return route;
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
	 * This method tries to find a start position for a vehicle such that the
	 * vehicle will be unlikely to collide with an existing vehicle. For
	 * simplicity, this method only checks the current route leg and the two
	 * adjacent legs. Therefore it is not guaranteed that the new position is
	 * safe, especially when all the three legs are very short.
	 */
	double getStartPositionInLane0(final Vehicle vehicle) {
		Edge currentEdge = vehicle.routeLegs.get(vehicle.indexLegOnRoute).edge;

		double headPosSpaceFront = currentEdge.length;

		if (vehicle.indexLegOnRoute + 1 < vehicle.routeLegs.size()) {
			RouteLeg legToCheck = vehicle.routeLegs
					.get(vehicle.indexLegOnRoute + 1);
			Lane laneToCheck = legToCheck.edge.lanes.get(0);
			if (laneToCheck.vehicles.size() > 0) {
				Vehicle vehicleToCheck = laneToCheck.vehicles
						.get(laneToCheck.vehicles.size() - 1);
				double endPosOfLastVehicleOnNextLeg = vehicleToCheck.headPosition
						+ currentEdge.length - vehicleToCheck.length;
				if (endPosOfLastVehicleOnNextLeg < headPosSpaceFront) {
					headPosSpaceFront = endPosOfLastVehicleOnNextLeg;
				}
			}
		}

		double headPosSpaceBack = 0;

		if (vehicle.indexLegOnRoute > 0) {
			RouteLeg legToCheck = vehicle.routeLegs
					.get(vehicle.indexLegOnRoute - 1);
			Lane laneToCheck = legToCheck.edge.lanes.get(0);
			if (laneToCheck.vehicles.size() > 0) {
				Vehicle vehicleToCheck = laneToCheck.vehicles.get(0);
				double headPosOfFirstVehicleOnPreviousLeg = -(laneToCheck.edge.length - vehicleToCheck.headPosition);
				if (headPosSpaceBack - vehicle.length < headPosOfFirstVehicleOnPreviousLeg) {
					headPosSpaceBack = headPosOfFirstVehicleOnPreviousLeg
							+ vehicle.length;
				}
			}
		}

		if (headPosSpaceFront <= headPosSpaceBack)
			return -1;

		final ArrayList<double[]> gaps = new ArrayList<>();
		if (currentEdge.lanes.get(0).vehicles.size() > 0) {

			double gapFront = headPosSpaceFront;
			for (Vehicle vehicleToCheck : currentEdge.lanes.get(0).vehicles) {
				if (gapFront - vehicle.length > vehicleToCheck.headPosition) {
					gaps.add(new double[] { gapFront,
							vehicleToCheck.headPosition + vehicle.length });
				}
				gapFront = vehicleToCheck.headPosition - vehicleToCheck.length;
				if (gapFront < headPosSpaceBack) {
					break;
				}
			}
		} else {
			gaps.add(new double[] { headPosSpaceFront, headPosSpaceBack });
		}

		if (gaps.size() == 0) {
			return -1;
		} else {
			// Pick a random position within a random gap
			final double[] gap = gaps.get(random.nextInt(gaps.size()));
			final double pos = gap[0]
					- (random.nextDouble() * (gap[0] - gap[1]));
			return pos;
		}
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
		for (final GridCell cell : workareaCells) {
			for (final Node node : cell.nodes) {
				if (((Settings.listRouteSourceWindowForInternalVehicle.size() == 0) && (Settings.listRouteSourceDestinationWindowForInternalVehicle
						.size() == 0))
						|| ((Settings.listRouteSourceWindowForInternalVehicle
								.size() > 0) && isNodeInsideRectangle(
								node,
								Settings.listRouteSourceWindowForInternalVehicle))
						|| ((Settings.listRouteSourceDestinationWindowForInternalVehicle
								.size() > 0) && isNodeInsideRectangle(
								node,
								Settings.listRouteSourceDestinationWindowForInternalVehicle))) {
					for (final Edge edge : node.outwardEdges) {
						if (isEdgeSuitableForRouteStartOfInternalVehicle(edge)) {
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
			if (((Settings.listRouteDestinationWindowForInternalVehicle.size() == 0) && (Settings.listRouteSourceDestinationWindowForInternalVehicle
					.size() == 0))
					|| ((Settings.listRouteDestinationWindowForInternalVehicle
							.size() > 0) && isNodeInsideRectangle(
							edge.endNode,
							Settings.listRouteDestinationWindowForInternalVehicle))
					|| ((Settings.listRouteSourceDestinationWindowForInternalVehicle
							.size() > 0) && isNodeInsideRectangle(
							edge.endNode,
							Settings.listRouteSourceDestinationWindowForInternalVehicle))) {
				if (isEdgeSuitableForRouteEndOfInternalVehicle(edge)) {
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
		identifyReferencesOfOnePublicTransportTypeInSourceDestinationWindow(
				internalTramRefInSdWindow, tramRoutes, internalTramStartEdges,
				internalTramEndEdges, internalTramStartEdgesInSourceWindow,
				internalTramEndEdgesInDestinationWindow);
		identifyReferencesOfOnePublicTransportTypeInSourceDestinationWindow(
				internalBusRefInSourceDestinationWindow, busRoutes,
				internalBusStartEdges, internalBusEndEdges,
				internalBusStartEdgesInSourceWindow,
				internalBusEndEdgesInDestinationWindow);
	}

	/**
	 * Collect the edges on tram routes that overlap with this work area.
	 */
	void identifyReferencesOfOnePublicTransportTypeInSourceDestinationWindow(
			final ArrayList<String> refsInSdWindow,
			final HashMap<String, ArrayList<Edge>> referencedRoutes,
			final ArrayList<Edge> routeStartEdges,
			final ArrayList<Edge> routeEndEdges,
			final HashMap<String, ArrayList<Edge>> routeStartEdgesInSourceWindow,
			final HashMap<String, ArrayList<Edge>> routeEndEdgesInDestinationWindow) {
		refsInSdWindow.clear();
		final Iterator it = referencedRoutes.entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry pairs = (Map.Entry) it.next();
			final String ref = (String) pairs.getKey();
			final ArrayList<Edge> edgesOnOneRoute = (ArrayList<Edge>) pairs
					.getValue();
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
			if (isStartCovered && (startEdgesOnOneRoute.size() > 0)
					&& isEndCovered && (endEdgesOnOneRoute.size() > 0)) {
				refsInSdWindow.add(ref);
				routeStartEdgesInSourceWindow.put(ref, startEdgesOnOneRoute);
				routeEndEdgesInDestinationWindow.put(ref, endEdgesOnOneRoute);
			}
		}
	}

	boolean isEdgeSuitableForRouteEndOfInternalVehicle(final Edge edge) {
		if ((edge == null)
				|| (edge.length < Settings.minLengthOfRouteStartEndEdge)
				|| edge.isRoundabout) {
			return false;
		}
		return true;
	}

	boolean isEdgeSuitableForRouteStartOfInternalVehicle(final Edge edge) {
		// Note: route cannot start from cross-border edge at the starting side
		// of the
		// edge. This is to prevent problem in transferring of vehicle.
		if ((edge == null)
				|| (edge.length < Settings.minLengthOfRouteStartEndEdge)
				|| edge.isRoundabout
				|| !workareaCells.contains(edge.endNode.gridCell)) {
			return false;
		}

		return true;
	}

	/**
	 * Moves vehicle to parking.
	 */
	public void parkOneVehicle(final Vehicle vehicle,
			final boolean isNewVehicle, final double timeNow) {
		vehicle.speed = 0;
		vehicle.acceleration = 0;
		vehicle.routeLegs.get(vehicle.indexLegOnRoute).edge.parkedVehicles
				.add(vehicle);
		if (isNewVehicle) {
			vehicle.earliestTimeToLeaveParking = vehicle.timeRouteStart
					+ vehicle.routeLegs.get(0).stopover;
		} else {
			vehicle.earliestTimeToLeaveParking = timeNow
					+ vehicle.routeLegs.get(vehicle.indexLegOnRoute).stopover;
		}
		if (vehicle.lane != null) {
			vehicle.lane.vehicles.remove(vehicle);
			vehicle.lane = null;
		}
	}

	/**
	 * Remove vehicles from their lanes and the whole traffic network.
	 */
	public void removeActiveVehicles(
			final ArrayList<Vehicle> vehiclesToBeRemoved) {

		for (final Vehicle v : vehiclesToBeRemoved) {
			// Cancel priority lanes
			if (v.type == VehicleType.PRIORITY) {
				VehicleUtil.setPriorityLanes(v, false);
			}

			// Makes vehicle inactive
			v.active = false;
			/*
			 * Remove vehicles from old lanes
			 */
			if (v.lane != null) {
				v.lane.vehicles.remove(v);
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
		for (final SerializableExternalVehicle vehicle : externalVehicleRepeatPerStep
				.keySet()) {
			double numRepeatThisStep = externalVehicleRepeatPerStep
					.get(vehicle);
			if (externalVehicleRepeatPerStep.get(vehicle) < 1.0) {
				final int numStepsPerRepeat = (int) (1.0 / numRepeatThisStep);
				if ((step % numStepsPerRepeat) == 0) {
					numRepeatThisStep = 1;
				} else {
					numRepeatThisStep = 0;
				}
			}

			for (int i = 0; i < (int) numRepeatThisStep; i++) {
				final VehicleType type = VehicleType
						.getVehicleTypeFromName(vehicle.vehicleType);
				final DriverProfile profile = DriverProfile
						.valueOf(vehicle.driverProfile);
				addNewVehicle(type, true, vehicle.foreground,
						createOneRouteFromSerializedData(vehicle.route), "",
						timeNow, vehicle.id + "_time_" + timeNow + "_" + i,
						profile);
			}
		}
	}

	public void resetTraffic() {
		// Clear vehicles from network
		vehicles.clear();
		externalVehicleRepeatPerStep.clear();
		// Reset temp values for lanes
		for (final Lane lane : lanes) {
			lane.vehicles.clear();
			lane.isBlocked = false;
			lane.speedOfLatestVehicleLeftThisWorker = 100;
			lane.endPositionOfLatestVehicleLeftThisWorker = 1000000000;
			lane.isPriority = false;
		}

		// Clear parked vehicles from edges
		for (final Edge edge : edges) {
			edge.parkedVehicles.clear();
		}

		// Reset temporary values
		numInternalNonPublicVehicle = 0;
		numInternalTram = 0;
		numInternalBus = 0;
		numInternalVehicleAllTime = 0;
		timeLastPublicVehicleCreated = 0;
	}

	/**
	 * Moves vehicle from parking area onto roads.
	 */
	public boolean startOneVehicleFromParking(final Vehicle vehicle) {
		final RouteLeg leg = vehicle.routeLegs.get(vehicle.indexLegOnRoute);
		final Edge edge = leg.edge;
		final Lane lane = edge.lanes.get(0);// Start from the lane closest to
											// roadside
		final double pos = getStartPositionInLane0(vehicle);
		if (pos >= 0) {
			edge.parkedVehicles.remove(vehicle);
			vehicle.lane = lane;
			vehicle.lane.vehicles.add(vehicle);
			vehicle.headPosition = pos;
			vehicle.speed = 0;
			Collections.sort(vehicle.lane.vehicles, vehiclePositionComparator);// Sort
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Update the timers related to tram stops.
	 */
	public void updateTramStopTimers() {
		for (int i = 0; i < internalTramStopEdges.size(); i++) {
			final Edge edge = internalTramStopEdges.get(i);
			if (edge.timeTramStopping > 0) {
				edge.timeTramStopping -= 1 / Settings.numStepsPerSecond;

				/*
				 * Ensure the tram stop CAN NOT block for some time after the
				 * block timer is gone: the first tram moves on and other
				 * vehicles waiting for it in parallel edges can also move on
				 * without stopping again.
				 */
				if (edge.timeTramStopping <= 0) {
					edge.timeNoTramStopping = Settings.minGapBetweenTramStopTimerCountDowns;
				}
			} else if (edge.timeNoTramStopping > 0) {
				edge.timeNoTramStopping -= 1 / Settings.numStepsPerSecond;
			}

		}
	}
}

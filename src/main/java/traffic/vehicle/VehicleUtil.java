package traffic.vehicle;

import java.awt.geom.Line2D;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadType;
import traffic.road.RoadUtil;
import traffic.routing.RouteLeg;
import traffic.vehicle.SlowdownFactor;

/**
 * This class finds impeding objects based on various factors, e.g., traffic
 * lights, front vehicles, conflicting traffic at intersection, etc.
 */
public class VehicleUtil {

	/**
	 * Compute the GPS coordinates of the head and end of a given vehicle
	 */
	public static double[] calculateCoordinates(final Vehicle v) {
		final double headToEdgeRatio = v.headPosition / v.lane.edge.length;
		final double tailToEdgeRatio = (v.headPosition - v.length) / v.lane.edge.length;

		final double headLon = v.lane.lonStart + (headToEdgeRatio * v.lane.lonLength);
		final double headLat = v.lane.latStart + (headToEdgeRatio * v.lane.latLength);
		final double tailLon = v.lane.lonStart + (tailToEdgeRatio * v.lane.lonLength);
		final double tailLat = v.lane.latStart + (tailToEdgeRatio * v.lane.latLength);

		final double[] coords = { headLon, headLat, tailLon, tailLat };
		return coords;
	}

	public static void findEdgeBeforeNextTurn(final Vehicle vehicle) {
		double examinedDist = 0;
		vehicle.edgeBeforeTurnLeft = null;
		vehicle.edgeBeforeTurnRight = null;
		int indexLegOnRouteBeingChecked = vehicle.indexLegOnRoute;
		while (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1)) {
			final Edge e1 = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
			final Edge e2 = vehicle.routeLegs.get(indexLegOnRouteBeingChecked + 1).edge;

			if (e1.startNode == e2.endNode) {
				// Vehicle is going to make U-turn
				vehicle.edgeBeforeTurnRight = e1;
			} else if (!e1.name.equals(e2.name) || (e1.type != e2.type)) {
				final Line2D.Double e1Seg = new Line2D.Double(e1.startNode.lon, e1.startNode.lat * Settings.lonVsLat,
						e1.endNode.lon, e1.endNode.lat * Settings.lonVsLat);
				final int ccw = e1Seg.relativeCCW(e2.endNode.lon, e2.endNode.lat * Settings.lonVsLat);
				if (ccw < 0) {
					vehicle.edgeBeforeTurnLeft = e1;
				} else if (ccw > 0) {
					vehicle.edgeBeforeTurnRight = e1;
				}
			}

			if ((vehicle.edgeBeforeTurnLeft != null) || (vehicle.edgeBeforeTurnRight != null)) {
				break;
			}

			examinedDist += e1.length;
			if (((examinedDist - vehicle.headPosition) < Settings.lookAheadDistance)
					&& (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1))) {
				indexLegOnRouteBeingChecked++;
			} else {
				break;
			}
		}
	}

	public static double getAverageSpeedOfTrip(final Vehicle vehicle) {
		// Get total road length in the trip
		double length = 0;
		for (final RouteLeg leg : vehicle.routeLegs) {
			length += leg.edge.length;
		}
		// Return average speed
		return (length / vehicle.timeTravel) * 3.6;
	}

	/**
	 * Get the closest vehicle whose head position is behind the head position
	 * of a given vehicle. The two vehicles may not be in the same lane.
	 *
	 */
	public static Vehicle getBackVehicleInTargetLane(final Vehicle vehicle, final Lane targetLane) {
		Vehicle backVehicle = null;
		for (int i = 0; i < targetLane.vehicles.size(); i++) {
			if (targetLane.vehicles.get(i).headPosition < vehicle.headPosition) {
				backVehicle = targetLane.vehicles.get(i);
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
	public static Vehicle getFrontVehicleInTargetLane(final Vehicle vehicle, final Lane targetLane,
			final double gapToTargetLane) {
		Vehicle frontVehicle = null;

		for (int i = targetLane.vehicles.size() - 1; i >= 0; i--) {
			if ((targetLane.vehicles.get(i).headPosition + gapToTargetLane) > vehicle.headPosition) {
				frontVehicle = targetLane.vehicles.get(i);
				break;
			}
		}
		return frontVehicle;
	}

	public static boolean isAllLanesOnLeftBlocked(Edge edge, int laneNumber) {
		if (Settings.isDriveOnLeft) {
			for (int num = laneNumber - 1; num >= 0; num--) {
				if (!edge.lanes.get(num).isBlocked) {
					return false;
				}
			}
		} else {
			for (int num = laneNumber + 1; num < edge.lanes.size(); num++) {
				if (!edge.lanes.get(num).isBlocked) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isAllLanesOnRightBlocked(Edge edge, int laneNumber) {
		if (Settings.isDriveOnLeft) {
			for (int num = laneNumber + 1; num < edge.lanes.size(); num++) {
				if (!edge.lanes.get(num).isBlocked) {
					return false;
				}
			}
		} else {
			for (int num = laneNumber - 1; num >= 0; num--) {
				if (!edge.lanes.get(num).isBlocked) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check whether a vehicle can travel from one node to another through an
	 * edge
	 */
	public static boolean canGoThrough(final Node nodeStart, final Node nodeEnd, final VehicleType vehicleType) {
		for (final Edge e : nodeStart.outwardEdges) {
			if (e.endNode == nodeEnd) {
				if (e.type == RoadType.tram) {
					if ((vehicleType == VehicleType.PRIORITY) && !Settings.isAllowPriorityVehicleUseTramTrack) {
						return false;
					} else if ((vehicleType != VehicleType.PRIORITY) && (vehicleType != VehicleType.TRAM)) {
						return false;
					}
				}
				if (!isEdgeBlocked(e)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isEdgeBlocked(Edge edge) {
		for (final Lane lane : edge.lanes) {
			if (!lane.isBlocked) {
				return false;
			}
		}
		return true;
	}

	public static void updateRoadBlockInfoForVehicle(Vehicle vehicle) {
		double examinedDist = 0;
		int indexLegOnRouteBeingChecked = vehicle.indexLegOnRoute;
		while (indexLegOnRouteBeingChecked <= (vehicle.routeLegs.size() - 1)) {
			final Edge edgeBeingChecked = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
			if (isEdgeBlocked(edgeBeingChecked)) {
				vehicle.isRoadBlockedAhead = true;
				return;
			}
			examinedDist += vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge.length;
			// Proceeds to the next leg on route if look-ahead distance is not exhausted
			if (((examinedDist - vehicle.headPosition) < Settings.lookAheadDistance)
					&& (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1))) {
				indexLegOnRouteBeingChecked++;
			} else {
				break;
			}
		}
		vehicle.isRoadBlockedAhead = false;
	}

	/**
	 * Get the last vehicle in a given lane. The vehicle is the closest vehicle
	 * to the start node of this lane's edge. In other words, this vehicle will
	 * be the last vehicle to leave the lane.
	 *
	 */
	public static Vehicle getLastVehicle(final Lane lane) {
		if (lane.vehicles.size() > 0) {
			return lane.vehicles.get(lane.vehicles.size() - 1);
		} else {
			return null;
		}
	}

	/**
	 * Get the braking distance for stopping a vehicle completely.
	 */
	public static double getBrakingDistance(final Vehicle vehicle) {
		return (vehicle.speed * vehicle.speed) / 2.0 / vehicle.driverProfile.IDM_b;
	}

	/**
	 * Set or cancel priority lanes within a certain distance.
	 */
	public static void setPriorityLanes(final Vehicle vehicle, final boolean isPriority) {
		double examinedDist = 0;
		int indexLegOnRoute = vehicle.indexLegOnRoute;
		int laneNumber = vehicle.lane.laneNumber;
		Edge edge = vehicle.lane.edge;
		while ((examinedDist < Settings.lookAheadDistance) && (indexLegOnRoute < (vehicle.routeLegs.size() - 1))) {
			final Edge targetEdge = vehicle.routeLegs.get(indexLegOnRoute).edge;
			if (!isPriority) {
				// Cancel priority status for all the lanes in the edge
				for (Lane lane : targetEdge.lanes) {
					lane.isPriority = false;
				}
			} else {
				// Set priority for the lane that will be used by the vehicle
				laneNumber = RoadUtil.getLaneNumberForTargetEdge(targetEdge, edge, laneNumber);
				targetEdge.lanes.get(laneNumber).isPriority = true;
			}
			examinedDist += targetEdge.length;
			indexLegOnRoute++;
			edge = targetEdge;
		}
	}

	public VehicleUtil() {
	}

	/**
	 * Find impeding object based on a certain factor. Impeding object's head
	 * position is ahead of the given vehicle's head position. Impeding object
	 * may not be in the same lane of the given vehicle when this method is
	 * called during lane-changing.
	 *
	 * @param vehicle
	 *            The vehicle whose route is used in the search
	 * @param indexLegOnRouteBeingChecked
	 *            Indicate the route leg from which search is started
	 * @param laneNumber
	 *            Indicate the lane where impeding object is searched. If the
	 *            search expands to multiple edges, only the lanes with the same
	 *            lane number are considered
	 * @param impedingObj
	 *            The virtual vehicle object that stores the properties of the
	 *            impeding object
	 * @param factor
	 *            Type of the impeding object, e.g., traffic lights
	 */
	public void updateImpedingObject(final Vehicle vehicle, int indexLegOnRouteBeingChecked, final int laneNumber,
			final Vehicle impedingObj, final SlowdownFactor factor) {
		double examinedDist = 0;
		impedingObj.headPosition = -1; // Initialize front vehicle's position.
		while ((impedingObj.headPosition < 0) && (indexLegOnRouteBeingChecked <= (vehicle.routeLegs.size() - 1))) {
			if (factor == SlowdownFactor.FRONT) {
				updateImpedingObject_Front(vehicle, examinedDist, indexLegOnRouteBeingChecked, laneNumber, impedingObj);
			} else if (factor == SlowdownFactor.TRAM) {
				updateImpedingObject_Tram(vehicle, examinedDist,
						vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge, impedingObj);
			} else if (factor == SlowdownFactor.LIGHT) {
				updateImpedingObject_Light(vehicle, examinedDist,
						vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge, impedingObj);
			} else if (factor == SlowdownFactor.CONFLICT) {
				updateImpedingObject_Conflict(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			} else if (factor == SlowdownFactor.LANEBLOCK) {
				updateImpedingObject_LaneBlock(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			} else if (factor == SlowdownFactor.TURN) {
				updateImpedingObject_Turn(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			} else if (factor == SlowdownFactor.PRIORITY_VEHICLE) {
				updateImpedingObject_PriorityVehicle(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			}
			if (impedingObj.headPosition < 0) {
				examinedDist += vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge.length;
				// Proceeds to the next leg on route if look-ahead distance is
				// not exhausted
				if (((examinedDist - vehicle.headPosition) < Settings.lookAheadDistance)
						&& (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1))) {
					indexLegOnRouteBeingChecked++;
				} else {
					// If no impeding object is found within look-ahead
					// distance, returns a virtual one that moves fast at long
					// distance
					impedingObj.speed = 100;
					impedingObj.headPosition = vehicle.headPosition + 10000;
					impedingObj.type = VehicleType.VIRTUAL_STATIC;
					impedingObj.length = 0;
					break;
				}
			}
		}

		// Make sure there is a virtual impeding object
		if (impedingObj.headPosition < 0) {
			impedingObj.speed = 100;
			impedingObj.headPosition = vehicle.headPosition + 10000;
			impedingObj.type = VehicleType.VIRTUAL_STATIC;
			impedingObj.length = 0;
		}

	}

	/**
	 * Find impeding object that is a vehicle traveling towards an upcoming
	 * intersection on the route of the given vehicle. If there are multiple
	 * conflicting vehicles, the impeding object is the vehicle that will arrive
	 * the intersection earlier than other conflicting vehicles.
	 *
	 */
	void updateImpedingObject_Conflict(final Vehicle vehicle, final double examinedDist,
			final int indexLegOnRouteBeingChecked, final Vehicle slowdownObj) {
		/*
		 * Search for traffic from possible conflicting approaches at the end of
		 * the current edge.
		 */
		if (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1)) {
			final Edge targetEdge = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
			final Edge nextEdge = vehicle.routeLegs.get(indexLegOnRouteBeingChecked + 1).edge;

			double earliestTime = 10000;

			// Gets the earliest time that conflicting traffic arrives at
			// intersection
			for (final Edge e : RoadUtil.getConflictingEdges(targetEdge, nextEdge)) {
				for (final Lane lane : e.lanes) {
					if (lane.vehicles.size() > 0) {
						final Vehicle firstV = lane.vehicles.get(0);
						if (firstV.speed > 0) {
							final double arrivalTime = (e.length - firstV.headPosition) / firstV.speed;
							if (arrivalTime < earliestTime) {
								earliestTime = arrivalTime;
							}
						}
					}
				}
			}

			// Give way to another road being used by priority vehicles
			boolean isGiveWayToPriorityVehicle = false;
			if (!vehicle.lane.isPriority) {
				for (final Edge e : targetEdge.endNode.inwardEdges) {
					if (e == targetEdge) {
						continue;
					}
					if (vehicle.lane.edge.name.equals(e.name) && (vehicle.lane.edge.type == e.type)) {
						continue;
					}
					if (RoadUtil.isEdgeOnPathOfPriorityVehicle(e)) {
						isGiveWayToPriorityVehicle = true;
						vehicle.isAffectedByPriorityVehicle = true;
						break;
					}
				}
			}

			/*
			 * If the nearest time that a conflicting vehicle arrives at the
			 * intersection is too close, this vehicle must stop at the
			 * intersection to prevent collision.
			 */
			if ((!vehicle.lane.isPriority && (earliestTime < Settings.minTimeSafeToCrossIntersection))
					|| isGiveWayToPriorityVehicle) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0) - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		}
	}

	/**
	 * Find the front vehicle which impedes the movement of the given vehicle.
	 * Note that the front vehicle can be in a different lane from the given
	 * vehicle. For example, if this method is called during lane-changing, the
	 * front vehicle can be in a lane on the left or right of the current lane
	 * of the given vehicle.
	 */
	void updateImpedingObject_Front(final Vehicle vehicle, final double examinedDist,
			final int indexLegOnRouteBeingChecked, int laneNumber, final Vehicle slowdownObj) {

		final Edge edgeBeingChecked = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
		// Adjust lane number based on continuity of lane
		int laneNumberBeingChecked = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
				laneNumber);

		// Returns the closest impeding object, whose head position is in front of the given vehicle.
		final Lane laneBeingChecked = edgeBeingChecked.lanes.get(laneNumberBeingChecked);
		final Vehicle frontVehicle = getFrontVehicleInTargetLane(vehicle, laneBeingChecked, examinedDist);
		if (frontVehicle != null) {
			slowdownObj.speed = frontVehicle.speed;
			slowdownObj.headPosition = examinedDist + frontVehicle.headPosition;
			slowdownObj.type = frontVehicle.type;
			slowdownObj.length = frontVehicle.length;
			// Do not cross the intersection that is immediately behind the front vehicle if the front vehicle is too slow and is too close to the intersection
			if (RoadUtil.hasIntersectionAtEdgeStart(frontVehicle.lane.edge)
					&& (vehicle.lane.edge != frontVehicle.lane.edge)
					&& (slowdownObj.speed < Settings.intersectionSpeedThresholdOfFront)
					&& (frontVehicle.headPosition - frontVehicle.length <= vehicle.driverProfile.IDM_s0
							+ vehicle.length)//The current vehicle cannot stop between the intersection and the front vehicle due to limited space
					&& (frontVehicle.headPosition - frontVehicle.length >= 0)//Only consider the situation where front vehicle has passed the intersection in whole
					&& (getBrakingDistance(vehicle) <= (examinedDist - vehicle.headPosition))) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = examinedDist - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		} else if (laneBeingChecked.endPositionOfLatestVehicleLeftThisWorker < laneBeingChecked.edge.length) {
			// A front vehicle may be running on a different worker. Therefore we check vehicle position sent from other workers.
			slowdownObj.speed = laneBeingChecked.speedOfLatestVehicleLeftThisWorker;
			slowdownObj.headPosition = examinedDist + laneBeingChecked.endPositionOfLatestVehicleLeftThisWorker;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.length = 0;
		}

	}

	/**
	 * Find impeding object caused by priority vehicle, e.g. ambulance.
	 */
	void updateImpedingObject_PriorityVehicle(final Vehicle vehicle, final double examinedDist,
			final int indexLegOnRouteBeingChecked, final Vehicle slowdownObj) {
		if (vehicle.type == VehicleType.PRIORITY) {
			return;
		}

		final Edge edgeBeingChecked = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
		// Adjust lane number based on continuity of lane
		int laneNumber = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
				vehicle.lane.laneNumber);

		// Stop vehicle if the emergency strategy requires non-priority vehicles to pull off
		for (int i = laneNumber + 1; i < edgeBeingChecked.lanes.size(); i++) {
			if (edgeBeingChecked.lanes.get(i).isPriority
					&& (Settings.emergencyStrategy == EmergencyStrategy.NonEmergencyPullOffToRoadside)) {
				final double brakingDist = VehicleUtil.getBrakingDistance(vehicle);
				slowdownObj.headPosition = vehicle.headPosition + brakingDist;
				slowdownObj.length = 0;
				slowdownObj.speed = 0;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			}
		}

		// Set flag that this vehicle is affected by priority vehicle
		if ((vehicle.type != VehicleType.PRIORITY) && RoadUtil.isEdgeOnPathOfPriorityVehicle(edgeBeingChecked)) {
			vehicle.isAffectedByPriorityVehicle = true;
		}
	}

	/**
	 * Find impeding object that is a blocked lane.
	 */
	void updateImpedingObject_LaneBlock(final Vehicle vehicle, final double examinedDist,
			final int indexLegOnRouteBeingChecked, final Vehicle slowdownObj) {
		if (indexLegOnRouteBeingChecked < vehicle.routeLegs.size()) {
			final Edge edgeBeingChecked = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
			// Adjust lane number based on continuity of lane
			int laneNumber = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
					vehicle.lane.laneNumber);
			final Lane targetLane = edgeBeingChecked.lanes.get(laneNumber);
			if (targetLane.isBlocked) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (examinedDist
						+ vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge.length + vehicle.driverProfile.IDM_s0)
						- 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		}
	}

	/**
	 * Find impeding object that is a traffic light.
	 */
	void updateImpedingObject_Light(final Vehicle vehicle, final double examinedDist, final Edge targetEdge,
			final Vehicle slowdownObj) {
		if (vehicle.type == VehicleType.PRIORITY) {
			// Priority vehicle ignores any traffic light
			return;
		}
		/*
		 * Checks traffic light at the end of the target lane's edge.
		 */
		if (Settings.trafficLightTiming != TrafficLightTiming.NONE) {

			// Ignore other lights if the vehicle already passed one of the lights
			// in the same group
			if (targetEdge.endNode.idLightNodeGroup == vehicle.idLightGroupPassed) {
				return;
			}

			// Ignore yellow lights if the vehicle is making a turn
			if ((vehicle.edgeBeforeTurnLeft == vehicle.lane.edge || vehicle.edgeBeforeTurnRight == vehicle.lane.edge)
					&& (targetEdge.lightColor == LightColor.GYR_Y)) {
				return;
			}

			// Flags the event that vehicle is within certain distance to light
			if (targetEdge.endNode.light && (((examinedDist + targetEdge.length)
					- vehicle.headPosition) < Settings.trafficLightDetectionDistance)) {
				targetEdge.isDetectedVehicleForLight = true;
			}

			boolean stopAtLight = false;
			if ((targetEdge.lightColor == LightColor.GYR_R) || (targetEdge.lightColor == LightColor.KEEP_RED)) {
				stopAtLight = true;
			} else if (targetEdge.lightColor == LightColor.GYR_Y) {
				if (getBrakingDistance(vehicle) <= ((examinedDist + targetEdge.length) - vehicle.headPosition)) {
					stopAtLight = true;
				}
			}

			if (stopAtLight) {
				slowdownObj.speed = 0;
				// The position may be put further out from edge end to avoid car stop before
				// entering an edge.
				if (targetEdge.length <= vehicle.driverProfile.IDM_s0) {
					slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0)
							- 0.00001;
				} else {
					slowdownObj.headPosition = examinedDist + targetEdge.length;
				}
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		}
	}

	/**
	 * Find impeding object related to tram stops. A tram stop is located at the
	 * end of an edge. The edge has a count-down timer, which is triggered when
	 * a tram approaches the tram stop. If the count-down is in process, the
	 * tram cannot pass the tram stop.
	 * 
	 * Tram stop also affects other vehicles that move in parallel lanes/edges
	 * besides tram track. Note that other vehicles must stop behind tram, as
	 * required by road rule.
	 * 
	 * In OpenStreetMap data, tram tracks are separated from other roads. Hence
	 * there are many roads parallel to tram tracks. The tram edges that are
	 * parallel to an edge are identified during pre-processing.
	 */
	void updateImpedingObject_Tram(final Vehicle vehicle, final double examinedDist, final Edge targetEdge,
			final Vehicle slowdownObj) {

		if ((vehicle.type == VehicleType.TRAM) && (targetEdge.timeTramStopping > 0)) {
			slowdownObj.speed = 0;
			slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0) - 0.00001;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.length = 0;
			return;
		}

		if ((vehicle.type != VehicleType.TRAM) && Settings.isAllowTramRule) {

			if (targetEdge.timeTramStopping > 0) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (((examinedDist + targetEdge.length) - VehicleType.TRAM.length)
						+ vehicle.driverProfile.IDM_s0) - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				return;
			}

			else {
				final Edge parallelTramEdgeWithTramStop = targetEdge.parallelTramEdgeWithTramStop;
				if ((parallelTramEdgeWithTramStop != null) && (parallelTramEdgeWithTramStop.timeTramStopping > 0)) {
					slowdownObj.speed = 0;
					slowdownObj.headPosition = (((examinedDist + targetEdge.distToTramStop) - VehicleType.TRAM.length)
							+ vehicle.driverProfile.IDM_s0) - 0.00001;
					slowdownObj.type = VehicleType.VIRTUAL_STATIC;
					slowdownObj.length = 0;
					return;
				}
			}
		}

	}

	/**
	 * Find impeding object that is an intersection where the given vehicle
	 * needs to make a turn, including a U-turn. The speed and position of the
	 * impeding object will allow the given vehicle to slow down and pass the
	 * intersection.
	 *
	 *
	 */
	void updateImpedingObject_Turn(final Vehicle vehicle, final double examinedDist,
			final int indexLegOnRouteBeingChecked, final Vehicle slowdownObj) {
		Edge edgeBeingChecked = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
		slowdownObj.headPosition = examinedDist + edgeBeingChecked.length + (VehicleType.VIRTUAL_SLOW.maxSpeed * 3);//The virtual object is a few seconds ahead 
		slowdownObj.type = VehicleType.VIRTUAL_SLOW;
		slowdownObj.speed = VehicleType.VIRTUAL_SLOW.maxSpeed;
		slowdownObj.length = 0;
	}

}

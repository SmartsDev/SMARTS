package traffic.vehicle.carfollow;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.Movement;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadUtil;
import traffic.vehicle.*;

/**
 * This class computes ideal acceleration based on various types of impeding
 * objects, such as traffic lights, front cars, etc. As safety is of priority,
 * the lowest acceleration based on all the factors is the final acceleration
 * value. For each factor, the acceleration is computed based on IDM model.
 *
 */
public class IDM {

    private Settings settings;

	public IDM(Settings settings) {
	    this.settings = settings;
    }

	/**
	 * Calculates the acceleration of vehicle based on its relation to an
	 * impeding object.
	 *
	 */
	public static double computeAcceleration(final Vehicle vehicle, final ImpedingObject impedingObject) {
		/*
		 * Actual bumper-to-bumper distance from the end of the front vehicle to
		 * the head of this vehicle. Value is in meters.
		 */
		final double s = impedingObject.headPosition - impedingObject.length - vehicle.headPosition;

		/*
		 * Current speed of this vehicle
		 */
		final double v = vehicle.speed;

		/*
		 * Difference between the speed of this vehicle and the speed of the
		 * front vehicle
		 */
		final double dV = vehicle.speed - impedingObject.speed;

		/*
		 * Desired dynamic distance
		 */
		final double sS = vehicle.driverProfile.IDM_s0 + (v * vehicle.driverProfile.IDM_T * vehicle.getHeadWayMultiplier())
				+ ((v * dV) / (2 * Math.sqrt(vehicle.driverProfile.IDM_a * vehicle.driverProfile.IDM_b)));
		/*
		 * Desired speed
		 */
		// Must be within vehicle capability and road speed limit
		double v0 = vehicle.lane.edge.freeFlowSpeed;
		if (v0 > vehicle.type.maxSpeed) {
			v0 = vehicle.type.maxSpeed;
		}

		/*
		 * Acceleration exponent
		 */
		final double delta = 4;
		/*
		 * Acceleration
		 */
		final double acceleration = vehicle.driverProfile.IDM_a * (1 - Math.pow(v / v0, delta) - Math.pow(sS / s, 2));

		return acceleration;
	}

	/**
	 * Gets the potential acceleration of vehicle based on a slow-down factor.
	 * First, the impeding object for this factor is found. Next, the
	 * acceleration is computed based on the impeding object.
	 */
	public double computeAccelerationWithImpedingObject(final Vehicle vehicle, final ImpedingObject impedingObject,
			final Lane targetLane, final SlowdownFactor factor) {
		updateImpedingObject(vehicle, vehicle.indexLegOnRoute, targetLane.laneNumber, impedingObject,
				factor);
		return computeAcceleration(vehicle, impedingObject);
	}

	/**
	 * Gets the lower acceleration between two values. Also updates the
	 * information about impeding object corresponding to the lowest
	 * acceleration.
	 *
	 *
	 */
	double getLowerAccelerationAndUpdateSlowdownFactor(final Vehicle vehicle, final ImpedingObject impedingObject,
			final double acc1, final double acc2) {
		if (acc1 > acc2) {
			vehicle.distToImpedingObject = impedingObject.headPosition - impedingObject.length - vehicle.headPosition;
			vehicle.spdOfImpedingObject = impedingObject.speed;
			vehicle.setRecentSlowDownFactor(impedingObject.factor);
			return acc2;
		} else {
			return acc1;
		}
	}

	/**
	 * Accelerations based on certain slow-down factors are computed. The lowest
	 * value is the final potential acceleration as safety is of priority.
	 *
	 */
	public double updateBasedOnAllFactors(final Vehicle vehicle) {
		final ImpedingObject impedingObject = new ImpedingObject();
		double lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, 10000, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.FRONT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.TRAM));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.LIGHT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.CONFLICT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.TURN));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.LANEBLOCK));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.PRIORITY_VEHICLE));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.WAITING_VEHICLE));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration, computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.QUEUE_SPILLBACK));
		return lowestAcceleration;
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
	 * @param impObj
	 *            The virtual vehicle object that stores the properties of the
	 *            impeding object
	 * @param factor
	 *            Type of the impeding object, e.g., traffic lights
	 */
	public void updateImpedingObject(final Vehicle vehicle, int indexLegOnRouteBeingChecked, final int laneNumber,
									 final ImpedingObject impObj, final SlowdownFactor factor) {
		double examinedDist = 0;
		impObj.headPosition = -1; // Initialize front vehicle's position.
		while ((impObj.headPosition < 0) && (indexLegOnRouteBeingChecked <= (vehicle.getRouteLegCount() - 1))) {
			if (factor == SlowdownFactor.FRONT) {
				updateImpedingObject_Front(vehicle, examinedDist, indexLegOnRouteBeingChecked, laneNumber, impObj);
			} else if (factor == SlowdownFactor.TRAM) {
				updateImpedingObject_Tram(vehicle, examinedDist,
						vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked), impObj);
			} else if (factor == SlowdownFactor.LIGHT) {
				updateImpedingObject_Light(vehicle, examinedDist,
						vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked), impObj);
			} else if (factor == SlowdownFactor.CONFLICT) {
				updateImpedingObject_Conflict(vehicle, examinedDist, indexLegOnRouteBeingChecked, impObj);
			} else if (factor == SlowdownFactor.LANEBLOCK) {
				updateImpedingObject_LaneBlock(vehicle, examinedDist, indexLegOnRouteBeingChecked, impObj);
			} else if (factor == SlowdownFactor.TURN) {
				updateImpedingObject_Turn(vehicle, examinedDist, indexLegOnRouteBeingChecked, impObj);
			} else if (factor == SlowdownFactor.PRIORITY_VEHICLE) {
				updateImpedingObject_PriorityVehicle(vehicle, examinedDist, indexLegOnRouteBeingChecked, impObj);
			} else if (factor == SlowdownFactor.WAITING_VEHICLE) {
				updateImpedingObject_WaitingVehicle(vehicle, examinedDist, indexLegOnRouteBeingChecked, impObj);
			}else if (factor == SlowdownFactor.QUEUE_SPILLBACK) {
				updateImpedingObject_QueueSpillback(vehicle, examinedDist, indexLegOnRouteBeingChecked, impObj);
			}
			if (impObj.headPosition < 0) {
				examinedDist += vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked).length;
				// Proceeds to the next leg on route if look-ahead distance is
				// not exhausted
				if (((examinedDist - vehicle.headPosition) < settings.lookAheadDistance)
						&& (indexLegOnRouteBeingChecked < (vehicle.getRouteLegCount() - 1))) {
					indexLegOnRouteBeingChecked++;
				} else {
					// If no impeding object is found within look-ahead
					// distance, returns a virtual one that moves fast at long
					// distance
					impObj.speed = 100;
					impObj.headPosition = vehicle.headPosition + 10000;
					impObj.type = VehicleType.VIRTUAL_STATIC;
					impObj.length = 0;
					impObj.factor = SlowdownFactor.FRONT;
					break;
				}
			}
		}

		// Make sure there is a virtual impeding object
		if (impObj.headPosition < 0) {
			impObj.speed = 100;
			impObj.headPosition = vehicle.headPosition + 10000;
			impObj.type = VehicleType.VIRTUAL_STATIC;
			impObj.length = 0;
			impObj.factor = SlowdownFactor.FRONT;
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
									   final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		/*
		 * Search for traffic from possible conflicting approaches at the end of
		 * the current edge.
		 */
		if (indexLegOnRouteBeingChecked < (vehicle.getRouteLegCount() - 1)) {
			final Edge targetEdge = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
			final Edge nextEdge = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked + 1);

			double earliestTime = 10000;

			// Gets the earliest time that conflicting traffic arrives at
			// intersection
			for (final Edge e : RoadUtil.getConflictingEdges(targetEdge, nextEdge, settings.isDriveOnLeft)) {
				for (final Lane lane : e.getLanes()) {
					if (lane.getVehicleCount() > 0) {
						final Vehicle firstV = lane.getFrontVehicleInLane();
						if (firstV.speed > 0) {
							final double arrivalTime = (e.length - e.getEndIntersectionSize() - firstV.headPosition) / firstV.speed;
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
					if (e.isEdgeOnPathOfPriorityVehicle()) {
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
			double pos = (examinedDist + targetEdge.length - targetEdge.getEndIntersectionSize() + vehicle.driverProfile.IDM_s0);
			if(pos < vehicle.headPosition) {
				return;
			}
			if ((!vehicle.lane.isPriority && (earliestTime < settings.minTimeSafeToCrossIntersection))
					|| isGiveWayToPriorityVehicle) {
				slowdownObj.speed = 0;
				//slowdownObj.headPosition = (examinedDist + targetEdge.length - targetEdge.getEndIntersectionSize() + vehicle.driverProfile.IDM_s0) - 0.00001;
				slowdownObj.headPosition = pos;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				slowdownObj.factor = SlowdownFactor.CONFLICT;
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
									final int indexLegOnRouteBeingChecked, int laneNumber, final ImpedingObject slowdownObj) {

		final Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		// Adjust lane number based on continuity of lane
		int laneNumberBeingChecked = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
				laneNumber);

		// Returns the closest impeding object, whose head position is in front of the given vehicle.
		final Lane laneBeingChecked = edgeBeingChecked.getLane(laneNumberBeingChecked);
		final Vehicle frontVehicle = laneBeingChecked.getClosestFrontVehicleInLane(vehicle, examinedDist);

		boolean correctLane = true;
		if (frontVehicle != null){
			if ((vehicle.lane.edge != frontVehicle.lane.edge) ){
				correctLane = false;
			}
		}

		if ((frontVehicle != null) && correctLane) {
			slowdownObj.speed = frontVehicle.speed;
			slowdownObj.headPosition = examinedDist + frontVehicle.headPosition;
			slowdownObj.type = frontVehicle.type;
			slowdownObj.length = frontVehicle.length;
			slowdownObj.factor = SlowdownFactor.FRONT;
			// Do not cross the intersection that is immediately behind the front vehicle if the front vehicle is too slow and is too close to the intersection
			if (RoadUtil.hasIntersectionAtEdgeStart(frontVehicle.lane.edge)
					&& (vehicle.lane.edge != frontVehicle.lane.edge)
					&& (slowdownObj.speed < settings.intersectionSpeedThresholdOfFront)
					&& (frontVehicle.headPosition - frontVehicle.length <= vehicle.driverProfile.IDM_s0 + vehicle.length)//The current vehicle cannot stop between the intersection and the front vehicle due to limited space
					&& (frontVehicle.headPosition - frontVehicle.length >= 0)//Only consider the situation where front vehicle has passed the intersection in whole
					&& (VehicleUtil.getBrakingDistance(vehicle) <= (examinedDist - vehicle.headPosition))) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = examinedDist - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				slowdownObj.factor = SlowdownFactor.FRONT;
			}
		} else if (laneBeingChecked.endPositionOfLatestVehicleLeftThisWorker < laneBeingChecked.edge.length) {
			// A front vehicle may be running on a different worker. Therefore we check vehicle position sent from other workers.
			slowdownObj.speed = laneBeingChecked.speedOfLatestVehicleLeftThisWorker;
			slowdownObj.headPosition = examinedDist + laneBeingChecked.endPositionOfLatestVehicleLeftThisWorker;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.length = 0;
			slowdownObj.factor = SlowdownFactor.FRONT;
		}

	}

	/**
	 * Find impeding object caused by priority vehicle, e.g. ambulance.
	 */
	void updateImpedingObject_PriorityVehicle(final Vehicle vehicle, final double examinedDist,
											  final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		if (vehicle.type == VehicleType.PRIORITY) {
			return;
		}

		final Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		// Adjust lane number based on continuity of lane
		int laneNumber = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
				vehicle.lane.laneNumber);

		// Stop vehicle if the emergency strategy requires non-priority vehicles to pull off
		for (int i = laneNumber + 1; i < edgeBeingChecked.getLaneCount(); i++) {
			if (edgeBeingChecked.getLane(i).isPriority
					&& (settings.emergencyStrategy == EmergencyStrategy.NonEmergencyPullOffToRoadside)) {
				final double brakingDist = VehicleUtil.getBrakingDistance(vehicle);
				slowdownObj.headPosition = vehicle.headPosition + brakingDist;
				slowdownObj.length = 0;
				slowdownObj.speed = 0;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.factor = SlowdownFactor.PRIORITY_VEHICLE;
			}
		}

		// Set flag that this vehicle is affected by priority vehicle
		if ((vehicle.type != VehicleType.PRIORITY) && edgeBeingChecked.isEdgeOnPathOfPriorityVehicle()) {
			vehicle.isAffectedByPriorityVehicle = true;
		}
	}

	/**
	 * Find impeding object that is a blocked lane.
	 */
	void updateImpedingObject_LaneBlock(final Vehicle vehicle, final double examinedDist,
										final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		if (indexLegOnRouteBeingChecked < vehicle.getRouteLegCount()) {
			final Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
			// Adjust lane number based on continuity of lane
			int laneNumber = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
					vehicle.lane.laneNumber);
			final Lane targetLane = edgeBeingChecked.getLane(laneNumber);
			if (targetLane.isBlocked) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (examinedDist
						+ vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked).length + vehicle.driverProfile.IDM_s0)
						- 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				slowdownObj.factor = SlowdownFactor.LANEBLOCK;
			}
		}
	}

	/**
	 * Find impeding object that is a traffic light.
	 */
	void updateImpedingObject_Light(final Vehicle vehicle, final double examinedDist, final Edge targetEdge,
									final ImpedingObject slowdownObj) {
		/*
		 * Checks traffic light at the end of the target lane's edge.
		 */
		if (settings.trafficLightTiming != TrafficLightTiming.NONE) {
			if (vehicle.type == VehicleType.PRIORITY) {
				// Priority vehicle ignores any traffic light
				return;
			}
		    if(vehicle.headPosition > targetEdge.length - targetEdge.getEndIntersectionSize()){
		        return;
            }
			Movement movement = vehicle.getCurrentMovement();
			if (vehicle.edgeBeforeTurnLeft == vehicle.lane.edge || vehicle.edgeBeforeTurnRight == vehicle.lane.edge) {
				LightColor movementLight = targetEdge.getMovementLight(movement);
				if(movementLight == LightColor.GYR_Y) {
					return;
				}

			}
			// Ignore other lights if vehicle already passed one of the lights
			// in the same group
			if (targetEdge.endNode.idLightNodeGroup == vehicle.idLightGroupPassed) {
				return;
			}

			// Flags the event that vehicle is within certain distance to light
			if ((((examinedDist + targetEdge.length)
					- vehicle.headPosition) < settings.trafficLightDetectionDistance)) {
				targetEdge.isDetectedVehicleForLight = true;
			}

			boolean stopAtLight = false;
			if(movement != null) {
				LightColor movementLight = targetEdge.getMovementLight(movement);
				if ((movementLight == LightColor.GYR_R) || (movementLight == LightColor.KEEP_RED)) {
					stopAtLight = true;
				} else if (movementLight == LightColor.GYR_Y) {
					if (VehicleUtil.getBrakingDistance(vehicle) <= ((examinedDist + targetEdge.length) - vehicle.headPosition)) {
						stopAtLight = true;
					}
				}
			}

			if (stopAtLight) {
				slowdownObj.speed = 0;
				//TODO put a fix where edge lengths are shorter
				slowdownObj.headPosition = examinedDist + targetEdge.length - (targetEdge.getEndIntersectionSize() - vehicle.driverProfile.IDM_s0);
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				//vehicle keeps the gap of IDM_S0 but here vehicles should go to the stop line
				slowdownObj.factor = SlowdownFactor.LIGHT;
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
								   final ImpedingObject slowdownObj) {

		if ((vehicle.type == VehicleType.TRAM) && (targetEdge.timeTramStopping > 0)) {
			slowdownObj.speed = 0;
			slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0) - 0.00001;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.length = 0;
			slowdownObj.factor = SlowdownFactor.TRAM;
			return;
		}

		if ((vehicle.type != VehicleType.TRAM) && settings.isAllowTramRule) {

			if (targetEdge.timeTramStopping > 0) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (((examinedDist + targetEdge.length) - VehicleType.TRAM.length)
						+ vehicle.driverProfile.IDM_s0) - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				slowdownObj.factor = SlowdownFactor.TRAM;
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
					slowdownObj.factor = SlowdownFactor.TRAM;
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
								   final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		if (edgeBeingChecked == vehicle.edgeBeforeTurnLeft || edgeBeingChecked == vehicle.edgeBeforeTurnRight) {
			// Block vehicle if vehicle's lane cannot be used for turning
			boolean isNeedToBlock = false; //TODO = VehicleUtil.isNeedLaneChangeForTurn(edgeBeingChecked, vehicle, settings.isDriveOnLeft);
			if (isNeedToBlock) {
				slowdownObj.headPosition = examinedDist + edgeBeingChecked.getBeforeTurnLaneChangePos(vehicle, settings.isDriveOnLeft)
						+ vehicle.driverProfile.IDM_s0; //TODO this should be properly implemented, may not work for realworld networks
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.speed = 0;
				slowdownObj.length = 0;
				slowdownObj.factor = SlowdownFactor.TURN;
			} else {
				slowdownObj.headPosition = examinedDist + edgeBeingChecked.length
						+ (VehicleType.CAR.maxSpeed * 3);//The virtual object is a few seconds ahead
				slowdownObj.type = VehicleType.CAR;
				slowdownObj.speed = VehicleType.CAR.maxSpeed;
				slowdownObj.length = 0;
				slowdownObj.factor = SlowdownFactor.TURN;
			}
		}
	}

	void updateImpedingObject_WaitingVehicle(final Vehicle vehicle, final double examinedDist,
											  final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj){
		Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		if(edgeBeingChecked.hasAWaitingVehicle(vehicle)){
			double giveChancePos = edgeBeingChecked.getLaneChangeGiveChancePos();
			slowdownObj.headPosition = examinedDist + giveChancePos + vehicle.driverProfile.IDM_s0;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.speed = 0;
			slowdownObj.length = 0;
			slowdownObj.factor = SlowdownFactor.WAITING_VEHICLE;
		}
	}

	void updateImpedingObject_QueueSpillback(final Vehicle vehicle, final double examinedDist,
											 final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj){
		Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		if(vehicle.getDecision() != null){
			Vehicle.IntersectionDecision decision = vehicle.getDecision();
			Edge current = vehicle.getCurrentEdge();
			if(current == decision.getStartLane().edge && edgeBeingChecked == decision.getEndLane().edge){
				if(!edgeBeingChecked.hasSpaceForAvehicleInBack(decision.getEndLane(), vehicle, settings.minTimeSafeToCrossIntersection) && vehicle.isNotWithinIntersections()){
				//if ( !edgeBeingChecked.hasSpaceInEndOfAllLane(decision.getEndLane())){
					slowdownObj.headPosition = examinedDist - current.getEndIntersectionSize() + vehicle.driverProfile.IDM_s0;
					slowdownObj.type = VehicleType.VIRTUAL_STATIC;
					slowdownObj.speed = 0;
//					slowdownObj.length = current.getEndIntersectionSize() + edgeBeingChecked.getStartIntersectionSize() - vehicle.driverProfile.IDM_s0;
					slowdownObj.length = 0;
					slowdownObj.factor = SlowdownFactor.QUEUE_SPILLBACK;
				}
			}
		}
	}
}

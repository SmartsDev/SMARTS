package processor.worker;

import java.util.ArrayList;
import java.util.Collections;

import common.Settings;
import processor.communication.message.SerializableTrajectoryPoint;
import traffic.TrafficNetwork;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;
import traffic.vehicle.CarFollow;
import traffic.vehicle.LaneChange;
import traffic.vehicle.LaneChangeDirection;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

/**
 * This class performs simulation at worker. The simulation includes a sequence
 * of tasks, such as moving vehicles forward based on their speed and
 * surrounding environment, making lane changes, updating traffic lights, etc.
 *
 */
public class Simulation {
	TrafficNetwork trafficNetwork;
	ArrayList<Fellow> connectedFellows;
	ArrayList<Vehicle> oneStepData_vehiclesReachedFellowWorker = new ArrayList<>();
	ArrayList<Vehicle> oneStepData_foregroundVehiclesReachedDestination = new ArrayList<>();
	ArrayList<Vehicle> oneStepData_allVehiclesReachedDestination = new ArrayList<>();
	VehicleUtil vehicleUtil = new VehicleUtil();
	LaneChange laneChange = new LaneChange(vehicleUtil);
	CarFollow carFollow = new CarFollow(vehicleUtil);

	public Simulation(final TrafficNetwork trafficNetwork, final ArrayList<Fellow> connectedFellows) {
		this.trafficNetwork = trafficNetwork;
		this.connectedFellows = connectedFellows;
	}

	void blockTramAtTramStop() {
		final double lookAheadDist = Settings.lookAheadDistance;
		for (int i = 0; i < trafficNetwork.vehicles.size(); i++) {
			final Vehicle vehicle = trafficNetwork.vehicles.get(i);
			if (!vehicle.active || (vehicle.lane == null)) {
				continue;
			}

			if (vehicle.type == VehicleType.TRAM) {
				final double brakingDist = VehicleUtil.getBrakingDistance(vehicle);
				double examinedDist = 0;
				for (int j = vehicle.indexLegOnRoute; j < vehicle.routeLegs.size(); j++) {
					final Edge edge = vehicle.routeLegs.get(j).edge;
					examinedDist += edge.length;
					if (edge.endNode.tramStop && ((examinedDist - vehicle.headPosition) < (2 * brakingDist))
							&& ((examinedDist - vehicle.headPosition) > brakingDist) && (edge.timeNoTramStopping <= 0)
							&& (edge.timeTramStopping <= 0)) {
						edge.timeTramStopping = Settings.periodOfTrafficWaitForTramAtStop;
						break;
					}
					if ((examinedDist - vehicle.headPosition) > lookAheadDist) {
						break;
					}
				}
			}
		}
	}

	void clearOneStepData() {
		oneStepData_vehiclesReachedFellowWorker.clear();
		oneStepData_foregroundVehiclesReachedDestination.clear();
		oneStepData_allVehiclesReachedDestination.clear();
	}

	void makeLaneChange(final double timeNow) {
		for (int i = 0; i < trafficNetwork.vehicles.size(); i++) {
			final Vehicle vehicle = trafficNetwork.vehicles.get(i);

			if ((vehicle.lane == null) || !vehicle.active || (vehicle.type == VehicleType.TRAM)
					|| ((timeNow - vehicle.timeOfLastLaneChange) < vehicle.driverProfile.minLaneChangeTimeGap)) {
				continue;
			}

			LaneChangeDirection laneChangeDecision = LaneChangeDirection.SAME;
			laneChangeDecision = laneChange.decideLaneChange(vehicle);

			if (laneChangeDecision != LaneChangeDirection.SAME) {

				// Cancel priority lanes
				if (vehicle.type == VehicleType.PRIORITY) {
					VehicleUtil.setPriorityLanes(vehicle, false);
				}

				vehicle.timeOfLastLaneChange = timeNow;
				final Lane currentLane = vehicle.lane;
				Lane nextLane = null;
				if (laneChangeDecision == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
					nextLane = currentLane.edge.lanes.get(currentLane.laneNumber + 1);
				} else if (laneChangeDecision == LaneChangeDirection.TOWARDS_ROADSIDE) {
					nextLane = currentLane.edge.lanes.get(currentLane.laneNumber - 1);
				}
				currentLane.vehicles.remove(vehicle);
				nextLane.vehicles.add(vehicle);
				vehicle.lane = nextLane;
				Collections.sort(vehicle.lane.vehicles, trafficNetwork.vehiclePositionComparator);// Sort

				// Set priority lanes
				if (vehicle.type == VehicleType.PRIORITY) {
					VehicleUtil.setPriorityLanes(vehicle, true);
				}
			}
		}
	}

	ArrayList<Vehicle> moveVehicleForward(final double timeNow, final ArrayList<Edge> edges, Worker worker) {
		final ArrayList<Vehicle> vehicles = new ArrayList<>();
		for (final Edge edge : edges) {
			for (final Lane lane : edge.lanes) {
				for (final Vehicle vehicle : lane.vehicles) {

					if (!vehicle.active) {
						continue;
					}

					vehicles.add(vehicle);

					// Reset priority vehicle effect flag
					vehicle.isAffectedByPriorityVehicle = false;
					// Update information regarding turning
					VehicleUtil.findEdgeBeforeNextTurn(vehicle);

					/*
					 * Reset jam start time if vehicle is not in jam
					 */
					if (vehicle.speed > Settings.congestionSpeedThreshold) {
						vehicle.timeJamStart = timeNow;
					}

					// Check whether road is explicitly blocked on vehicle's route
					VehicleUtil.updateRoadBlockInfoForVehicle(vehicle);

					/*
					 * Re-route vehicle in certain situations
					 */
					if (Settings.isAllowReroute) {
						boolean reRoute = false;
						// Reroute happens if vehicle has moved too slowly for too long or the road is
						// blocked ahead
						if (vehicle.indexLegOnRoute < (vehicle.routeLegs.size() - 1)) {
							if ((timeNow - vehicle.timeJamStart) > vehicle.driverProfile.minRerouteTimeGap
									|| vehicle.isRoadBlockedAhead) {
								reRoute = true;
							}
						}

						if (reRoute) {
							// Cancel priority lanes
							if (vehicle.type == VehicleType.PRIORITY) {
								VehicleUtil.setPriorityLanes(vehicle, false);
							}

							// Reroute vehicle
							trafficNetwork.routingAlgorithm.reRoute(vehicle);

							// Reset jam start time
							vehicle.timeJamStart = timeNow;
							// Increment reroute count
							vehicle.numReRoute++;
							// Limit number of re-route for internal vehicle
							if ((vehicle.numReRoute > Settings.maxNumReRouteOfVehicle)) {
								oneStepData_allVehiclesReachedDestination.add(vehicle);
							}
						}
					}

					// Set priority lanes
					if (vehicle.type == VehicleType.PRIORITY) {
						VehicleUtil.setPriorityLanes(vehicle, true);
					}

					// Find impeding objects and compute acceleration based on the objects
					vehicle.acceleration = carFollow.computeAccelerationBasedOnImpedingObjects(vehicle);

				}

			}
		}

		// Update speed, position and travel time of vehicles
		for (Vehicle vehicle : vehicles) {
			// Update vehicle speed, which must be between 0 and free-flow speed
			vehicle.speed += vehicle.acceleration / Settings.numStepsPerSecond;
			if (vehicle.speed > vehicle.lane.edge.freeFlowSpeed) {
				vehicle.speed = vehicle.lane.edge.freeFlowSpeed;
			}
			if (vehicle.speed < 0) {
				vehicle.speed = 0;
			}
			// Vehicle cannot collide with its impeding object
			final double distToImpedingObjectAtNextStep = vehicle.distToImpedingObject
					+ ((vehicle.spdOfImpedingObject - vehicle.speed) / Settings.numStepsPerSecond);
			if (distToImpedingObjectAtNextStep < vehicle.driverProfile.IDM_s0) {
				vehicle.speed = 0;
				vehicle.acceleration = 0;
			}

			// Move forward
			vehicle.headPosition += vehicle.speed / Settings.numStepsPerSecond;
			vehicle.timeTravel = timeNow - vehicle.timeRouteStart;

		}

		return vehicles;
	}

	void moveVehicleToNextLink(final double timeNow, final ArrayList<Vehicle> vehiclesToCheck) {
		for (final Vehicle vehicle : vehiclesToCheck) {

			if (!vehicle.active) {
				continue;
			}
			double overshootDist = vehicle.headPosition - vehicle.lane.edge.length;

			if (overshootDist >= 0) {

				// Cancel priority lanes
				if (vehicle.type == VehicleType.PRIORITY) {
					VehicleUtil.setPriorityLanes(vehicle, false);
				}

				final Lane oldLane = vehicle.lane;

				while ((vehicle.indexLegOnRoute < vehicle.routeLegs.size()) && (overshootDist >= 0)) {
					// Update head position
					vehicle.headPosition -= vehicle.lane.edge.length;
					// Update route leg
					vehicle.indexLegOnRoute++;

					// Check whether vehicle finishes trip
					if (vehicle.active && (vehicle.indexLegOnRoute >= vehicle.routeLegs.size())) {
						oneStepData_allVehiclesReachedDestination.add(vehicle);

						if (vehicle.isForeground) {
							oneStepData_foregroundVehiclesReachedDestination.add(vehicle);
						}
						break;
					}
					// Locate the new lane of vehicle. If the specified lane does not exist (e.g.,
					// moving from primary road to secondary road), change to the one with the
					// highest lane number
					final RouteLeg nextLeg = vehicle.routeLegs.get(vehicle.indexLegOnRoute);
					final Edge nextEdge = nextLeg.edge;
					if (nextEdge.lanes.size() <= vehicle.lane.laneNumber) {
						vehicle.lane = nextEdge.lanes.get(nextEdge.lanes.size() - 1);
					} else {
						vehicle.lane = nextEdge.lanes.get(vehicle.lane.laneNumber);
					}
					// Remember the cluster of traffic lights
					if (nextEdge.startNode.idLightNodeGroup != 0) {
						vehicle.idLightGroupPassed = nextEdge.startNode.idLightNodeGroup;
					}
					// Update the overshoot distance of vehicle
					overshootDist -= nextEdge.length;
					// Check whether vehicle reaches fellow worker
					if (reachFellow(vehicle)) {
						oneStepData_vehiclesReachedFellowWorker.add(vehicle);
						break;
					}
					// Park vehicle as plan if vehicle remains on the same
					// worker
					if (nextLeg.stopover > 0) {
						trafficNetwork.parkOneVehicle(vehicle, false, timeNow);
						break;
					}
				}

				// Remove vehicle from old lane
				oldLane.vehicles.remove(vehicle);
				// Add vehicle to new lane
				if (vehicle.lane != null) {
					vehicle.lane.vehicles.add(vehicle);
					Collections.sort(vehicle.lane.vehicles, trafficNetwork.vehiclePositionComparator);// Sort
				}

				// Set priority lanes
				if (vehicle.type == VehicleType.PRIORITY) {
					VehicleUtil.setPriorityLanes(vehicle, true);
				}
			}
		}

	}

	/**
	 * Pause thread. This can be useful for observing simulation on GUI.
	 */
	void pause() {
		if (Settings.pauseTimeBetweenStepsInMilliseconds > 0) {
			try {
				Thread.sleep(Settings.pauseTimeBetweenStepsInMilliseconds);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check whether a vehicle reaches the work area of a fellow worker.
	 */
	boolean reachFellow(final Vehicle vehicle) {
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

	/**
	 * Try to move vehicle from parking area onto roads. A vehicle can only be
	 * released from parking if the current time has passed the earliest start time
	 * of the vehicle.
	 *
	 */
	void releaseVehicleFromParking(final double timeNow) {
		for (int i = 0; i < trafficNetwork.vehicles.size(); i++) {
			final Vehicle vehicle = trafficNetwork.vehicles.get(i);
			if (vehicle.active && (vehicle.lane == null) && (timeNow >= vehicle.earliestTimeToLeaveParking)) {
				trafficNetwork.startOneVehicleFromParking(vehicle);
			}
		}
	}

	synchronized void simulateOneStep(final Worker worker, boolean isNewNonPubVehiclesAllowed,
			boolean isNewTramsAllowed, boolean isNewBusesAllowed) {
		worker.isSimulatingOneStep = true;
		pause();
		final ArrayList<Vehicle> vehiclesAroundBorder = moveVehicleForward(worker.timeNow, worker.pspBorderEdges,
				worker);
		moveVehicleToNextLink(worker.timeNow, vehiclesAroundBorder);
		if (!Settings.isServerBased) {
			worker.transferVehicleDataToFellow();
		}
		final ArrayList<Vehicle> vehiclesNotAroundBorder = moveVehicleForward(worker.timeNow, worker.pspNonBorderEdges,
				worker);
		moveVehicleToNextLink(worker.timeNow, vehiclesNotAroundBorder);
		trafficNetwork.removeActiveVehicles(oneStepData_allVehiclesReachedDestination);
		makeLaneChange(worker.timeNow);
		if (Settings.trafficLightTiming != TrafficLightTiming.NONE) {
			trafficNetwork.lightCoordinator.updateLights();
		}
		trafficNetwork.updateTramStopTimers();
		releaseVehicleFromParking(worker.timeNow);
		blockTramAtTramStop();
		trafficNetwork.removeActiveVehicles(oneStepData_vehiclesReachedFellowWorker);
		trafficNetwork.createInternalVehicles(worker.numLocalRandomPrivateVehicles, worker.numLocalRandomTrams,
				worker.numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed,
				worker.timeNow);
		trafficNetwork.repeatExternalVehicles(worker.step, worker.timeNow);

		// Clear one-step data
		clearOneStepData();

		worker.isSimulatingOneStep = false;
	}
}

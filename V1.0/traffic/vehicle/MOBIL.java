package traffic.vehicle;

import java.util.HashMap;
import java.util.Random;

import common.Settings;
import traffic.light.LightColor;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadUtil;
import traffic.vehicle.SlowdownFactor;
import traffic.vehicle.LaneChangeDirection;

/**
 * MOBIL model for lane-changing.
 *
 */
public class MOBIL {

	/**
	 * Calculate the advantage gain in terms of acceleration rate for a vehicle
	 * if it changes lane
	 *
	 */
	static double getPotentialAdvatangeGainOfThisVehicleInTargetLane(final double newAcc, final double oldAcc) {
		return newAcc - oldAcc;
	}

	Vehicle backVehicleInTargetLane = null;

	Vehicle frontVehicleInTargetLane = new Vehicle();

	Random random = new Random();
	VehicleUtil vehicleUtil;
	Vehicle impedingObject = new Vehicle();

	IDM idm;

	public MOBIL(final VehicleUtil vehicleUtil) {
		this.vehicleUtil = vehicleUtil;
		idm = new IDM(vehicleUtil);
	}

	/**
	 * Makes decision about lane-changing.
	 *
	 * @return One of the possible lane-changing decisions.
	 */
	public LaneChangeDirection decideLaneChange(final Vehicle vehicle) {
		LaneChangeDirection decision = LaneChangeDirection.SAME;
		double overallGainForChangeTowardsRoadside = 0, overallGainForChangeAwayFromRoadside = 0;

		if (isSafeToChange(vehicle, LaneChangeDirection.TOWARDS_ROADSIDE)) {

			final double newAccByChangeTowardsRoadside = getPotentialAccelerationOfThisVehicleInTargetLane(vehicle,
					vehicle.lane.edge.lanes.get(vehicle.lane.laneNumber - 1));
			overallGainForChangeTowardsRoadside = getPotentialAdvatangeGainOfThisVehicleInTargetLane(
					newAccByChangeTowardsRoadside, vehicle.acceleration)
					- getPotentialDisadvantageGainOfBackVehicleInTargetLane(vehicle);

			overallGainForChangeTowardsRoadside += getAdditionalIncentive(vehicle,
					LaneChangeDirection.TOWARDS_ROADSIDE);
		}
		if (isSafeToChange(vehicle, LaneChangeDirection.AWAY_FROM_ROADSIDE)) {

			final double newAccByChangeAwayFromRoadside = getPotentialAccelerationOfThisVehicleInTargetLane(vehicle,
					vehicle.lane.edge.lanes.get(vehicle.lane.laneNumber + 1));
			overallGainForChangeAwayFromRoadside = getPotentialAdvatangeGainOfThisVehicleInTargetLane(
					newAccByChangeAwayFromRoadside, vehicle.acceleration)
					- getPotentialDisadvantageGainOfBackVehicleInTargetLane(vehicle);

			overallGainForChangeAwayFromRoadside += getAdditionalIncentive(vehicle,
					LaneChangeDirection.AWAY_FROM_ROADSIDE);
		}

		if ((overallGainForChangeAwayFromRoadside > 0)
				&& ((overallGainForChangeAwayFromRoadside - overallGainForChangeTowardsRoadside) > 0)) {
			decision = LaneChangeDirection.AWAY_FROM_ROADSIDE;
		} else if ((overallGainForChangeTowardsRoadside > 0)
				&& ((overallGainForChangeTowardsRoadside - overallGainForChangeAwayFromRoadside) > 0)) {
			decision = LaneChangeDirection.TOWARDS_ROADSIDE;
		} else if ((overallGainForChangeAwayFromRoadside > 0) && (overallGainForChangeTowardsRoadside > 0)) {
			if (random.nextBoolean()) {
				decision = LaneChangeDirection.AWAY_FROM_ROADSIDE;
			} else {
				decision = LaneChangeDirection.TOWARDS_ROADSIDE;
			}
		}

		return decision;
	}

	/**
	 * Get additional incentive of changing. A positive incentive encourages
	 * change. A negative incentive discourages change.
	 */
	double getAdditionalIncentive(final Vehicle vehicle, final LaneChangeDirection direction) {

		double incentive = 0;
		final Edge currentEdge = vehicle.routeLegs.get(vehicle.indexLegOnRoute).edge;

		if (direction == LaneChangeDirection.TOWARDS_ROADSIDE) {
			// Encourage change for making a turn			
			if (Settings.isDriveOnLeft && vehicle.edgeBeforeTurnLeft != null
					&& vehicle.lane.laneNumber >= currentEdge.numLeftLanes) {
				incentive = 5;
			} else if (!Settings.isDriveOnLeft && vehicle.edgeBeforeTurnRight != null
					&& vehicle.lane.laneNumber >= currentEdge.numRightLanes) {
				incentive = 5;
			}
			// Encourage change for giving way to priority vehicle based on emergency strategy
			if (RoadUtil.isEdgeOnPathOfPriorityVehicle(vehicle.lane.edge) && (vehicle.type != VehicleType.PRIORITY)
					&& (Settings.emergencyStrategy != EmergencyStrategy.Flexible)) {
				incentive = 10;
			}

		} else if (direction == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
			// Encourage change for making a turn
			if (Settings.isDriveOnLeft && vehicle.edgeBeforeTurnRight != null
					&& (vehicle.lane.laneNumber < (currentEdge.lanes.size() - currentEdge.numRightLanes))) {
				incentive = 5;
			} else if (!Settings.isDriveOnLeft && vehicle.edgeBeforeTurnLeft != null
					&& (vehicle.lane.laneNumber < (currentEdge.lanes.size() - currentEdge.numLeftLanes))) {
				incentive = 5;
			}
		}

		return incentive;

	}

	double getLowerAcceleration(final Vehicle vehicle, final double acc1, final double acc2,
			final SlowdownFactor factor) {
		if (acc1 > acc2) {
			return acc2;
		} else {
			return acc1;
		}
	}

	/**
	 * Gets the potential acceleration of vehicle if it changes to the given
	 * lane at this moment.
	 *
	 *
	 */
	double getPotentialAccelerationOfThisVehicleInTargetLane(final Vehicle vehicle, final Lane targetLane) {
		double lowestAcceleration = 10000;
		// To avoid frequent changes for the first vehicle at intersection, check red lights and conflicting traffic for that vehicle
		if (vehicle.lane.vehicles.get(0) == vehicle) {
			if (vehicle.lane.edge.lightColor == LightColor.GYR_R
					|| vehicle.lane.edge.lightColor == LightColor.KEEP_RED) {
				// No lane-change at red light
				lowestAcceleration = 0;
			}else {
				// No red light, consider slow down due to conflicting traffic at intersection
				lowestAcceleration = idm.getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
						idm.computeAccelerationWithImpedingObject(vehicle, impedingObject, targetLane, SlowdownFactor.CONFLICT));		
			}
		}
		
		lowestAcceleration = idm.getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				idm.computeAccelerationWithImpedingObject(vehicle, impedingObject, targetLane, SlowdownFactor.FRONT));		

		return lowestAcceleration;
	}

	/**
	 * Calculate the disadvantage in terms of acceleration rate for the back
	 * vehicle in the target lane, assuming the given vehicle changes to the
	 * target lane at this moment.
	 *
	 */
	double getPotentialDisadvantageGainOfBackVehicleInTargetLane(final Vehicle vehicle) {
		if (backVehicleInTargetLane == null) {
			return vehicle.driverProfile.MOBIL_a_thr;
		} else {
			final double currentAccBackVehicleTargetLane = backVehicleInTargetLane.acceleration;
			final double nextAccBackVehicleTargetLane = idm.computeAcceleration(backVehicleInTargetLane, vehicle);
			return (vehicle.driverProfile.MOBIL_p * (currentAccBackVehicleTargetLane - nextAccBackVehicleTargetLane))
					+ vehicle.driverProfile.MOBIL_a_thr;
		}
	}

	/**
	 * Check whether it's safe to change lane now.
	 *
	 *
	 */
	boolean isSafeToChange(final Vehicle vehicle, final LaneChangeDirection direction) {
		// Do not allow tram to change lane
		if (vehicle.type == VehicleType.TRAM) {
			return false;
		}

		Lane targetLane = null;
		if (Settings.isDriveOnLeft) {
			// Drive on LEFT
			if (direction == LaneChangeDirection.TOWARDS_ROADSIDE) {
				if (vehicle.lane.laneNumber == 0) {
					// Vehicle is already in the left-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft == null)
						&& (vehicle.lane.laneNumber <= vehicle.lane.edge.numLeftOnlyLanes) && !vehicle.lane.isBlocked) {
					// Cannot move to left-only lane if vehicle will not turn left, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnRight != null)
						&& ((vehicle.lane.edge.lanes.size()
								- (vehicle.lane.laneNumber - 1) > vehicle.lane.edge.numRightLanes))
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing right-turn if vehicle will turn right, unless the current lane is blocked
					return false;
				} else if (VehicleUtil.isAllLanesOnLeftBlocked(vehicle.lane.edge, vehicle.lane.laneNumber)) {
					// Cannot move to left as all lanes on the left are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.lanes.get(vehicle.lane.laneNumber - 1);
				}
			} else if (direction == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
				if (vehicle.lane.laneNumber == (vehicle.lane.edge.lanes.size() - 1)) {
					// Vehicle is already in the right-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnRight == null)
						&& ((vehicle.lane.edge.lanes.size()
								- (vehicle.lane.laneNumber + 1)) <= vehicle.lane.edge.numRightOnlyLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot move to right-only lane if vehicle will not turn right, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft != null)
						&& ((vehicle.lane.laneNumber + 1) >= vehicle.lane.edge.numLeftLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing left-turn if vehicle will turn left, unless the current lane is blocked
					return false;
				} else if (VehicleUtil.isAllLanesOnRightBlocked(vehicle.lane.edge, vehicle.lane.laneNumber)) {
					// Cannot move to right as all lanes on the right are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.lanes.get(vehicle.lane.laneNumber + 1);
				}
			}
		} else {
			// Drive on RIGHT
			if (direction == LaneChangeDirection.TOWARDS_ROADSIDE) {
				if (vehicle.lane.laneNumber == 0) {
					// Vehicle is already in the right-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnRight == null)
						&& (vehicle.lane.laneNumber <= vehicle.lane.edge.numRightOnlyLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot move to right-only lane if vehicle will not turn right, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft != null)
						&& ((vehicle.lane.edge.lanes.size()
								- (vehicle.lane.laneNumber - 1) > vehicle.lane.edge.numLeftLanes))
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing left-turn if vehicle will turn left, unless the current lane is blocked
					return false;
				} else if (VehicleUtil.isAllLanesOnRightBlocked(vehicle.lane.edge, vehicle.lane.laneNumber)) {
					// Cannot move to right as all lanes on the right are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.lanes.get(vehicle.lane.laneNumber - 1);
				}
			} else if (direction == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
				if (vehicle.lane.laneNumber == (vehicle.lane.edge.lanes.size() - 1)) {
					// Vehicle is already in the left-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft == null)
						&& ((vehicle.lane.edge.lanes.size()
								- (vehicle.lane.laneNumber + 1)) <= vehicle.lane.edge.numLeftOnlyLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot move to left-only lane if vehicle will not turn left, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnRight != null)
						&& ((vehicle.lane.laneNumber + 1) >= vehicle.lane.edge.numRightLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing right-turn if vehicle will turn right, unless the current lane is blocked
					return false;
				} else if (VehicleUtil.isAllLanesOnLeftBlocked(vehicle.lane.edge, vehicle.lane.laneNumber)) {
					// Cannot move to left as all lanes on the left are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.lanes.get(vehicle.lane.laneNumber + 1);
				}
			}
		}

		// Cannot change if front vehicle in target lane is too close
		frontVehicleInTargetLane = VehicleUtil.getFrontVehicleInTargetLane(vehicle, targetLane, 0);
		if ((frontVehicleInTargetLane != null) && ((frontVehicleInTargetLane.headPosition
				- frontVehicleInTargetLane.length - vehicle.headPosition) < vehicle.driverProfile.IDM_s0)) {
			return false;
		}

		// Cannot change if back vehicle in target lane is too close
		backVehicleInTargetLane = VehicleUtil.getBackVehicleInTargetLane(vehicle, targetLane);
		if ((backVehicleInTargetLane != null) && ((vehicle.headPosition - vehicle.length
				- backVehicleInTargetLane.headPosition) < vehicle.driverProfile.IDM_s0)) {
			return false;
		}

		// Cannot change if back vehicle in target lane cannot safely decelerate
		if (backVehicleInTargetLane == null) {
			// No back vehicle: safe
			return true;
		} else {
			final double newAccBackVehicleTargetLane = idm.computeAcceleration(backVehicleInTargetLane, vehicle);
			if (newAccBackVehicleTargetLane > (-1 * vehicle.driverProfile.MOBIL_b_save)) {
				return true;
			} else {
				// Deceleration of back vehicle in target lane would be too significant: unsafe
				return false;
			}
		}
	}
}

package traffic.vehicle;

import traffic.road.Lane;
import traffic.vehicle.SlowdownFactor;

/**
 * This class computes ideal acceleration based on various types of impeding
 * objects, such as traffic lights, front cars, etc. As safety is of priority,
 * the lowest acceleration based on all the factors is the final acceleration
 * value. For each factor, the acceleration is computed based on IDM model.
 *
 */
public class IDM {

	VehicleUtil vehicleUtil;

	public IDM(final VehicleUtil vU) {
		vehicleUtil = vU;
	}

	/**
	 * Calculates the acceleration of vehicle based on its relation to an
	 * impeding object.
	 *
	 */
	public double computeAcceleration(final Vehicle vehicle, final Vehicle impedingObject) {
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
		final double sS = vehicle.driverProfile.IDM_s0 + (v * vehicle.driverProfile.IDM_T)
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
	public double computeAccelerationWithImpedingObject(final Vehicle vehicle, final Vehicle impedingObject,
			final Lane targetLane, final SlowdownFactor factor) {
		vehicleUtil.updateImpedingObject(vehicle, vehicle.indexLegOnRoute, targetLane.laneNumber, impedingObject,
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
	double getLowerAccelerationAndUpdateSlowdownFactor(final Vehicle vehicle, final Vehicle impedingObject,
			final double acc1, final double acc2) {
		if (acc1 > acc2) {
			vehicle.distToImpedingObject = impedingObject.headPosition - impedingObject.length - vehicle.headPosition;
			vehicle.spdOfImpedingObject = impedingObject.speed;
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
		final Vehicle impedingObject = new Vehicle();
		double lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, 10000,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.FRONT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.TRAM));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.LIGHT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.CONFLICT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.TURN));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.LANEBLOCK));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane,
						SlowdownFactor.PRIORITY_VEHICLE));

		return lowestAcceleration;
	}
}

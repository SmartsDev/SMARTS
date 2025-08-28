package traffic.vehicle.carfollow;

import common.Settings;
import traffic.vehicle.Vehicle;

/**
 * This class computes vehicle's acceleration based on car-following model and
 * impeding objects. By default, car-following is based on IDM model. This can
 * be changed to other models.
 *
 */
public class CarFollow {
	IDM idm;

	public CarFollow(Settings settings) {
		idm = new IDM(settings);
	}

	public double computeAccelerationBasedOnImpedingObjects(final Vehicle vehicle) {
		return idm.updateBasedOnAllFactors(vehicle);
	}
}

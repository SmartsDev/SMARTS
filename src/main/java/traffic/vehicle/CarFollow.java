package traffic.vehicle;

/**
 * This class computes vehicle's acceleration based on car-following model and
 * impeding objects. By default, car-following is based on IDM model. This can
 * be changed to other models.
 *
 */
public class CarFollow {
	IDM idm;

	public CarFollow(final VehicleUtil vU) {
		idm = new IDM(vU);
	}

	public double computeAccelerationBasedOnImpedingObjects(final Vehicle vehicle) {
		return idm.updateBasedOnAllFactors(vehicle);
	}
}

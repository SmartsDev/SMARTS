package traffic.vehicle;

/**
 * This class makes lane-change decisions. Current implementation uses MOBIL
 * model, which can be changed to other models.
 *
 */
public class LaneChange {

	MOBIL mobil;

	public LaneChange(final VehicleUtil vU) {
		mobil = new MOBIL(vU);
	}

	/**
	 * Uses a lane-changing model to decide lane change.
	 *
	 */
	public LaneChangeDirection decideLaneChange(final Vehicle vehicle) {
		return mobil.decideLaneChange(vehicle);
	}
}

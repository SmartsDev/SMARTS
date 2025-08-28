package traffic.vehicle.lanechange;

import common.Settings;
import traffic.vehicle.Vehicle;

/**
 * This class makes lane-change decisions. Current implementation uses MOBIL
 * model, which can be changed to other models.
 *
 */
public class LaneChange {

	//MOBIL mobil;
	MOBIL_OLD mobil;

	public LaneChange(Settings settings) {
		//mobil = new MOBIL();
		mobil = new MOBIL_OLD(settings);
	}

	/**
	 * Uses a lane-changing model to decide lane change.
	 *
	 */
	public LaneChangeDirection decideLaneChange(MOBILInput input, final Vehicle vehicle) {
		return mobil.decideLaneChange(vehicle);
	}

	public LaneChangeDirection dynamicLaneChange(final  Vehicle vehicle) {
		return mobil.dynamicLaneChange(vehicle);
	}
}

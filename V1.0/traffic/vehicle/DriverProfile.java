package traffic.vehicle;

/**
 * DriverProfile defines parameters of driver models such as car-following and
 * lane-changing. Personal characteristics of drivers are controlled by the
 * parameters.
 *
 */
public enum DriverProfile {
	HIGHLY_AGGRESSIVE(0.5, 0.5, 4, 5, 5, 0.1, 0.01, 2, 5), AGGRESSIVE(1, 1, 3, 4, 4, 0.2, 0.25, 4, 30), NORMAL(2, 2, 2,
			3, 3, 0.3, 0.5, 6,
			60), POLITE(5, 3.5, 1.5, 2.5, 2.5, 0.4, 0.75, 8, 90), HIGHLY_POLITE(10, 5, 1, 2, 2, 0.5, 1, 10, 120);

	/**
	 * Minimum bumper-to-bumper distance to the front vehicle.
	 */
	public double IDM_s0 = 2;
	/**
	 * Desired safety headway time when following other vehicles
	 */
	public double IDM_T = 2;
	/**
	 * Acceleration in everyday traffic
	 */
	public double IDM_a = 2;
	/**
	 * "Comfortable" braking deceleration in everyday traffic.
	 */
	public double IDM_b = 3;
	/**
	 * Maximum safe deceleration
	 */
	public double MOBIL_b_save = 3;
	/**
	 * Threshold for avoiding frantic lane hopping
	 */
	public double MOBIL_a_thr = 0.3;
	/**
	 * Politeness factor (1: extremely polite; 0: selfish)
	 */
	public double MOBIL_p = 0.5;
	/**
	 * Minimal time gap between two lane-change in seconds
	 */
	public double minLaneChangeTimeGap = 6;
	/**
	 * Minimal time gap between re-route
	 */
	public double minRerouteTimeGap = 90;

	DriverProfile(final double IDM_s0, final double IDM_T, final double IDM_a, final double IDM_b,
			final double MOBIL_b_save, final double MOBIL_a_thr, final double MOBIL_p,
			final double minLaneChangeTimeGap, final double minRerouteTimeGap) {
		this.IDM_s0 = IDM_s0;
		this.IDM_T = IDM_T;
		this.IDM_a = IDM_a;
		this.IDM_b = IDM_b;
		this.MOBIL_b_save = MOBIL_b_save;
		this.MOBIL_a_thr = MOBIL_a_thr;
		this.MOBIL_p = MOBIL_p;
		this.minLaneChangeTimeGap = minLaneChangeTimeGap;
		this.minRerouteTimeGap = minRerouteTimeGap;
	}

}

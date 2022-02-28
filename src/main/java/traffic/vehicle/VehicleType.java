package traffic.vehicle;

public enum VehicleType {
	CAR(4, 55), BIKE(2, 55), TRUCK(8, 27), BUS(10, 27), TRAM(25, 11), PRIORITY(5, 55), VIRTUAL_STATIC(0,
			0), VIRTUAL_SLOW(0, 5);

	public static VehicleType getVehicleTypeFromName(final String name) {
		for (final VehicleType vehicleType : VehicleType.values()) {
			if (name.equals(vehicleType.name())) {
				return vehicleType;
			}
		}
		return VehicleType.CAR;
	}

	/**
	 * Length of vehicle in meters.
	 */
	public double length;

	/**
	 * The max speed in meters per second.
	 */
	public double maxSpeed;

	VehicleType(final double length, final double maxSpeed) {
		this.length = length;
		this.maxSpeed = maxSpeed;
	}

}

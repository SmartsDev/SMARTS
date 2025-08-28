package traffic.vehicle;

public enum VehicleType {
	CAR(4, 55, 1.75), BIKE(2, 55, 0.5), TRUCK(8, 27, 2.75), BUS(10, 27, 2.75), TRAM(25, 11, 2.75), PRIORITY(5, 55, 2.5), VIRTUAL_STATIC(0,
			0,0), VIRTUAL_SLOW(0, 5,0);

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

	public double width;

	VehicleType(final double length, final double maxSpeed, final double width) {
		this.length = length;
		this.maxSpeed = maxSpeed;
		this.width = width;
	}

}

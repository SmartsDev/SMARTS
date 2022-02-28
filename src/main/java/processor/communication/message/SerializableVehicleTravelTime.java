package processor.communication.message;

public class SerializableVehicleTravelTime {
	public String vehicleId;
	public double time;
	public double avgSpd;

	public SerializableVehicleTravelTime() {

	}

	public SerializableVehicleTravelTime(final String vehicleId, final double time, final double avgSpd) {
		super();
		this.vehicleId = vehicleId;
		this.time = time;
		this.avgSpd = avgSpd;
	}
}

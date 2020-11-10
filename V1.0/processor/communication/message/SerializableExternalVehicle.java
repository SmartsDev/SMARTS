package processor.communication.message;

import java.util.ArrayList;

public class SerializableExternalVehicle {
	public boolean foreground;
	public String id;
	public double startTime;
	public String vehicleType;
	public String driverProfile;
	public double numberRepeatPerSecond;
	public ArrayList<SerializableRouteLeg> route = new ArrayList<>();

	public SerializableExternalVehicle() {

	}

	public SerializableExternalVehicle(final boolean foreground, final String id, final double startTime,
			final String vehicleType, final String driverProfile, final double repeatRate,
			final ArrayList<SerializableRouteLeg> route) {
		super();
		this.foreground = foreground;
		this.id = id;
		this.startTime = startTime;
		this.vehicleType = vehicleType;
		this.driverProfile = driverProfile;
		numberRepeatPerSecond = repeatRate;
		this.route = route;
	}
}

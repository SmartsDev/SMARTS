package processor.communication.message;

import java.util.ArrayList;

public class SerializableRoute {
	public String vehicleId = "";
	public String type = "";
	public double startTime;
	public ArrayList<SerializableRouteDumpPoint> routeDumpPoints = new ArrayList<>();
	public String driverProfile = "";
	public boolean isForeground;

	public SerializableRoute() {

	}

	public SerializableRoute(final String vehicleId, final String type, final double startTime,
			final ArrayList<SerializableRouteDumpPoint> routeDumpPoints, final String driverProfile,
			boolean isForeground) {
		super();
		this.vehicleId = vehicleId;
		this.type = type;
		this.startTime = startTime;
		this.routeDumpPoints = routeDumpPoints;
		this.driverProfile = driverProfile;
		this.isForeground = isForeground;
	}

}

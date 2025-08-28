package processor.communication.message;

import java.util.ArrayList;

public class SerializableRouteDump {
	public String vehicleId = "";
	public int vid;
	public String type = "";
	public double startTime;
	public int s;
	public int d;
	public double spTime = 0;
	public double spLength = 0;
	public String route;
	public ArrayList<SerializableRouteDumpPoint> routeDumpPoints = new ArrayList<>();
	public String driverProfile = "";

	public SerializableRouteDump() {

	}

	public SerializableRouteDump(final String vehicleId, final int vid, final String type, final double startTime, int s, int d, double spTime, double spLength,
			String route, final ArrayList<SerializableRouteDumpPoint> routeDumpPoints, final String driverProfile) {
		super();
		this.vehicleId = vehicleId;
		this.vid = vid;
		this.type = type;
		this.startTime = startTime;
		this.s = s;
		this.d = d;
		this.spTime = spTime;
		this.spLength = spLength;
		this.route = route;
		this.routeDumpPoints = routeDumpPoints;
		this.driverProfile = driverProfile;
	}

}

package processor.communication.message;

public class SerializableTrajectoryPoint {

	public double lon, lat;
	public double realTime;

	public SerializableTrajectoryPoint() {

	}

	public SerializableTrajectoryPoint(final double lon, final double lat, final double realTime) {
		this.lon = lon;
		this.lat = lat;
		this.realTime = realTime;
	}

}

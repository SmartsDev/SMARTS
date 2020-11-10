package processor.communication.message;

public class Serializable_GPS_Rectangle {
	public double minLon, maxLat, maxLon, minLat;

	public Serializable_GPS_Rectangle() {

	}

	public Serializable_GPS_Rectangle(final double minLon, final double maxLat, final double maxLon,
			final double minLat) {
		super();
		this.minLon = minLon;
		this.maxLat = maxLat;
		this.maxLon = maxLon;
		this.minLat = minLat;
	}

}

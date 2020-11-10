package processor.communication.message;

public class SerializableTravelTime {
	public String ID;
	public double travelTime;

	public SerializableTravelTime() {

	}

	public SerializableTravelTime(String ID, double travelTime) {
		this.ID = ID;
		this.travelTime = travelTime;
	}
}

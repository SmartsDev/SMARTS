package processor.communication.message;

public class SerializableRouteDumpPoint {

	public long nodeId;
	public double stopDuration;

	public SerializableRouteDumpPoint() {

	}

	public SerializableRouteDumpPoint(final long nodeId, final double stopDuration) {
		super();
		this.nodeId = nodeId;
		this.stopDuration = stopDuration;
	}

}

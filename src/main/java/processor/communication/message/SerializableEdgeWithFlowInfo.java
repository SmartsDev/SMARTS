package processor.communication.message;

public class SerializableEdgeWithFlowInfo {
	public int edgeIndex;
	public double currentSpeed;
	public double freeFlowSpeed;

	public SerializableEdgeWithFlowInfo() {

	}

	public SerializableEdgeWithFlowInfo(final int edgeIndex, final double currentSpeed, final double freeFlowSpeed) {
		super();
		this.edgeIndex = edgeIndex;
		this.currentSpeed = currentSpeed;
		this.freeFlowSpeed = freeFlowSpeed;
	}

}

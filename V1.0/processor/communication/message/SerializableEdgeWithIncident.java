package processor.communication.message;

public class SerializableEdgeWithIncident {
	public int edgeIndex;
	public String blockedLaneNumber = "";

	public SerializableEdgeWithIncident() {

	}

	public SerializableEdgeWithIncident(final int edgeIndex, final String blockedLaneNumber) {
		super();
		this.edgeIndex = edgeIndex;
		this.blockedLaneNumber = blockedLaneNumber;
	}

}

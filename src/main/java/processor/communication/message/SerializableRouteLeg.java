package processor.communication.message;

public class SerializableRouteLeg {
	public int edgeIndex;
	public double stopover;

	public SerializableRouteLeg() {

	}

	public SerializableRouteLeg(final int edgeIndex, final double stopover) {
		super();
		this.edgeIndex = edgeIndex;
		this.stopover = stopover;
	}
}

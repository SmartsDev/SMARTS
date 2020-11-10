package processor.communication.message;

public class Message_SW_BlockLane {
	public int laneIndex;
	public boolean isBlocked;

	public Message_SW_BlockLane() {

	}

	public Message_SW_BlockLane(int laneIndex, boolean isBlocked) {
		this.laneIndex = laneIndex;
		this.isBlocked = isBlocked;
	}
}

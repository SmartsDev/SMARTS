package processor.communication.message;

public class Message_WS_SetupDone {
	public String workerName;
	public int numFellowWorkers;

	public Message_WS_SetupDone() {
	}

	public Message_WS_SetupDone(final String name, final int numFellow) {
		workerName = name;
		numFellowWorkers = numFellow;
	}
}

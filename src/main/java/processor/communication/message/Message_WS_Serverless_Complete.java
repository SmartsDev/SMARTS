package processor.communication.message;

public class Message_WS_Serverless_Complete {
	public String workerName;
	public int step;
	public int numVehicles;

	public Message_WS_Serverless_Complete() {

	}

	public Message_WS_Serverless_Complete(final String workerName, final int step, final int numVehicles) {
		super();
		this.workerName = workerName;
		this.step = step;
		this.numVehicles = numVehicles;
	}

}

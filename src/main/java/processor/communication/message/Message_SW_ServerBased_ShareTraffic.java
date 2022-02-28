package processor.communication.message;

/**
 * Server-to-worker message that asks a worker to share certain traffic
 * information with the worker's neighbors.
 */
public class Message_SW_ServerBased_ShareTraffic {
	public int currentStep;

	public Message_SW_ServerBased_ShareTraffic() {
	}

	public Message_SW_ServerBased_ShareTraffic(final int currentStep) {
		this.currentStep = currentStep;
	}

}

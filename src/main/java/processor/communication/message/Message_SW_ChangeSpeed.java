package processor.communication.message;

/**
 * Server-to-worker message that asks worker to set the pause time after
 * simulating one step. This message is sent when user make corresponding change
 * on GUI.
 */
public class Message_SW_ChangeSpeed {
	public int pauseTimeBetweenStepsInMilliseconds;

	public Message_SW_ChangeSpeed() {

	}

	public Message_SW_ChangeSpeed(final int pauseTimeBetweenStepsInMilliseconds) {
		this.pauseTimeBetweenStepsInMilliseconds = pauseTimeBetweenStepsInMilliseconds;
	}
}

package processor.communication.message;

public class Message_SW_Serverless_Start {
	public int startStep;

	public Message_SW_Serverless_Start() {

	}

	public Message_SW_Serverless_Start(final int stepCurrent) {
		startStep = stepCurrent;
	}
}

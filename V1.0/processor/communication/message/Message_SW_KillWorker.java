package processor.communication.message;

public class Message_SW_KillWorker {
	public boolean isSharedJVM;

	public Message_SW_KillWorker() {
	}

	public Message_SW_KillWorker(final boolean share) {
		isSharedJVM = share;
	}
}

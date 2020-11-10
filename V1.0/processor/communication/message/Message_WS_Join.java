package processor.communication.message;

public class Message_WS_Join {
	public String workerName;
	public String workerAddress;
	public int workerPort;

	public Message_WS_Join() {

	}

	public Message_WS_Join(final String name, final String address, final int port) {
		workerName = name;
		workerAddress = address;
		workerPort = port;
	}
}

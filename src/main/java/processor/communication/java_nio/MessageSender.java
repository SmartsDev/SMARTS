package processor.communication.java_nio;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import processor.communication.message.MessageUtil;

public class MessageSender {
	public String address;
	public int port;
	public Socket socket;
	PrintWriter out = null;
	MessageUtil messageUtil = new MessageUtil();

	public MessageSender(final Socket socket) {
		try {
			this.socket = socket;
			this.socket.setTcpNoDelay(true);
			out = new PrintWriter(this.socket.getOutputStream(), true);
		} catch (final SocketException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public MessageSender(final String receiverAddress, final int receiverPort) {
		try {

			socket = new Socket(receiverAddress, receiverPort);

			socket.setTcpNoDelay(true);
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		address = receiverAddress;
		port = receiverPort;
	}

	public void send(final Object message) {
		out.println(messageUtil.compose(message));
	}
}

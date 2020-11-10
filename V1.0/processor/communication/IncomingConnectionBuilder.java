package processor.communication;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * Listens connection request on a given port and builds socket connections upon
 * request. When a new connection is established, a new thread will be created
 * for processing messages sent from the other end of the connection. This class
 * builds connections for server-worker and worker-worker communications.
 *
 */
public class IncomingConnectionBuilder extends Thread {
	int port;
	MessageHandler messageHandler;
	boolean running = true;
	ServerSocket incomingConnectionRequestSocket;
	ArrayList<MessageListener> incomingConnections = new ArrayList<>();

	public IncomingConnectionBuilder(final int port, final MessageHandler messageHandler) {
		this.port = port;
		this.messageHandler = messageHandler;
	}

	@Override
	public void run() {
		try {
			incomingConnectionRequestSocket = new ServerSocket();
			incomingConnectionRequestSocket.bind(new InetSocketAddress(port));
			while (running) {
				final Socket newSocket = incomingConnectionRequestSocket.accept();
				final MessageListener messageListener = new MessageListener(newSocket, messageHandler);
				incomingConnections.add(messageListener);
				final Thread thread = new Thread(messageListener);
				thread.start();
			}
		} catch (final SocketException socketException) {
			System.out.println("ConnectionBuilder thread ends.");
			return;
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void terminate() {
		try {
			for (final MessageListener listener : incomingConnections) {
				listener.terminate();
			}
			incomingConnectionRequestSocket.close();
			running = false;
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}

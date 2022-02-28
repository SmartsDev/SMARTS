package processor.communication.java_nio;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import processor.communication.MessageHandler;
import processor.communication.message.MessageUtil;

/**
 * This class passes the received messages to message handler for processing.
 *
 */
public class MessageListener implements Runnable {
	class MessageProcessRunnable implements Runnable {

		String input = "";

		public MessageProcessRunnable(final String string) {
			input = string;
		}

		public void run() {
			final Object received = MessageUtil.read(input);
			messageHandler.processReceivedMsg(received);
		}
	}

	Socket socket = null;
	BufferedReader bufferedReader;
	InputStream inputStream;
	InputStreamReader inputStreamReader;
	MessageHandler messageHandler = null;

	boolean running = true;

	/**
	 * @param socket
	 *            An established connection.
	 * @param messageHandler
	 *            Entity that processes received messages.
	 */
	public MessageListener(final Socket socket, final MessageHandler messageHandler) {
		this.socket = socket;
		this.messageHandler = messageHandler;
	}

	/**
	 * Constantly listen for messages. When a message arrives, starts a new
	 * thread to handle it.
	 */
	@Override
	public void run() {
		try {
			inputStream = socket.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream);
			bufferedReader = new BufferedReader(inputStreamReader);
			String nextLine;
			while (running && ((nextLine = bufferedReader.readLine()) != null)) {
				final Thread thread = new Thread(new MessageProcessRunnable(nextLine));
				thread.start();
			}
		} catch (final Exception e) {
		}
	}

	public void terminate() {
		try {
			bufferedReader.close();
			inputStreamReader.close();
			inputStream.close();
			socket.close();
			running = false;
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}
}

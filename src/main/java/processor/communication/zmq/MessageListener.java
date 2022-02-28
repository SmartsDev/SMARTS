package processor.communication.zmq;

import org.zeromq.*;
import processor.communication.MessageHandler;
import processor.communication.message.MessageUtil;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * This class passes the received messages to message handler and the subscriber for processing.
 */
public class MessageListener implements Runnable {
	private MessageHandler handler;
	private ZMQ.Socket subscriber;
	private ExecutorService service = Executors.newFixedThreadPool(3);


	MessageListener(ZMQ.Socket sub, MessageHandler msgHandler) {
		handler = msgHandler;
		subscriber = sub;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			String topic = subscriber.recvStr(Charset.defaultCharset());
			String msg = subscriber.recvStr(0, Charset.defaultCharset());
			service.submit(new MessageProcessRunnable(msg));

		}
	}

	class MessageProcessRunnable implements Runnable {

		String input = "";

		MessageProcessRunnable(String string) {
			input = string;
		}

		public void run() {
			final Object received = MessageUtil.read(input);
			handler.processReceivedMsg(received);
		}
	}

}

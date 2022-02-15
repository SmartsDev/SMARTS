package processor.communication.zmq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import processor.communication.message.MessageUtil;

/**
 *  This publisher class to publish messages with a specific topics.
 */
public class Publisher {
    private final ZMQ.Socket publisher;
    private final MessageUtil messageUtil = new MessageUtil();

    Publisher(ZContext context, int PORT) {

        publisher = context.createSocket(ZMQ.PUB);

        publisher.bind("tcp://*:" + PORT);
    }

    void send(String message, String topic) {
        publisher.sendMore(topic);
        publisher.send(message, 0);
    }



    void send(Object message, String topic) {
        publisher.sendMore(topic);
        String msg = messageUtil.compose(message);
        publisher.send(msg);
    }


    public void close() {
        publisher.close();

    }

}
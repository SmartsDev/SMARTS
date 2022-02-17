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
    int port;

    Publisher(ZContext context, int PORT) {

        publisher = context.createSocket(ZMQ.PUB);
        port = PORT;
        publisher.bind("tcp://*:" + port);
    }

    void send(Object message, String topic) {
        publisher.sendMore(topic);
        String msg = messageUtil.compose(message);
        publisher.send(msg);
    }


    public void close() {
        publisher.setLinger(0);
        publisher.close();

    }

}
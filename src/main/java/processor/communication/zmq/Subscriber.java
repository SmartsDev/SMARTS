package processor.communication.zmq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import processor.communication.MessageHandler;

import java.util.List;

/**
 * This subscriber class to keep listening for messages with a specific topics.
 */
public class Subscriber {
    private ZMQ.Socket subscriber;
    MessageHandler messageHandler = null;
    Thread listenerThread;

    Subscriber(ZContext context, MessageHandler messageHandler) {
        subscriber = context.createSocket(ZMQ.SUB);
        this.messageHandler = messageHandler;
    }

    public void connect(String address, int PORT) {
        subscriber.connect("tcp://" + address + ":" + PORT);
    }

    void subscribe(List<String> topics) {
        for(String topic: topics) {
            subscriber.subscribe(topic.getBytes());
        }

    }

    public void listen() {
        Runnable runnable = new MessageListener(subscriber, messageHandler);
        listenerThread = new Thread(runnable);
        listenerThread.start();
    }
    public void shutdown(){
        listenerThread.interrupt();
    }
    public void disconnect(String add,int port){
        subscriber.disconnect("tcp://" + add + ":" + port);
    }

    public void close() {

        subscriber.close();
        //subscriber.setLinger(0);
    }
}

package processor.communication.zmq;
/*
    The 0MQ lightweight messaging kernel is a library which extends
    the standard socket interfaces with features traditionally
    provided by specialised messaging middleware products. 0MQ sockets
    provide an abstraction of asynchronous message queues, multiple
    messaging patterns, message filtering (subscriptions), seamless
    access to multiple transport protocols and more.
    [https://zeromq.org/languages/java/]

 */
import common.Settings;
import org.zeromq.*;

import processor.communication.MessageHandler;
import processor.server.WorkerMeta;
import processor.worker.Fellow;
import processor.worker.Worker;

import java.util.Collection;
import java.util.List;
import java.util.Queue;




public class ZMQClient {
    Subscriber subscriber;
    Publisher publisher;
    Receive receiver;
    MessageHandler handler;
    int port;
    ZContext context;
    List <String> topics;
    public Request request;
    public ZMQClient(MessageHandler handler, int port, List<String> _topics){
        context = new ZContext();
        this.port = port;
        this.handler = handler;
        this.topics = _topics;
        publisher = new Publisher(context,port);
        request = new Request(context,Settings.serverAddress, Settings.serverListeningPortForWorkers);
        receiver = new Receive(context,handler,port);
    }

    // **** SERVER METHODS ****
    public void setConnectionOfServerToWorkers(Collection <WorkerMeta> workers){
        initSubscriber();
        System.out.println("Connect To All Workers...:");
        for(WorkerMeta worker:workers) {
            subscriber.connect(worker.address, worker.port);
        }

        
       
        System.out.println("subscribe to topic: "+topics);
        subscriber.subscribe(topics);
        subscriber.listen();
    }


    public void waitForWorkersToJoin(){

        receiver.listenForSettingUp();

    }
    public void waitForRemoteControlToJoin(){
        System.out.println("Waiting for RemoteControl to join...");
        receiver.listenForSettingUp();

    }

    private void initSubscriber(){
        subscriber = new Subscriber(context,handler);

    }


    /*
        SENDING methods
     */
    public void sendTo(Object msg, String topic){
        publisher.send(msg, topic);
    }

    // ************ WORKERS METHODS ************

    public void joinToServer(Object message){
        // it will only fire once
        request.sendRequest(message);

    }

    public void connectToWorkers(Collection<Fellow> workers){
        System.out.println("Connecting to Workers...");
        for(Fellow worker:workers) {
            System.out.println("Connecting to:"+worker.getAddress()+":"+worker.getPort());
            subscriber.connect(worker.getAddress(), worker.getPort());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Connect To:"+workers.size()+"  Workers...");
    }
    

    /*
     Close Connections
     */
    public void terminateConnection(Collection<Fellow> workers) {
        request.closeRequest();
        receiver.close();
        publisher.close();
        for(Fellow w:workers){
            subscriber.disconnect(w.getAddress(),w.getPort());
        }
        subscriber.shutdown();
        subscriber.close();

    }


    public boolean setConnectionOfClientToServer(){
        initSubscriber();
        // connect to server to get the published messages
        subscriber.connect(Settings.serverAddress, Settings.serverListeningPortForWorkers);
        subscriber.subscribe(topics);
        subscriber.listen();
        return true;
    }



}

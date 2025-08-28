package processor;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;
import org.zeromq.SocketType;
import processor.communication.externalMessage.ExternalSimulationListener;

public class TMS_MQ {

    private static TMS_MQ single_instance = null;

    private ZContext context = null;
    private ZMQ.Socket socket = null;

    // disable constructor
    private TMS_MQ(){}

    // static method to create instance of TMS_MQ class
    public static TMS_MQ getInstance()
    {
        if (single_instance == null)
            single_instance = new TMS_MQ();
            System.out.println("First instance created");

        return single_instance;
    }

    public void init(){
        context = new ZContext();
        socket = context.createSocket(SocketType.REQ);
        socket.connect("tcp://*:5556");
        System.out.println("Created the client connection from SMARTS");
    }

    public void getGraph(){}

    public void sendMessage(){

    }


}

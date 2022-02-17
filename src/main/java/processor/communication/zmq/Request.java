package processor.communication.zmq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import processor.communication.message.MessageUtil;

/**
 * This class is to join to the system.
 */
public class Request {
    ZMQ.Socket sender;
    String temproryAddress;
    MessageUtil messageUtil = new MessageUtil();


    public Request(ZContext context, String address, int PORT) {
        sender = context.createSocket(ZMQ.PUSH);
        temproryAddress = address + ":"+(PORT+1);
    }


    public void sendRequest( Object msg){
        sender.connect("tcp://"+ temproryAddress);
        String msgStr = messageUtil.compose(msg);
        sender.send(msgStr);
        sender.disconnect("tcp://"+ temproryAddress);
        sender.close();

    }

    public void closeRequest(){
        sender.close();


    }


}

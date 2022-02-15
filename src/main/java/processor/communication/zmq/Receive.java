package processor.communication.zmq;

import common.Settings;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import processor.communication.MessageHandler;
import processor.communication.message.MessageUtil;

import java.nio.charset.Charset;

/**
 * This class used at the beginning only by the server to build the connection.
 */
public class Receive {
    private ZMQ.Socket receiver;
    private MessageHandler handler;
    private MessageUtil messageUtil = new MessageUtil();

    Receive(ZContext context, MessageHandler messageHandler, int _port) {
        this.handler = messageHandler;
        receiver =  context.createSocket(ZMQ.PULL);
        receiver.bind("tcp://*:" + (_port+1));
    }

    void listenForSettingUp(){
        int count = 0;
        while (count < Settings.numWorkers) {
            count++;
            String msg = receiver.recvStr(Charset.defaultCharset());
            Object received = messageUtil.read(msg);
            handler.processReceivedMsg(received);
        }

    }

    public void close(){
        receiver.close();
        receiver.setLinger(0);
    }
}

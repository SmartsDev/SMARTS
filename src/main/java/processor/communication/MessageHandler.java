package processor.communication;

/**
 * Interface of classes that need to process messages sent between two entities.
 *
 */
public interface MessageHandler {

	public void processReceivedMsg(Object message);

}

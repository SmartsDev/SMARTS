package processor.server;

import java.util.ArrayList;

import processor.communication.MessageSender;
import processor.communication.message.SerializableExternalVehicle;
import processor.worker.Workarea;
import traffic.road.GridCell;

/**
 * Meta data of worker.
 */
public class WorkerMeta {
	public String name;
	public Workarea workarea;
	public double laneLengthRatioAgainstWholeMap;
	public MessageSender sender;
	public int numRandomPrivateVehicles;
	public int numRandomTrams;
	public int numRandomBuses;
	public WorkerState state = WorkerState.NEW;
	public ArrayList<SerializableExternalVehicle> externalRoutes = new ArrayList<>();

	/**
	 *
	 * @param name
	 *            Name of the worker.
	 * @param address
	 *            Network address of the worker.
	 * @param port
	 *            Worker's port for receiving messages from server.
	 */
	public WorkerMeta(final String name, final String address, final int port) {
		this.name = name;
		workarea = new Workarea(name, new ArrayList<GridCell>());
		sender = new MessageSender(address, port);
	}

	void send(final Object message) {
		sender.send(message);
	}

	public void setState(final WorkerState state) {
		this.state = state;
	}
}

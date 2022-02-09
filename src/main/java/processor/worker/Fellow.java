package processor.worker;

import java.util.ArrayList;

import processor.communication.MessageSender;
import traffic.road.Edge;
import traffic.road.GridCell;
import traffic.road.Node;
import traffic.vehicle.Vehicle;

/**
 * A worker has one or more fellow workers when there are multiple workers in
 * simulation. A fellow worker may share road links (edges) with the worker.
 * This class describes a fellow worker.
 */
public class Fellow {
	String name;

	MessageSender senderForFellow;

	Workarea workarea;
	public FellowState state;
	public ArrayList<Edge> inwardEdgesAcrossBorder = new ArrayList<>(1000);
	public ArrayList<Edge> outwardEdgesAcrossBorder = new ArrayList<>(1000);
	public ArrayList<Vehicle> vehiclesToCreateAtBorder = new ArrayList<>();
	String address;

	int port;

	public Fellow(final String name, final String address, final int port, final ArrayList<GridCell> cellsInWorkarea) {
		this.name = name;
		this.address = address;
		this.port = port;
		workarea = new Workarea(name, cellsInWorkarea);
	}

	/**
	 * Get a list of edges that start from the work area of another worker and
	 * end in this fellow worker.
	 */
	void getEdgesFromAnotherArea(final Workarea fromArea) {

		inwardEdgesAcrossBorder.clear();

		for (final GridCell cell : fromArea.workCells) {
			for (final Node node : cell.nodes) {
				for (final Edge outwardEdge : node.outwardEdges) {
					if (workarea.workCells.contains(outwardEdge.endNode.gridCell)) {
						inwardEdgesAcrossBorder.add(outwardEdge);
					}
				}
			}
		}
	}

	/**
	 * Get a list of edges that start from this fellow worker and end in the
	 * work area of another worker.
	 */
	void getEdgesToAnotherArea(final Workarea toArea) {
		outwardEdgesAcrossBorder.clear();
		for (final GridCell cell : workarea.workCells) {
			for (final Node node : cell.nodes) {
				for (final Edge outwardEdge : node.outwardEdges) {
					if (toArea.workCells.contains(outwardEdge.endNode.gridCell)) {
						outwardEdgesAcrossBorder.add(outwardEdge);
					}
				}
			}
		}
	}

	public String getName() {
		return name;
	}

	public void prepareCommunication() {
		senderForFellow = new MessageSender(address, port);
		vehiclesToCreateAtBorder.clear();
	}

	void send(final Object message) {
		senderForFellow.send(message);
	}

}

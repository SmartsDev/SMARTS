package processor.communication.message;

import java.util.ArrayList;

import processor.server.WorkerMeta;
import traffic.road.GridCell;

public class SerializableWorkerMetadata {
	public String name;
	public String address;
	public int port;
	public ArrayList<SerializableGridCell> gridCells = new ArrayList<>();

	public SerializableWorkerMetadata() {

	}

	public SerializableWorkerMetadata(final WorkerMeta worker) {
		name = worker.name;
		address = worker.sender.address;
		port = worker.sender.port;
		gridCells = getSerializableGridCells(worker);
	}

	private ArrayList<SerializableGridCell> getSerializableGridCells(final WorkerMeta worker) {
		final ArrayList<SerializableGridCell> listSerializableGridCell = new ArrayList<>();
		for (final GridCell gridCell : worker.workarea.getWorkCells()) {
			listSerializableGridCell.add(new SerializableGridCell(gridCell));
		}
		return listSerializableGridCell;
	}
}

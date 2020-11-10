package processor.communication.message;

import traffic.road.GridCell;

public class SerializableGridCell {
	public int row;
	public int column;

	public SerializableGridCell() {

	}

	public SerializableGridCell(final GridCell gridCell) {
		row = gridCell.row;
		column = gridCell.col;
	}
}

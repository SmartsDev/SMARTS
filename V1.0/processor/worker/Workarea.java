package processor.worker;

import java.util.ArrayList;

import traffic.road.GridCell;

/**
 * Responsible area of a worker. The area consists of one or more grid cells,
 * which partition the whole space.
 */
public class Workarea {
	/**
	 * Name of the worker that works on this area.
	 */
	public String workerName = "";
	/**
	 * The grid cells within the area.
	 */
	public ArrayList<GridCell> workCells = new ArrayList<>(1000);

	/**
	 * @param workerName
	 */
	public Workarea(final String workerName, final ArrayList<GridCell> cellsInWorkarea) {
		super();
		this.workerName = workerName;
		workCells = cellsInWorkarea;
	}

	public ArrayList<GridCell> getWorkCells() {
		return workCells;
	}

	public void setWorkCells(final ArrayList<GridCell> workCells) {
		this.workCells = workCells;
	}
}

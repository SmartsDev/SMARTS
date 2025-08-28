package processor.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import common.Settings;
import processor.communication.message.Message_SW_Setup;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableInt;
import processor.worker.Workarea;
import processor.worker.Worker;
import traffic.road.GridCell;
import traffic.road.Node;
import traffic.road.RoadNetwork;

/**
 * Balance work load, i.e., the number of random background vehicles, between
 * workers. A worker will not generate new vehicles if the number of vehicles
 * running at the worker reaches the assigned volume.
 */
public class WorkloadBalancer {
	private Random random;
	private List<WorkerMeta> workerMetaList;
	private RoadNetwork roadNetwork;

	public WorkloadBalancer(List<WorkerMeta> workerMetaList, RoadNetwork roadNetwork){
		this.random = new Random();
		this.workerMetaList = workerMetaList;
		this.roadNetwork = roadNetwork;
	}

	public void balanceLoad(Settings settings, int step, List<SerializableExternalVehicle> vehicleList, List<Node> nodesRoAddLight, List<Node> nodesToRemoveLight){
		// Reset worker status
		for (final WorkerMeta worker : workerMetaList) {
			worker.setState(WorkerState.NEW);
		}
		if(settings.isNewEnvironment){
			partitionGridCells(settings, workerMetaList, roadNetwork);
		}
		// Determine the number of internal vehicles at all workers
		assignNumInternalVehiclesToWorkers(settings, workerMetaList, roadNetwork);
		// Assign vehicle routes from external file to workers
		assignVehicleToWorker(workerMetaList, roadNetwork, vehicleList);
		assignLightNodes(workerMetaList, roadNetwork, nodesRoAddLight, nodesToRemoveLight);
		// Send simulation configuration to workers
		for (final WorkerMeta worker : workerMetaList) {
			worker.send(new Message_SW_Setup(settings, workerMetaList, worker, roadNetwork.edges, step));
		}
		System.out.println("Sent simulation configuration to all workers.");
	}

	private void assignNumInternalVehiclesToWorkers(Settings settings, List<WorkerMeta> workers, RoadNetwork roadNetwork) {
		if ((settings.listRouteSourceWindowForInternalVehicle.size() == 0)
				&& (settings.listRouteSourceDestinationWindowForInternalVehicle.size() == 0)) {
			assignNumInternalVehiclesToWorkersBasedOnWorkarea(settings, workers);
		} else {
			assignNumInternalVehiclesToWorkersBasedOnSourceWindow(settings, workers, roadNetwork);
		}
	}

	/**
	 * Determine the number of random background vehicles at each worker. There
	 * should be at least one user-defined source window when this method is
	 * called. A worker, whose work area contains cells that intersect one or
	 * more source windows, needs to maintain a certain number of random
	 * background vehicles. A worker with more intersected cells needs to
	 * maintain a higher number of vehicles.
	 *
	 */
	private void assignNumInternalVehiclesToWorkersBasedOnSourceWindow(Settings settings, List<WorkerMeta> workers,
			RoadNetwork roadNetwork) {
		final ArrayList<double[]> windows = new ArrayList<>();
		windows.addAll(settings.listRouteSourceDestinationWindowForInternalVehicle);
		windows.addAll(settings.listRouteSourceWindowForInternalVehicle);
		final double latPerRow = (Math.abs(roadNetwork.maxLat - roadNetwork.minLat) / settings.numGridRows) + 0.0000001;
		final double lonPerCol = (Math.abs(roadNetwork.maxLon - roadNetwork.minLon) / settings.numGridCols) + 0.0000001;
		final ArrayList<GridCell> cellsInWindows = new ArrayList<>();
		for (final double[] window : windows) {
			final int minCol = (int) Math.floor(Math.abs(window[0] - roadNetwork.minLon) / lonPerCol);
			final int maxRow = (int) Math.floor(Math.abs(window[1] - roadNetwork.minLat) / latPerRow);
			final int maxCol = (int) Math.floor(Math.abs(window[2] - roadNetwork.minLon) / lonPerCol);
			final int minRow = (int) Math.floor(Math.abs(window[3] - roadNetwork.minLat) / latPerRow);

			for (int row = minRow; row <= maxRow; row++) {
				for (int col = minCol; col <= maxCol; col++) {
					if (row < 0 || col < 0 || row >= settings.numGridRows || col >= settings.numGridCols) {
						continue;
					}
					if (!cellsInWindows.contains(roadNetwork.grid[row][col])) {
						cellsInWindows.add(roadNetwork.grid[row][col]);
					}
				}
			}
		}

		for (final WorkerMeta worker : workers) {
			int numWorkerareaCellsInWindows = 0;
			for (final GridCell cell : worker.workarea.workCells) {
				if (cellsInWindows.contains(cell)) {
					numWorkerareaCellsInWindows++;
				}
			}
			final double ratio = (double) numWorkerareaCellsInWindows / cellsInWindows.size();
			worker.numRandomPrivateVehicles = (int) (settings.numGlobalRandomPrivateVehicles * ratio);
			worker.numRandomTrams = (int) (settings.numGlobalRandomTrams * ratio);
			worker.numRandomBuses = (int) (settings.numGlobalRandomBuses * ratio);

		}
	}

	/**
	 * Determine the number of random background vehicles at each worker. Each
	 * worker gets roughly the same number of vehicles as the total lane length
	 * at each worker is roughly the same.
	 *
	 */
	private void assignNumInternalVehiclesToWorkersBasedOnWorkarea(Settings settings, List<WorkerMeta> workers) {
		int totalNumAssignedPrivateVehicles = 0;
		int totalNumAssignedTrams = 0;
		int totalNumAssignedBuses = 0;

		// Assign numbers to workers except the last one
		for (int i = 0; i < workers.size(); i++) {
			WorkerMeta worker = workers.get(i);

			if(i != workers.size()-1) {
				double ratio = worker.laneLengthRatioAgainstWholeMap;
				worker.numRandomPrivateVehicles = (int) (ratio * settings.numGlobalRandomPrivateVehicles);
				totalNumAssignedPrivateVehicles += worker.numRandomPrivateVehicles;
				worker.numRandomTrams = (int) (ratio * settings.numGlobalRandomTrams);
				totalNumAssignedTrams += worker.numRandomTrams;
				worker.numRandomBuses = (int) (ratio * settings.numGlobalRandomBuses);
				totalNumAssignedBuses += worker.numRandomBuses;
			}else{
				// Assign numbers to the last worker
				worker.numRandomPrivateVehicles = settings.numGlobalRandomPrivateVehicles
						- totalNumAssignedPrivateVehicles;
				worker.numRandomTrams = settings.numGlobalRandomTrams - totalNumAssignedTrams;
				worker.numRandomBuses = settings.numGlobalRandomBuses - totalNumAssignedBuses;
			}
		}

	}

	/**
	 * Determine work areas of workers. A work area consists of one or more grid
	 * cells. The total lane length of a worker area is similar to that of
	 * another work area.
	 */
	private void partitionGridCells(Settings settings, List<WorkerMeta> workers, RoadNetwork roadNetwork) {

		// Clear existing grid cells in the work area of each worker
		for (final WorkerMeta worker : workers) {
			worker.workarea.workCells.clear();
		}

		final GridCell[][] grid = roadNetwork.grid;

		double laneLengthWholeMap = 0;
		for (int i = 0; i < settings.numGridRows; i++) {
			for (int j = 0; j < settings.numGridCols; j++) {
				laneLengthWholeMap += grid[i][j].laneLength;
			}
		}

		final double optimalLaneLengthPerWorker = laneLengthWholeMap / settings.numWorkers;

		int totalLaneLengthInCurrentWorkarea = 0;
		int workerIndex = 0;
		for (int row = 0; row < settings.numGridRows; row++) {
			for (int col = 0; col < settings.numGridCols; col++) {

				final int nextTotalLength = totalLaneLengthInCurrentWorkarea + grid[row][col].laneLength;

				if ((nextTotalLength > optimalLaneLengthPerWorker) && (workerIndex < (settings.numWorkers - 1))) {
					final boolean isAddCellToCurrentWorker = random.nextBoolean();
					if (isAddCellToCurrentWorker) {
						totalLaneLengthInCurrentWorkarea += grid[row][col].laneLength;
						workers.get(workerIndex).workarea.workCells.add(grid[row][col]);
						// Update lane length ratio (roads in current worker vs. whole map)
						workers.get(workerIndex).laneLengthRatioAgainstWholeMap = totalLaneLengthInCurrentWorkarea
								/ laneLengthWholeMap;
						workerIndex++;
						totalLaneLengthInCurrentWorkarea = 0;
					} else {
						workerIndex++;
						totalLaneLengthInCurrentWorkarea = 0;
						totalLaneLengthInCurrentWorkarea += grid[row][col].laneLength;
						workers.get(workerIndex).workarea.workCells.add(grid[row][col]);
					}

				} else {
					totalLaneLengthInCurrentWorkarea += grid[row][col].laneLength;
					workers.get(workerIndex).workarea.workCells.add(grid[row][col]);
					// Update lane length ratio (roads in current worker vs. whole map)
					workers.get(workerIndex).laneLengthRatioAgainstWholeMap = totalLaneLengthInCurrentWorkarea
							/ laneLengthWholeMap;
				}

			}
		}

		for (final WorkerMeta worker : workers) {
			System.out.println("Worker " + worker.name + "'s area has " + worker.workarea.workCells.size() + " cells.");
		}

	}


	/**
	 * Append route of vehicles to the workers whose work area covers the first
	 * node of the route.
	 */
	static void assignVehicleToWorker(List<WorkerMeta> workers, RoadNetwork roadNetwork, List<SerializableExternalVehicle> vehicles) {
		// Clear routes from previous loading
		for (final WorkerMeta worker : workers) {
			worker.externalRoutes.clear();
		}
		for (SerializableExternalVehicle ev : vehicles) {
			final Node routeStartNode = roadNetwork.edges.get(ev.route.get(0).edgeIndex).startNode;
			WorkerMeta routeStartWorker = null;
			for (final WorkerMeta worker : workers) {
				if (worker.workarea.workCells.contains(routeStartNode.gridCell)) {
					routeStartWorker = worker;
				}
			}
			if(routeStartWorker != null) {
				routeStartWorker.externalRoutes.add(ev);
			}
		}
	}

	public void assignLightNodes(List<WorkerMeta> workers, RoadNetwork roadNetwork, List<Node> nodesRoAddLight, List<Node> nodesToRemoveLight){
		for (int i = 0; i < workers.size(); i++) {
			WorkerMeta worker = workers.get(i);
			for (final Node node : roadNetwork.nodes) {
				if (worker.workarea.workCells.contains(node.gridCell) && node.light && (node.inwardEdges.size() > 0)) {
					if(!nodesToRemoveLight.contains(node)) {
						worker.lightNodes.add(node);
					}
				}else{
					if(nodesRoAddLight.contains(node)){
						worker.lightNodes.add(node);
					}
				}
			}
		}
	}
}

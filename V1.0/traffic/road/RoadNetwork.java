package traffic.road;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

import common.Settings;

/**
 * A road network consists of a number of nodes that are connected with edges.
 * The road network is mapped to a virtual grid.
 *
 */
public class RoadNetwork {
	/**
	 * Collection of grid cells that partition the whole region.
	 */
	public GridCell[][] grid = new GridCell[0][0];
	/**
	 * Collection of nodes, each of which is the end point of one or more edges.
	 */
	public ArrayList<Node> nodes = new ArrayList<>(300000);
	/**
	 * Collection of unidirectional edges, each of which has a start node and an
	 * end node. Two-way link consists of two edges in different directions.
	 */
	public ArrayList<Edge> edges = new ArrayList<>(500000);
	/**
	 * Collection of lanes, each of which belongs to a single edge. There can be
	 * multiple lanes on an edge.
	 */
	public ArrayList<Lane> lanes = new ArrayList<>(1000000);
	/**
	 * Bounds of map.
	 */
	public double minLat = 0, minLon = 0, maxLat = 0, maxLon = 0;
	/**
	 * Size of map.
	 */
	public double mapWidth = 0, mapHeight = 0;
	/**
	 * Edges on tram routes. Each route is with a reference.
	 */
	public HashMap<String, ArrayList<Edge>> tramRoutes = new HashMap<>();
	/**
	 * Edges on bus routes. Each routes is with a reference.
	 */
	public HashMap<String, ArrayList<Edge>> busRoutes = new HashMap<>();
	/**
	 * Length of the shortest edge
	 */
	public double shortestEdgeLength = Double.MAX_VALUE;

	public double gridCellHeight;
	public double gridCellWidth;
	protected Settings settings;

	public RoadNetwork(Settings settings) {
		this.settings = settings;
		importNodesEdges();
		buildGrid();
		setIndexes();
		addLightAndNameToTramEdges();
		findIntersectionPolygons();
	}

	public Settings getSettings() {
		return settings;
	}

	/**
	 * Add traffic signal information and street name to tram tracks that are
	 * parallel to roads without tram tracks. This should be done if the tram
	 * tracks in map data are separated from normal roads (e.g., in
	 * OpenStreetMap data).
	 */
	void addLightAndNameToTramEdges() {
		final double maxDistLightToTramEdgeEndInMeters = 10;

		final ArrayList<Node> initialNodesWithLight = new ArrayList<>();
		for (final Node node : nodes) {
			if (node.light) {
				initialNodesWithLight.add(node);
			}
		}

		for (final Node node : initialNodesWithLight) {
			for (final Edge inwardEdgeAtImportedLight : node.inwardEdges) {
				final ArrayList<Edge> candidateEdges = getEdgesParallelToEdge(inwardEdgeAtImportedLight.startNode.lat,
						inwardEdgeAtImportedLight.startNode.lon, inwardEdgeAtImportedLight.endNode.lat,
						inwardEdgeAtImportedLight.endNode.lon);

				/*
				 * Find the the parallel tram edge, whose end node is the
				 * closest to the light.
				 */
				Edge closestTramEdge = null;
				double minDist = maxDistLightToTramEdgeEndInMeters;
				for (final Edge edge : candidateEdges) {
					if (edge.type != RoadType.tram) {
						continue;
					}
					final double dist = RoadUtil.getDistInMeters(edge.endNode.lat, edge.endNode.lon, node.lat,
							node.lon);
					if (dist < minDist) {
						closestTramEdge = edge;
						minDist = dist;
					}
				}

				/*
				 * Add traffic light to the parallel edge.
				 */
				if (closestTramEdge != null) {
					closestTramEdge.endNode.light = true;
					closestTramEdge.name = inwardEdgeAtImportedLight.name;
				}
			}
		}
	}

	/**
	 * Build the grid partition of space.
	 */
	public void buildGrid() {
		mapHeight = RoadUtil.getDistInMeters(minLat, minLon, maxLat, minLon);
		mapWidth = RoadUtil.getDistInMeters(minLat, minLon, minLat, maxLon);
		settings.numGridRows = settings.numWorkers;
		settings.numGridCols = settings.numWorkers;

		/*
		 * Calculate the number of rows and columns in the grid. The size of a
		 * grid cell should be kept under a limit.
		 */
		gridCellHeight = mapHeight / settings.numGridRows;
		gridCellWidth = mapWidth / settings.numGridCols;
		while ((gridCellHeight > settings.maxGridCellWidthHeightInMeters)
				|| (gridCellWidth > settings.maxGridCellWidthHeightInMeters)) {
			if (gridCellHeight > settings.maxGridCellWidthHeightInMeters) {
				settings.numGridRows++;
				gridCellHeight = mapHeight / settings.numGridRows;
			}
			if (gridCellWidth > settings.maxGridCellWidthHeightInMeters) {
				settings.numGridCols++;
				gridCellWidth = mapWidth / settings.numGridCols;
			}
		}

		/*
		 * Initialize grid
		 */
		grid = new GridCell[settings.numGridRows][settings.numGridCols];
		for (int i = 0; i < settings.numGridRows; i++) {
			for (int j = 0; j < settings.numGridCols; j++) {
				grid[i][j] = new GridCell();
				grid[i][j].row = i;
				grid[i][j].col = j;
			}
		}

		/*
		 * Put nodes in road graph to grid cells.
		 */
		final double latPerRow = (Math.abs(maxLat - minLat) / settings.numGridRows) + 0.0000001;
		final double lonPerCol = (Math.abs(maxLon - minLon) / settings.numGridCols) + 0.0000001;

		for (final Node mapNode : nodes) {
			final int row = (int) Math.floor(Math.abs(mapNode.lat - minLat) / latPerRow);
			final int col = (int) Math.floor(Math.abs(mapNode.lon - minLon) / lonPerCol);
			grid[row][col].nodes.add(mapNode);
			mapNode.gridCell = grid[row][col];
		}

		/*
		 * Calculate the lane length in every grid cell.
		 */
		computeTotalOutwardLaneLength(grid);
	}

	/**
	 * Calculate the length of edges.
	 */
	void calculateEdgeLength() {
		for (int i = 0; i < edges.size(); i++) {
			final Edge edge = edges.get(i);
			final Node startNode = edge.startNode;
			final double sourceLat = startNode.lat;
			final double sourceLon = startNode.lon;
			final Node endNode = edge.endNode;
			final double destLat = endNode.lat;
			final double destLon = endNode.lon;
			edge.length = RoadUtil.getDistInMeters(sourceLat, sourceLon, destLat, destLon);
		}
	}

	/**
	 * Calculate the total length of lanes coming out of the nodes in a cell.
	 */
	void computeTotalOutwardLaneLength(final GridCell[][] grid) {
		for (int i = 0; i < settings.numGridRows; i++) {
			for (int j = 0; j < settings.numGridCols; j++) {
				for (final Node node : grid[i][j].nodes) {
					for (final Edge edge : node.outwardEdges) {
						grid[i][j].laneLength += edge.length * edge.getLaneCount();
					}
				}
			}
		}
	}

	/**
	 * Create lanes on edges.
	 */
	void createLanes(final Edge edge, final int numLanes) {
		for (int i = 0; i < numLanes; i++) {
			final Lane lane = new Lane(edge);
			lanes.add(lane);
			lane.laneNumber = i;
			edge.addLane(lane);
		}
	}

	/**
	 * Find the next edge adjacent to a specified edge on the same road
	 *
	 * @param currentEdge
	 * @param searchStartNode
	 * @return
	 */
	public Edge findNextEdgeOnRoad(final Edge currentEdge, final boolean searchStartNode) {
		ArrayList<Edge> edgesToSearch = null;
		if (searchStartNode) {
			edgesToSearch = currentEdge.startNode.inwardEdges;
		} else {
			edgesToSearch = currentEdge.endNode.outwardEdges;
		}
		for (final Edge edge : edgesToSearch) {
			if (edge.name.equals(currentEdge.name)) {
				if ((edge.startNode != currentEdge.endNode) || (edge.endNode != currentEdge.startNode)) {
					return edge;
				}
			}
		}
		return null;
	}

	/**
	 * Find the Lane of within x, y
	 * @return Lane
	 */

	public Lane findLaneAtPoint(final double lat, final double lon){

		double minDist = 100;
        Lane lane = null;
		for (Lane l : lanes){

			double[] line = RoadUtil.findLineEquation(l.latStart, l.latEnd, l.lonStart, l.lonEnd);

            double distance  = Line2D.ptSegDist(l.lonStart, l.latStart * settings.lonVsLat,
                    l.lonEnd, l.latEnd * settings.lonVsLat, lon, lat * settings.lonVsLat);

			if (Math.abs(distance) < minDist) {
				minDist = Math.abs(distance);
				lane = l;
			}
		}
		if (minDist < 0.001)
			return lane;
		else return null;
	}

	/**
	 * Get the index of the nearest edge to a given point.
	 */
	public Edge getEdgeAtPoint(final double lat, final double lon) {
		final double latPerRow = Math.abs(maxLat - minLat) / settings.numGridRows;
		final double lonPerCol = Math.abs(maxLon - minLon) / settings.numGridCols;
		final int row = (int) Math.floor(Math.abs(lat - minLat) / latPerRow);
		final int col = (int) Math.floor(Math.abs(lon - minLon) / lonPerCol);
		double minDistToEdge = 10000000;
		double distToEdge = minDistToEdge;
		Edge edgeFound = null;
		for (int i = row - 1; i <= (row + 1); i++) {
			if ((i < 0) || (i >= settings.numGridRows)) {
				continue;
			}
			for (int j = col - 1; j <= (col + 1); j++) {
				if ((j < 0) || (j >= settings.numGridCols)) {
					continue;
				}
				for (final Node node : grid[i][j].nodes) {
					for (final Edge edge : node.inwardEdges) {
						/*
						 * Skip edges whose bounding rectangle does not contain
						 * the point.
						 */
						if ((edge.startNode.lat < lat) && (edge.endNode.lat < lat)) {
							continue;
						}
						if ((edge.startNode.lon < lon) && (edge.endNode.lon < lon)) {
							continue;
						}
						if ((edge.startNode.lat > lat) && (edge.endNode.lat > lat)) {
							continue;
						}
						if ((edge.startNode.lon > lon) && (edge.endNode.lon > lon)) {
							continue;
						}

						distToEdge = Line2D.ptLineDist(edge.startNode.lon, edge.startNode.lat * settings.lonVsLat,
								edge.endNode.lon, edge.endNode.lat * settings.lonVsLat, lon, lat * settings.lonVsLat);
						if (distToEdge < minDistToEdge) {
							edgeFound = edge;
							minDistToEdge = distToEdge;
						}
					}
				}
			}
		}

		if (minDistToEdge < (5 * RoadUtil.getLongitudeDegreePerMeter(lat))) {
			return edgeFound;
		} else {
			return null;
		}
	}

	public HashSet<Edge> getEdgesOnGivenRoadAtGivenAngle(final String roadName, final double inputStartLat,
			final double inputStartLon, final double inputEndLat, final double inputEndLon) {
		final double latPerRow = Math.abs(maxLat - minLat) / settings.numGridRows;
		final double lonPerCol = Math.abs(maxLon - minLon) / settings.numGridCols;
		final int row = (int) Math.floor(Math.abs(inputStartLat - minLat) / latPerRow);
		final int col = (int) Math.floor(Math.abs(inputStartLon - minLon) / lonPerCol);
		final double maxLat = inputStartLat > inputEndLat ? inputStartLat : inputEndLat;
		final double maxLon = inputStartLon > inputEndLon ? inputStartLon : inputEndLon;
		final double minLat = inputStartLat < inputEndLat ? inputStartLat : inputEndLat;
		final double minLon = inputStartLon < inputEndLon ? inputStartLon : inputEndLon;
		final double angleRangeInRadians = Math.PI / 2.0;
		final double angleInput = Math.atan2(inputEndLat - inputStartLat, inputEndLon - inputStartLon);
		final HashSet<Edge> edgesFound = new HashSet<>();
		for (int i = row - 1; i <= (row + 1); i++) {
			if ((i < 0) || (i >= settings.numGridRows)) {
				continue;
			}
			for (int j = col - 1; j <= (col + 1); j++) {
				if ((j < 0) || (j >= settings.numGridCols)) {
					continue;
				}
				for (final Node node : grid[i][j].nodes) {
					if ((node.lat < minLat) || (node.lat > maxLat) || (node.lon < minLon) || (node.lon > maxLon)) {
						continue;
					}
					for (final Edge edge : node.outwardEdges) {
						// Edge's name must match the given name
						if (edge.name.equals(roadName)) {
							// Edge's angle should be similar to that of the given segment
							final double angleEdge = Math.atan2(edge.endNode.lat - edge.startNode.lat,
									edge.endNode.lon - edge.startNode.lon);
							if (Math.abs(angleInput - angleEdge) > angleRangeInRadians) {
								continue;
							}
							edgesFound.add(edge);
						}
					}
				}
			}
		}
		return edgesFound;
	}

	/**
	 * Get the edges aligned with a directional line
	 */
	public ArrayList<Edge> getEdgesParallelToEdge(final double inputStartLat, final double inputStartLon,
			final double inputEndLat, final double inputEndLon) {

		final double latPerRow = Math.abs(maxLat - minLat) / settings.numGridRows;
		final double lonPerCol = Math.abs(maxLon - minLon) / settings.numGridCols;
		final int row = (int) Math.floor(Math.abs(inputStartLat - minLat) / latPerRow);
		final int col = (int) Math.floor(Math.abs(inputStartLon - minLon) / lonPerCol);

		final double scanRangeInMeters = 10;
		final double angleRangeInRadians = 0.1;
		final double angleInput = Math.atan2(inputEndLat - inputStartLat, inputEndLon - inputStartLon);

		final double distInputStartEndInMeters = RoadUtil.getDistInMeters(inputStartLat, inputStartLon, inputEndLat,
				inputEndLon);
		final double distInputStartEndInGps = Point2D.distance(inputStartLon, inputStartLat * settings.lonVsLat,
				inputEndLon, inputEndLat * settings.lonVsLat);
		final double distInputMeterToGpsRatio = distInputStartEndInMeters / distInputStartEndInGps;

		final ArrayList<Edge> edgesFound = new ArrayList<>();

		for (int i = row - 1; i <= (row + 1); i++) {
			if ((i < 0) || (i >= settings.numGridRows)) {
				continue;
			}
			for (int j = col - 1; j <= (col + 1); j++) {
				if ((j < 0) || (j >= settings.numGridCols)) {
					continue;
				}
				for (final Node node : grid[i][j].nodes) {
					for (final Edge edge : node.outwardEdges) {
						// Edge's start or end must be close to the input segment
						final double distEdgeStartToInputSegmentInMeters = Line2D.ptSegDist(inputStartLon,
								inputStartLat * settings.lonVsLat, inputEndLon, inputEndLat * settings.lonVsLat,
								edge.startNode.lon, edge.startNode.lat * settings.lonVsLat) * distInputMeterToGpsRatio;
						final double distEdgeEndToInputSegmentInMeters = Line2D.ptSegDist(inputStartLon,
								inputStartLat * settings.lonVsLat, inputEndLon, inputEndLat * settings.lonVsLat,
								edge.endNode.lon, edge.endNode.lat * settings.lonVsLat) * distInputMeterToGpsRatio;
						if ((distEdgeStartToInputSegmentInMeters > scanRangeInMeters)
								&& (distEdgeEndToInputSegmentInMeters > scanRangeInMeters)) {
							continue;
						}
						// Tow edges' angles must be similar
						final double angleEdge = Math.atan2(edge.endNode.lat - edge.startNode.lat,
								edge.endNode.lon - edge.startNode.lon);
						if (Math.abs(angleInput - angleEdge) > angleRangeInRadians) {
							continue;
						}
						edgesFound.add(edge);
					}
				}
			}
		}

		return edgesFound;
	}

	/**
	 * Get street names near a given point.
	 */
	public HashSet<String> getStreetNamesNearPoint(final double lat, final double lon) {
		final HashSet<String> names = new HashSet<>();
		final double latPerRow = Math.abs(maxLat - minLat) / settings.numGridRows;
		final double lonPerCol = Math.abs(maxLon - minLon) / settings.numGridCols;
		final int row = (int) Math.floor(Math.abs(lat - minLat) / latPerRow);
		final int col = (int) Math.floor(Math.abs(lon - minLon) / lonPerCol);
		final double scanRange = 30;
		for (int i = row - 1; i <= (row + 1); i++) {
			if ((i < 0) || (i >= settings.numGridRows)) {
				continue;
			}
			for (int j = col - 1; j <= (col + 1); j++) {
				if ((j < 0) || (j >= settings.numGridCols)) {
					continue;
				}
				for (final Node node : grid[i][j].nodes) {
					for (final Edge edge : node.inwardEdges) {
						/*
						 * Skip edges whose bounding rectangle does not contain
						 * the point.
						 */
						if ((edge.startNode.lat < lat) && (edge.endNode.lat < lat)) {
							continue;
						}
						if ((edge.startNode.lon < lon) && (edge.endNode.lon < lon)) {
							continue;
						}
						if ((edge.startNode.lat > lat) && (edge.endNode.lat > lat)) {
							continue;
						}
						if ((edge.startNode.lon > lon) && (edge.endNode.lon > lon)) {
							continue;
						}

						final double distToEdge = Line2D.ptLineDist(edge.startNode.lon,
								edge.startNode.lat * settings.lonVsLat, edge.endNode.lon,
								edge.endNode.lat * settings.lonVsLat, lon, lat * settings.lonVsLat);
						if ((distToEdge < scanRange) && (edge.name.length() > 0)) {
							names.add(edge.name);
						}
					}
				}
			}
		}
		return names;
	}

	/**
	 * Construct a road network graph based on nodes and edges using a text file
	 * with road graph data.
	 */
	void importNodesEdges() {
		final String[] rows = settings.roadGraph.split(settings.delimiterItem);

		/*
		 * Bounds of the map
		 */
		String[] fields = rows[0].split(settings.delimiterSubItem);
		minLat = Double.parseDouble(fields[1]);
		minLon = Double.parseDouble(fields[3]);
		maxLat = Double.parseDouble(fields[5]);
		maxLon = Double.parseDouble(fields[7]);
		int numNodes = Integer.parseInt(fields[9]);

		/*
		 * Update the distance ratio between longitude and latitude.
		 */
		final double midLat = 0.5 * (minLat + maxLat);
		settings.lonVsLat = 1.0 / Math.cos(Math.toRadians(midLat));

		/*
		 * Construct nodes and outward edges from the nodes while reading data.
		 */

		for (int i = 1; i < rows.length; i++) {
			// Ignore the first row
			fields = rows[i].split(settings.delimiterSubItem, -1);

			int position = 0;

			// Basic information of the node			
			int nodeIndex = Integer.parseInt(fields[++position]);
			long osmId = Long.parseLong(fields[++position]);
			final String nameNode = fields[++position];
			double lat = Double.parseDouble(fields[++position]);
			double lon = Double.parseDouble(fields[++position]);
			boolean hasTrafficSignal = Boolean.parseBoolean(fields[++position]);
			boolean tram_stop = Boolean.parseBoolean(fields[++position]);
			boolean bus_stop = Boolean.parseBoolean(fields[++position]);
			final Node node = new Node(settings, osmId, nameNode, lat, lon, hasTrafficSignal, tram_stop, bus_stop);
			nodes.add(node);
			// Information of outward edges
			int numEdges = Integer.parseInt(fields[++position]);
			for (int j = 0; j < numEdges; j++) {
				int startNodeIndex = nodeIndex;
				position++;//Skip <Edge> tag
				int endNodeIndex = Integer.parseInt(fields[++position]);
				int numLanes = Integer.parseInt(fields[++position]);
				int numRightLanes = Integer.parseInt(fields[++position])+1; //TODO: update numright lanes and numleft lanes
				int numLeftLanes = Integer.parseInt(fields[++position])+1;
				int numRightOnlyLanes = Integer.parseInt(fields[++position]);
				int numLeftOnlyLanes = Integer.parseInt(fields[++position]);
				final String type = fields[++position];
				final String nameEdge = fields[++position];
				double maxspeed = Double.parseDouble(fields[++position]) / 3.6;// speed needs to be converted from km/h to m/s
				boolean roundabout = Boolean.parseBoolean(fields[++position]);
				final ArrayList<String> tramRoutesRef = new ArrayList<>(Arrays.asList(fields[++position].split("-")));
				if ((tramRoutesRef.size() > 0) && (tramRoutesRef.get(0).length() == 0)) {
					tramRoutesRef.clear();
				}
				final ArrayList<String> busRoutesRef = new ArrayList<>(Arrays.asList(fields[++position].split("-")));
				if ((busRoutesRef.size() > 0) && (busRoutesRef.get(0).length() == 0)) {
					busRoutesRef.clear();
				}
				final Edge edge = new Edge(startNodeIndex, endNodeIndex, type, nameEdge, maxspeed, roundabout,
						tramRoutesRef, busRoutesRef, numRightLanes, numLeftLanes, numRightOnlyLanes, numLeftOnlyLanes, settings);
				edges.add(edge);
				createLanes(edge, numLanes);
				node.outwardEdges.add(edge);

				/*
				 * Add edge to routes of trams/buses
				 */
				if (tramRoutesRef.size() > 0) {
					for (final String ref : tramRoutesRef) {
						if (!tramRoutes.containsKey(ref)) {
							tramRoutes.put(ref, new ArrayList<Edge>());
						}
						tramRoutes.get(ref).add(edge);
					}
				}
				if (busRoutesRef.size() > 0) {
					for (final String ref : busRoutesRef) {
						if (!busRoutes.containsKey(ref)) {
							busRoutes.put(ref, new ArrayList<Edge>());
						}
						busRoutes.get(ref).add(edge);
					}
				}

			}
		}

		/*
		 * Assign start/end nodes of edges
		 */
		for (final Edge e : edges) {
			final Node startNode = nodes.get(e.importedStartNodeIndex);
			final Node endNode = nodes.get(e.importedEndNodeIndex);
			e.startNode = startNode;
			e.endNode = endNode;
			e.angleOutward = Math.atan2((e.startNode.lat - e.endNode.lat) * settings.lonVsLat,
					e.startNode.lon - e.endNode.lon);
			if (e.angleOutward < 0) {
				e.angleInward = e.angleOutward + Math.PI;
			} else {
				e.angleInward = e.angleOutward - Math.PI;
			}
		}

		/*
		 * Get the length of each edge
		 */
		calculateEdgeLength();

		/*
		 * Store the inward edges at nodes
		 */
		for (final Node startNode : nodes) {
			for (final Edge outwardEdge : startNode.outwardEdges) {
				final Node endNode = outwardEdge.endNode;
				endNode.inwardEdges.add(outwardEdge);
			}
		}

		/*
		 * Calculate GPS points of lanes
		 */
		for (final Lane lane : lanes) {
			final double[] gpsPoints = RoadUtil.getLaneGPS(lane, settings.isDriveOnLeft, settings.laneWidthInMeters, settings.lonVsLat);
			lane.lonStart = gpsPoints[0];
			lane.latStart = gpsPoints[1];
			lane.lonEnd = gpsPoints[2];
			lane.latEnd = gpsPoints[3];
		}

		/*
		 * Sort inward edges based on angle
		 */
		for (final Node node : nodes) {
			node.connectedNodes = RoadUtil.sortEdgesBasedOnAngle(node, settings.lonVsLat);
		}

	}

	public boolean isNodeInsideRectangle(final Node node, final List<double[]> listRect) {
		for (final double[] rect : listRect) {
			if ((node.lon >= rect[0]) && (node.lon <= rect[2]) && (node.lat >= rect[3]) && (node.lat <= rect[1])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set the index of objects in the corresponding lists.
	 */
	void setIndexes() {
		/*
		 * Assign node id
		 */
		for (int i = 0; i < nodes.size(); i++) {
			nodes.get(i).index = i;
		}
		/*
		 * Assign edge id
		 */
		for (int i = 0; i < edges.size(); i++) {
			edges.get(i).index = i;
		}
		/*
		 * Assign lane id
		 */
		for (int i = 0; i < lanes.size(); i++) {
			lanes.get(i).index = i;
		}
	}

	public void findIntersectionPolygons(){
		for (Node node : nodes) {
			node.findIntersectionPolygon();
		}
	}

	public void updateLaneDirections(ArrayList<Integer> edges){
		for (Integer edgeIndex : edges) {
			Edge edge =  this.edges.get(edgeIndex);
			Lane lane = edge.getLane(edge.getLaneCount()-1);
			lane.isDirectionChanging = true;
		}
	}

}

package traffic.routing;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.road.RoadUtil;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

public class RandomAStar extends Routing {
	double overdoFactorInAStar = 2;//Parameter in Overdo A* routing
	boolean isRandomOverdoFactorInAStar = false;//Whether use random Overdo A*
	boolean isConsiderTrafficInOverdoAStar = false;//If true, heavy traffic increases cost in Overdo A*
	double minRandomOverdoFactorInAStar = 1;//Lower bound of random factor in Overdo A*
	double maxRandomOverdoFactorInAStar = 2;//Upper bound of random factor in Overdo A*

	class DijkstraEdge {
		public final DijkstraVertex target;
		public final double weight;
		public final Edge edge;

		public DijkstraEdge(final DijkstraVertex argTarget, final double argWeight, final Edge argEdge) {
			target = argTarget;
			weight = argWeight;
			edge = argEdge;
		}
	}

	class DijkstraVertex {
		public Node node;
		public ArrayList<DijkstraEdge> adjacencies = new ArrayList<>();
		public double knownCost = Double.POSITIVE_INFINITY;
		public double knownCostPlusHeuristicCost = Double.POSITIVE_INFINITY;
		public double directDistanceToDestination = Double.POSITIVE_INFINITY;
		public DijkstraVertex previous = null;
		RoadNetwork roadNetwork;
		DijkstraVertexState state = DijkstraVertexState.None;

		public DijkstraVertex(final Node node) {
			this.node = node;
		}
	}

	class DijkstraVertexComparator implements Comparator<DijkstraVertex> {

		@Override
		public int compare(final DijkstraVertex o1, final DijkstraVertex o2) {
			if (o1.knownCostPlusHeuristicCost < o2.knownCostPlusHeuristicCost) {
				return -1;
			}
			if (o1.knownCostPlusHeuristicCost > o2.knownCostPlusHeuristicCost) {
				return 1;
			}
			return 0;
		}

	}

	enum DijkstraVertexState {
		Unvisited, Visited, None
	};

	ArrayList<DijkstraVertex> vertices = new ArrayList<>();

	DijkstraVertex startVertex, endVertex;

	Random random = new Random();

	public RandomAStar(final TrafficNetwork trafficNetwork) {
		super(trafficNetwork);

		// Clear existing vertex list
		vertices = new ArrayList<>(trafficNetwork.nodes.size());

		// Collect vertices inside search space
		for (final Node node : trafficNetwork.nodes) {
			final DijkstraVertex vertex = new DijkstraVertex(node);
			vertices.add(vertex);
		}

		// Add outward edges to adjacent list
		Node targetNode = null;
		for (final DijkstraVertex vertex : vertices) {
			final ArrayList<DijkstraEdge> adjacentEdges = new ArrayList<>();
			for (final Edge outwardEdge : vertex.node.outwardEdges) {
				targetNode = outwardEdge.endNode;
				final DijkstraEdge edge = new DijkstraEdge(vertices.get(targetNode.index), outwardEdge.length,
						outwardEdge);
				adjacentEdges.add(edge);
			}
			vertex.adjacencies = adjacentEdges;
		}
	}

	public void computePathsFromTo(final Edge startEdge, final Edge endEdge, final VehicleType type) {
		// Reset
		final Node sourceNode = startEdge.startNode;
		final Node destinationNode = endEdge.endNode;
		final double metersPerLongitude = RoadUtil
				.getMetersPerLongitudeDegree((sourceNode.lon + destinationNode.lon) / 2);
		for (final DijkstraVertex v : vertices) {
			v.knownCost = Double.POSITIVE_INFINITY;
			v.knownCostPlusHeuristicCost = Double.POSITIVE_INFINITY;
			v.previous = null;
			v.state = DijkstraVertexState.None;
			// Use simple approach to approximate distance
			v.directDistanceToDestination = metersPerLongitude * Point2D.distance(sourceNode.lon,
					sourceNode.lat * Settings.lonVsLat, destinationNode.lon, destinationNode.lat * Settings.lonVsLat);
		}

		final DijkstraVertex source = vertices.get(sourceNode.index);
		source.knownCost = 0;
		source.previous = null;

		PriorityQueue<DijkstraVertex> unvisited = new PriorityQueue<>(10, new DijkstraVertexComparator());
		unvisited.add(source);
		source.state = DijkstraVertexState.Unvisited;

		while (unvisited.size() > 0) {
			final DijkstraVertex u = unvisited.poll();
			u.state = DijkstraVertexState.Visited;

			if (u.node == destinationNode) {
				break;
			}

			// Visit each edge exiting u
			for (final DijkstraEdge e : u.adjacencies) {
				if (!VehicleUtil.canGoThrough(u.node, e.target.node, type)) {
					continue;
				}
				final DijkstraVertex v = e.target;
				if (v.state == DijkstraVertexState.Visited) {
					continue;
				}

				final double knownCostPlusHeuristicCostThroughU = getKnownCostPlusHeuristicCostThroughPoint(u, e, v,
						overdoFactorInAStar);

				if (knownCostPlusHeuristicCostThroughU < v.knownCostPlusHeuristicCost) {
					v.knownCost = u.knownCost + e.weight;
					v.knownCostPlusHeuristicCost = knownCostPlusHeuristicCostThroughU;
					v.previous = u;
				}

				if (v.state == DijkstraVertexState.None) {
					unvisited.offer(v);
					v.state = DijkstraVertexState.Unvisited;
				}
			}

			if (isRandomOverdoFactorInAStar) {
				unvisited = updateCostOfUnvisitedWithRandomFactor(unvisited);
			}
		}
	}

	public ArrayList<RouteLeg> createCompleteRoute(final Edge startEdge, final Edge endEdge, final VehicleType type) {
		computePathsFromTo(startEdge, endEdge, type);
		final List<DijkstraVertex> path = getPathTo(endEdge.endNode);
		if (path.size() < 2) {
			return null;
		}
		final ArrayList<RouteLeg> legsOnRoute = new ArrayList<>(1000);
		for (int i = 1; i < path.size(); i++) {
			final Edge edge = getEdgeFromVertices(path.get(i - 1), path.get(i));
			legsOnRoute.add(new RouteLeg(edge, 0));
		}
		RouteUtil.removeRepeatSections(legsOnRoute);
		return legsOnRoute;
	}

	Edge getEdgeFromVertices(final DijkstraVertex startVertex, final DijkstraVertex endVertex) {
		for (final Edge edge : startVertex.node.outwardEdges) {
			if (edge.endNode == endVertex.node) {
				return edge;
			}
		}
		return null;
	}

	double getFlowDensity(final Edge edge) {
		double numV = 0;
		for (final Lane lane : edge.lanes) {
			numV += lane.vehicles.size();
		}
		return numV / edge.length;
	}

	double getKnownCostPlusHeuristicCostThroughPoint(final DijkstraVertex u, final DijkstraEdge e,
			final DijkstraVertex v, final double overdoFactor) {
		double knownCostPlusHeuristicCostThroughU = Double.POSITIVE_INFINITY;
		if (isConsiderTrafficInOverdoAStar) {

			knownCostPlusHeuristicCostThroughU = u.knownCost + e.weight
					+ (overdoFactor * v.directDistanceToDestination * getFlowDensity(e.edge));
		} else {
			knownCostPlusHeuristicCostThroughU = u.knownCost + e.weight
					+ (overdoFactor * v.directDistanceToDestination);
		}

		return knownCostPlusHeuristicCostThroughU;
	}

	List<DijkstraVertex> getPathTo(final Node destination) {
		final List<DijkstraVertex> path = new ArrayList<>(1000);

		for (DijkstraVertex vertex = vertices.get(destination.index); vertex != null; vertex = vertex.previous) {
			path.add(vertex);
		}

		Collections.reverse(path);

		return path;
	}

	PriorityQueue<DijkstraVertex> updateCostOfUnvisitedWithRandomFactor(final PriorityQueue<DijkstraVertex> unvisited) {
		final double overdoFactor = minRandomOverdoFactorInAStar
				+ ((maxRandomOverdoFactorInAStar - minRandomOverdoFactorInAStar) * random.nextDouble());
		final PriorityQueue<DijkstraVertex> randomizedQueue = new PriorityQueue<>(10, new DijkstraVertexComparator());
		while (unvisited.size() > 0) {
			final DijkstraVertex u = unvisited.poll();
			final DijkstraVertex p = u.previous;

			for (final DijkstraEdge e : p.adjacencies) {
				if (e.target == u) {
					final double knownCostPlusHeuristicCostThroughP = getKnownCostPlusHeuristicCostThroughPoint(p, e, u,
							overdoFactor);
					u.knownCostPlusHeuristicCost = knownCostPlusHeuristicCostThroughP;
					randomizedQueue.offer(u);
					break;
				}
			}
		}
		return randomizedQueue;
	}
}

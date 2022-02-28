package traffic.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

public class Dijkstra extends Routing {

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
		public double minDistance = Double.POSITIVE_INFINITY;
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
			if (o1.minDistance < o2.minDistance) {
				return -1;
			}
			if (o1.minDistance > o2.minDistance) {
				return 1;
			}
			return 0;
		}

	};

	enum DijkstraVertexState {
		Unvisited, Visited, None
	}

	ArrayList<DijkstraVertex> vertices = new ArrayList<>();

	DijkstraVertex startVertex, endVertex;

	public Dijkstra(final TrafficNetwork trafficNetwork) {
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
		for (final DijkstraVertex v : vertices) {
			v.minDistance = Double.POSITIVE_INFINITY;
			v.previous = null;
			v.state = DijkstraVertexState.None;
		}

		final Node sourceNode = startEdge.startNode;
		final Node destinationNode = endEdge.endNode;

		final DijkstraVertex source = vertices.get(sourceNode.index);
		source.minDistance = 0;
		source.previous = null;

		final PriorityQueue<DijkstraVertex> unvisited = new PriorityQueue<>(10, new DijkstraVertexComparator());
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

				final double distanceThroughU = u.minDistance + e.weight;
				if (distanceThroughU < v.minDistance) {
					v.minDistance = distanceThroughU;
					v.previous = u;
				}

				if (v.state == DijkstraVertexState.None) {
					unvisited.offer(v);
					v.state = DijkstraVertexState.Unvisited;
				}
			}
		}
	}

	public ArrayList<RouteLeg> createCompleteRoute(final Edge startEdge, final Edge endEdge, final VehicleType type) {

		computePathsFromTo(startEdge, endEdge, type);
		final List<DijkstraVertex> path = getPathTo(endEdge.endNode);
		if (path.size() < 2) {
			return null;
		}
		final ArrayList<RouteLeg> legsOnRoute = new ArrayList<>();
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

	List<DijkstraVertex> getPathTo(final Node destination) {
		final List<DijkstraVertex> path = new ArrayList<>();

		for (DijkstraVertex vertex = vertices.get(destination.index); vertex != null; vertex = vertex.previous) {
			path.add(vertex);
		}

		Collections.reverse(path);

		return path;
	}
}

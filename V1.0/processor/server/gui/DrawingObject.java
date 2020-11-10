package processor.server.gui;

import java.util.Comparator;

import traffic.road.RoadType;

public class DrawingObject {

	public static class EdgeObject {
		double startNodeLon, startNodeLat, endNodeLon, endNodeLat;
		int index, numLanes;
		String note = "";
		boolean leftLaneBlocked = false, rightLaneBlocked = false;
		boolean[] laneBlocks;
		double length;
		RoadType type;

		public EdgeObject(final double startNodeLon, final double startNodeLat, final double endNodeLon,
				final double endNodeLat, final int index, final int numLanes, final String note, final double length,
				final RoadType type) {
			super();
			this.startNodeLon = startNodeLon;
			this.startNodeLat = startNodeLat;
			this.endNodeLon = endNodeLon;
			this.endNodeLat = endNodeLat;
			this.index = index;
			this.numLanes = numLanes;
			this.note = note;
			laneBlocks = new boolean[this.numLanes];
			for (int i = 0; i < laneBlocks.length; i++) {
				laneBlocks[i] = false;
			}
			this.length = length;
			this.type = type;
		}

	}

	public static class EdgeObjectComparator implements Comparator<EdgeObject> {
		@Override
		public int compare(final EdgeObject e1, final EdgeObject e2) {
			return e1.index < e2.index ? -1 : e1.index == e2.index ? 0 : 1;
		}
	}

	public static class IntersectionObject {
		double lon, lat;
		int edgeIndex;
		boolean isAtEdgeStart = true;

		public IntersectionObject(final double lon, final double lat, final int edgeIndex,
				final boolean isAtEdgeStart) {
			this.lon = lon;
			this.lat = lat;
			this.edgeIndex = edgeIndex;
			this.isAtEdgeStart = isAtEdgeStart;
		}
	}

	public static class TramStopObject {
		double lon, lat;

		public TramStopObject(final double lon, final double lat) {
			super();
			this.lon = lon;
			this.lat = lat;
		}
	}
}

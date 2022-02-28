package traffic.road;

/**
 * Types of roads. The type names are consistent with OpenStreetMap data.
 *
 */
public enum RoadType {

	tram(14, 30, 1), motorway(13, 100, 4), motorway_link(13, 80, 1), trunk(11, 70, 3), trunk_link(11, 70, 1), primary(9,
			60, 2), primary_link(9, 60, 1), secondary(7, 50, 2), secondary_link(7, 50, 1), tertiary(5, 40,
					2), tertiary_link(5, 40, 1), residential(3, 40, 2), unclassified(2, 50, 2), service(1, 30, 1);

	public static boolean hasValue(final String value) {
		for (final RoadType type : values()) {
			if (type.name().equals(value)) {
				return true;
			}
		}
		return false;
	}

	public static boolean match(final RoadType type, final String[] values) {
		for (final String value : values) {
			if (type.name().equals(value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Priority of the road. Vehicles on a low-priority road may need to give
	 * way to vehicles on a high-priority road at an intersection.
	 */
	public int priority;

	/**
	 * Max speed.
	 */
	public int maxSpeed;

	/**
	 * Default number of lanes.
	 */
	public int numLanes;

	RoadType(final int priority, final int maxspeed, final int numLanes) {
		this.priority = priority;
		maxSpeed = maxspeed;
		this.numLanes = numLanes;
	}
}

package traffic.light;

public class LightUtil {
	public static TrafficLightTiming getLightTypeFromString(final String selected) {
		for (final TrafficLightTiming type : TrafficLightTiming.values()) {
			if (selected.equalsIgnoreCase(type.name())) {
				return type;
			}
		}
		return TrafficLightTiming.DYNAMIC;
	}
}

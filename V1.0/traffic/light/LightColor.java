package traffic.light;

/**
 * Colors of traffic light. The timing of each color can be different.
 *
 */
public enum LightColor {
	GYR_G("G"), GYR_Y("Y"), GYR_R("R"), KEEP_RED("KR");
	
	public String color;

	LightColor(final String color) {
		this.color = color;
	}

}

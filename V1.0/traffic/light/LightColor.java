package traffic.light;

/**
 * Colors of traffic light. The timing of each color can be different.
 *
 */
public enum LightColor {
	GYR_G("G", 10, 180, 30), GYR_Y("Y", 10, 10, 10), GYR_R("R", 5, 5, 5), KEEP_RED("KR", 0, 0, 0);
	
	public String color;
	public double minDynamicTime;
	public double maxDynamicTime;
	public double fixedTime;

	LightColor(final String color, final double minDynamicTime, final double maxDynamicTime, final double fixedTime) {
		this.color = color;
		this.minDynamicTime = minDynamicTime;
		this.maxDynamicTime = maxDynamicTime;
		this.fixedTime = fixedTime;
	}

}

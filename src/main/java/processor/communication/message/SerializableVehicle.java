package processor.communication.message;

import java.util.ArrayList;

public class SerializableVehicle {
	public String type;
	public ArrayList<SerializableRouteLeg> routeLegs = new ArrayList<>();
	public int indexRouteLeg;
	public int laneIndex;
	public double headPosition;
	public double speed;
	public double timeRouteStart;
	public String id;
	public boolean isExternal;
	public boolean isForeground;
	public long idLightGroupPassed = -1;
	public String driverProfile;

	public SerializableVehicle() {

	}
}

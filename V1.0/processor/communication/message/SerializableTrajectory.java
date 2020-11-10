package processor.communication.message;

import java.util.ArrayList;

public class SerializableTrajectory {
	public String vehicleId = "";
	public ArrayList<SerializableTrajectoryPoint> trajectoryPoints = new ArrayList<>();

	public SerializableTrajectory() {

	}

	public SerializableTrajectory(final String vehicleId,
			final ArrayList<SerializableTrajectoryPoint> trajectoryPoints) {
		this.vehicleId = vehicleId;
		this.trajectoryPoints = trajectoryPoints;
	}

}

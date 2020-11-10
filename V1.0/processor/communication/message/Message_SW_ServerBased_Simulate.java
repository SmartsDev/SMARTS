package processor.communication.message;

import java.util.UUID;

/**
 * Server-to-worker message that asks worker to simulate one step.
 */
public class Message_SW_ServerBased_Simulate {

	public boolean isNewNonPubVehiclesAllowed;
	public boolean isNewTramsAllowed;
	public boolean isNewBusesAllowed;
	public String uuid;

	public Message_SW_ServerBased_Simulate() {

	}

	public Message_SW_ServerBased_Simulate(boolean isNewNonPubVehiclesAllowed, boolean isNewTramsAllowed,
			boolean isNewBusesAllowed, String uuid) {
		this.isNewNonPubVehiclesAllowed = isNewNonPubVehiclesAllowed;
		this.isNewTramsAllowed = isNewTramsAllowed;
		this.isNewBusesAllowed = isNewBusesAllowed;
		this.uuid = uuid;
	}

}

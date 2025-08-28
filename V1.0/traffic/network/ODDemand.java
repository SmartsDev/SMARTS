package traffic.network;

import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.VehicleType;

public class ODDemand{

    public ODDemand(){
        this.origin = null;
        this.destination = null;
        this.numVehicles = 0;
        this.vehicleType = null;
    }


    public ODDemand(Node origin, Node destination, int numVehicles, VehicleType vehicleType) {
        this.origin = origin;
        this.destination = destination;
        this.numVehicles = numVehicles;
        this.vehicleType = vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public Node getOrigin() {
        return origin;
    }

    public void setOrigin(Node origin) {
        this.origin = origin;
    }

    public void setDestination(Node destination) {
        this.destination = destination;
    }

    public void setNumVehicles(int numVehicles) {
        this.numVehicles = numVehicles;
    }

    public Node getDestination() {
        return destination;
    }

    public int getNumVehicles() {
        return numVehicles;
    }

    private Node origin;
    private Node destination;
    private int numVehicles;
    private VehicleType vehicleType;

}

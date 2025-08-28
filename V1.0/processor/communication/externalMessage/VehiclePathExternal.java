package processor.communication.externalMessage;

import java.util.ArrayList;

public class VehiclePathExternal {
    private ArrayList<Integer> path = null;
    private int numVehicles = 0;

    public VehiclePathExternal(ArrayList<Integer> path, int numVehicles) {
        this.path = path;
        this.numVehicles = numVehicles;
    }

    public ArrayList<Integer> getPath() {
        return path;
    }

    public void setPath(ArrayList<Integer> path) {
        this.path = path;
    }

    public int getNumVehicles() {
        return numVehicles;
    }

    public void setNumVehicles(int numVehicles) {
        this.numVehicles = numVehicles;
    }
}

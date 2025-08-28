package traffic.network;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.VehicleType;

import java.util.ArrayList;
import java.util.List;

public class RushHourTrafficGenerator extends TrafficGenerator {

    public RushHourTrafficGenerator(ODDistributor odDistributor, TemporalDistributor temporalDistributor, VehicleTypeDistributor vehicleTypeDistributor, int numODpairs) {
        super(odDistributor, temporalDistributor, vehicleTypeDistributor);
        this.numODPairs = numODpairs;
    }

    private int numODPairs = 0;

    @Override
    public ArrayList<ODDemand> getGeneratedTraffic(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeStep) {

        ArrayList<ODDemand> ODMatrix = new ArrayList<>();


        for (int i = 0; i < numODPairs; i++){
            ODDemand odDemand = new ODDemand();

            Node[] OD = getOdDistributor().getStartAndEndEdge(trafficNetwork,  possibleStartEdges,  possibleEndEdges, timeStep);
            odDemand.setOrigin(OD[0]);
            odDemand.setDestination(OD[1]);

            int numVehicles = getTemporalDistributor().getCurrentVehicleLimit(0, timeStep, 18000);
            odDemand.setNumVehicles(numVehicles);

            VehicleType vehicleType = getVehicleTypeDistributor().getVehicleType();

            odDemand.setVehicleType(vehicleType);

            ODMatrix.add(odDemand);
        }

        return ODMatrix;

    }

    @Override
    public void getSettings(Settings settings) {
        super.getSettings(settings);
        this.numODPairs = settings.numODPairs;
    }
}

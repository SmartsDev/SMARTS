package traffic.network;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.trafficData.Coordinates;
import traffic.trafficData.TrafficLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewYorkTaxiGenerator extends TrafficGenerator{

    TrafficLoader tfl = null;

    public NewYorkTaxiGenerator(){
        super();
        tfl = new TrafficLoader();
        setVehicleTypeDistributor(new DefaultVehicleTypeDistributor());
        tfl.setTimeLimitRead(420, 450);
        tfl.setDate("1/06/2015");

        setOdDistributor(new PredefinedODLoader(0));
        setTemporalDistributor(new PreDefinedDemandLoader(1));
    }

    @Override
    public ArrayList<ODDemand> getGeneratedTraffic(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeStep) {
        ArrayList<ODDemand> odDemands = new ArrayList<>();

        //convert time to minutes

        int timeInminutes = (int) ((timeStep/trafficNetwork.getSettings().numStepsPerSecond)/60) + 1;

        ArrayList<Coordinates> coordinates = tfl.readUntilTime(timeInminutes);


        for (Coordinates coord : coordinates){
            ArrayList<Node> ODpair = mapCoordToNodes(coord, trafficNetwork);

            if ((ODpair.get(0) != null) && (ODpair.get(1) != null) && (ODpair.get(0) != ODpair.get(1))) {
                ODDemand odDemand = new ODDemand();
                odDemand.setOrigin(ODpair.get(0));
                odDemand.setDestination(ODpair.get(1));
                odDemand.setNumVehicles(12); // Value 20 for upscale
                                             // 12 for 7 am
                                             // 14 for 12 pm
                odDemand.setVehicleType(getVehicleTypeDistributor().getVehicleType());

                odDemands.add(odDemand);
            }
        }

        return odDemands;
    }

    public ArrayList<Node> mapCoordToNodes(Coordinates coord, TrafficNetwork trafficNetwork){
        ArrayList<Node> nodes = new ArrayList<>();
        Double distanceToSource = 200.0; // a large distance
        Double distanceToDestination = 200.0;  // a large distance

        Node source = null;
        Node destination = null;

        for (Node node : trafficNetwork.nodes){

            Double distNodeToStart = RoadUtil.getDistInMeters(node.lat, node.lon, coord.getLatStart(), coord.getLonStart());
            Double distNodeToEnd = RoadUtil.getDistInMeters(node.lat, node.lon, coord.getLatEnd(), coord.getLonEnd());

            if (distanceToSource > distNodeToStart){
                distanceToSource = distNodeToStart;
                source = node;
            }

            if (distanceToDestination > distNodeToEnd){
                distanceToDestination = distNodeToEnd;
                destination = node;
            }
        }

        nodes.add(source);
        nodes.add(destination);

        return nodes;

    }
}

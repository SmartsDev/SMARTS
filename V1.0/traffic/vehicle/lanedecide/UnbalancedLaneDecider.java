package traffic.vehicle.lanedecide;

import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;

public class UnbalancedLaneDecider extends LaneDecider {
    @Override
    public void computeLanes(Vehicle vehicle) {

    }

    @Override
    public Lane getNextEdgeLane(Vehicle vehicle) {
        if(vehicle.lane != null){
            Edge nextEdge = vehicle.getNextEdge();
            if (nextEdge.endNode == vehicle.lane.edge.startNode) { // for U turn
                return nextEdge.getLane(0);
            }
            else if (vehicle.edgeBeforeTurnRight == vehicle.lane.edge){
                Lane newLane;
                int laneNumberFromRoadside = vehicle.lane.edge.getLaneCount() - vehicle.lane.laneNumber;

                if (nextEdge.getLaneCount()-laneNumberFromRoadside > 0){
                    newLane = nextEdge.getLane(nextEdge.getLaneCount()-laneNumberFromRoadside);
                }
                else{
                    newLane = nextEdge.getLane(0);
                }
                return newLane;
            }

            else {
                Lane newLane;
                if (nextEdge.getLaneCount() <= vehicle.lane.laneNumber) {
                    newLane = nextEdge.getLane(nextEdge.getLaneCount() - 1);

                    /*int index = 2;
                    while (nextEdge.getLaneCount()-index >= 0) {
                        if (newLane.spaceLeftAtEnd() > nextEdge.getLane(nextEdge.getLaneCount() - index).spaceLeftAtEnd()) {
                            newLane = nextEdge.getLane(nextEdge.getLaneCount() - index);
                        }
                        index++;
                    }*/

                } else if (nextEdge.getLane(vehicle.lane.laneNumber).isDirectionChanging){
                    newLane = nextEdge.getLane(nextEdge.getLaneCount() - 2);
                } else {
                     if (nextEdge.getLane(vehicle.lane.laneNumber).isBlocked){
                         newLane = nextEdge.getFirstNonBlockedLane();
                     }
                     else {
                         newLane = nextEdge.getLane(vehicle.lane.laneNumber);
                     }
                }
                return newLane;
            }
        }else{
            RouteLeg firstLeg = vehicle.getRouteLeg(vehicle.indexLegOnRoute);
            return firstLeg.edge.getFirstNonBlockedLane();
        }
    }
}
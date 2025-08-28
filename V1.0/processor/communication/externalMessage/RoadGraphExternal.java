package processor.communication.externalMessage;

import traffic.road.RoadNetwork;
import traffic.road.Edge;

import java.util.ArrayList;

public class RoadGraphExternal {

    ArrayList<RoadExternal> roadlist = null;

    public RoadGraphExternal(){
        roadlist = new ArrayList<>();
    }

    public void buildRoadGraph(RoadNetwork roadNetwork){
        for (Edge edge : roadNetwork.edges){
            RoadExternal roadExt = new RoadExternal();

            roadExt.setIndex(edge.index);
            roadExt.setNumLanes(edge.getLaneCount());
            roadExt.setStartNode(edge.startNode.index);
            roadExt.setEndNode(edge.endNode.index);

            roadExt.setStartLat(edge.startNode.lat);
            roadExt.setStartLon(edge.startNode.lon);
            roadExt.setEndLat(edge.endNode.lat);
            roadExt.setEndLon(edge.endNode.lon);

            roadlist.add(roadExt);
        }
    }

}

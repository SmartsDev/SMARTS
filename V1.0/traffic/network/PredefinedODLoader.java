package traffic.network;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadNetwork;

import java.util.List;

public class PredefinedODLoader extends ODDistributor {

    private int counter = 0;
    private int iterator = 0;
    private int trafficType = 0;

    public PredefinedODLoader(int maxCounter) {
        this.maxCounter = maxCounter;
    }

    private int maxCounter = 2;
    //private int [][][] odMatrix = {{{13,1}, {1,13}, {8,11}, {11,8}, {7,4}, {4,7}, {14,2}, {2,14}},
    //                               {{12,2}, {2,12}, {13,7}, {7,13}, {14,2}, {2,14}, {8,3}, {3,8}}};

    //private int [][][] odMatrix = {{{6,42}, {42,6}, {8,11}, {11,8}, {7,4}, {4,7}, {14,2}, {2,14}},
    //        {{12,2}, {2,12}, {13,7}, {7,13}, {14,2}, {2,14}, {8,3}, {3,8}}};

    private  int [][][] odMatrix = {{ {40,26}, {33,47}, {46,4}, {4,46}, {45,3}, {3,45}, {44,2}, {2, 44}, {43,1}, {1,43},
                                     {41,35}, {35,41}, {34,28}, {28,34}, {27,21}, {21,27}, {20,14}, {14,20}, {13,7}, {7,13}}};

    //private int [][][] odMatrix = {{{20,14}, {14,20}}};
    //private int [][][] odMatrix ={{{7,0}}};
    //private int [][][] odMatrix ={{{5,0}}};

    //private int [][][] odMatrix ={{{4,1}, {5,1}}};

    @Override
    public Node[] getStartAndEndEdge(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeNow) {
        Node[] odpair = {trafficNetwork.nodes.get(odMatrix[trafficType][counter][0]), trafficNetwork.nodes.get(odMatrix[trafficType][counter][1])};
        iterate();
        if (timeNow > 2000) {
            trafficType = 0;
        }
        else { trafficType = 0;}
        return odpair;
    }

    @Override
    public void getSettings(Settings settings) {
        this.maxCounter = settings.numODPairs;
    }

    private void iterate(){
        counter ++;
        iterator ++;
        if (counter == maxCounter){ counter = 0;}

    }
}

package traffic.network;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;

import java.util.List;

public class RandomlyDistributedODDistributor extends ODDistributor {

    //private int[] nodeInArea1 = {40,43,34,53,69,74,41,30,56,10};
    //private int[] nodeInArea2 = {13,14,16,17,19,20,23,35,38,42};

    //7x7
    private int[] nodeInArea1 = {27,5,6,20,11,12,3,13,4,18};
    private int[] nodeInArea2 = {38,42,43,28,29,44,45,36,30,22};

    //12x12
    //private int[] nodeInArea1 = {132,134,122,136,124,84,85};
    //private int[] nodeInArea2 = {57,35,10,8,56,82,95};

    //9x9
    //private int[] nodeInArea1 = {64,72,55,63,74,65,77};
    //private int[] nodeInArea2 = {26,16,8,6,5,24,33};

    //5x5
    //private int[] nodeInArea1 = {20,15,10,22};
    //private int[] nodeInArea2 = {4,9,2,3};

    //3x3
    //private int[] nodeInArea1 = {1,2,3,6,7,14};
    //private int[] nodeInArea2 = {4,8,9,12,13,14};

    private int selectArea = 0;

    @Override
    public Node[] getStartAndEndEdge(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeNow) {

        Node node1 = trafficNetwork.nodes.get(nodeInArea1[getRandom().nextInt(nodeInArea1.length)]);
        Node node2 = trafficNetwork.nodes.get(nodeInArea2[getRandom().nextInt(nodeInArea2.length)]);

        int areaDecider = getRandom().nextInt(2);

        if (areaDecider == 0){
            selectArea = 1;
            return new Node[]{node1,node2};
        }
        else {
            selectArea = 0;
            return new Node[]{node2,node1};
        }

    }

    @Override
    public void getSettings(Settings settings) {

    }

}

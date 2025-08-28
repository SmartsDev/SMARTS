package traffic.routing;

import traffic.TrafficNetwork;

public class RoutingAlgoFactory {

    public Routing getRoutingAlgo(Routing.Algorithm algoName, TrafficNetwork trafficNetwork){
        if ( algoName == Routing.Algorithm.DIJKSTRA){
            return new Dijkstra(trafficNetwork);
        } else if (algoName == Routing.Algorithm.DIJKSTRA_LPF){
            return new Dijkstra_LPF(trafficNetwork);
        } else if (algoName == Routing.Algorithm.RANDOM_A_STAR){
            return  new RandomAStar(trafficNetwork);
        } else {
            return null;
        }

    }

}

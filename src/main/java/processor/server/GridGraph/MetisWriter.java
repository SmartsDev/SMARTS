package processor.server.GridGraph;


import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import traffic.road.GridCell;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by eman on 6/11/2015.
 */
class MetisWriter {
    
    String printGraph(Graph<Integer, DefaultWeightedEdge> g, Map<GridCell, Integer> cellGraphIndex, Map<String, GridCell> workCellsSet, OutputStream ss) {
        PrintStream os = new PrintStream(ss);
        os.print(g.vertexSet().size());
        os.print(' ');
        os.print(g.edgeSet().size());
        os.print(' ');
        os.print("011");
        os.print('\n');

//        if (g.vertexSet().contains(0))
//            throw new IllegalArgumentException("graph has vertex 0 which is not supported by metis");
        for(int vertex:g.vertexSet()){
            if(Objects.requireNonNull(GraphGenerator.getKey(cellGraphIndex, vertex)).laneLength < 1)
                os.print(1);
            else
                os.print(Objects.requireNonNull(GraphGenerator.getKey(cellGraphIndex, vertex)).laneLength);
            Set<Integer> neighbours= Graphs.neighborSetOf(g,vertex);
            for(int n:neighbours){
                os.print(' ');
                //print n id
                os.print(n);
                //print n weight
                os.print(' ');
                DefaultWeightedEdge e = g.getEdge(vertex,n);
                os.print((int) Math.round(g.getEdgeWeight(e)));
            }
            os.print('\n');
        }
        os.flush();
        os.close();
        return new String( os.toString() );
    }
    

//    public static void main(String[] args) throws IOException, ParseException, GraphBuildException {
//        Grph g = new InMemoryGrph();
//        g.grid(3, 3);
//        Relabelling rl = new Incrementlabelling(1);
//        Grph gg = rl.compute(g);
//        String s = new MetisWriter().printGraph(gg);
//        System.out.println(s);
//         gg.display();
//
//    }
    
    
}



package processor.server.GridGraph;

import common.Settings;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import traffic.road.Edge;
import traffic.road.GridCell;
import traffic.road.Node;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;

public class GraphGenerator {
    Graph<Integer, DefaultWeightedEdge> graph ;
    public Map<GridCell,Integer> cellGraphIndex = new HashMap<>();
    Map<String,GridCell> gridCells;
    private final Random r = new Random();
    public GraphGenerator(GridCell[][] grid) {

        gridCells  = new HashMap<>();
        for (int row = 0; row < Settings.numGridRows; row++) {
            for (int col = 0; col < Settings.numGridCols; col++) {
                gridCells.put(grid[row][col].id,grid[row][col]);
            }
        }

        this.graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    }
    
    public static <K, V> GridCell getKey(Map<GridCell, Integer> map, int value) {
        for (Map.Entry<GridCell, Integer> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
    public void buildGridGraph(Map<String,GridCell> workCellsSet)
    {
        int i = 0;
        for(GridCell cell:workCellsSet.values()) {
            int nodeWeight =(cell.laneLength);

            /*This is due to METIS issue (NO ZEROS)*/
            if (nodeWeight < 1) {
                nodeWeight = 1;

            }
            i++;
            cellGraphIndex.put(cell,i);
            graph.addVertex(i);

        }
       
        Map<Pair,Integer> edgeWeights = getEdgeWeights(workCellsSet);
        
        for (Pair l:edgeWeights.keySet()){
            int weight = edgeWeights.get(l);
            if (weight > 0) {
                graph.addEdge(l.getLeft(),l.getRight());
                graph.setEdgeWeight(l.getLeft(),l.getRight(),weight);

            }
        }
   }
    private Map<Pair,Integer>  getEdgeWeights(Map<String,GridCell> workerCells) {
        Map<Pair,Integer> edgeWeights = new HashMap<>();
        for(GridCell cell:workerCells.values()) {
            for (Node n : cell.nodes) {
                for (Edge e : n.outwardEdges) {
                    if(workerCells.containsKey(e.endNode.gridCell.id)) {
                        int source = (cellGraphIndex.get(workerCells.get(e.startNode.gridCell.id)));
                        int target = (cellGraphIndex.get(workerCells.get(e.endNode.gridCell.id)));
                        if (source != target) {
                            Pair link1 = new Pair(source, target);
                            Pair link2 = new Pair(target, source);
                            int cost = 1;
                            if (edgeWeights.containsKey(link1)) {
                                cost = edgeWeights.get(link1) + e.lanes.size();
                                edgeWeights.put(link1, cost);
                            } else if (edgeWeights.containsKey(link2)) {
                                cost = edgeWeights.get(link2) + e.lanes.size();
                                edgeWeights.put(link2, cost);
                                
                            } else if (!edgeWeights.containsKey(link1) && !edgeWeights.containsKey(link2)) {
                                edgeWeights.put(link1, e.lanes.size());
                                
                            }
                        }
                    }
                }
            }
        }
        return edgeWeights;
    }

    public List<IntSet> computePartitions(int numParts, Map<String,GridCell> workCellsSet) {

        Collection<IntSet> sets = new Metis().compute(this.graph, cellGraphIndex, numParts, new Random(), workCellsSet);

        return new ArrayList<>(sets);

    }

    public Map<Integer,Set<String>> balance() {
        buildGridGraph(gridCells);
        List<IntSet> parts = computePartitions(Settings.numWorkers,gridCells);
        return balanceByMETIS( parts, cellGraphIndex);
    }

    public  Map<Integer,Set<String>> balanceByMETIS(List<IntSet> parts,  Map<GridCell, Integer> cellGraphIndex) {
        Map<Integer,Set<String>> cellsParts = new HashMap<>();

        ArrayList<int[]> partitions = new ArrayList<>();
        for( IntSet item: parts){
            partitions.add(item.toIntArray());
        }

        for (int groupId = 0; groupId < partitions.size(); groupId++) {
            Set<String> cells = new HashSet<>();
            for (int cellIndex : partitions.get(groupId)) {


                cells.add(Objects.requireNonNull(GraphGenerator.getKey(cellGraphIndex, cellIndex)).id);
            }
            cellsParts.put((groupId+1),cells);

        }


        return cellsParts;

    }
    

}

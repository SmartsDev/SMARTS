package processor.communication.externalMessage;

import com.google.gson.Gson;
import org.apache.commons.statistics.distribution.PoissonDistribution;

import java.util.ArrayList;
import java.util.Random;


public class NetworkControls {
    public ArrayList<RoadControl> edges;

    NetworkControls(){
        edges = new ArrayList<>();
    }

    public int getPoissonRandom(double mean, Random r) {
        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        do {
            p = p * r.nextDouble();
            k++;
        } while (p > L);
        return k - 1;
    }

    public static void main(final String[] args) {
        NetworkControls rd = new NetworkControls();

        RoadControl rc1 = new RoadControl(1,false, 10);
        RoadControl rc2 = new RoadControl(2,true, 20);

        rd.edges.add(rc1);
        rd.edges.add(rc2);


        Random rnd = new Random(7);
        for (int i = 0; i < 10; i++){
            System.out.println("Number "+ i + " " + rd.getPoissonRandom(10, rnd));
        }

        Gson gson = new Gson();
        System.out.println(gson.toJson(rd));
    }

}

package traffic.network;

import common.Settings;

import java.util.Random;

public class PreDefinedDemandLoader extends TemporalDistributor {

    private int counter = 0;
    private int maxCounter = 2;
    private int demand = 20;
    private int freq = 4000;
    private int [][] demandMatrix;
    public int trafficType = 0;
    private boolean isUnidirectional = false;
    private int trafficGenarateDuration = 12000;

    private Random rand;

    public PreDefinedDemandLoader(int maxCounter) {
        this.maxCounter = maxCounter;
    }
    public PreDefinedDemandLoader(int maxCounter, int freq) {
        this.maxCounter = maxCounter;
        this.freq = freq;
    }
    public PreDefinedDemandLoader(int maxCounter, int freq, int demand) {
        this.maxCounter = maxCounter;
        this.freq = freq;
        this.demand = demand;

        rand = new Random(5);

        demandMatrix = new int[][]{{demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2},
            {2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand}};



        //demandMatrix = new int[][]{{demand,demand,demand,demand,demand,demand,demand,demand,demand,demand,demand,2,demand,2,demand,2,demand,2,demand,2},
        //        {demand,demand,demand,demand,demand,demand,demand,demand,demand,demand,2,demand,2,demand,2,demand,2,demand,2,demand}};

    }

    private  void setDemandMatrix(){ //TODO: Implement if more than one traffic type
        demandMatrix = new int[2][maxCounter];

        for (int i = 0; i < maxCounter; i++){
            if (i%2 == 0) {
                demandMatrix[0][i] = demand;
                demandMatrix[1][i] = 1;
            } else {
                demandMatrix[0][i] = 1;
                demandMatrix[1][i] = demand;
            }
            //demandMatrix[0][1] = 5;
        }


    }

    @Override
    public void getSettings(Settings settings){
        this.maxCounter = settings.numODPairs;
        this.freq = settings.demandChangedFreq;
        this.demand = settings.demandPerOneInterval;
        this.isUnidirectional = settings.isUnidirectional;
        this.trafficGenarateDuration = settings.trafficGenerateDuration;
        setDemandMatrix();
    }

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    @Override
    /** public int getCurrentVehicleLimit(int amount, int currentStep, int maxStep) {

        selectTrafficType(currentStep);
        int numVehicles = 0;
        if (true) {
            if (currentStep % 18000 < 15000) {
                numVehicles =  getPoissonRandom(15, rand);
            }
            else {
                numVehicles = 0; //demandMatrix[trafficType][counter];
            }

            if (currentStep%18000 == 0){
                rand = new Random(5);
            }

        }
        else
            numVehicles = 0;
        //System.out.println("Traffic: " + currentStep  + " " + demandMatrix[trafficType][0] + " and " +demandMatrix[trafficType][1]);
        iterate();
        return numVehicles;

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

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }

    public void selectTrafficType(int currentTime){
        //if (currentTime%freq == 0) {
        //    demandMatrix[trafficType][0] = getRandom().nextInt(36);
        //    demandMatrix[trafficType][1] = getRandom().nextInt(36);
        //}

        if ((currentTime/freq)%2 == 0) trafficType = 1;
        else trafficType = 0;
    }**/
    public int getCurrentVehicleLimit(int amount, int currentStep, int maxStep) {

        selectTrafficType(currentStep);
        int numVehicles = 0;
        if (currentStep < trafficGenarateDuration) {
            if (currentStep % freq < 250 ) {
                numVehicles = 0;
            }
             else {
              numVehicles = demandMatrix[trafficType][counter];
            }
        }
        else
            numVehicles = 0;

        iterate();
        return numVehicles;

    }

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }

    public void selectTrafficType(int currentTime){
        if (isUnidirectional == false){
            if ((currentTime/freq)%2 == 0) trafficType = 0;
            else trafficType = 1;
        }
    }

}

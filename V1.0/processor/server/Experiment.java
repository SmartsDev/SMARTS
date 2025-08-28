package processor.server;

import traffic.light.TrafficLightTiming;
import traffic.routing.Routing;

import java.util.ArrayList;
import java.util.List;

public class Experiment {

    String experimentId;
    int maxNumSteps;
    int numRandomPrivateVehicles;
    int numRandomTrams;
    int numRandomBusses;
    String foregroundVehicleFile;
    boolean loadOnlyODPairs;
    String backgroundVehicleFile;
    boolean outputInitialRoute;
    boolean outputSimulationLog;
    boolean outputTrajectory;
    boolean outputForegroundTravelTime;
    boolean outputSafety;
    boolean allowReroute;
    int numRuns = 1;
    double lookAheadDistance;
    int numStepsPerSecond;
    boolean serverBased;
    String osmMapFile;
    TrafficLightTiming trafficLightTiming;
    Routing.Algorithm routingAlgorithm;
    int trafficReportStepGapInServerlessMode;
    boolean driveOnLeft;
    String downLoadDirectory;
    String testName;
    double headwayMultiplier;
    String simulationListener;
    String odDistributor;
    String temporalDistributor;
    String vehicleTypeDistributor;
    boolean stopsAtMaxSteps;
    int updateStepInterval;
    String tlManager;

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public void setMaxNumSteps(int maxNumSteps) {
        this.maxNumSteps = maxNumSteps;
    }

    public void setNumRandomPrivateVehicles(int numRandomPrivateVehicles) {
        this.numRandomPrivateVehicles = numRandomPrivateVehicles;
    }

    public void setNumRandomTrams(int numRandomTrams) {
        this.numRandomTrams = numRandomTrams;
    }

    public void setNumRandomBusses(int numRandomBusses) {
        this.numRandomBusses = numRandomBusses;
    }

    public void setForegroundVehicleFile(String foregroundVehicleFile) {
        this.foregroundVehicleFile = foregroundVehicleFile;
    }

    public void setLoadOnlyODPairs(boolean loadOnlyODPairs) {
        this.loadOnlyODPairs = loadOnlyODPairs;
    }

    public void setBackgroundVehicleFile(String backgroundVehicleFile) {
        this.backgroundVehicleFile = backgroundVehicleFile;
    }

    public void setOutputInitialRoute(boolean outputInitialRoute) {
        this.outputInitialRoute = outputInitialRoute;
    }

    public void setOutputSimulationLog(boolean outputSimulationLog) {
        this.outputSimulationLog = outputSimulationLog;
    }

    public void setOutputTrajectory(boolean outputTrajectory) {
        this.outputTrajectory = outputTrajectory;
    }

    public void setOutputForegroundTravelTime(boolean outputForegroundTravelTime) {
        this.outputForegroundTravelTime = outputForegroundTravelTime;
    }

    public void setOutputSafety(boolean outputSafety) {
        this.outputSafety = outputSafety;
    }

    public void setAllowReroute(boolean allowReroute) {
        this.allowReroute = allowReroute;
    }

    public void setNumRuns(int numRuns) {
        this.numRuns = numRuns;
    }

    public void setLookAheadDistance(double lookAheadDistance) {
        this.lookAheadDistance = lookAheadDistance;
    }

    public void setNumStepsPerSecond(int numStepsPerSecond) {
        this.numStepsPerSecond = numStepsPerSecond;
    }

    public void setServerBased(boolean serverBased) {
        this.serverBased = serverBased;
    }

    public void setOsmMapFile(String osmMapFile) {
        this.osmMapFile = osmMapFile;
    }

    public void setTrafficLightTiming(TrafficLightTiming trafficLightTiming) {
        this.trafficLightTiming = trafficLightTiming;
    }

    public void setRoutingAlgorithm(Routing.Algorithm routingAlgorithm) {
        this.routingAlgorithm = routingAlgorithm;
    }

    public void setTrafficReportStepGapInServerlessMode(int trafficReportStepGapInServerlessMode) {
        this.trafficReportStepGapInServerlessMode = trafficReportStepGapInServerlessMode;
    }

    public void setDriveOnLeft(boolean driveOnLeft) {
        this.driveOnLeft = driveOnLeft;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public int getMaxNumSteps() {
        return maxNumSteps;
    }

    public int getNumRandomPrivateVehicles() {
        return numRandomPrivateVehicles;
    }

    public int getNumRandomTrams() {
        return numRandomTrams;
    }

    public int getNumRandomBusses() {
        return numRandomBusses;
    }

    public String getForegroundVehicleFile() {
        return foregroundVehicleFile;
    }

    public boolean isLoadOnlyODPairs() {
        return loadOnlyODPairs;
    }

    public String getBackgroundVehicleFile() {
        return backgroundVehicleFile;
    }

    public boolean isOutputInitialRoute() {
        return outputInitialRoute;
    }

    public boolean isOutputSimulationLog() {
        return outputSimulationLog;
    }

    public boolean isOutputTrajectory() {
        return outputTrajectory;
    }

    public boolean isOutputForegroundTravelTime() {
        return outputForegroundTravelTime;
    }

    public boolean isOutputSafety() {
        return outputSafety;
    }

    public boolean isAllowReroute() {
        return allowReroute;
    }

    public int getNumRuns() {
        return numRuns;
    }

    public double getLookAheadDistance() {
        return lookAheadDistance;
    }

    public int getNumStepsPerSecond() {
        return numStepsPerSecond;
    }

    public boolean isServerBased() {
        return serverBased;
    }

    public String getOsmMapFile() {
        return osmMapFile;
    }

    public TrafficLightTiming getTrafficLightTiming() {
        return trafficLightTiming;
    }

    public Routing.Algorithm getRoutingAlgorithm() {
        return routingAlgorithm;
    }

    public int getTrafficReportStepGapInServerlessMode() {
        return trafficReportStepGapInServerlessMode;
    }

    public boolean isDriveOnLeft() {
        return driveOnLeft;
    }

    public String getDownLoadDirectory() {
        return downLoadDirectory;
    }

    public void setDownLoadDirectory(String downLoadDirectory) {
        this.downLoadDirectory = downLoadDirectory;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public double getHeadwayMultiplier() {
        return headwayMultiplier;
    }

    public void setHeadwayMultiplier(double headwayMultiplier) {
        this.headwayMultiplier = headwayMultiplier;
    }

    public String getSimulationListener() {
        return simulationListener;
    }

    public void setSimulationListener(String simulationListener) {
        this.simulationListener = simulationListener;
    }

    public String getOdDistributor() {
        return odDistributor;
    }

    public void setOdDistributor(String odDistributor) {
        this.odDistributor = odDistributor;
    }

    public String getTemporalDistributor() {
        return temporalDistributor;
    }

    public void setTemporalDistributor(String temporalDistributor) {
        this.temporalDistributor = temporalDistributor;
    }

    public String getVehicleTypeDistributor() {
        return vehicleTypeDistributor;
    }

    public void setVehicleTypeDistributor(String vehicleTypeDistributor) {
        this.vehicleTypeDistributor = vehicleTypeDistributor;
    }

    public boolean isStopsAtMaxSteps() {
        return stopsAtMaxSteps;
    }

    public void setStopsAtMaxSteps(boolean stopsAtMaxSteps) {
        this.stopsAtMaxSteps = stopsAtMaxSteps;
    }

    public int getUpdateStepInterval() {
        return updateStepInterval;
    }

    public void setUpdateStepInterval(int updateStepInterval) {
        this.updateStepInterval = updateStepInterval;
    }

    public String getTlManager() {
        return tlManager;
    }

    public void setTlManager(String tlManager) {
        this.tlManager = tlManager;
    }

    public void setValues(String key, String value){
        switch (key) {
            case "driveOnLeft": {
                setDriveOnLeft(Boolean.parseBoolean(value));
                break;
            }
            case "maxNumSteps": {
                setMaxNumSteps(Integer.parseInt(value));
                break;
            }
            case "numRandomPrivateVehicles": {
                setNumRandomPrivateVehicles(Integer.parseInt(value));
                break;
            }
            case "numRandomTrams": {
                setNumRandomTrams(Integer.parseInt(value));
                break;
            }
            case "numRandomBuses": {
                setNumRandomBusses(Integer.parseInt(value));
                break;
            }
            case "foregroundVehicleFile": {
                setForegroundVehicleFile(value);
                break;
            }
            case "loadOnlyODPairs": {
                setLoadOnlyODPairs(Boolean.parseBoolean(value));
                break;
            }
            case "backgroundVehicleFile": {
                setBackgroundVehicleFile(value);
                break;
            }
            case "outputSimulationLog": {
                setOutputSimulationLog(Boolean.parseBoolean(value));
                break;
            }
            case "outputForegroundTravelTime": {
                setOutputForegroundTravelTime(Boolean.parseBoolean(value));
                break;
            }
            case "outputTrajectory": {
                setOutputTrajectory(Boolean.parseBoolean(value));
                break;
            }
            case "outputInitialRoute": {
                setOutputInitialRoute(Boolean.parseBoolean(value));
                break;
            }
            case "numRuns": {
                setNumRuns(Integer.parseInt(value));
                break;
            }
            case "lookAheadDistance": {
                setLookAheadDistance(Double.parseDouble(value));
                break;
            }
            case "numStepsPerSecond": {
                setNumStepsPerSecond(Integer.parseInt(value));
                break;
            }
            case "serverBased": {
                setServerBased(Boolean.parseBoolean(value));
                break;
            }
            case "openStreetMapFile": {
                setOsmMapFile(value);
                break;
            }
            case "trafficLightTiming": {
                try {
                    TrafficLightTiming newTiming = TrafficLightTiming.valueOf(value);
                    setTrafficLightTiming(newTiming);
                } catch (Exception e) {
                    System.out.println("Traffic light timing value is invalid.");
                }
                break;
            }
            case "tlManager": {
                setTlManager(value);
                break;
            }
            case "routingAlgorithm": {
                try {
                    Routing.Algorithm newAlgorithm = Routing.Algorithm.valueOf(value);
                    setRoutingAlgorithm(newAlgorithm);
                } catch (Exception e) {
                    System.out.println("Routing algorithm value is invalid.");
                }
                break;
            }
            case "trafficReportStepGapInServerlessMode": {
                setTrafficReportStepGapInServerlessMode(Integer.parseInt(value));
                break;
            }
            case "allowReroute": {
                setAllowReroute(Boolean.parseBoolean(value));
                break;
            }
            case "outputSafety": {
                setOutputSafety(Boolean.parseBoolean(value));
                break;
            }
            case "downloadDirectory": {
                setDownLoadDirectory(value);
                break;
            }
            case "testName": {
                setTestName(value);
                break;
            }
            case "headWayMultiplier":{
                setHeadwayMultiplier(Double.parseDouble(value));
                break;
            }
            case "simulationListener":{
                setSimulationListener(value);
                break;
            }
            case "odDistributor":{
                setOdDistributor(value);
                break;
            }
            case "temporalDistributor":{
                setTemporalDistributor(value);
                break;
            }
            case "vehicleTypeDistributor":{
                setVehicleTypeDistributor(value);
                break;
            }
            case "stopsAtMaxSteps":{
                setStopsAtMaxSteps(Boolean.valueOf(value));
                break;
            }
            case "updateStepInterval":{
                setUpdateStepInterval(Integer.valueOf(value));
                break;
            }
        }
    }

    public Experiment clone(){
        Experiment experiment = new Experiment();
        experiment.setDriveOnLeft(driveOnLeft);
        experiment.setMaxNumSteps(maxNumSteps);
        experiment.setNumRandomPrivateVehicles(numRandomPrivateVehicles);
        experiment.setNumRandomTrams(numRandomTrams);
        experiment.setNumRandomBusses(numRandomBusses);
        experiment.setForegroundVehicleFile(foregroundVehicleFile);
        experiment.setLoadOnlyODPairs(loadOnlyODPairs);
        experiment.setBackgroundVehicleFile(backgroundVehicleFile);
        experiment.setOutputSimulationLog(outputSimulationLog);
        experiment.setOutputForegroundTravelTime(outputForegroundTravelTime);
        experiment.setOutputTrajectory(outputTrajectory);
        experiment.setOutputInitialRoute(outputInitialRoute);
        experiment.setNumRuns(numRuns);
        experiment.setLookAheadDistance(lookAheadDistance);
        experiment.setNumStepsPerSecond(numStepsPerSecond);
        experiment.setServerBased(serverBased);
        experiment.setOsmMapFile(osmMapFile);
        experiment.setTrafficLightTiming(trafficLightTiming);
        experiment.setTlManager(tlManager);
        experiment.setRoutingAlgorithm(routingAlgorithm);
        experiment.setTrafficReportStepGapInServerlessMode(trafficReportStepGapInServerlessMode);
        experiment.setAllowReroute(allowReroute);
        experiment.setOutputSafety(outputSafety);
        experiment.setDownLoadDirectory(downLoadDirectory);
        experiment.setTestName(testName);
        experiment.setHeadwayMultiplier(headwayMultiplier);
        experiment.setSimulationListener(simulationListener);
        experiment.setOdDistributor(odDistributor);
        experiment.setTemporalDistributor(temporalDistributor);
        experiment.setVehicleTypeDistributor(vehicleTypeDistributor);
        experiment.setStopsAtMaxSteps(stopsAtMaxSteps);
        experiment.setUpdateStepInterval(updateStepInterval);
        return experiment;
    }
}

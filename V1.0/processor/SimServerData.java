package processor;

import com.google.gson.Gson;
import common.Settings;
import osm.OSM;
import processor.communication.message.*;
import processor.server.*;
import processor.server.gui.GUI;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.road.RoadUtil;
import traffic.road.Edge;
import traffic.road.Lane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Copyright (c) 2019, The University of Melbourne.
 * All rights reserved.
 * <p>
 * You are permitted to use the code for research purposes provided that the following conditions are met:
 * <p>
 * * You cannot redistribute any part of the code.
 * * You must give appropriate citations in any derived work.
 * * You cannot use any part of the code for commercial purposes.
 * * The copyright holder reserves the right to change the conditions at any time without prior notice.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * Created by tmuthugama on 3/18/2019
 */
public class SimServerData {
    private RoadNetwork roadNetwork;
    private GUI gui;
    private ConsoleUI consoleUI;
    private ScriptLoader scriptLoader = new ScriptLoader();
    private FileOutput fileOutput;
    private double simulationWallTime = 0;//Total time length spent on simulation
    private int totalNumWwCommChannels = 0;//Total number of communication channels between workers. A worker has two channels with a neighbor worker, one for sending and one for receiving.
    private ArrayList<SerializableRouteDump> allRoutes = new ArrayList<SerializableRouteDump>();
    private HashMap<String, TreeMap<Double, double[]>> allTrajectories = new HashMap<String, TreeMap<Double, double[]>>();
    private long timeStamp = 0;
    private int numInternalNonPubVehiclesAtAllWorkers = 0;
    private int numInternalTramsAtAllWorkers = 0;
    private int numInternalBusesAtAllWorkers = 0;
    private ArrayList<Node> nodesToAddLight = new ArrayList<>();
    private ArrayList<Node> nodesToRemoveLight = new ArrayList<>();
    private int numVehiclesCreatedDuringSetup = 0;//For updating setup progress on GUI
    private int numVehiclesNeededAtStart = 0;//For updating setup progress on GUI
    private int step = 0;//Time step in the current simulation
    private TrjOutput trjOutput;
    private VehicleDataOutput vdOutput;
    private List<Experiment> experiments = null;
    private int experimentIndex = 0;
    private int runIndex = 0;
    private int lastVehicleFinishedStep = 0;
    private int notFinishedVehicles = 0;
    private Settings settings;
    private boolean isNewMap;

    public SimServerData(Settings settings) {
        this.settings = settings;
        this.fileOutput = new FileOutput(settings);
    }

    public void updateSettings(Settings settings){
        this.settings = settings;
    }

    public void showNumberOfConnectedWorkers(int number) {
        if (settings.isVisualize) {
            gui.updateNumConnectedWorkers(number);
        } else {
            System.out.println(number + "/" + settings.numWorkers + " workers connected.");
        }
    }

    public void initRoadNetwork() {
        // Load default road network
        settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile(settings.inputBuiltinRoadGraph);
        roadNetwork = new RoadNetwork(settings);
    }

    public void initUIs(SimulationProcessor processor) {
        // Start GUI or load simulation configuration without GUI
        if (settings.isVisualize) {
            if (gui != null) {
                gui.dispose();
            }
            final GUI newGUI = new GUI(processor);
            gui = newGUI;
            gui.setVisible(true);
        } else {
            consoleUI = new ConsoleUI(processor);
            consoleUI.acceptInitialConfigFromConsole();
        }
    }

    public void prepareForSetup() {
        if (settings.isVisualize) {
            gui.getReadyToSetup();
        } else {
            consoleUI.acceptSimScriptFromConsole();// Let user input simulation script path
        }
    }

    public boolean isNewMap() {
        return isNewMap;
    }

    public void setNewMap(boolean newMap) {
        isNewMap = newMap;
    }

    public void prepareForSetupAgain() {
        updateExperimentIndices();
        if ((settings.isVisualize)) {
            gui.getReadyToSetup();
            if (experiments != null && experiments.size() != experimentIndex) {
                System.out.println("Loading configuration of new simulation...");
                runAutomatedSim();
            }
        } else {
            //consoleUI.acceptConsoleCommandAtSimEnd();
            experiments =  scriptLoader.getExperiments();
            experimentIndex = 0;
            System.out.println("Loading configuration of new simulation...");
            startSimulationFromLoadedScript();
        }
    }

    public void updateFromReport(final ArrayList<Serializable_GUI_Vehicle> vehicleList,
                                 final ArrayList<Serializable_GUI_Light> lightList, final String workerName, final int numWorkers,
                                 final int step, ArrayList<SerializableRouteDump> randomRoutes,
                                 ArrayList<Serializable_Finished_Vehicle> finished, int numInternalNonPubVehicles,
                                 int numInternalTrams, int numInternalBuses, ArrayList<SerializableLaneIndex> laneList,
                                 ArrayList<SerializableLaneIndex> edgeList) {
        // Update GUI
        if (settings.isVisualize) {
            gui.updateObjectData(vehicleList, lightList, workerName, numWorkers, step);
        }
        // Add new vehicle position to its trajectory
        double timeStamp = step / settings.numStepsPerSecond;
        notFinishedVehicles = vehicleList.size();
        if (settings.isOutputTrajectory) {
            for (Serializable_GUI_Vehicle vehicle : vehicleList) {
                if (!allTrajectories.containsKey(vehicle.id)) {
                    allTrajectories.put(vehicle.id, new TreeMap<Double, double[]>());
                }
                allTrajectories.get(vehicle.id).put(timeStamp, new double[]{vehicle.displacement, vehicle.latHead, vehicle.lonHead});
            }
        }
        if(finished.size() > 0){
            lastVehicleFinishedStep = step;
        }
        for (Serializable_Finished_Vehicle finishedVehicle : finished) {
            vdOutput.outputVehicleData(finishedVehicle, timeStamp);
        }
        //trjOutput.outputTrajData(step, vehicleList); //TODO: Removed by Udesh: TrjOutput
        // Store routes of new vehicles created since last report
        if (settings.isOutputInitialRoutes) {
            allRoutes.addAll(randomRoutes);
        }
        // Increment vehicle counts
        numInternalNonPubVehiclesAtAllWorkers += numInternalNonPubVehicles;
        numInternalTramsAtAllWorkers += numInternalTrams;
        numInternalBusesAtAllWorkers += numInternalBuses;

        if (settings.isVisualize) updateLanes(laneList);
        getChangedLanesFromWorker(edgeList);
    }

    private void getChangedLanesFromWorker(ArrayList<SerializableLaneIndex> laneIndexes){
        ArrayList<Integer> edges = new ArrayList<>();
        for (SerializableLaneIndex index :laneIndexes){
            edges.add(index.index);
        }
        getRoadNetwork().updateLaneDirections(edges);
    }


    private void updateLanes(ArrayList<SerializableLaneIndex> laneIndexes){
        for (SerializableLaneIndex laneIndex : laneIndexes) {
            Lane lane = getRoadNetwork().lanes.get(laneIndex.index);
            Edge currEdge = lane.edge;
            lane.moveLaneToOppositeEdge();
            Edge newEdge = lane.edge;

            gui.updateEdgeObjects(currEdge);
            gui.updateEdgeObjects(newEdge);
        }

    }

    public void startSimulationInUI() {
        if (settings.isVisualize) {
            gui.startSimulation();
        }
    }

    public void resetStepInUI() {
        if (settings.isVisualize) {
            gui.stepToDraw = 0;
        }
    }

    public void updateSetupprogressInUI(int created) {
        numVehiclesCreatedDuringSetup += created;
        double createdVehicleRatio = (double) numVehiclesCreatedDuringSetup / numVehiclesNeededAtStart;
        if (createdVehicleRatio > 1) {
            createdVehicleRatio = 1;
        }

        if (settings.isVisualize) {
            gui.updateSetupProgress(createdVehicleRatio);
        }
    }

    public RoadNetwork getRoadNetwork() {
        return roadNetwork;
    }

    public void changeMap() {
        settings.listRouteSourceWindowForInternalVehicle.clear();
        settings.listRouteDestinationWindowForInternalVehicle.clear();
        settings.listRouteSourceDestinationWindowForInternalVehicle.clear();
        settings.isNewEnvironment = true;

        if (settings.inputOpenStreetMapFile.length() > 0) {
            final OSM osm = new OSM();
            osm.processOSM(settings.inputOpenStreetMapFile, true, settings);
            settings.isBuiltinRoadGraph = false;
            // Revert to built-in map if there was an error when converting new map
            if (settings.roadGraph.length() == 0) {
                settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile(settings.inputBuiltinRoadGraph);
                settings.isBuiltinRoadGraph = true;
            }
        } else {
            settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile(settings.inputBuiltinRoadGraph);
            settings.isBuiltinRoadGraph = true;
        }

        roadNetwork = new RoadNetwork(settings);//Build road network based on new road graph
    }

    public void initFileOutput() {
        // Initialize output
        fileOutput.init();
        //TODO: Removed by Udesh: trjOutput
        //trjOutput = new TrjOutput(fileOutput.getTrjFos(), settings.numWorkers, (int) settings.numStepsPerSecond, settings.maxNumSteps,
                //roadNetwork.minLon, roadNetwork.minLat, roadNetwork.mapWidth, roadNetwork.mapHeight);
        //trjOutput.outputMapData();
        vdOutput = new VehicleDataOutput(fileOutput.getVdFos());
    }

    public void writeOutputFiles(int step) {
        fileOutput.outputSimLog(step, simulationWallTime, totalNumWwCommChannels, (int) settings.numStepsPerSecond);
        fileOutput.outputRoutes(allRoutes);
        fileOutput.outputBestTTData(allRoutes);
        allRoutes.clear();
        fileOutput.outputTrajectories(allTrajectories);
        allTrajectories.clear();
        fileOutput.close();
    }

    public void startReLogging(){
        writeOutputFiles(step);
        initFileOutput();
    }


    /**
     * Updates wall time spent on simulation.
     */
    synchronized public void updateSimulationTime() {
        simulationWallTime += (double) (System.nanoTime() - timeStamp) / 1000000000;
    }

    public void takeTimeStamp() {
        timeStamp = System.nanoTime();
    }

    public boolean[] updateVehicleCounts() {
        boolean isNewNonPubVehiclesAllowed = true; //numInternalNonPubVehiclesAtAllWorkers < settings.numGlobalRandomPrivateVehicles;
        boolean isNewTramsAllowed = numInternalTramsAtAllWorkers < settings.numGlobalRandomTrams;
        boolean isNewBusesAllowed = numInternalBusesAtAllWorkers < settings.numGlobalRandomBuses;

        // Clear vehicle counts from last step
        numInternalNonPubVehiclesAtAllWorkers = 0;
        numInternalTramsAtAllWorkers = 0;
        numInternalBusesAtAllWorkers = 0;
        boolean[] required = new boolean[3];
        required[0] = isNewNonPubVehiclesAllowed;
        required[1] = isNewTramsAllowed;
        required[2] = isNewBusesAllowed;
        return required;
    }

    public void setLightChangeNode(Node node) {
        node.light = !node.light;
        nodesToAddLight.remove(node);
        nodesToRemoveLight.remove(node);
        if (node.light) {
            nodesToAddLight.add(node);
        } else {
            nodesToRemoveLight.add(node);
        }
    }

    public void resetVariablesForSetup() {
        // Reset temporary variables
        simulationWallTime = 0;
        totalNumWwCommChannels = 0;
        numVehiclesCreatedDuringSetup = 0;
        numVehiclesNeededAtStart = 0;
        step = 0;
        allRoutes.clear();
        allTrajectories.clear();
    }

    public void reserVariablesAtStop() {
        numInternalNonPubVehiclesAtAllWorkers = 0;
        numInternalTramsAtAllWorkers = 0;
        numInternalBusesAtAllWorkers = 0;
        nodesToAddLight.clear();
        nodesToRemoveLight.clear();
    }

    public void updateNoOfVehiclesNeededAtStart(int vehiclesInRouteLoader) {
        // Get number of vehicles needed
        numVehiclesNeededAtStart = vehiclesInRouteLoader + settings.numGlobalRandomPrivateVehicles
                + settings.numGlobalRandomTrams + settings.numGlobalRandomBuses;
    }

    public ArrayList<Node> getNodesToAddLight() {
        return nodesToAddLight;
    }

    public ArrayList<Node> getNodesToRemoveLight() {
        return nodesToRemoveLight;
    }

    public void updateChannelCount(int count) {
        totalNumWwCommChannels += count;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    /**
     * Get simulation setup from the imported setup list. The setup will be sent
     * to workers when server informs the workers to set up simulation. The
     * retrieved setup will be removed from the list.
     */
    public boolean setupForExperimentSimulation(Experiment experiment) {
        isNewMap = false;

        settings.isDriveOnLeft = experiment.isDriveOnLeft();
        settings.maxNumSteps = experiment.getMaxNumSteps();
        settings.numGlobalRandomPrivateVehicles = experiment.getNumRandomPrivateVehicles();
        settings.numGlobalRandomTrams = experiment.getNumRandomTrams();
        settings.numGlobalRandomBuses = experiment.getNumRandomBusses();
        if (experiment.getForegroundVehicleFile() != null && !experiment.getForegroundVehicleFile().equals("-")) {
            settings.inputForegroundVehicleFile = experiment.getForegroundVehicleFile();
        } else {
            settings.inputForegroundVehicleFile = "";
        }
        settings.inputOnlyODPairsOfForegroundVehicleFile = experiment.isLoadOnlyODPairs();
        if (experiment.getBackgroundVehicleFile() != null && !experiment.getBackgroundVehicleFile().equals("-")) {
            settings.inputBackgroundVehicleFile = experiment.getBackgroundVehicleFile();
        } else {
            settings.inputBackgroundVehicleFile = "";
        }
        settings.isOutputSimulationLog = experiment.isOutputSimulationLog();
//		settings.isOutputForegroundTravelTime = experiment.isOutputForegroundTravelTime();
        settings.isOutputTrajectory = experiment.isOutputTrajectory();
        settings.isOutputInitialRoutes = experiment.isOutputInitialRoute();
        settings.lookAheadDistance = experiment.getLookAheadDistance();
        settings.numStepsPerSecond = experiment.getNumStepsPerSecond();
        settings.isServerBased = experiment.isServerBased();
        if (experiment.getOsmMapFile() != null && !experiment.getOsmMapFile().equals("-")) {
            settings.inputOpenStreetMapFile = experiment.getOsmMapFile();
            isNewMap = true;
        }

        settings.trafficLightTiming = experiment.getTrafficLightTiming();
        if (experiment.getRoutingAlgorithm() != null) settings.routingAlgorithm = experiment.getRoutingAlgorithm();
        settings.trafficReportStepGapInServerlessMode = experiment.getTrafficReportStepGapInServerlessMode();
        settings.isAllowReroute = experiment.isAllowReroute();
        //settings.downloadDirectory = experiment.getDownLoadDirectory();
        settings.testName = experiment.getTestName();

        if (experiment.getOdDistributor() != null) settings.odDistributor = experiment.getOdDistributor();
        if (experiment.getTemporalDistributor() != null) settings.temporalDistributor = experiment.getTemporalDistributor();
        if (experiment.getSimulationListener() != null) settings.simulationListener = experiment.getSimulationListener();
        if (experiment.getVehicleTypeDistributor() != null) settings.vehicleTypeDistributor = experiment.getVehicleTypeDistributor();
        settings.stopsAtMaxSteps = experiment.isStopsAtMaxSteps();
        settings.safetyHeadwayMultiplier = experiment.getHeadwayMultiplier();
        settings.updateStepInterval = experiment.getUpdateStepInterval();
        if (experiment.getTlManager() != null) settings.tlManager = experiment.getTlManager();

        return isNewMap;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public int getExperimentIndex() {
        return experimentIndex;
    }

    public void startMultipleExperimentRunning() {
        scriptLoader.loadScriptFile(settings.inputSimulationScript);
        experiments = scriptLoader.getExperiments();
        experimentIndex = 0;
        runIndex = 1;
        runAutomatedSim();
    }

    public void updateExperimentIndices() {
        if(experiments != null) {
            if (experiments.size() == experimentIndex) {
                resetVariablesEndOfExperiment();
            }else {
                int runsNeed = experiments.get(experimentIndex).getNumRuns();
                if (runIndex == runsNeed) {
                    runIndex = 1;
                    experimentIndex++;
                } else {
                    runIndex++;
                }
            }
        }
    }

    public void resetVariablesEndOfExperiment(){
        settings.testName = settings.defaultTestName;
        settings.downloadDirectory = settings.defaultDownloadDirectory;
        experimentIndex = 0;
        runIndex = settings.defaultRunIndex;
        experiments = null;
    }

    public void loadSettingsFromScript() {
        experiments =  scriptLoader.getExperiments();
        Experiment experiment = experiments.get(0);
        boolean isNewMap = setupForExperimentSimulation(experiment);
        if (isNewMap) {
            changeMap();
        }
    }

    void startSimulationFromLoadedScript() {
        Experiment experiment = experiments.get(experimentIndex);
        boolean isNewMap = setupForExperimentSimulation(experiment);
        if (isNewMap) {
            changeMap();
        }
        consoleUI.startSimulationFromLoadedScript();
    }

    public void runAutomatedSim(){
        Experiment experiment = experiments.get(experimentIndex);
        boolean isNewMap = setupForExperimentSimulation(experiment);
        if (isNewMap) {
            gui.loadMapChange();
        }
        gui.updateGuiComps();
        System.out.println("Run next simulation");
        gui.runSimulationAuto();
    }

    public boolean loadScript() {
        return scriptLoader.loadScriptFile(settings.inputSimulationScript);
    }


    public boolean isSimulationStopReached(){
        if(getStep() < settings.maxNumSteps){
            return false;
        }else{
            if(settings.stopsAtMaxSteps) {
                return true;
            }else{
                if (notFinishedVehicles > 0) {
                    if ((getStep() - lastVehicleFinishedStep) > settings.gridlockDetectionTime * settings.numStepsPerSecond) {
                        System.out.println("Deadlock Reached at step " + getStep());
                        return true;
                    } else {
                        settings.maxNumSteps += 100;
                        System.out.println("Max step count extended at step " + getStep());
                        return false;
                    }
                } else {
                    System.out.println("All vehicles finished at step " + getStep());
                    return true;
                }
            }
        }
    }
}

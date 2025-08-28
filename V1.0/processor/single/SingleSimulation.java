package processor.single;

import common.Settings;
import processor.SimulationListener;
import processor.SimulationProcessor;
import processor.communication.message.SerializableRouteDump;
import processor.server.*;
import processor.server.gui.GUI;
import processor.worker.Fellow;
import processor.worker.Worker;
import traffic.TrafficNetwork;
import traffic.road.*;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
 * Created by tmuthugama on 9/19/2019
 */
public class SingleSimulation implements SimulationProcessor {

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

    public SingleSimulation() {
        this.settings = new Settings();
        this.fileOutput = new FileOutput(settings);
    }

    public void simulate(){
        //init road network
        settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile(settings.inputBuiltinRoadGraph);
        roadNetwork = new RoadNetwork(settings);
        //init uis
        if (settings.isVisualize) {
            if (gui != null) {
                gui.dispose();
            }
            final GUI newGUI = new GUI(this);
            gui = newGUI;
            gui.setVisible(true);
        } else {
            consoleUI = new ConsoleUI(this);
            consoleUI.acceptInitialConfigFromConsole();
        }
        //ready to setup
        if (settings.isVisualize) {
            gui.getReadyToSetup();
        } else {
            consoleUI.acceptSimScriptFromConsole();// Let user input simulation script path
        }
    }

    public void askWorkersChangeLaneDirection(int index) {}

    @Override
    public RoadNetwork getRoadNetwork() {
        return null;
    }

    @Override
    public void stopSim() {

    }

    @Override
    public void resumeSim() {

    }

    @Override
    public void pauseSim() {

    }

    @Override
    public void askWorkersChangeLaneBlock(int laneIndex, boolean block) {

    }

    @Override
    public void setLightChangeNode(Node nodeSelected) {

    }

    @Override
    public void changeMap() {

    }

    @Override
    public void onClose() {

    }

    @Override
    public void changeSpeed(int pauseTimeEachStep) {

    }

    @Override
    public void setupNewSim() {

    }

    @Override
    public void setupMultipleSim() {

    }

    @Override
    public boolean loadScript() {
        return false;
    }

    @Override
    public Settings getSettings() {
        return null;
    }
}

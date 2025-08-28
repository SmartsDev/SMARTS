package processor.communication.externalMessage;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import common.Settings;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import processor.communication.message.Message_WS_TrafficReport;
import traffic.TrafficNetwork;
import traffic.road.RoadNetwork;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class LaneManager implements ExternalSimulationListener {

    private LaneManager() {
        init();
    }

    private static class LaneManagerHelper{
        private static final LaneManager instance = new LaneManager();
    }


    public static ExternalSimulationListener getInstance(){
        return LaneManagerHelper.instance;
    }

    private ZContext context = null;
    private ZMQ.Socket socket = null;
    private RoadGraphExternal roadGraph = null;
    private RoadIndex rdIndex = null;
    private boolean isSettingsSent = false;

    private Settings settings = null;

    @Override
    public void init() {
        context = new ZContext();
        socket = context.createSocket(ZMQ.REP);
        socket.bind("tcp://localhost:5555");
        System.out.println("Created the server-side connection from SMARTS");
    }

    @Override
    public void getRoadGraph(RoadNetwork roadNetwork) {
        roadGraph = new RoadGraphExternal();
        roadGraph.buildRoadGraph(roadNetwork);
        // process and send
        Gson gson = new Gson();
        RoadExternal rd = new RoadExternal();
        String str = gson.toJson(roadGraph);

        sendNReceiveMessage(str);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void setSettings(Settings settings){
        this.settings = settings;
    }

    @Override
    public void getMessage(Message_WS_TrafficReport trafficReport) {
        // process and send
    }

    @Override
    public void getTrafficData(TrafficNetwork trafficNetwork) {
        TrafficData trafficData = new TrafficData();
        trafficData.setTrafficData(trafficNetwork);
        Gson gson = new Gson();
        String str = gson.toJson(trafficData);


        sendMessage(str); //update for server side
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        trafficNetwork.clearPaths();
    }

    public void sendTrafficData(TrafficNetwork trafficNetwork) {
        TrafficData trafficData = new TrafficData();
        trafficData.setTrafficData(trafficNetwork);
        Gson gson = new Gson();
        JsonElement jsonToSend = gson.toJsonTree(trafficData);
        String toSend = jsonToSend.toString();


        if (isSettingsSent == false) {
            JsonObject obj = jsonToSend.getAsJsonObject();
            JsonElement jsonTrafficData =  obj.get("trafficData");

            isSettingsSent = true;
            Gson gson_settings = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
            JsonElement jsonSettings = gson_settings.toJsonTree(settings);

            JsonObject combined = new JsonObject();
            combined.add("trafficData", jsonTrafficData);
            combined.add("settings", jsonSettings);

            toSend = combined.toString();
        }


        sendMessage(toSend);
        trafficNetwork.clearPaths();
    }

    @Override
    public void waitForAction() {
        byte[] reply = socket.recv();
        System.out.println("Action received");
        String data = new String(reply, ZMQ.CHARSET);
        Gson gson = new Gson();
        RoadIndex rdIndex = gson.fromJson(data, RoadIndex.class);
        this.rdIndex = rdIndex;
    }


    public RoadIndex getRoadDirChange(){
        RoadIndex extSend = new RoadIndex();
        extSend = this.rdIndex;
        this.rdIndex = null;
        return extSend;
    }

    @Override
    public ArrayList<Integer> laneChangeMessage() {
        return null;
    }

    private void sendNReceiveMessage(String str){
        socket.send(str.getBytes(ZMQ.CHARSET), 0);
        byte[] reply = socket.recv();
        String data = new String(reply, ZMQ.CHARSET);
        Gson gson = new Gson();
        RoadIndex rdIndex = gson.fromJson(data, RoadIndex.class);
        this.rdIndex = rdIndex;
    }

    private void sendMessage(String str){
        socket.send(str.getBytes(ZMQ.CHARSET), 0);
    }

    private void decodeMessageLayer(){

    }

    public void getAvailableControls(String command){

    }
}

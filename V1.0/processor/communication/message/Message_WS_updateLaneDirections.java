package processor.communication.message;

import java.util.ArrayList;

public class Message_WS_updateLaneDirections {
    ArrayList<SerializableLaneIndex> edgeIndexList = null;

    public Message_WS_updateLaneDirections(ArrayList<Integer> edgeIndexList){
        for (final Integer i : edgeIndexList){
            this.edgeIndexList.add(new SerializableLaneIndex(i));
        }
    }

    public ArrayList<SerializableLaneIndex> getEdges(){
        return edgeIndexList;
    }
}

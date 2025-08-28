package processor.communication.externalMessage;

public class RoadControl {
    public int index = 0;
    public boolean laneChange = false;
    public int speed = 0;

    public RoadControl(int index, boolean laneChange, int speed) {
        this.index = index;
        this.laneChange = laneChange;
        this.speed = speed;
    }
}

/**
 * This RoadExternal class defines the fields required to send to external listener
 */
package processor.communication.externalMessage;

public class RoadExternal {

    private int index;

    private int numVehicles;
    private int numVehiclesLeft;
    private int numVehiclesRight;
    private int numVehiclesStraight;

    private int numVehiclesOnMove;
    private int numVehiclesStopped;

    private double numVehiclesMvg;
    private double numVehiclesLeftMvg;
    private double numVehiclesRightMvg;
    private double numVehiclesStraightMvg;

    private int numLanes;

    private int startNode;
    private int endNode;

    private double startLat;
    private double startLon;

    private double endLat;
    private double endLon;

    public void updateMvgData(double numVehiclesMvg, double numVehiclesStraightMvg,
                              double numVehiclesRightMvg, double numVehiclesLeftMvg){
        this.numVehiclesMvg = numVehiclesMvg;
        this.numVehiclesStraightMvg = numVehiclesStraightMvg;
        this.numVehiclesRightMvg = numVehiclesRightMvg;
        this.numVehiclesLeftMvg = numVehiclesLeftMvg;
    }

    public double getStartLat() {
        return startLat;
    }

    public void setStartLat(double startLat) {
        this.startLat = startLat;
    }

    public double getStartLon() {
        return startLon;
    }

    public void setStartLon(double startLon) {
        this.startLon = startLon;
    }

    public double getEndLat() {
        return endLat;
    }

    public void setEndLat(double endLat) {
        this.endLat = endLat;
    }

    public double getEndLon() {
        return endLon;
    }

    public void setEndLon(double endLon) {
        this.endLon = endLon;
    }


    public int getStartNode() {
        return startNode;
    }

    public void setStartNode(int startNode) {
        this.startNode = startNode;
    }

    public int getEndNode() {
        return endNode;
    }

    public void setEndNode(int endNode) {
        this.endNode = endNode;
    }

    public RoadExternal(int index, int numVehicles, int numVehiclesLeft, int numVehiclesRight, int numVehiclesStraight, int numLanes) {
        this.index = index;
        this.numVehicles = numVehicles;
        this.numVehiclesLeft = numVehiclesLeft;
        this.numVehiclesRight = numVehiclesRight;
        this.numVehiclesStraight = numVehiclesStraight;
        this.numLanes = numLanes;
    }

    public RoadExternal(){ }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setNumVehicles(int numVehicles) {
        this.numVehicles = numVehicles;
    }

    public void setNumVehiclesLeft(int numVehiclesLeft) {
        this.numVehiclesLeft = numVehiclesLeft;
    }

    public void setNumVehiclesRight(int numVehiclesRight) {
        this.numVehiclesRight = numVehiclesRight;
    }

    public void setNumVehiclesStraight(int numVehiclesStraight) {
        this.numVehiclesStraight = numVehiclesStraight;
    }

    public void setNumLanes(int numLanes) {
        this.numLanes = numLanes;
    }

    public int getIndex() {
        return index;
    }

    public int getNumVehicles() {
        return numVehicles;
    }

    public int getNumVehiclesLeft() {
        return numVehiclesLeft;
    }

    public int getNumVehiclesRight() {
        return numVehiclesRight;
    }

    public int getNumVehiclesStraight() {
        return numVehiclesStraight;
    }

    public int getNumLanes() {
        return numLanes;
    }

    public int getNumVehiclesOnMove() {
        return numVehiclesOnMove;
    }

    public void setNumVehiclesOnMove(int numVehiclesOnMove) {
        this.numVehiclesOnMove = numVehiclesOnMove;
    }

    public int getNumVehiclesStopped() {
        return numVehiclesStopped;
    }

    public void setNumVehiclesStopped(int numVehiclesStopped) {
        this.numVehiclesStopped = numVehiclesStopped;
    }

    public double getNumVehiclesMvg() {
        return numVehiclesMvg;
    }

    public void setNumVehiclesMvg(double numVehiclesMvg) {
        this.numVehiclesMvg = numVehiclesMvg;
    }

    public double getNumVehiclesLeftMvg() {
        return numVehiclesLeftMvg;
    }

    public void setNumVehiclesLeftMvg(double numVehiclesLeftMvg) {
        this.numVehiclesLeftMvg = numVehiclesLeftMvg;
    }

    public double getNumVehiclesRightMvg() {
        return numVehiclesRightMvg;
    }

    public void setNumVehiclesRightMvg(double numVehiclesRightMvg) {
        this.numVehiclesRightMvg = numVehiclesRightMvg;
    }

    public double getNumVehiclesStraightMvg() {
        return numVehiclesStraightMvg;
    }

    public void setNumVehiclesStraightMvg(double numVehiclesStraightMvg) {
        this.numVehiclesStraightMvg = numVehiclesStraightMvg;
    }
}

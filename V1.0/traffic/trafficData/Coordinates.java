package traffic.trafficData;

public class Coordinates {

    private double latStart;
    private double latEnd;
    private double lonStart;
    private double lonEnd;

    public Coordinates(double latStart, double latEnd, double lonStart, double lonEnd) {
        this.latStart = latStart;
        this.latEnd = latEnd;
        this.lonStart = lonStart;
        this.lonEnd = lonEnd;
    }

    public Coordinates() {
        this.latStart = 0;
        this.latEnd = 0;
        this.lonStart = 0;
        this.lonEnd = 0;
    }

    public double getLatStart() {
        return latStart;
    }

    public void setLatStart(double latStart) {
        this.latStart = latStart;
    }

    public double getLatEnd() {
        return latEnd;
    }

    public void setLatEnd(double latEnd) {
        this.latEnd = latEnd;
    }

    public double getLonStart() {
        return lonStart;
    }

    public void setLonStart(double lonStart) {
        this.lonStart = lonStart;
    }

    public double getLonEnd() {
        return lonEnd;
    }

    public void setLonEnd(double lonEnd) {
        this.lonEnd = lonEnd;
    }
}

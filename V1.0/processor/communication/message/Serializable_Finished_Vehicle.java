package processor.communication.message;

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
 * Created by tmuthugama on 4/10/2019
 */
public class Serializable_Finished_Vehicle {
    public String id;
    public int vid;
    public int source;
    public int destination;
    public double bestTravelTime;
    public double actualTravelTime;
    public double routeLength;
    public double timeOnDirectionalTraffic;
    public double timeOnDirectionalTraffic_speed;
    public String route;

    public Serializable_Finished_Vehicle() {
    }

    public Serializable_Finished_Vehicle(String id, int vid, int source, int destination, double bestTravelTime, double actualTravelTime, double routeLength, String route) {
        this.id = id;
        this.vid = vid;
        this.source = source;
        this.destination = destination;
        this.bestTravelTime = bestTravelTime;
        this.actualTravelTime = actualTravelTime;
        this.routeLength = routeLength;
        this.timeOnDirectionalTraffic = 0;
        this.timeOnDirectionalTraffic_speed = 0;
        this.route = route;
    }

    public Serializable_Finished_Vehicle(String id, int vid, int source, int destination, double bestTravelTime, double actualTravelTime, double routeLength, String route, double timeOnDirectionalTraffic, double timeOnDirectionalTraffic_speed) {
        this.id = id;
        this.vid = vid;
        this.source = source;
        this.destination = destination;
        this.bestTravelTime = bestTravelTime;
        this.actualTravelTime = actualTravelTime;
        this.routeLength = routeLength;
        this.timeOnDirectionalTraffic = timeOnDirectionalTraffic;
        this.timeOnDirectionalTraffic_speed = timeOnDirectionalTraffic_speed;
        this.route = route;
    }


}

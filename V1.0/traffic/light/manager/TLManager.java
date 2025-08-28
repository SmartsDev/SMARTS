package traffic.light.manager;

import traffic.TrafficNetwork;
import traffic.light.TrafficLightCluster;
import traffic.light.phasehandler.TLPhaseHandler;
import traffic.light.scheduler.TLScheduleHandler;

import java.util.List;

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
 * Created by tmuthugama on 8/11/2019
 */
public abstract class TLManager {

    private TrafficNetwork trafficNetwork;
    private List<TrafficLightCluster> clusters;
    private TLScheduleHandler scheduleHandler;
    private TLPhaseHandler phaseHandler;
    private double horizon;

    public List<TrafficLightCluster> getClusters() {
        return clusters;
    }

    public TLScheduleHandler getScheduleHandler() {
        return scheduleHandler;
    }

    public TLPhaseHandler getPhaseHandler() {
        return phaseHandler;
    }

    public double getHorizon() {
        return horizon;
    }

    public void setHorizon(double horizon) {
        this.horizon = horizon;
    }

    public void setClusters(List<TrafficLightCluster> clusters) {
        this.clusters = clusters;
    }

    public void setScheduleHandler(TLScheduleHandler scheduleHandler) {
        this.scheduleHandler = scheduleHandler;
    }

    public void setPhaseHandler(TLPhaseHandler phaseHandler) {
        this.phaseHandler = phaseHandler;
    }

    public void init(TrafficNetwork trafficNetwork){
        this.trafficNetwork = trafficNetwork;
        setHandlers();
        scheduleHandler.update(trafficNetwork, clusters, phaseHandler, horizon, 0);
        for (TrafficLightCluster cluster : clusters) {
            cluster.updateLights(0);
        }
    }

    public TrafficNetwork getTrafficNetwork() {
        return trafficNetwork;
    }

    public abstract void setHandlers();

    public void schedule(double timeNow){
        scheduleHandler.update(trafficNetwork, clusters, phaseHandler, horizon, timeNow);
    }
}

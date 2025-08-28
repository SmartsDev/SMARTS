package traffic.light.scheduler;

import traffic.TrafficNetwork;
import traffic.light.*;
import traffic.light.phasehandler.TLPhaseHandler;
import traffic.light.schedule.TrafficLightSchedule;
import traffic.road.Lane;
import traffic.vehicle.Vehicle;

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
 * Created by tmuthugama on 8/22/2019
 */
public class DynamicSchedule extends TLScheduleHandler{

    private TreeMap<LightColor, Double> fixedPeriods;
    private double maxGreenTime;

    public DynamicSchedule() {
        fixedPeriods = new TreeMap<>();
        fixedPeriods.put(LightColor.GYR_G, 10.0);
        fixedPeriods.put(LightColor.GYR_Y, 10.0);
        fixedPeriods.put(LightColor.GYR_R, 5.0);
        maxGreenTime = 60.0;
    }

    @Override
    public void update(TrafficNetwork trafficNetwork, List<TrafficLightCluster> clusters, TLPhaseHandler phaseHandler, double horizon, double timeNow) {
        for (TrafficLightCluster cluster : clusters) {
            extendPhase(phaseHandler, cluster, timeNow);
            updateSchedule(phaseHandler, cluster, horizon, timeNow);
        }
    }

    public void extendPhase(TLPhaseHandler phaseHandler, TrafficLightCluster cluster, double timeNow){
        //if (cluster.hasInactivePhasePriorityVehicles() && !cluster.hasActivePhasePriorityVehicles()) {
        //Grant green light to an inactive approach it has priority vehicle and the current active approach does not have one
        //                cluster.setActivePhase(cluster.getInactivePhaseWithPriorityVehicles());
        //                setGYR(LightColor.GYR_G);
        //            }
        //            if (!cluster.hasInactivePhasePriorityVehicles() && cluster.hasActivePhasePriorityVehicles()) {
        //                // Grant green light to current active approach if it has a priority vehicle and inactive approaches do not have priority vehicle
        //                setGYR(LightColor.GYR_G);
        //            }
        TrafficLightSchedule schedule = cluster.getLightSchedule();
        LightPeriod current = schedule.getCurrentPeriod();
        if(current != null){
            if(current.getColor() == LightColor.GYR_G && timeNow >= current.getEnd()){
                if(hasActivePhaseTraffic(current.getPhase()) && current.getDur() < maxGreenTime && hasSpaceInNextEdge(current.getPhase())){
                    schedule.extendDuration(current, 1);
                }
            }
        }
        // Reset vehicle detection flag at all edges
        for (Phase phase : phaseHandler.getPhaseList(cluster, timeNow)) {
            for (Movement m : phase.getMovements()) {
                m.getControlEdge().isDetectedVehicleForLight = false;
            }
        }
    }

    public void updateSchedule(TLPhaseHandler phaseHandler, TrafficLightCluster cluster, double horizon, double timeNow) {
        TrafficLightSchedule existing = cluster.getLightSchedule();
        LightPeriod end = existing.getEndPeriod();
        List<Phase> phases = phaseHandler.getPhaseList(cluster, timeNow);
        double scheduleRemainder = 0;
        if(end != null){
            scheduleRemainder = end.getEnd() - timeNow;
        }
        while (horizon - scheduleRemainder > 0){
            Phase phase;
            double t;
            if(end != null){
                int i = phases.indexOf(end.getPhase());
                phase = phases.get((i + 1) % phases.size());
                t = end.getEnd();
            }else{
                phase = phases.get(0);
                t = 0;
            }
            for (LightColor color : fixedPeriods.keySet()) {
                double dur = fixedPeriods.get(color);
                existing.addLightPeriod(new LightPeriod(phase, color, t, t + dur));
                t = t + dur;
            }
            end = existing.getEndPeriod();
            scheduleRemainder = end.getEnd() - timeNow;
        }
    }


    public boolean hasActivePhaseTraffic(Phase phase) {
        for (Movement m : phase.getMovements()) {
            if (m.getControlEdge().isDetectedVehicleForLight) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpaceInNextEdge(Phase phase) {

        Movement movement = findMajorMovement(phase);

        if (movement != null){
            return movement.getEndEdge().hasSpaceInEndOfAllLane();
        }
        else{
            return true;
        }
    }

    public Movement findMajorMovement(Phase phase) {
        Movement movement = null;
        int vehicleCount = 0;
        int maxVehicleCount = 0;

        for (Movement m : phase.getMovements()) {
            for( Lane lane : m.getControlEdge().getLanes()) {
                for (Vehicle v : lane.getVehicles()) {
                    if (v.hasNextEdge()) {
                        if (m.getEndEdge().index == v.getNextEdge().index) {
                            vehicleCount++;
                        }
                    }
                }
            }

            if (vehicleCount > maxVehicleCount){
                movement = m;
                maxVehicleCount = vehicleCount;
            }
        }

        if (maxVehicleCount == 0) return null;
        else return movement;
    }

    boolean hasInactivePhaseTraffic(Phase active, List<Phase> phases) {
        int activePhase = phases.indexOf(active);
        for (int i = 0; i < phases.size(); i++) {
            if (i == activePhase) {
                continue;
            }
            for (Movement m : phases.get(i).getMovements()) {
                if (m.getControlEdge().isDetectedVehicleForLight) {
                    return true;
                }
            }
        }
        return false;
    }
}

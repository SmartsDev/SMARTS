package traffic.light;

import traffic.light.schedule.TrafficLightSchedule;

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
 * Created by tmuthugama on 8/9/2019
 */

/**
 * A cluster of lights consists of one or more phases.  At any time, only one of the phases
 * is in a green-yellow-red cycle. This phasehandler is called the active phasehandler. All other phases remain in red color.
 */
public class TrafficLightCluster {

    private List<Movement> movements;
    private TrafficLightSchedule lightSchedule;

    public TrafficLightCluster(List<Movement> movements) {
        this.movements = movements;
        lightSchedule = new TrafficLightSchedule();
    }

    public List<Movement> getMovements() {
        return movements;
    }

    public void setLightSchedule(TrafficLightSchedule lightSchedule) {
        this.lightSchedule = lightSchedule;
    }

    public TrafficLightSchedule getLightSchedule() {
        return lightSchedule;
    }

    public void updateLights(double timeNow){
        lightSchedule.updateDisplays(movements, timeNow);
    }

    /*public int getInactivePhaseWithPriorityVehicles() {
        for (int i = 0; i < phases.size(); i++) {
            if (i == activePhase) {
                continue;
            }
            for (Edge e : getPhase(i).getEdges()) {
                if (e.isEdgeContainsPriorityVehicle()) {
                    return i;
                }
            }
        }
        return -1;
    }

    boolean hasActivePhasePriorityVehicles() {
        for (final Edge e : getActivePhaseEdges()) {
            if (e.isEdgeContainsPriorityVehicle()) {
                return true;
            }
        }
        return false;
    }

    boolean hasInactivePhasePriorityVehicles() {
        for (int i = 0; i < phases.size(); i++) {
            for (final Edge e : getPhase(i).getEdges()) {
                if (e.isEdgeContainsPriorityVehicle()) {
                    return true;
                }
            }
        }
        return false;
    }*/

}

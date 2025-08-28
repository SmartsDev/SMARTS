package traffic.light.manager.schic;

import traffic.TrafficNetwork;
import traffic.light.manager.TLManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Created by tmuthugama on 8/12/2019
 */
public class SchICTLManager extends TLManager {

    private int clusterCount = 0;

    @Override
    public void setHandlers() {

    }

    @Override
    public void schedule(double timeNow) {

    }

    public List<Integer> getPartialSchedule(StateGroup xs, int y, int k){

        for (int l = k; l >= 1; l--) {

        }
        return null;
    }

    /**
     * Algorithm 2 in SchIC paper
     * @param s - last phasehandler of state
     * @param t - time of state
     * @param d - delay of state
     * @param c - cluster to add
     * @param phases - phases
     * @return array consist with time and delay after cluster is added to the state
     */
    public double[] update(int s, double t, double d, Cluster c, List<Phase> phases){
        int i = c.getI();
        Phase k = phases.get(i);
        double pst = t + minSwitch(s, i, phases);
        double ast = Math.max(c.getArr(), pst);
        if(pst > c.getArr() && s != i){
            ast = ast + k.getSult();
        }
        double delta_d = c.getC() * (Math.max(ast - c.getArr(), 0));
        t = ast + c.getDur();
        d = d + delta_d;
        return new double[]{t,d};
    }

    /**
     * Min Switch in SchIC paper
     * @param a - start phasehandler
     * @param b - end phasehandler
     * @param phases - phases
     * @return - minimum switching time from phasehandler a to b
     */
    public double minSwitch(int a, int b, List<Phase> phases){
        if(b < a){
            b = b + phases.size();
        }
        double switchTime = 0;
        for (int i = a; i < b; i++) {
            Phase phase = phases.get(i % phases.size());
            if(i > a){
                switchTime += phase.getG_min();
            }
            switchTime += (phase.getY() + phase.getR());
        }
        return switchTime;
    }

    public Schedule getPartialSchedule(ScheduleStatus x, int s, int y, int k){
        int sl = 0;
        for(int l = k; l >= 0; l--){
            sl = s;
        }
        return null;
    }

    public Schedule forwardRecursion(Schedule empty, int i_c, List<Phase> phases){
        for (Phase phase : phases) {
            clusterCount += phase.getClusterCount();
        }
        Map<Integer, StateGroup> stateGroups = new HashMap<>();
        for (int k = 0; k < clusterCount; k++) {
            /*Set<ScheduleStatus> statusSet = stateGroups.get(k);
            for (ScheduleStatus status : statusSet) {
                for (Phase phasehandler : phases) {
                    if(status.getX(phasehandler) > 0){

                    }
                }
            }*/
        }
        return null;
    }

    public void calculateValueRows(List<Phase> phases, double H){
        Cluster c = null;
        for (Phase phase : phases) {
            StateGroup xs = null;
            for (int y = 0; y < xs.getValueRowCount(); y++) {
                ValueRow row = xs.getValueRow(y);
                double[] td = update(phase.getI(), row.getT(), row.getD(), c, phases);
                double t = td[0];
                double d = td[1];
                if(t <= H){

                }
            }
        }
    }
}

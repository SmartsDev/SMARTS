package traffic.light.phasehandler;

import traffic.light.Movement;
import traffic.light.Phase;
import traffic.light.TrafficLightCluster;

import java.util.ArrayList;
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
 * Created by tmuthugama on 8/22/2019
 */
public class FixedDefaultFourPhase extends TLPhaseHandler {

    private Map<TrafficLightCluster, List<Phase>> fixedPhases;

    public FixedDefaultFourPhase(List<TrafficLightCluster> clusters) {
        this.fixedPhases = new HashMap<>();
        createFixedPhases(clusters);
    }

    private void createFixedPhases(List<TrafficLightCluster> clusters) {
        for (TrafficLightCluster cluster : clusters) {
            Map<String, Phase> groups = new HashMap<>();
            for (Movement movement : cluster.getMovements()) {
                String groupName = String.valueOf(movement.getControlEdge().index);
                if(!groups.containsKey(groupName)){
                    groups.put(groupName, new Phase());
                }
                groups.get(groupName).addMovement(movement);
            }
            fixedPhases.put(cluster, new ArrayList<>(groups.values()));
        }
    }

    @Override
    public List<Phase> getPhaseList(TrafficLightCluster cluster, double timeNow) {
        return fixedPhases.get(cluster);
    }
}

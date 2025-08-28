package traffic.vehicle.lanedecide;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;

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
 * Created by tmuthugama on 10/25/2019
 */
public class PredefinedLaneDecider extends LaneDecider {

    @Override
    public void computeLanes(Vehicle vehicle) {
        for (int i = 0; i < vehicle.getRouteLegs().size(); i++) {
            RouteLeg leg = vehicle.getRouteLeg(i);
            if(i == vehicle.getRouteLegs().size() - 1){
                leg.setLane(0);
            }else{
                RouteLeg next = vehicle.getRouteLeg(i+1);
                leg.setLane(leg.edge.getLaneForNextEdge(next.edge));
            }
        }
    }

    @Override
    public Lane getNextEdgeLane(Vehicle vehicle) {
        RouteLeg nextLeg = null;
        if(vehicle.lane == null){
            nextLeg = vehicle.getRouteLeg(vehicle.indexLegOnRoute);
        }else{
            nextLeg = vehicle.getRouteLeg(vehicle.indexLegOnRoute + 1);
        }
        return nextLeg.edge.getLane(nextLeg.lane);
    }
}

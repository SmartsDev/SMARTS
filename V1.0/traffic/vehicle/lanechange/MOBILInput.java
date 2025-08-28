package traffic.vehicle.lanechange;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.Movement;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;
import traffic.vehicle.EmergencyStrategy;
import traffic.vehicle.VehicleType;

import java.awt.geom.Line2D;
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
 * Created by tmuthugama on 3/12/2019
 */
public class MOBILInput {
    private Lane lane;
    private List<RouteLeg> routeLookAhead;
    private boolean aboutToTurnTowardsRoadSide;
    private boolean aboutToTurnAwayFromRoadSide;
    private int towardsRoadSideOnlyLanes;
    private int awayFromRoadSideOnlyLanes;
    private VehicleType type;
    private Settings settings;

    public MOBILInput(Settings settings, Lane lane, List<RouteLeg> routeLookAhead, VehicleType type) {
        this.settings = settings;
        this.lane = lane;
        this.routeLookAhead = routeLookAhead;
        this.aboutToTurnTowardsRoadSide = false;
        this.aboutToTurnAwayFromRoadSide = false;
        this.towardsRoadSideOnlyLanes = 0;
        this.awayFromRoadSideOnlyLanes = 0;
        this.type = type;
        updateAboutToTurnDetails();
        updateLanes();
    }

    public boolean isLaneMostRoadSide(){
        return lane.laneNumber == 0;
    }

    public boolean isLaneMostAwayFromRoadSide(){
        return lane.laneNumber == lane.edge.getLaneCount()-1;
    }

    public boolean isAllLanesOnRoadSideBlocked(){
        return lane.edge.isAllLanesOnRoadSideBlocked(lane.laneNumber);
    }

    public boolean isAllLanesAwayRoadSideBlocked(){
        return lane.edge.isAllLanesAwayRoadSideBlocked(lane.laneNumber);
    }

    public void updateAboutToTurnDetails() {
        for (int i = 0; i < routeLookAhead.size() - 1; i++) {
            Edge e1 = routeLookAhead.get(i).edge;
            Edge e2 = routeLookAhead.get(i + 1).edge;
            if (e1.startNode == e2.endNode) {
                // Vehicle is going to make U-turn
                aboutToTurnAwayFromRoadSide = true;
            } else if (!e1.name.equals(e2.name) || (e1.type != e2.type)) {
                final Line2D.Double e1Seg = new Line2D.Double(e1.startNode.lon, e1.startNode.lat * settings.lonVsLat,
                        e1.endNode.lon, e1.endNode.lat * settings.lonVsLat);
                final int ccw = e1Seg.relativeCCW(e2.endNode.lon, e2.endNode.lat * settings.lonVsLat);
                if(settings.isDriveOnLeft) {
                    if (ccw < 0) {
                        aboutToTurnTowardsRoadSide = true;
                    } else if (ccw > 0) {
                        aboutToTurnAwayFromRoadSide = true;
                    }
                }else{
                    if (ccw < 0) {
                        aboutToTurnAwayFromRoadSide = true;
                    } else if (ccw > 0) {
                        aboutToTurnTowardsRoadSide = true;
                    }
                }
            }
        }
    }

    public void updateLanes(){
        if(settings.isDriveOnLeft){
            towardsRoadSideOnlyLanes = lane.edge.numLeftOnlyLanes;
            awayFromRoadSideOnlyLanes = lane.edge.numRightOnlyLanes;
        }else{
            towardsRoadSideOnlyLanes = lane.edge.numRightOnlyLanes;
            awayFromRoadSideOnlyLanes = lane.edge.numLeftOnlyLanes;
        }
    }

    public boolean canLeaveLaneIfNotTurnTowardsRoadSide(){
        return !aboutToTurnTowardsRoadSide &&
                (lane.laneNumber <= towardsRoadSideOnlyLanes) && !lane.isBlocked;
    }

    public boolean canLeaveLaneIfTurnAwayFromRoadSide() {
        return aboutToTurnAwayFromRoadSide &&
                ((lane.edge.getLaneCount() - (lane.laneNumber - 1) > awayFromRoadSideOnlyLanes)) && !lane.isBlocked;
    }

    public boolean canLeaveLaneIfNotTurnAwayFromRoadSide(){
        return !aboutToTurnAwayFromRoadSide &&
                ((lane.edge.getLaneCount() - (lane.laneNumber + 1)) <= awayFromRoadSideOnlyLanes) && !lane.isBlocked;
    }

    public boolean canLeaveLaneIfTurnTowardsRoadSide() {
        return aboutToTurnTowardsRoadSide && ((lane.laneNumber + 1) >= towardsRoadSideOnlyLanes) && !lane.isBlocked;
    }

    public boolean isMovementInRedLight(Movement movement){
        LightColor color = lane.edge.getMovementLight(movement);
        return color == LightColor.KEEP_RED || color == LightColor.GYR_R;
    }

    public boolean isEncouragedToChangeTowardsRoadSide(){
        return aboutToTurnTowardsRoadSide && lane.laneNumber >= towardsRoadSideOnlyLanes;
    }

    public boolean isEncouragedToChangeAwayFromRoadSide(){
        return aboutToTurnAwayFromRoadSide && lane.laneNumber < lane.edge.getLaneCount() - awayFromRoadSideOnlyLanes;
    }

    public boolean isEncouragedToGiveWayToPriorityVehicles(){
        return lane.edge.isEdgeOnPathOfPriorityVehicle() && (type != VehicleType.PRIORITY)
                && (settings.emergencyStrategy != EmergencyStrategy.Flexible);
    }

    public boolean isALaneChangingVehicle(){
        return type != VehicleType.TRAM;
    }

    public Lane getNextTowardsRoadSideLane(){
        return lane.edge.getLane(lane.laneNumber - 1);
    }

    public Lane getNextAwayFromRoadSideLane(){
        return lane.edge.getLane(lane.laneNumber + 1);
    }
}

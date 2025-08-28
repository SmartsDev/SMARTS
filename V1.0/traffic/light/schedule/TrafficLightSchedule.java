package traffic.light.schedule;

import traffic.light.LightColor;
import traffic.light.LightPeriod;
import traffic.light.Movement;
import traffic.vehicle.Vehicle;

import java.util.*;

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
 * Created by tmuthugama on 8/20/2019
 */
public class TrafficLightSchedule implements IntersectionControlSchedule{

    private LinkedList<LightPeriod> schedule;

    public TrafficLightSchedule() {
        this.schedule = new LinkedList<>();
    }

    public TrafficLightSchedule(LinkedList<LightPeriod> schedule) {
        this.schedule = schedule;
    }

    @Override
    public void updateDisplays(List<Movement> movementList, double timeNow) {
        for (Movement movement : movementList) {
            LightColor color = getLight(movement, timeNow);
            movement.getControlEdge().setMovementLight(movement, color);
        }
    }

    protected LightColor getLight(Movement movement, double time){
        LightPeriod first = schedule.getFirst();
        if(first.getEnd() < time){
            schedule.remove();
            first = schedule.getFirst();
        }
        if(first.getPhase().hasMovement(movement)){
            return first.getColor();
        }else{
            return LightColor.KEEP_RED;
        }
    }

    @Override
    public boolean isAllowedToPass(Vehicle vehicle) {
        return false;
    }



    public LightPeriod getCurrentPeriod(){
        if(!schedule.isEmpty()) {
            return schedule.getFirst();
        }
        return null;
    }

    public LightPeriod getEndPeriod(){
        if(!schedule.isEmpty()) {
            return schedule.getLast();
        }
        return null;
    }

    public void addLightPeriod(LightPeriod period){
        schedule.add(period);
    }

    public void extendDuration(LightPeriod period, double delta){
        period.addDur(delta);
        int next = schedule.indexOf(period) + 1;
        for (int i = next; i < schedule.size() - 1; i++) {
            schedule.get(i).shift(delta);
        }
    }

    public void adjustDuration(LightPeriod start, LightPeriod end, double delta){
        start.addDur(delta);
        int s =  schedule.indexOf(start) + 1;
        int e =  schedule.indexOf(end);
        for (int i = s; i < e; i++) {
            schedule.get(i).shift(delta);
        }
        end.reduceDurStart(delta);
    }

    public List<LightPeriod> getGreenPeriods(Movement movement){
        List<LightPeriod> periods = new ArrayList<>();
        for (LightPeriod lightPeriod : schedule) {
            if(lightPeriod.getColor() == LightColor.GYR_G && lightPeriod.getPhase().hasMovement(movement)){
                periods.add(lightPeriod);
            }
        }
        return periods;
    }

    public List<LightPeriod> getGreenPeriods(){
        List<LightPeriod> periods = new ArrayList<>();
        for (LightPeriod lightPeriod : schedule) {
            if(lightPeriod.getColor() == LightColor.GYR_G){
                periods.add(lightPeriod);
            }
        }
        return periods;
    }

    public LinkedList<LightPeriod> getSchedule() {
        return schedule;
    }

    public LinkedList<LightPeriod>  getOngoingSchedule() {
        if(!schedule.isEmpty()) {
            LightPeriod first = schedule.getFirst();
            int bound = 0;
            if(first.getColor() == LightColor.GYR_G){
                bound += 3;
            }else if(first.getColor() == LightColor.GYR_Y){
                bound += 2;
            }else if(first.getColor() == LightColor.GYR_R){
                bound += 1;
            }

            LinkedList onGoing = new LinkedList();
            onGoing.addAll(schedule.subList(0,bound));
            return onGoing;
        }
        return null;
    }
}

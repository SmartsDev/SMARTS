package traffic.light.manager.schic;

import java.util.ArrayList;
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
 * Created by tmuthugama on 8/12/2019
 */
public class Schedule {

    private Phase i_c;
    private List<Phase> pss;
    private List<Phase> phases;
    private ScheduleStatus status;
    private double t;
    private double d;

    public Schedule(List<Phase> phases, Phase i_c) {
        this.i_c = i_c;
        this.pss = new ArrayList<>();
        this.status = new ScheduleStatus(phases);
        this.phases = phases;
    }

    public int getS(){
        return pss.get(pss.size() - 1).getI();
    }

    public void add(Phase k){
        pss.add(k);
        int j = status.getX(k);
        Cluster c = k.getCluster(j);
        int s = getS();
        int i = k.getI();
        double pst = t /*+ minSwitch(s, i)*/;
        double ast = Math.max(c.getArr(), pst);
        if(pst > c.getArr() && s != i){
            ast = ast + k.getSult();
        }
        double delta_d = c.getC() * (Math.max(ast - c.getArr(), 0));
        t = ast + c.getDur();
        d = d + delta_d;
        status.setX(k, j + 1);
    }




}

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
public class Phase {

    private int i;
    private List<Cluster> clusters;
    private double g_min;
    private double g_max;
    private double y;
    private double r;
    private double sult;

    public Phase(int i, double g_min, double g_max, double y, double r, double sult) {
        this.i = i;
        this.g_min = g_min;
        this.g_max = g_max;
        this.y = y;
        this.r = r;
        this.sult = sult;
        this.clusters = new ArrayList<>();
    }

    public Cluster getCluster(int j){
        return clusters.get(j);
    }

    public int getClusterCount(){
        return clusters.size();
    }

    public int getI() {
        return i;
    }

    public double getG_min() {
        return g_min;
    }

    public double getG_max() {
        return g_max;
    }

    public double getY() {
        return y;
    }

    public double getR() {
        return r;
    }

    public double getSult() {
        return sult;
    }
}

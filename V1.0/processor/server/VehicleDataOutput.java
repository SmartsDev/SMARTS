package processor.server;

import processor.communication.message.Serializable_Finished_Vehicle;

import java.io.FileOutputStream;
import java.io.IOException;

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
 * Created by tmuthugama on 4/10/2019
 */
public class VehicleDataOutput {

    private FileOutputStream fos;

    public VehicleDataOutput(FileOutputStream fos){
        this.fos = fos;
    }

    public void outputVehicleData(Serializable_Finished_Vehicle vehicle){
        StringBuilder sb = new StringBuilder();
        sb.append(vehicle.id+",");
        sb.append(vehicle.vid+",");
        sb.append(vehicle.source+",");
        sb.append(vehicle.destination+",");
        sb.append(vehicle.bestTravelTime+",");
        sb.append(vehicle.actualTravelTime+",");
        sb.append(vehicle.routeLength+",");
        sb.append(vehicle.route+ System.getProperty("line.separator"));
        outputStringToFile(fos, sb.toString());
    }

    public void outputVehicleData(Serializable_Finished_Vehicle vehicle, double timeNow){
        StringBuilder sb = new StringBuilder();
        sb.append(vehicle.id+",");
        sb.append(vehicle.vid+",");
        sb.append(vehicle.source+",");
        sb.append(vehicle.destination+",");
        sb.append(vehicle.bestTravelTime+",");
        sb.append(vehicle.actualTravelTime+",");
        sb.append(timeNow+",");
        sb.append(vehicle.routeLength+",");
        sb.append(vehicle.timeOnDirectionalTraffic+",");
        sb.append(vehicle.timeOnDirectionalTraffic_speed+",");
        sb.append(vehicle.route+ System.getProperty("line.separator"));
        outputStringToFile(fos, sb.toString());
    }

    private void outputStringToFile(final FileOutputStream fos, String str) {
        try {
            fos.write(str.getBytes());
            fos.flush();
        } catch (final IOException e) {
        }
    }
}

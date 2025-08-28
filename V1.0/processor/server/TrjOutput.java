package processor.server;

import processor.communication.message.Serializable_GUI_Vehicle;
import traffic.road.RoadUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
 * Created by tmuthugama on 3/12/2019
 */
public class TrjOutput {
    private double minLon;
    private double minLat;
    private int maxX;
    private int maxY;
    private FileOutputStream fos;
    private int vehicleIndex = 0;
    private int noOfWorkers;
    private int noStepsPerSecond;
    private int maxSteps;
    private int lastFinishedStep = 0;
    private float metresPerUnit = 1;

    public TrjOutput(FileOutputStream fos, int noOfWorkers, int noStepsPerSecond, int maxSteps, double minLon, double minLat, double width, double height) {
        this.fos = fos;
        this.noOfWorkers = noOfWorkers;
        this.noStepsPerSecond = noStepsPerSecond;
        this.maxSteps = maxSteps;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxX = (int) Math.ceil(width/metresPerUnit) + 25;
        this.maxY = (int) Math.ceil(height/metresPerUnit) + 25;
    }

    public void outputMapData(){
        outputStringToFile(fos, getFormatBytes('L', 1.04f));
        outputStringToFile(fos, getDimensionBytes(1, metresPerUnit, 0, 0, maxX, maxY));
    }

    public void outputTrajData(int step, List<Serializable_GUI_Vehicle> gui_vehicles){
        outputTimeStepData(step, gui_vehicles);
    }

    public void outputTimeStepData(int timestep, List<Serializable_GUI_Vehicle> vehicles){
        outputStringToFile(fos, getTimeStepBytes((float) timestep/(float) noStepsPerSecond));
        for (Serializable_GUI_Vehicle vehicle : vehicles) {
            int vehicleId = vehicle.vid;
            int linkId = vehicle.edgeIndex;
            int laneId = vehicle.laneIndex;
            float frontX = (float) RoadUtil.getDistInMeters(minLat, minLon,minLat,  vehicle.lonHead)/metresPerUnit;
            float frontY = (float) RoadUtil.getDistInMeters(minLat, minLon, vehicle.latHead, minLon)/metresPerUnit;
            float rearX = (float) RoadUtil.getDistInMeters(minLat, minLon, minLat, vehicle.lonTail)/metresPerUnit;
            float rearY = (float) RoadUtil.getDistInMeters(minLat, minLon, vehicle.latTail, minLon)/metresPerUnit;
            float length = (float) vehicle.length/metresPerUnit;
            float width = 0.5f/metresPerUnit;
            float speed = (float) vehicle.speed/metresPerUnit;
            float acceleration = (float) vehicle.acceleration/metresPerUnit;
            outputStringToFile(fos,getVehicleBytes(vehicleId,linkId,laneId,frontX,frontY,rearX,rearY,
                    length,width,speed,acceleration));
        }
    }

    private void outputStringToFile(final FileOutputStream fos, byte[] str) {
        try {
            fos.write(str);
            fos.flush();
        } catch (final IOException e) {
        }
    }

    private byte[] getFormatBytes(char endianness, float version){
        ByteBuffer byteBuffer = ByteBuffer.allocate(6);
        byteBuffer.put(getByteBytes((byte) 0));
        byteBuffer.put(getByteBytes((byte) endianness));
        byteBuffer.put(getFloatBytes(version));
        return byteBuffer.array();
    }

    private byte[] getDimensionBytes(int unit, float scale, int minX, int minY, int maxX, int maxY){
        ByteBuffer byteBuffer = ByteBuffer.allocate(22);
        byteBuffer.put(getByteBytes((byte) 1));
        byteBuffer.put(getByteBytes((byte) unit));
        byteBuffer.put(getFloatBytes(scale));
        byteBuffer.put(getIntBytes(minX));
        byteBuffer.put(getIntBytes(minY));
        byteBuffer.put(getIntBytes(maxX));
        byteBuffer.put(getIntBytes(maxY));
        return byteBuffer.array();
    }

    private byte[] getTimeStepBytes(float timestep){
        ByteBuffer byteBuffer = ByteBuffer.allocate(5);
        byteBuffer.put(getByteBytes((byte) 2));
        byteBuffer.put(getFloatBytes(timestep));
        return byteBuffer.array();
    }

    private byte[] getVehicleBytes(int vehicleId, int linkId, int laneId,
                                   float frontX, float frontY, float rearX, float rearY,
                                   float length, float width, float speed, float acceleration){
        ByteBuffer byteBuffer = ByteBuffer.allocate(42);
        byteBuffer.put(getByteBytes((byte) 3));
        byteBuffer.put(getIntBytes(vehicleId));
        byteBuffer.put(getIntBytes(linkId));
        byteBuffer.put(getByteBytes((byte) laneId));
        byteBuffer.put(getFloatBytes(frontX));
        byteBuffer.put(getFloatBytes(frontY));
        byteBuffer.put(getFloatBytes(rearX));
        byteBuffer.put(getFloatBytes(rearY));
        byteBuffer.put(getFloatBytes(length));
        byteBuffer.put(getFloatBytes(width));
        byteBuffer.put(getFloatBytes(speed));
        byteBuffer.put(getFloatBytes(acceleration));
        return byteBuffer.array();
    }


    private byte[] getByteBytes(byte a){
        ByteBuffer bytebuf = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN);
        bytebuf.put(a);
        return bytebuf.array();
    }

    private byte[] getIntBytes(int a){
        ByteBuffer bytebuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bytebuf.putInt(a);
        return bytebuf.array();
    }

    private byte[] getFloatBytes(float a){
        ByteBuffer bytebuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bytebuf.putFloat(a);
        return bytebuf.array();
    }

}

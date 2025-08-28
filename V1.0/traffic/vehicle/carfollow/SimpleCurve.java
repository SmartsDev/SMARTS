package traffic.vehicle.carfollow;


import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.vehicle.VehicleUtil;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

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
 * Created by tmuthugama on 7/24/2019
 */
public class SimpleCurve {

    private Node junc;
    private Edge sEdge;
    private Edge eEdge;
    private boolean isUTurn;
    private Line2D sLaneLine;
    private Line2D eLaneLine;
    private Line2D sStopLine;
    private Line2D eStopLine;
    private double sIntSize;
    private double eIntSize;

    private Point2D sLaneStop;
    private Point2D eLaneStop;
    private Point2D sLaneMeetPoint;
    private Point2D eLaneMeetPoint;
    private double sR;
    private double eR;
    private double bigR;
    private double R;
    private double sGap;
    private double eGap;

    private Point2D curveCenterPoint;
    private Point2D curveStartPoint;
    private Point2D curveEndPoint;

    private Line2D curveStartLine;
    private Line2D curveEndLine;


    private double THETA;

    public SimpleCurve(Lane startLane, Lane endLane){
        this.sEdge = startLane.edge;
        this.eEdge = endLane.edge;
        this.junc = sEdge.endNode;
        this.isUTurn = sEdge.startNode.index == eEdge.endNode.index;
        this.sLaneLine = startLane.getLaneLine();
        this.eLaneLine = endLane.getLaneLine();
        this.sStopLine = junc.getStopLine(sEdge.startNode);
        this.eStopLine = junc.getStopLine(eEdge.endNode);
        this.sIntSize = junc.getIntersectionSize(sEdge.startNode);
        this.eIntSize = junc.getIntersectionSize(eEdge.endNode);

        this.sLaneStop = RoadUtil.getDividingPoint(sLaneLine.getP1(), sLaneLine.getP2(), sEdge.length - sIntSize, sIntSize);
        this.eLaneStop = RoadUtil.getDividingPoint(eLaneLine.getP1(), eLaneLine.getP2(), eIntSize, eEdge.length - eIntSize);
        this.sLaneMeetPoint = getStartLaneMeetPoint();
        this.eLaneMeetPoint = getEndLaneMeetPoint();
        this.sR = getSR();
        this.eR = getER();
        this.bigR = getBigRadius();
        this.sGap = sR - bigR;
        this.eGap = eR - bigR;
        this.curveStartLine = getCurveStartLine();
        this.curveEndLine = getCurveEndLine();
        this.curveStartPoint = RoadUtil.getIntersectionPoint(curveStartLine, sLaneLine);
        this.curveEndPoint = RoadUtil.getIntersectionPoint(curveEndLine, eLaneLine);
        this.curveCenterPoint = getCurveCenterPoint();
        this.R = RoadUtil.getDistInMeters(curveStartPoint.getY(), curveStartPoint.getX(), curveCenterPoint.getY(), curveCenterPoint.getX());
        this.THETA = getTotalTheta(new Line2D.Double(curveCenterPoint, curveEndPoint), new Line2D.Double(curveCenterPoint, curveStartPoint));
    }



    private double getBigRadius(){
        if(!isUTurn){
            return Math.min(eR, sR);
        }else{
            return RoadUtil.getDistInMeters(sLaneLine.getY2(), sLaneLine.getX2(), sLaneStop.getY(), sLaneStop.getX());
        }
    }

    private double getSR(){
        if(!isUTurn){
            return RoadUtil.getDistInMeters(sLaneMeetPoint.getY(), sLaneMeetPoint.getX(), sLaneStop.getY(), sLaneStop.getX());
        }else{
            return RoadUtil.getDistInMeters(sLaneLine.getY2(), sLaneLine.getX2(), sLaneStop.getY(), sLaneStop.getX());
        }
    }

    private double getER(){
        if(!isUTurn){
            return RoadUtil.getDistInMeters(eLaneMeetPoint.getY(), eLaneMeetPoint.getX(), eLaneStop.getY(), eLaneStop.getX());
        }else{
            return RoadUtil.getDistInMeters(eLaneLine.getY1(), eLaneLine.getX1(), eLaneStop.getY(), eLaneStop.getX());
        }
    }

    private Point2D getCurveCenterPoint(){
        if(!isUTurn){
            return RoadUtil.getIntersectionPoint(curveStartLine, curveEndLine);
        }else{
            return RoadUtil.getDividingPoint(curveStartPoint, curveEndPoint, 1, 1);
        }
    }


    private Point2D getStartLaneMeetPoint(){
        if(!isUTurn) {
            return RoadUtil.getIntersectionPoint(sLaneLine, eLaneLine);
        }else{
            return sLaneLine.getP2();
        }
    }

    private Point2D getEndLaneMeetPoint(){
        if(!isUTurn) {
            return sLaneMeetPoint;
        }else{
            return eLaneLine.getP1();
        }
    }


    private Point2D getStopLineMeetPoint(){
        if(!isUTurn){
            return RoadUtil.getIntersectionPoint(sStopLine, eStopLine);
        }else{
            return RoadUtil.getDividingPoint(sLaneStop, eLaneStop, 1,1);
        }
    }

    private Line2D getCurveStartLine(){
        Line2D pv = junc.getLeftPavement(sEdge.startNode);
        double l = sEdge.length - sIntSize + sGap;
        double m = sIntSize - sGap;
        Point2D p1 = RoadUtil.getDividingPoint(sLaneLine.getP1(), sLaneLine.getP2(), l, m);
        Point2D p2 = RoadUtil.getDividingPoint(pv.getP2(), pv.getP1(),l,m);
        return new Line2D.Double(p1, p2);
    }

    private Line2D getCurveEndLine(){
        Line2D pv = junc.getLeftPavement(eEdge.endNode);
        double l = eIntSize - eGap;
        double m = eEdge.length - eIntSize + eGap;
        Point2D p1 = RoadUtil.getDividingPoint(eLaneLine.getP1(), eLaneLine.getP2(), l, m);
        Point2D p2 = RoadUtil.getDividingPoint(pv.getP1(), pv.getP2(),l,m);
        return new Line2D.Double(p1, p2);
    }

    private double getTotalTheta(Line2D l1, Line2D l2){
        double diff = RoadUtil.getSmallestDiff(l1, l2, Math.PI/180);
        if(diff == 0 && isUTurn){
            diff = Math.PI;
        }
        return diff;
    }

    private double getHeadPositionTheta(double headPosOnCurve){
        return THETA * (headPosOnCurve/(R * THETA));
    }

    public double getIntersectionHeadPos(double headPos, Lane lane){
        if(lane.edge.index == sEdge.index){
            return headPos - (sEdge.length - sIntSize);
        }else if(lane.edge.index == eEdge.index){
            return headPos + sIntSize;
        }else{
            return headPos;
        }
    }

    public Point2D[] getMappedPositions(double headPos, double length, Lane lane){
        double totalHeadPos = getIntersectionHeadPos(headPos, lane);
        double endSeg = eGap > length ? eGap : length;
        double lengthRatio = (sGap + R * THETA + endSeg)/(sIntSize + eIntSize + endSeg);
        double actualHead = totalHeadPos * lengthRatio;
        double vehicleLenTheta = getVehicleLengthEquivalentAngle(length);
        double maxL = getThetaEquivalentLength(THETA);
        Point2D[] points = null;

        if(actualHead <= sGap){
            points =  VehicleUtil.calculateCoordinates(actualHead, length, lane);
        }else if ( actualHead <= (sGap + R * THETA)){
            double h_THETA = getHeadPositionTheta(actualHead);
            if(h_THETA < vehicleLenTheta){
                double str = straightPartInMixed(length, R, h_THETA);
                points = new Point2D[]{getMappedPosition(h_THETA),RoadUtil.getDividingPoint(sLaneMeetPoint, curveStartPoint, (sR - sGap + str), - (str))};
            }else{
                double tailTheta = h_THETA - vehicleLenTheta;
                points = new Point2D[]{getMappedPosition(h_THETA), getMappedPosition(tailTheta)};
            }
        }else{
            double afterCurveLen = actualHead - (sGap + R * THETA);
            if(afterCurveLen < length) {
                double alpha = alphaPartInMixed(length, R, afterCurveLen);
                points = new Point2D[]{RoadUtil.getDividingPoint(eLaneMeetPoint, curveEndPoint, (eR - eGap + afterCurveLen), - (afterCurveLen)),
                        getMappedPosition(THETA - alpha)};
            }else{
                points =  VehicleUtil.calculateCoordinates(actualHead, length, lane);
            }
        }
        if(RoadUtil.getDistInMeters(points[0].getY(), points[0].getX(), points[1].getY(), points[1].getX())> length*(1.1) && length < 10){
            System.out.println("HAS ERROR");
        }
        return points;
    }

    private double straightPartInMixed(double l, double r, double theta){
        return Math.sqrt(Math.pow(l,2) - Math.pow(r * (1-Math.cos(theta)), 2))- r * Math.sin(theta);
    }

    private double alphaPartInMixed(double l, double r, double x){
        double x_2 = Math.pow(x,2);
        double l_2 = Math.pow(l,2);
        double r_2 = Math.pow(r,2);
        double x_2_r_2 = Math.sqrt( x_2 + r_2 );
        double alpha = Math.asin((l_2 - x_2 - 2 * r_2)/(2 * r * x_2_r_2));
        double beta = Math.asin(r/x_2_r_2);
        return alpha + beta;
    }

    public Point2D getMappedPosition(double theta){
        double COS_THETA = Math.cos(theta);
        double SIN_THETA = Math.sin(theta);
        Point2D p1 = RoadUtil.getDividingPoint(curveStartPoint, curveCenterPoint, R * (1 - COS_THETA), R * COS_THETA);
        Point2D p2 = RoadUtil.getDividingPoint(curveStartPoint, sLaneMeetPoint, R * SIN_THETA, sR - R * SIN_THETA);

        double x = p1.getX() + p2.getX() - curveStartPoint.getX();
        double y = p1.getY() + p2.getY() - curveStartPoint.getY();
        return new Point2D.Double(x, y);
    }

    private double getVehicleLengthEquivalentAngle(double length){
        return 2* Math.asin(length/(2*R));
    }

    private double getThetaEquivalentLength(double THETA){
        return 2 * R * Math.sin(THETA/2);
    }


    public static boolean hasCurve(Edge startEdge, Edge endEdge, int startLaneIndex, int endLaneIndex){
        if(startEdge.endNode.connectedNodes.size() < 3){
            return false;
        }
        if(startEdge.startNode.index == endEdge.endNode.index){
            return true; //U Turn
        }
        Lane startLane = startEdge.getLane(startLaneIndex);
        Lane endLane = endEdge.getLane(endLaneIndex);
        Line2D startLaneLine = startLane.getLaneLine();
        Line2D endLaneLine = endLane.getLaneLine();
        if(RoadUtil.isParalell(startLaneLine, endLaneLine, Math.PI/180)){
            return false; //Lines less than 1 degree difference is parallel
        }
        return true;
    }
}

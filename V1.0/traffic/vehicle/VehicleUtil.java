package traffic.vehicle;

import common.Settings;
import traffic.road.*;
import traffic.vehicle.carfollow.SimpleCurve;

import javax.xml.crypto.dsig.SignatureMethod;
import java.awt.geom.Point2D;

/**
 * This class finds impeding objects based on various factors, e.g., traffic
 * lights, front vehicles, conflicting traffic at intersection, etc.
 */
public class VehicleUtil {

	/**
	 * Compute the GPS coordinates of the head and end of a given vehicle
	 */
	public static double[] calculateCoordinates(final Vehicle v) {
        //SimpleCurve curve = getIntersectionCurve(v);
        SimpleCurve curve = null;
	    if(curve != null){
	        Point2D[] points = curve.getMappedPositions(v.headPosition, v.length, v.lane);
            final double[] coords = {points[0].getX(), points[0].getY(), points[1].getX(), points[1].getY()};
            return coords;
        }else {
            final double headToEdgeRatio = v.headPosition / v.lane.edge.length;
            final double tailToEdgeRatio = (v.headPosition - v.length) / v.lane.edge.length;

            final double lonLength = v.lane.lonEnd - v.lane.lonStart;
            final double latLength = v.lane.latEnd - v.lane.latStart;

            final double headLon = v.lane.lonStart + (headToEdgeRatio * lonLength);
            final double headLat = v.lane.latStart + (headToEdgeRatio * latLength);
            final double tailLon = v.lane.lonStart + (tailToEdgeRatio * lonLength);
            final double tailLat = v.lane.latStart + (tailToEdgeRatio * latLength);

            final double[] coords = {headLon, headLat, tailLon, tailLat};
            return coords;
        }
	}

	public static SimpleCurve getIntersectionCurve(Vehicle v){
        Edge edge = v.lane.edge;
        Node junc = null;
        if(v.headPosition > edge.length - edge.endNode.getIntersectionSize(edge.startNode)){
            junc = edge.endNode;
        }else if(v.headPosition <= edge.startNode.getIntersectionSize(edge.endNode) + v.length){
            junc = edge.startNode;
        }
        if(junc != null){
            if (v.getDecision() != null){
                if ( v.getDecision().getVehicleCurve()!= null){
                    return v.getDecision().getVehicleCurve();
                }
                else{
                    return junc.getCurve(v);
                }
            }
            else {
                return junc.getCurve(v);
            }
        }
        return null;
    }

    public static Point2D[] calculateCoordinates(double headPos, double length, Lane lane) {
        final double headToEdgeRatio = headPos / lane.edge.length;
        final double tailToEdgeRatio = (headPos - length) / lane.edge.length;

        final double lonLength = lane.lonEnd - lane.lonStart;
        final double latLength = lane.latEnd - lane.latStart;

        final double headLon = lane.lonStart + (headToEdgeRatio * lonLength);
        final double headLat = lane.latStart + (headToEdgeRatio * latLength);
        final double tailLon = lane.lonStart + (tailToEdgeRatio * lonLength);
        final double tailLat = lane.latStart + (tailToEdgeRatio * latLength);

        return new Point2D[]{new Point2D.Double(headLon, headLat), new Point2D.Double(tailLon, tailLat)};
    }






	/**
	 * Check whether a vehicle can travel from one node to another through an
	 * edge
	 */
	public static boolean canGoThrough(final Node nodeStart, final Node nodeEnd, final VehicleType vehicleType, boolean isAllowPriorityVehicleUseTramTrack) {
		for (final Edge e : nodeStart.outwardEdges) {
			if (e.endNode == nodeEnd) {
				if (e.type == RoadType.tram) {
					if ((vehicleType == VehicleType.PRIORITY) && !isAllowPriorityVehicleUseTramTrack) {
						return false;
					} else if ((vehicleType != VehicleType.PRIORITY) && (vehicleType != VehicleType.TRAM)) {
						return false;
					}
				}
				if (!e.isBlocked()) {
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * Get the braking distance for stopping a vehicle completely.
	 */
	public static double getBrakingDistance(final Vehicle vehicle) {
		return (vehicle.speed * vehicle.speed) / 2.0 / vehicle.driverProfile.IDM_b;
	}


	public static boolean isNeedLaneChangeForTurn(Edge edgeBeingChecked, Vehicle vehicle, boolean isDriveOnLeft){
        int laneNumberBeingChecked = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
                vehicle.lane.laneNumber);
        boolean isNeedToBlock = false;
        if (isDriveOnLeft) {
            if (edgeBeingChecked == vehicle.edgeBeforeTurnLeft
                    && laneNumberBeingChecked >= edgeBeingChecked.numLeftLanes
                    && !edgeBeingChecked.isAllLanesOnLeftBlocked(laneNumberBeingChecked, isDriveOnLeft)) {
                isNeedToBlock = true;
            } else if (edgeBeingChecked == vehicle.edgeBeforeTurnRight
                    && laneNumberBeingChecked < edgeBeingChecked.getLaneCount() - edgeBeingChecked.numRightLanes
            && !edgeBeingChecked.isAllLanesOnRightBlocked(laneNumberBeingChecked, isDriveOnLeft)) {
                isNeedToBlock = true;
            }
        } else { //TODO : add lane check driveOnRight
            if (edgeBeingChecked == vehicle.edgeBeforeTurnLeft
                    && laneNumberBeingChecked < edgeBeingChecked.getLaneCount() - edgeBeingChecked.numLeftLanes
                    && !edgeBeingChecked.isAllLanesOnLeftBlocked(laneNumberBeingChecked, isDriveOnLeft)) {
                isNeedToBlock = true;
            } else if (edgeBeingChecked == vehicle.edgeBeforeTurnRight
                    && laneNumberBeingChecked >= edgeBeingChecked.numRightLanes
                    && !edgeBeingChecked.isAllLanesOnRightBlocked(laneNumberBeingChecked, isDriveOnLeft)) {
                isNeedToBlock = true;
            }
        }
        return isNeedToBlock;
    }




}

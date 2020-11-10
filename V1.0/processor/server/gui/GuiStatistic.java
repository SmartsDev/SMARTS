package processor.server.gui;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JDialog;

import processor.communication.message.Serializable_GUI_Vehicle;
import processor.server.gui.DrawingObject.EdgeObject;

public class GuiStatistic extends JDialog {
	Rectangle2D.Double queryWindow = null;
	int numEdges = 0;
	int numVehicles = 0;
	double sumSpeed = 0;
	double avgSpeed = 0;

	public GuiStatistic() {

	}

	void setVehicleData(ArrayList<Serializable_GUI_Vehicle> guiVehicleList) {
		if (queryWindow == null) {
			return;
		}
		numVehicles = 0;
		sumSpeed = 0;
		avgSpeed = 0;
		for (Serializable_GUI_Vehicle vehicle : guiVehicleList) {
			if (queryWindow.contains(vehicle.lonHead, vehicle.latHead)) {
				numVehicles++;
				sumSpeed += vehicle.speed;
			}
		}
	}

	void calculateAverageSpeed() {
		if (queryWindow == null) {
			return;
		}
		avgSpeed = (sumSpeed / numVehicles) * 3.6;
	}

	public int getNumEdges(final ArrayList<EdgeObject> roadEdges) {
		numEdges = 0;
		if (queryWindow == null) {
			return 0;
		}
		for (final EdgeObject edge : roadEdges) {
			final Line2D.Double line = new Line2D.Double(edge.startNodeLon, edge.startNodeLat, edge.endNodeLon,
					edge.endNodeLat);
			if (line.intersects(queryWindow)) {
				numEdges++;
			}
		}
		return numEdges;
	}

	public void resetStatistics() {
		numEdges = 0;
		numVehicles = 0;
		numVehicles = 0;
		sumSpeed = 0;
		avgSpeed = 0;
	}

	public void clearQueryWindow() {
		queryWindow = null;
	}

	public void setQueryWindow(final Point2D.Double topLeft, final Point2D.Double bottomRight) {
		double startX = topLeft.x;
		if (startX > bottomRight.x) {
			startX = bottomRight.x;
		}
		double startY = topLeft.y;
		if (startY > bottomRight.y) {
			startY = bottomRight.y;
		}
		queryWindow = new Rectangle2D.Double(startX, startY, Math.abs(bottomRight.x - topLeft.x),
				Math.abs(topLeft.y - bottomRight.y));
	}

	public String getResult() {
		String result = "Statistics: " + numEdges + " edges; " + numVehicles + " vehicles; "
				+ String.format("%.2f", avgSpeed) + " km/h average speed";
		return result;
	}

}

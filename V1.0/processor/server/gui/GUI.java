package processor.server.gui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import common.Settings;
import osm.OSM;
import processor.communication.message.Serializable_GUI_Light;
import processor.communication.message.Serializable_GUI_Vehicle;
import processor.server.Server;
import traffic.road.RoadNetwork;
import traffic.road.RoadUtil;

/**
 * GUI contains the panels for controlling simulation and showing the progress
 * of simulation in real time.
 *
 */
public class GUI extends JFrame {

	GuiStatistic queryResult;
	MonitorPanel monitorPanel;
	ControlPanel controlPanel;
	JScrollPane controlPanelHolder;
	Server server;
	int frameWidth, frameHeight;
	public int stepToDraw = 0;
	int lastVisualizedStep = 0;
	long lastSpeedUpUpdateTimeStamp = 0;
	double lastSpeedUp = 0;

	enum VehicleDetailType {
		Type, Remaining_Links, ID_Worker, Driver_Profile
	}

	private final HashMap<String, ArrayList<Serializable_GUI_Vehicle>> guiVehicleList = new HashMap<>();
	private final HashMap<String, ArrayList<Serializable_GUI_Light>> guiLightList = new HashMap<>();

	public GUI() {
	}

	public GUI(final Server server) {
		setTitle("SMARTS Traffic Simulator");
		computeFrameDimension();
		setSize(new Dimension(frameWidth, frameHeight));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
		setLayout(null);
		this.server = server;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		initComponenets(server);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent we) {
				server.killConnectedWorkers();
				System.exit(0);
			}
		});
	}

	public void changeMap() {
		server.stopSim();
		server.changeMap();
		monitorPanel.setEnabled(false);
		monitorPanel.changeMap(server.roadNetwork.minLon, server.roadNetwork.minLat, server.roadNetwork.maxLon,
				server.roadNetwork.maxLat);
		getReadyToSetup();
		monitorPanel.setEnabled(true);
	}

	public void clearObjectData() {
		guiVehicleList.clear();
		guiLightList.clear();
	}

	void computeFrameDimension() {
		final int maxWidthMonitorPanel = 3840;
		final int maxHeightMonitorPanel = 2160;
		final int maxWidthWholeFrame = maxWidthMonitorPanel + Settings.controlPanelWidth
				+ Settings.controlPanelGapToRight;
		frameWidth = GuiUtil.getWorkingScreenWidth() > maxWidthWholeFrame ? maxWidthWholeFrame
				: GuiUtil.getWorkingScreenWidth();
		frameHeight = GuiUtil.getWorkingScreenHeight() > maxHeightMonitorPanel ? maxHeightMonitorPanel
				: GuiUtil.getWorkingScreenHeight();
	}

	/**
	 * The width of monitor panel is a even number. This is because the static
	 * map image from Google Map, which will be filled to this panel, is scaled
	 * at 2.
	 *
	 */
	int getMonitorPanelWidth() {
		int width = frameWidth - Settings.controlPanelWidth - Settings.controlPanelGapToRight;
		if ((width % 2) != 0) {
			width -= 1;
		}
		return width;
	}

	int getMonitorPanelHeightWidth() {
		int height = frameHeight;
		if ((height % 2) != 0) {
			height -= 1;
		}
		return height;
	}

	public void getReadyToSetup() {
		lastVisualizedStep = 0;
		lastSpeedUp = 0;
		controlPanel.prepareToSetup();
	}

	void initComponenets(final Server server) {
		queryResult = new GuiStatistic();

		monitorPanel = new MonitorPanel(server, this, queryResult,
				new Dimension(getMonitorPanelWidth(), getMonitorPanelHeightWidth()));
		add(monitorPanel);

		final Dimension rightPanelDimension = new Dimension(frameWidth - getMonitorPanelWidth(), frameHeight);
		controlPanel = new ControlPanel(server, this, monitorPanel.displayPanelDimension.width, 0, rightPanelDimension);
		controlPanelHolder = new JScrollPane();
		controlPanelHolder.setViewportView(controlPanel);
		controlPanelHolder.getVerticalScrollBar().setUnitIncrement(16);
		controlPanelHolder.getHorizontalScrollBar().setUnitIncrement(16);
		controlPanelHolder.setBounds(monitorPanel.displayPanelDimension.width, 0, rightPanelDimension.width - 10,
				rightPanelDimension.height + 10);
		add(controlPanelHolder);

		controlPanel.setMonitorPanel(monitorPanel);
		monitorPanel.setControlPanel(controlPanel);

		monitorPanel.addListener(controlPanel);
		controlPanel.addListener();

		GuiUtil.setEnabledStatusOfComponents(controlPanel.cpMiscConfig, false);
		GuiUtil.setEnabledStatusOfComponents(controlPanel.cpRealtime, false);
	}

	public void startSimulation() {
		clearObjectData();
		controlPanel.startSimulation();
		monitorPanel.lblSetupProgress.setVisible(false);
	}

	public void updateNumConnectedWorkers(final int num) {
		controlPanel.updateNumConnectedWorkers(num);
	}

	public void updateObjectData(final ArrayList<Serializable_GUI_Vehicle> vehicleList,
			final ArrayList<Serializable_GUI_Light> lightList, final String workerName, final int numWorkers,
			final int step) {
		if (step > stepToDraw) {
			stepToDraw = step;
		}

		if (step == this.stepToDraw) {
			// Add data for new frame
			guiVehicleList.put(workerName, vehicleList);
			guiLightList.put(workerName, lightList);

			// Draw frame if data is received from all workers for the same step
			if (guiVehicleList.size() == numWorkers) {
				final double timeGapToLastSpeedUpUpdate = (System.currentTimeMillis() - lastSpeedUpUpdateTimeStamp)
						/ 1000.0;
				double realTimeFactor = lastSpeedUp;
				if (timeGapToLastSpeedUpUpdate > 1) {
					final int stepGap = this.stepToDraw - lastVisualizedStep;
					realTimeFactor = stepGap / Settings.numStepsPerSecond / timeGapToLastSpeedUpUpdate;
					lastSpeedUpUpdateTimeStamp = System.currentTimeMillis();
					lastSpeedUp = realTimeFactor;
					lastVisualizedStep = this.stepToDraw;
				}
				monitorPanel.update(guiVehicleList, guiLightList, lastSpeedUp);
				clearObjectData();
			}
		}

	}

	public void updateSetupProgress(final double createdVehicleRatio) {
		monitorPanel.updateSetupProgress(createdVehicleRatio);
	}

}

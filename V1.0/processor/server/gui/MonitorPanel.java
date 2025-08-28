package processor.server.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputAdapter;

import common.Settings;
import common.SysUtil;
import processor.SimulationProcessor;
import processor.communication.message.Serializable_GUI_Light;
import processor.communication.message.Serializable_GUI_Vehicle;
import processor.server.Server;
import processor.server.gui.DrawingObject.EdgeObject;
import processor.server.gui.DrawingObject.LaneObject;
import processor.server.gui.DrawingObject.EdgeObjectComparator;
import processor.server.gui.DrawingObject.IntersectionObject;
import processor.server.gui.DrawingObject.TramStopObject;
import processor.server.gui.GUI.VehicleDetailType;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.road.RoadUtil;
import traffic.vehicle.VehicleType;

import static processor.server.gui.GUI.VehicleDetailType.Slowdown_Factor;

/**
 * Panel that shows moving vehicles on map.
 *
 */
public class MonitorPanel extends JPanel {

	enum EdgeEndMarkerType {
		none, start, end
	}

	class MyListener extends MouseInputAdapter {

		ControlPanel controlPanel;

		public MyListener(final ControlPanel controlPanel) {
			this.controlPanel = controlPanel;
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			final Point mousePoint = e.getPoint();
			// Single click left button to select road
			if ((e.getClickCount() == 1) && ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK)) {
				// Popup menu (note menu opens at current mouse point, not reversed point)
				if (sdWindowAtMousePoint != null) {
					showPopupMenu("setup", e.getPoint().x, e.getPoint().y);
				} else if (laneAtMousePoint != null){
					showPopupMenu("intersection", e.getPoint().x, e.getPoint().y);
				} else if (roadIntersectionAtMousePoint != null) {
					showPopupMenu("intersection", e.getPoint().x, e.getPoint().y);
				} else if (roadEdgeAtMousePoint != null) {
					showPopupMenu("edge", e.getPoint().x, e.getPoint().y);
				} else {
					hidePopupMenu();
				}
				// Clear statistic query window
				if (!validateQueryWindow()) {
					guiStatistic.clearQueryWindow();
					guiStatistic.resetStatistics();
					statisticLabel.setText(guiStatistic.getResult());
					statisticLabel.setVisible(false);
				}

			} // Double click to zoom in/out
			else if (e.getClickCount() == 2) {
				if (mousePoint != null) {
					if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
						changeZoomFromPoint(currentZoom + 1, mousePoint);
					} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
						changeZoomFromPoint(currentZoom - 1, mousePoint);
					}
				}
			}
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
				/*
				 * Update the mouse state
				 */
				draggingMap = true;
				needNewStaticMapImage = false;
				/*
				 * Update position of virtual panel
				 */
				updateVirtualPanelDuringDragging(e);
				/*
				 * Repaint
				 */
				repaint();
			} else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
				if ((queryWindowTopLeft != null)) {
					if (currentZoom >= minZoomExceptStaticMapImage) {
						final Point mousePoint = e.getPoint();
						if (mousePoint != null) {
							queryWindowBottomRight = new Point2D.Double(convertXToLon(mousePoint.x),
									convertYToLat(mousePoint.y));
							/*
							 * Prepare overlay image
							 */
							prepareOverlayImage();
							/*
							 * Repaint
							 */
							repaint();
						}
					} else {
						lblNeedToZoom.setText("You need to zoom in further to draw query window");
						lblNeedToZoom.setVisible(true);
					}
				}
			}
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
		}

		@Override
		public void mouseExited(final MouseEvent e) {
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			mousePoint = e.getPoint();
			if (mousePoint != null) {

				findEdgeAtMousePoint(mousePoint);
				findSdWindowAtMousePoint(mousePoint);
				findLaneAreaAtMousePoint(mousePoint);
				/*
				 * Prepare overlay image.
				 */
				prepareOverlayImage();
				/*
				 * Repaint
				 */
				repaint();
			}
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			// Left button pressed...
			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
				if (currentZoom >= minZoomExceptStaticMapImage) {
					queryWindowTopLeft = new Point2D.Double(convertXToLon(mousePoint.x), convertYToLat(mousePoint.y));
					queryWindowBottomRight = null;
				}
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
				lblNeedToZoom.setVisible(false);
				/*
				 * Set query window.
				 */
				if ((currentZoom >= minZoomExceptStaticMapImage) && validateQueryWindow()) {
					statisticLabel.setVisible(true);
					guiStatistic.setQueryWindow(queryWindowTopLeft, queryWindowBottomRight);
					guiStatistic.resetStatistics();
					guiStatistic.getNumEdges(edgeObjects);
					guiStatistic.setVehicleData(vehicleObjects);
					guiStatistic.calculateAverageSpeed();
					statisticLabel.setText(guiStatistic.getResult());
					if (!controlPanel.isDuringSimulation) {
						// Show setup menu after user define query window by dragging mouse
						showPopupMenu("setup", e.getPoint().x, e.getPoint().y);
					}
				} else {
					queryWindowTopLeft = null;
					queryWindowBottomRight = null;
					hidePopupMenu();
					guiStatistic.clearQueryWindow();
					statisticLabel.setVisible(false);
				}

				/*
				 * Prepare overlay image
				 */
				prepareOverlayImage();
				/*
				 * Repaint
				 */
				repaint();
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {

				/*
				 * Update positions
				 */
				moveMapCenterToPoint(e.getPoint().x - mousePoint.x, e.getPoint().y - mousePoint.y);

			}
			/*
			 * Update the mouse state
			 */
			draggingMap = false;
		}

		/**
		 * Zoom using mouse wheel
		 */
		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			/*
			 * Skip if not Windows
			 */
			if (System.getProperty("os.name").toLowerCase().indexOf("win") < 0) {
				return;
			}

			/*
			 * New zooming scale
			 */
			final int notches = e.getWheelRotation();
			int newscale = currentZoom;
			if (notches > 0) {
				newscale--;
			}
			if (notches < 0) {
				newscale++;
			}
			if (e.getPoint() != null) {
				changeZoomFromPoint(newscale, e.getPoint());
			}
		}
	}

	class UserDefinedRoute {

		EdgeObject startEdge = null;
		EdgeObject endEdge = null;
		String id = SysUtil.getRandomID(3);

		public UserDefinedRoute(final EdgeObject endEdge) {
			this.endEdge = endEdge;
		}
	}

	enum VehicleObjectType {
		normal, mousePoint
	}

	SimulationProcessor processor;
	GUI gui;
	public GuiStatistic guiStatistic;
	public Dimension displayPanelDimension;
	int maxPanelWidthHeight = 900;
	double mapWidthInLonDegree, mapHeightInLonDegree;
	double minLat, maxLat, minLon, maxLon;
	ArrayList<Serializable_GUI_Vehicle> vehicleObjects = new ArrayList<>();
	ArrayList<Serializable_GUI_Light> lightObjects = new ArrayList<>();
	ArrayList<EdgeObject> edgeObjects = new ArrayList<>(300000);
	List<DrawingObject.NodeObject> nodeObjects = new ArrayList<>();
	ArrayList<TramStopObject> tramStopObjects = new ArrayList<>(3000);
	Point2D.Double queryWindowTopLeft = null, queryWindowBottomRight = null;
	boolean isReceivingData = false;
	boolean isComposingObjectImage = false;
	double offset_MapAreaToDisplayPanel_TopLeftX = 0, offset_MapAreaToDisplayPanel_TopLeftY = 0;
	int offsetImageTopLeftX = 0, offsetImageTopLeftY = 0;
	int offsetStaticMapImageTopLeftX = 0, offsetStaticMapImageTopLeftY = 0;
	Point mousePoint = new Point(0, 0);
	/*
	 * A virtual panel holds the whole picture of the map. The actual panel is
	 * smaller than the virtual panel if the map is zoomed in.
	 */
	double mapAreaWidthInPixels = 1, mapAreaHeightInPixels = 1;
	double mapAreaWidthInPixelsOld = 1, mapAreaHeightInPixelsOld = 1;
	BufferedImage roadNetworkImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	BufferedImage staticMapImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	BufferedImage objectsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	BufferedImage overlayImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	HashMap<Integer, EdgeObject> roadEdgesWithBlockingSign = new HashMap<>();
	boolean roadNetworkOn = true;
	boolean vehicleDetailOn = false;
	String vehicleDetailType = "Type";
	ArrayList<BufferedImage> imageCars = new ArrayList<>(6);
	BufferedImage imageAlert = null;
	BufferedImage imageEmergencyVehicle = null;
	ArrayList<Integer>[] pixelMappingForAngledView = null;
	/*
	 * Online API for downloading static map image.
	 */
	String staticMapImageProvider = "OpenStreetMap"; // Check max zoom scale!
	OpenStreetMapTile openStreetMapTile = new OpenStreetMapTile();
	boolean staticMapImageOn = false;
	int maxZoomStaticMapImage = 19;// OpenStreetMap:19
	int minZoomStaticMapImage = 5;
	int zoomAtRelocation = 9;
	int maxZoomExceptStaticMapImage = 22;
	int minZoomExceptStaticMapImage = 8;
	int currentZoom = 16;
	boolean needNewStaticMapImage = false;
	boolean isLoadingStaticMapImage = false;
	boolean isDownloadingOSM = false;

	boolean isUseTrafficFlowToReplaceVehicle = false;
	/*
	 * Whether the mouse is being pressed. This affects what objects need to be
	 * drawn at the moment.
	 */
	boolean draggingMap = false;
	/*
	 * Query window coordinates at mouse point
	 */
	double[] sdWindowAtMousePoint = null;
	/*
	 * Road edge at mouse point.
	 */
	EdgeObject roadEdgeAtMousePoint = null;

	/*
	 * Lane at mouse point
	 */

	LaneObject laneAtMousePoint = null;

	/*
	 * Road intersection at mouse point
	 */
	IntersectionObject roadIntersectionAtMousePoint = null;

	/*
	 * Sub-panel for manipulating map
	 */
	JPanel panelMapOperation;
	private ControlPanel controlPanel;

	private final JLabel statusLabel;
	private final JLabel statisticLabel;
	private final JLabel lblNeedToZoom;
	/*
	 * Popup menu at mouse point
	 */
	JPopupMenu popup = new JPopupMenu();
	boolean isInsidePopupMenu = false;
	/*
	 * Label showing download progress
	 */
	JLabel lblDownloading;

	JLabel lblSetupProgress;

	Random random = new Random();
	private Settings settings;

	public MonitorPanel(SimulationProcessor processor, final GUI gui, final GuiStatistic qDialog,
						final Dimension displayPanelDimension) {
		setBackground(Color.WHITE);
		this.processor = processor;
		this.settings = processor.getSettings();
		this.gui = gui;
		this.guiStatistic = qDialog;
		RoadNetwork roadNetwork = processor.getRoadNetwork();
		this.minLon = roadNetwork.minLon;
		this.minLat = roadNetwork.minLat;
		this.maxLon = roadNetwork.maxLon;
		this.maxLat = roadNetwork.maxLat;
		this.displayPanelDimension = displayPanelDimension;
		this.setBounds(0, 0, displayPanelDimension.width, displayPanelDimension.height);
		setLayout(null);

		/*
		 * Border
		 */
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		loadIconImages();

		/*
		 * Assume mouse point is at the center of panel
		 */
		mousePoint.x = (int) (0.5 * displayPanelDimension.width);
		mousePoint.y = (int) (0.5 * displayPanelDimension.height);
		/*
		 * Map area dimension
		 */
		mapWidthInLonDegree = Math.abs(maxLon - minLon);
		mapHeightInLonDegree = Math.abs(maxLat - minLat) * settings.lonVsLat;
		/*
		 * Size of map area in pixels
		 */
		updateMapAreaDimension();
		/*
		 * Update virtual panel
		 */
		updateOffsetMapAreaToDisplayPanel(mousePoint, 0.5, 0.5);
		/*
		 * Time label
		 */
		statusLabel = new JLabel("");
		statusLabel.setVerticalAlignment(SwingConstants.CENTER);
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusLabel.setForeground(Color.black);
		statusLabel.setFont(new Font("Tahoma", Font.PLAIN, 17));
		statusLabel.setBounds(10, 10, 500, 30);
		statusLabel.setPreferredSize(new Dimension(300, 30));
		add(statusLabel);

		// Statistic label
		statisticLabel = new JLabel("");
		statisticLabel.setVerticalAlignment(SwingConstants.CENTER);
		statisticLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statisticLabel.setForeground(Color.black);
		statisticLabel.setFont(new Font("Tahoma", Font.PLAIN, 17));
		statisticLabel.setBounds(10, displayPanelDimension.height - 70, 500, 30);
		statisticLabel.setPreferredSize(new Dimension(300, 30));
		add(statisticLabel);
		statisticLabel.setVisible(false);

		/*
		 * Sub-panel for manipulating map
		 */
		panelMapOperation = new JPanel();
		panelMapOperation.setPreferredSize(new Dimension(100, 200));
		panelMapOperation.setBackground(Color.BLACK);
		final int x_panelMapOperation = displayPanelDimension.width - panelMapOperation.getPreferredSize().width;
		final int y_panelMapOperation = 1;

		lblNeedToZoom = new JLabel("Need to zoom");
		lblNeedToZoom.setVerticalAlignment(SwingConstants.CENTER);
		lblNeedToZoom.setHorizontalAlignment(SwingConstants.LEFT);
		lblNeedToZoom.setForeground(Color.RED);
		lblNeedToZoom.setFont(new Font("Tahoma", Font.BOLD, 20));
		lblNeedToZoom.setPreferredSize(new Dimension(600, 50));
		lblNeedToZoom.setVisible(false);
		lblNeedToZoom.setBounds(1, 35, lblNeedToZoom.getPreferredSize().width, lblNeedToZoom.getPreferredSize().height);
		add(lblNeedToZoom);

		panelMapOperation.setBounds(x_panelMapOperation, y_panelMapOperation, 100, 200);
		add(panelMapOperation);
		panelMapOperation.setLayout(null);

		final JButton btnMapZoomIn = new JButton("");
		btnMapZoomIn.setToolTipText("Zoom in from center");
		btnMapZoomIn.setBounds(5, 5, 32, 32);
		panelMapOperation.add(btnMapZoomIn);
		btnMapZoomIn.setOpaque(false);
		btnMapZoomIn.setContentAreaFilled(false);
		btnMapZoomIn.setBorderPainted(false);
		btnMapZoomIn
				.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "Zoom-In.png")));
		btnMapZoomIn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				changeZoomFromCenter(currentZoom + 1);
			}
		});

		final JButton btnMapZoomOut = new JButton("");
		btnMapZoomOut.setToolTipText("Zoom out from center");
		btnMapZoomOut.setBounds(60, 5, 32, 32);
		panelMapOperation.add(btnMapZoomOut);
		btnMapZoomOut.setOpaque(false);
		btnMapZoomOut.setContentAreaFilled(false);
		btnMapZoomOut.setBorderPainted(false);
		btnMapZoomOut
				.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "Zoom-Out.png")));
		btnMapZoomOut.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				changeZoomFromCenter(currentZoom - 1);
			}
		});

		final JButton btnMapUp = new JButton("");
		btnMapUp.setToolTipText("Pan up");
		btnMapUp.setBounds(34, 45, 32, 32);
		panelMapOperation.add(btnMapUp);
		btnMapUp.setOpaque(false);
		btnMapUp.setContentAreaFilled(false);
		btnMapUp.setBorderPainted(false);
		btnMapUp.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "up.png")));
		btnMapUp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				moveMapByClickButtons("up");
			}
		});

		final JButton btnMapDown = new JButton("");
		btnMapDown.setToolTipText("Pan down");
		btnMapDown.setBounds(34, 105, 32, 32);
		panelMapOperation.add(btnMapDown);
		btnMapDown.setOpaque(false);
		btnMapDown.setContentAreaFilled(false);
		btnMapDown.setBorderPainted(false);
		btnMapDown.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "down.png")));
		btnMapDown.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				moveMapByClickButtons("down");
			}
		});

		final JButton btnMapLeft = new JButton("");
		btnMapLeft.setToolTipText("Pan left");
		btnMapLeft.setBounds(0, 75, 32, 32);
		panelMapOperation.add(btnMapLeft);
		btnMapLeft.setOpaque(false);
		btnMapLeft.setContentAreaFilled(false);
		btnMapLeft.setBorderPainted(false);
		btnMapLeft.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "left.png")));
		btnMapLeft.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				moveMapByClickButtons("left");
			}
		});

		final JButton btnMapRight = new JButton("");
		btnMapRight.setToolTipText("Pan right");
		btnMapRight.setBounds(68, 75, 32, 32);
		panelMapOperation.add(btnMapRight);
		btnMapRight.setOpaque(false);
		btnMapRight.setContentAreaFilled(false);
		btnMapRight.setBorderPainted(false);
		btnMapRight.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "right.png")));
		btnMapRight.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				moveMapByClickButtons("right");
			}
		});

		final JButton btnResetMap = new JButton("");
		btnResetMap.setToolTipText("Reset zoom and position");
		btnResetMap.setBounds(20, 160, 64, 32);
		panelMapOperation.add(btnResetMap);
		btnResetMap.setOpaque(false);
		btnResetMap.setContentAreaFilled(false);
		btnResetMap.setBorderPainted(false);
		btnResetMap.setIcon(new ImageIcon(MonitorPanel.class.getResource(settings.inputBuiltinResource + "reset.png")));
		btnResetMap.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent arg0) {
				resetMap();
			}
		});

		/*
		 * Label showing download progress
		 */
		lblDownloading = new JLabel("Downloading...");
		lblDownloading.setForeground(Color.BLUE);
		lblDownloading.setFont(new Font("Tahoma", Font.BOLD, 14));
		lblDownloading.setBounds(5, 40, 250, 30);
		lblDownloading.setVisible(false);
		add(lblDownloading);

		lblSetupProgress = new JLabel("Setup progress: ");
		lblSetupProgress.setVerticalAlignment(SwingConstants.CENTER);
		lblSetupProgress.setOpaque(true);
		lblSetupProgress.setHorizontalAlignment(SwingConstants.CENTER);
		lblSetupProgress.setForeground(Color.BLUE);
		lblSetupProgress.setFont(new Font("Serif", Font.BOLD, 30));
		lblSetupProgress.setBackground(Color.WHITE);
		lblSetupProgress.setBounds(0, (displayPanelDimension.height / 2) - 100, displayPanelDimension.width, 200);
		add(lblSetupProgress);
		lblSetupProgress.setVisible(false);

		/*
		 * Set cursor
		 */
		setCursorBasedOnZoom();
		/*
		 * Download static map image
		 */
		prepareStaticMapImage();
		/*
		 * Import road edges
		 */
		importRoadEdges(roadNetwork, minLon, minLat, maxLon, maxLat);
		/*
		 * Prepare road network image
		 */
		prepareRoadNetworkImage();
		/*
		 * Prepare image for objects, such as traffic lights
		 */
		prepareObjectsImage();
		/*
		 * Start a timer to prevent frequent static map downloading
		 */
		final Timer mouseZoomTimer = new Timer(500, new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (needNewStaticMapImage && !isLoadingStaticMapImage) {
					// Download static map image
					prepareStaticMapImage();
				}
			}
		});
		mouseZoomTimer.start();
	}

	/*
	 * Register mouse listeners
	 */
	public void addListener(final ControlPanel controlPanel) {
		final MyListener myListener = new MyListener(controlPanel);
		addMouseListener(myListener);
		addMouseMotionListener(myListener);
		addMouseWheelListener(myListener);
	}

	/**
	 * Block/unblock lanes
	 */
	void blockUnblockLane(final EdgeObject selectedEdge, final int laneNumber, final boolean blocked) {
		final Edge edge = processor.getRoadNetwork().edges.get(selectedEdge.index);
		final Lane lane = edge.getLane(laneNumber);

		// Update the blocked edges for visualization
		if (blocked) {
			roadEdgesWithBlockingSign.put(selectedEdge.index, selectedEdge);
			selectedEdge.laneBlocks[laneNumber] = true;
		} else {
			selectedEdge.laneBlocks[laneNumber] = false;
			boolean isAllLanesOpen = true;
			for (final boolean isBlocked : selectedEdge.laneBlocks) {
				if (isBlocked) {
					isAllLanesOpen = false;
					break;
				}
			}
			if (isAllLanesOpen) {
				roadEdgesWithBlockingSign.remove(selectedEdge.index);
			}
		}

		// Inform server about the action
		processor.askWorkersChangeLaneBlock(lane.index, blocked);
	}

	void changeLaneDirection(final EdgeObject selectedEdge) {
		Edge edge = processor.getRoadNetwork().edges.get(selectedEdge.index);
		processor.getRoadNetwork().updateLaneDirections(new ArrayList<Integer>(edge.index));
		// Inform server about the action
		processor.askWorkersChangeLaneDirection(edge.index);
	}

	/*void blockUnblockLane(final LaneObject selectedLane) {
		final Lane lane = processor.getRoadNetwork().lanes.get(selectedLane.index);
		final Edge edge = lane.edge;

		// Update the blocked edges for visualization
		if (blocked) {
			roadEdgesWithBlockingSign.put(lane.index, selectedEdge);
			selectedEdge.laneBlocks[laneNumber] = true;
		} else {
			selectedEdge.laneBlocks[laneNumber] = false;
			boolean isAllLanesOpen = true;
			for (final boolean isBlocked : selectedEdge.laneBlocks) {
				if (isBlocked) {
					isAllLanesOpen = false;
					break;
				}
			}
			if (isAllLanesOpen) {
				roadEdgesWithBlockingSign.remove(selectedEdge.index);
			}
		}

		// Inform server about the action
		processor.askWorkersChangeLaneBlock(lane.index, blocked);
	}*/

	void changeMap(final double minLon, final double minLat, final double maxLon, final double maxLat) {
		this.minLon = minLon;
		this.minLat = minLat;
		this.maxLon = maxLon;
		this.maxLat = maxLat;
		importRoadEdges(processor.getRoadNetwork(), minLon, minLat, maxLon, maxLat);
		resetMap();
	}

	/**
	 * Changes traffic drawing method (showing individual vehicles or colored
	 * traffic flow)
	 */
	public void changeTrafficDrawingMethod(final String type) {
		isUseTrafficFlowToReplaceVehicle = !type.equals("Vehicles");
		if (!isReceivingData) {
			prepareObjectsImage();
			repaint();
		}
	}

	/**
	 * Change vehicle detail type
	 */
	public void changeVehicleDetailType(final String detailType) {
		vehicleDetailType = detailType;
		if (!isReceivingData) {
			prepareObjectsImage();
			repaint();
		}
	}

	void changeZoomFromCenter(final int newZoom) {
		final Point zoomPoint = new Point((int) (0.5 * displayPanelDimension.width),
				(int) (0.5 * displayPanelDimension.height));
		changeZoomFromPoint(newZoom, zoomPoint);
	}

	void changeZoomFromPoint(int newZoom, final Point zoomPoint) {
		/*
		 * Set limit of scale.
		 */
		int minZoom = minZoomExceptStaticMapImage < minZoomStaticMapImage ? minZoomExceptStaticMapImage
				: minZoomStaticMapImage;
		int maxZoom = maxZoomExceptStaticMapImage > maxZoomStaticMapImage ? maxZoomExceptStaticMapImage
				: maxZoomStaticMapImage;
		int oldZoom = currentZoom;

		if (newZoom < minZoom) {
			newZoom = minZoom;
		}
		if (newZoom > maxZoom) {
			newZoom = maxZoom;
		}

		/*
		 * Do not need to process event if the new scale is the same as the
		 * previous one
		 */
		if (newZoom == currentZoom) {
			return;
		} else {
			currentZoom = newZoom;
		}

		/*
		 * Set cursor
		 */
		setCursorBasedOnZoom();
		/*
		 * Update virtual panel
		 */
		updateMapAreaAfterZoom(zoomPoint);

		/*
		 * Prepare images except static map image
		 */
		if (minZoomExceptStaticMapImage <= currentZoom && currentZoom <= maxZoomExceptStaticMapImage) {
			prepareRoadNetworkImage();
			if (!isReceivingData) {
				prepareObjectsImage();
			}
			prepareOverlayImage();
		}

		/*
		 * Start downloading new static map image
		 */
		needNewStaticMapImage = true;

		/*
		 * Repaint
		 */
		repaint();

	}

	public void clearObjectsFromLastSimulation() {
		roadEdgesWithBlockingSign.clear();
		sdWindowAtMousePoint = null;
		roadEdgeAtMousePoint = null;
		roadIntersectionAtMousePoint = null;
		for (final EdgeObject eo : edgeObjects) {
			for (int i = 0; i < eo.laneBlocks.length; i++) {
				eo.laneBlocks[i] = false;
			}
		}

	}

	/**
	 * Convert latitude to Y.
	 */
	int convertLatToY(final double latitude) {
		return (int) (((((maxLat - latitude) * settings.lonVsLat) / mapHeightInLonDegree) * mapAreaHeightInPixels)
				+ offset_MapAreaToDisplayPanel_TopLeftY);
	}

	/**
	 * Convert longitude to X.
	 */
	int convertLonToX(final double longitude) {
		return (int) ((((longitude - minLon) / mapWidthInLonDegree) * mapAreaWidthInPixels)
				+ offset_MapAreaToDisplayPanel_TopLeftX);
	}

	/**
	 * Convert X to longitude.
	 */
	double convertXToLon(final double x) {
		return minLon + (((x - offset_MapAreaToDisplayPanel_TopLeftX) / mapAreaWidthInPixels) * mapWidthInLonDegree);
	}

	/**
	 * Convert Y to latitude.
	 */
	double convertYToLat(final double y) {
		return maxLat - ((((y - offset_MapAreaToDisplayPanel_TopLeftY) / mapAreaHeightInPixels) * mapHeightInLonDegree)
				/ settings.lonVsLat);
	}

	/**
	 * Draw one intersection on buffered image.
	 */
	void drawIntersectionOnImage(final Graphics2D g2d, final IntersectionObject intersection) {
		final int x = (convertLonToX(intersection.lon));
		final int y = (convertLatToY(intersection.lat));
		g2d.fillOval(x - 10, y - 10, 20, 20);
	}

	/**
	 * Draw query rectangle.
	 */
	void drawQueryRectangle(final Graphics2D g2d, final double topLeftLon, final double topLeftLat,
			final double bottomRightLon, final double bottomRightLat) {
		g2d.drawRect(convertLonToX(topLeftLon), convertLatToY(topLeftLat),
				convertLonToX(bottomRightLon) - convertLonToX(topLeftLon),
				-(convertLatToY(topLeftLat) - convertLatToY(bottomRightLat)));
	}

	/**
	 * Draw one road edge on buffered image.
	 */
	void drawRoadEdgeOnImage(final Graphics2D g2d, final Rectangle actualDrawingArea, final EdgeObject roadEdge,
			final EdgeEndMarkerType endMarkerType) {
		int iter = 0;
		for (DrawingObject.LaneObject lane : roadEdge.lanes) {
				/*
				 * Calculate final positions
				 */
				final int startX = (convertLonToX(lane.lonStart));
				final int startY = (convertLatToY(lane.latStart));
				final int endX = (convertLonToX(lane.lonEnd));
				final int endY = (convertLatToY(lane.latEnd));

				/*
				 * Draw a line representing the edge if it intersects the area shown in
				 * the actual panel
				 */
				if (actualDrawingArea.intersectsLine(startX, startY, endX, endY)) {
					g2d.drawLine(startX, startY, endX, endY);
				}
		}
	}

	void drawRoadLaneOnImage(final Graphics2D g2d, final Rectangle actualDrawingArea, final EdgeObject roadEdge,
							 final EdgeEndMarkerType endMarkerType) {

		int iter = 0;

		for (DrawingObject.LaneObject lane : roadEdge.lanes) {

			if (roadEdge.laneBlocks[iter]) {
				/*
				 * Calculate final positions
				 */
				final int startX = (convertLonToX(lane.lonStart));
				final int startY = (convertLatToY(lane.latStart));
				final int endX = (convertLonToX(lane.lonEnd));
				final int endY = (convertLatToY(lane.latEnd));

				/*
				 * Draw a line representing the edge if it intersects the area shown in
				 * the actual panel
				 */
				if (actualDrawingArea.intersectsLine(startX, startY, endX, endY)) {
					g2d.drawLine(startX, startY, endX, endY);
				}
			}
			iter++;
		}

	}

	/**
	 * Draw one tram stop on buffered image.
	 */
	void drawTramStopOnImage(final Graphics2D g2d, final Rectangle actualDrawingArea,
			final TramStopObject tramStopObject) {
		/*
		 * Calculate final positions
		 */
		final int x = (convertLonToX(tramStopObject.lon));
		final int y = (convertLatToY(tramStopObject.lat));

		/*
		 * Draw a line representing the edge if it intersects the area shown in
		 * the actual panel
		 */
		if (actualDrawingArea.contains(x, y)) {
			if (currentZoom > 18) {
				g2d.drawOval(x - 1, y - 1, 2, 2);
			} else {
				g2d.drawOval(x, y, 1, 1);
			}
		}
	}

	/**
	 * Draw one tram stop on buffered image.
	 */
	void drawNodeOnImage(final Graphics2D g2d, final Rectangle actualDrawingArea,
							 final DrawingObject.NodeObject nodeObject) {
		Polygon polygon = new Polygon();
		for (Point2D point2D : nodeObject.polygon) {
			int x = convertLonToX(point2D.getX());
			int y = convertLatToY(point2D.getY());
			polygon.addPoint(x,y);
		}
		g2d.drawPolygon(polygon);
	}

	/**
	 * Draw one vehicle on buffered image.
	 */
	void drawVehicleOnImage(final Rectangle actualDrawingArea, final Graphics2D g2d,
			final Serializable_GUI_Vehicle vehicle, final VehicleObjectType drawType) {
		g2d.setStroke(new BasicStroke((int) (vehicle.width/getMetersPerPixel()), BasicStroke.CAP_BUTT,
				BasicStroke.CAP_BUTT));
		final int headX = convertLonToX(vehicle.lonHead);
		final int headY = convertLatToY(vehicle.latHead);
		final int tailX = convertLonToX(vehicle.lonTail);
		final int tailY = convertLatToY(vehicle.latTail);

		/*
		 * Highlight emergency vehicle.
		 */
		if (VehicleType.getVehicleTypeFromName(vehicle.type) == VehicleType.PRIORITY) {
			g2d.drawImage(imageEmergencyVehicle, tailX - (imageEmergencyVehicle.getWidth() / 2),
					tailY - imageEmergencyVehicle.getHeight(), null);
		}

		/*
		 * speed category
		 */
		int colourType = 0;
		if (drawType == VehicleObjectType.normal) {
			final double speedRatio = vehicle.speed / vehicle.originalEdgeMaxSpeed;
			if (speedRatio < 0.25) {
				colourType = 0;
				g2d.setColor(new Color(255, 0, 0, 255));
			} else if ((0.25 <= speedRatio) && (speedRatio < 0.5)) {
				colourType = 1;
				g2d.setColor(new Color(153, 102, 255, 240));
			} else if ((0.5 <= speedRatio) && (speedRatio < 0.75)) {
				colourType = 2;
				g2d.setColor(new Color(255, 230, 0, 220));
			} else {
				colourType = 3;
				g2d.setColor(new Color(0, 255, 0, 180));
			}
		} else if (drawType == VehicleObjectType.mousePoint) {
			colourType = 4;
			g2d.setColor(Color.CYAN);
		}
		/*
		 * Draw the object if it is in the area shown in the actual panel
		 */
		if (actualDrawingArea.contains(new Point(headX, headY))) {
			g2d.drawLine(headX, headY, tailX, tailY);

			/*
			 * Display vehicle details.
			 */
			if (vehicleDetailOn) {
				String detail = "";
				switch (VehicleDetailType.valueOf(vehicleDetailType)) {
					case Type:
						detail = vehicle.type;
						break;
					case Remaining_Links:
						detail = String.valueOf(vehicle.numLinksToGo);
						break;
					case ID_Worker:
						detail = vehicle.vid + "@" + vehicle.worker;
						break;
					case Driver_Profile:
						detail = vehicle.driverProfile;
						break;
					case Slowdown_Factor:
						detail = vehicle.slowDownFactor;
						break;
					case HeadwayMultiplier:
						detail = String.valueOf(vehicle.headwayMultiplier);
						break;
				}

				g2d.setColor(Color.black);
				g2d.drawString(" " + detail, headX, headY);
			}

			/*
			 * Highlight vehicle affected by priority vehicle
			 */
			if (vehicle.isAffectedByPriorityVehicle) {
				g2d.setColor(Color.CYAN);
				g2d.drawOval(((tailX + headX) / 2) - 3, ((tailY + headY) / 2) - 3, 6, 6);
			}
		}
	}

	/*
	 * Select edge or edge end at mouse point.
	 */
	synchronized void findEdgeAtMousePoint(final Point mousePoint) {
		final double ratioXtoWidth = (-offset_MapAreaToDisplayPanel_TopLeftX + mousePoint.x) / mapAreaWidthInPixels;
		final double ratioYtoHeight = (-offset_MapAreaToDisplayPanel_TopLeftY + mousePoint.y) / mapAreaHeightInPixels;

		final double lat = maxLat - (ratioYtoHeight * Math.abs(maxLat - minLat));
		final double lon = minLon + (ratioXtoWidth * Math.abs(maxLon - minLon));
		final Edge edgeAtPoint = processor.getRoadNetwork().getEdgeAtPoint(lat, lon);
		if (edgeAtPoint != null) {
			// Check whether an intersection should be selected
			final double distToStartPoint = RoadUtil.getDistInMeters(lat, lon, edgeAtPoint.startNode.lat,
					edgeAtPoint.startNode.lon);
			final double distToEndPoint = RoadUtil.getDistInMeters(lat, lon, edgeAtPoint.endNode.lat,
					edgeAtPoint.endNode.lon);
			if ((distToStartPoint <= distToEndPoint) && (distToStartPoint < 0.3)) {
				roadIntersectionAtMousePoint = new IntersectionObject(edgeAtPoint.startNode.lon,
						edgeAtPoint.startNode.lat, edgeAtPoint.index, true);
				roadEdgeAtMousePoint = null;
			} else if ((distToEndPoint < distToStartPoint) && (distToEndPoint < 0.3)) {
				roadIntersectionAtMousePoint = new IntersectionObject(edgeAtPoint.endNode.lon, edgeAtPoint.endNode.lat,
						edgeAtPoint.index, false);
				roadEdgeAtMousePoint = null;
			} else {
				final EdgeObject dummyEdge = new EdgeObject(0, 0, 0, 0, edgeAtPoint.index, 0, "", 0, null, null);
				roadEdgeAtMousePoint = edgeObjects
						.get(Collections.binarySearch(edgeObjects, dummyEdge, new EdgeObjectComparator()));
				roadIntersectionAtMousePoint = null;
			}
		} else {
			roadEdgeAtMousePoint = null;
			roadIntersectionAtMousePoint = null;
		}
	}

	synchronized void findLaneAreaAtMousePoint(final Point mousePoint){
		final double ratioXtoWidth = (-offset_MapAreaToDisplayPanel_TopLeftX + mousePoint.x) / mapAreaWidthInPixels;
		final double ratioYtoHeight = (-offset_MapAreaToDisplayPanel_TopLeftY + mousePoint.y) / mapAreaHeightInPixels;

		final double lat = maxLat - (ratioYtoHeight * Math.abs(maxLat - minLat));
		final double lon = minLon + (ratioXtoWidth * Math.abs(maxLon - minLon));

		final Lane laneAtPoint = processor.getRoadNetwork().findLaneAtPoint(lat, lon);



		if (laneAtPoint != null){
			//laneAtMousePoint = new LaneObject(laneAtPoint.latStart, laneAtPoint.lonStart,
			//								  laneAtPoint.latEnd, laneAtPoint.lonEnd, laneAtPoint.index);
			Edge edgeAtPoint = laneAtPoint.edge;
			final EdgeObject dummyEdge = new EdgeObject(0, 0, 0, 0, edgeAtPoint.index, 0, "", 0, null, null);
			roadEdgeAtMousePoint = edgeObjects
					.get(Collections.binarySearch(edgeObjects, dummyEdge, new EdgeObjectComparator()));
			roadIntersectionAtMousePoint = null;
		}
		else{
			roadEdgeAtMousePoint = null;
			laneAtMousePoint = null;
		}
	}

	/*
	 * Select source/destination window at mouse point
	 */
	synchronized void findSdWindowAtMousePoint(final Point mousePoint) {
		final double ratioXtoWidth = (-offset_MapAreaToDisplayPanel_TopLeftX + mousePoint.x) / mapAreaWidthInPixels;
		final double ratioYtoHeight = (-offset_MapAreaToDisplayPanel_TopLeftY + mousePoint.y) / mapAreaHeightInPixels;

		final double lat = maxLat - (ratioYtoHeight * Math.abs(maxLat - minLat));
		final double lon = minLon + (ratioXtoWidth * Math.abs(maxLon - minLon));

		sdWindowAtMousePoint = getSourceDestinationWindowAtPoint(lat, lon);
	}

	double[] getCoordOfQueryWindow() {
		return new double[] { queryWindowTopLeft.x, queryWindowTopLeft.y, queryWindowBottomRight.x,
				queryWindowBottomRight.y };
	}

	/**
	 * Calculate the latitude of the center of display panel.
	 */
	double getDisplayPanelCenterLatitude() {
		return convertYToLat(0.5 * displayPanelDimension.height);
	}

	/**
	 * Calculate the longitude of the center of display panel.
	 */
	double getDisplayPanelCenterLongitude() {
		return convertXToLon(0.5 * displayPanelDimension.width);
	}

	double getMetersPerPixel() {
		return (Math.cos((maxLat * Math.PI) / 180) * 2 * Math.PI * 6371000) / (256 * (Math.pow(2, currentZoom)));
	}

	Edge getSelectedRoadEdge() {
		return processor.getRoadNetwork().edges.get(roadEdgeAtMousePoint.index);
	}

	Node getSelectedRoadIntersectionNode() {
		final Edge edge = processor.getRoadNetwork().edges.get(roadIntersectionAtMousePoint.edgeIndex);
		if (roadIntersectionAtMousePoint.isAtEdgeStart) {
			return edge.startNode;
		} else {
			return edge.endNode;
		}
	}

	public double[] getSourceDestinationWindowAtPoint(final double lat, final double lon) {
		double[] windowFound = getWindowAtPoint(settings.guiSourceWindowsForInternalVehicle, lon, lat);
		if (windowFound == null) {
			windowFound = getWindowAtPoint(settings.guiDestinationWindowsForInternalVehicle, lon, lat);
		}
		if (windowFound == null) {
			windowFound = getWindowAtPoint(settings.guiSourceDestinationWindowsForInternalVehicle, lon, lat);
		}
		return windowFound;
	}

	double[] getWindowAtPoint(final List<double[]> windowList, final double lon, final double lat) {
		double topLeftLon, topLeftLat, btmRightLon, btmRightLat;
		for (final double[] window : windowList) {
			topLeftLon = window[0];
			topLeftLat = window[1];
			btmRightLon = window[2];
			btmRightLat = window[3];
			if (((Math.abs(lon - topLeftLon) < 0.0001) && ((topLeftLat > lat) && (btmRightLat < lat)))
					|| ((Math.abs(lon - btmRightLon) < 0.0001) && ((topLeftLat > lat) && (btmRightLat < lat)))
					|| ((Math.abs(lat - topLeftLat) < 0.0001) && ((btmRightLon > lon) && (topLeftLon < lon)))
					|| ((Math.abs(lat - btmRightLat) < 0.0001) && ((btmRightLon > lon) && (topLeftLon < lon)))) {
				return new double[] { topLeftLon, topLeftLat, btmRightLon, btmRightLat };
			}
		}
		return null;
	}

	void hidePopupMenu() {
		popup.setVisible(false);
	}

	/**
	 * Import the information of each edge from a string
	 *
	 *
	 */
	void importRoadEdges(final RoadNetwork roadNetwork, final double minLon, final double minLat, final double maxLon,
			final double maxLat) {
		nodeObjects.clear();
		edgeObjects.clear();
		lightObjects.clear();
		tramStopObjects.clear();
		vehicleObjects.clear();

		double startNodeX, startNodeY, endNodeX, endNodeY;
		int numLanes;

		for (final Edge edge : roadNetwork.edges) {
			startNodeX = edge.startNode.lon;
			startNodeY = edge.startNode.lat;
			endNodeX = edge.endNode.lon;
			endNodeY = edge.endNode.lat;
			numLanes = edge.getLaneCount();
			String note = "";
			if (edge.name.length() > 0) {
				note += "\"" + edge.name + "\", ";
			}
			note += edge.type.name() + ", ";
			note += numLanes + " lane(s), ";
			note += (int) edge.length + "m, ";
			note += "'" + edge.startNode.index + "' to '" + edge.endNode.index + "', ";
			note += "Idx " + edge.index + ", ";
			note += (int) (edge.freeFlowSpeed * 3.6) + "kmh";

			List<DrawingObject.LaneObject> laneObjs = new ArrayList<>();
			for (Lane lane : edge.getLanes()) {
				DrawingObject.LaneObject laneObject = new DrawingObject.LaneObject(lane.latStart, lane.lonStart, lane.latEnd, lane.lonEnd);
				laneObjs.add(laneObject);
			}
			// Create simplified edge object
			final EdgeObject e = new EdgeObject(startNodeX, startNodeY, endNodeX, endNodeY, edge.index, numLanes, note,
					edge.length, edge.type, laneObjs);
			edgeObjects.add(e);

			if (edge.endNode.tramStop) {
				final TramStopObject t = new TramStopObject(endNodeX, endNodeY);
				tramStopObjects.add(t);
			}
		}
		Collections.sort(edgeObjects, new EdgeObjectComparator());

		/*
		 * Generate a temporary set of traffic lights, which will be drawn and
		 * can be edited by user.
		 */
		for (final Node node : roadNetwork.nodes) {
			DrawingObject.NodeObject nodeObject = new DrawingObject.NodeObject(node.lon, node.lat, node.getIntersectionPolygon());
			nodeObjects.add(nodeObject);
			if (node.light) {
				final Serializable_GUI_Light lightObject = new Serializable_GUI_Light(node.lon, node.lat, "G");
				lightObjects.add(lightObject);
			}
		}
	}

	void loadIconImages() {
		try {
			for (int i = 0; i < 4; i++) {
				final BufferedImage img = ImageIO
						.read(getClass().getResourceAsStream(settings.inputBuiltinResource + "Car" + i + ".png"));
				imageCars.add(img);
			}
			imageAlert = ImageIO.read(getClass().getResourceAsStream((settings.inputBuiltinResource + "alert.png")));
			imageEmergencyVehicle = ImageIO
					.read(getClass().getResourceAsStream((settings.inputBuiltinResource + "pilot.png")));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void moveMapByClickButtons(final String direction) {
		final double scalePerMove = 0.1;
		int gapX = 0, gapY = 0;
		switch (direction.toLowerCase()) {
		case "up":
			gapY = (int) (displayPanelDimension.height * scalePerMove);
			offsetStaticMapImageTopLeftY += (int) (displayPanelDimension.height * scalePerMove);
			break;
		case "down":
			gapY = -(int) (displayPanelDimension.height * scalePerMove);
			offsetStaticMapImageTopLeftY -= (int) (displayPanelDimension.height * scalePerMove);
			break;
		case "left":
			gapX = (int) (displayPanelDimension.width * scalePerMove);
			offsetStaticMapImageTopLeftX += (int) (displayPanelDimension.width * scalePerMove);
			break;
		case "right":
			gapX = -(int) (displayPanelDimension.width * scalePerMove);
			offsetStaticMapImageTopLeftX -= (int) (displayPanelDimension.width * scalePerMove);
			break;
		}
		moveMapCenterToPoint(gapX, gapY);
	}

	void moveMapCenterToPoint(final int gapX, final int gapY) {
		updateVirtualPanelAfterDragging(gapX, gapY);
		/*
		 * Prepare map image
		 */
		needNewStaticMapImage = true;
		/*
		 * Prepare road network image.
		 */
		prepareRoadNetworkImage();
		/*
		 * Prepare objects image
		 */
		prepareObjectsImage();

		/*
		 * Prepare overlay image
		 */
		prepareOverlayImage();
		/*
		 * Repaint
		 */
		repaint();
	}

	@Override
	synchronized public void paintComponent(final Graphics g) {

		super.paintComponent(g);
		final Graphics2D g2dPanel = (Graphics2D) g;

		/*
		 * Draw static map image
		 */
		if (staticMapImageOn) {
			g2dPanel.drawImage(staticMapImage, offsetStaticMapImageTopLeftX, offsetStaticMapImageTopLeftY, null);
		}

		/*
		 * Draw images except static map image
		 */
		if (minZoomExceptStaticMapImage <= currentZoom && currentZoom <= maxZoomExceptStaticMapImage) {
			if (roadNetworkOn) {
				g2dPanel.drawImage(roadNetworkImage, offsetImageTopLeftX, offsetImageTopLeftY, null);
			}
			g2dPanel.drawImage(objectsImage, offsetImageTopLeftX, offsetImageTopLeftY, null);
			g2dPanel.drawImage(overlayImage, offsetImageTopLeftX, offsetImageTopLeftY, null);
		}

	}

	/**
	 * Draw vehicle objects to an image
	 */
	synchronized void prepareObjectsImage() {
		isComposingObjectImage = true;

		final Rectangle actualDrawingArea = new Rectangle(displayPanelDimension);
		objectsImage = new BufferedImage(displayPanelDimension.width, displayPanelDimension.height,
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = objectsImage.createGraphics();

		if (!isUseTrafficFlowToReplaceVehicle) {
			/*
			 * Draw traffic signals
			 */
			g2d.setStroke(new BasicStroke(2));
			for (final Serializable_GUI_Light light : lightObjects) {

				final int x = convertLonToX(light.longitude);
				final int y = convertLatToY(light.latitude);

				/*
				 * Color
				 */
				if (light.color.equals("G")) {
					g2d.setColor(Color.GREEN);
				} else if (light.color.equals("Y")) {
					g2d.setColor(Color.YELLOW);
				} else if (light.color.equals("R") || light.color.equals("KR")) {
					g2d.setColor(Color.RED);
				}

				/*
				 * Draw the object if it is in the area shown in the actual
				 * panel
				 */
				if (actualDrawingArea.contains(new Point(x, y))) {
					if (currentZoom > 18) {
						g2d.fillOval(x - 5, y - 5, 10, 10);
					} else {
						g2d.fillOval(x - 1, y - 1, 2, 2);
					}
				}
			}
			/*
			 * Draw vehicles
			 */
			for (final Serializable_GUI_Vehicle vehicle : vehicleObjects) {
				drawVehicleOnImage(actualDrawingArea, g2d, vehicle, VehicleObjectType.normal);
			}
		} else {
			final BasicStroke thickStroke = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND);
			final BasicStroke thinStroke = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND);
			// Get average ratio (vehicle speed/free-flow speed) at individual
			// edges
			final double[][] accumulatedSpdRatio = new double[edgeObjects.size()][2];
			for (final Serializable_GUI_Vehicle vehicle : vehicleObjects) {
				accumulatedSpdRatio[vehicle.edgeIndex][0]++;
				accumulatedSpdRatio[vehicle.edgeIndex][1] += vehicle.speed / vehicle.originalEdgeMaxSpeed;
			}

			// Draw green edges first
			g2d.setStroke(thinStroke);
			for (int i = 0; i < edgeObjects.size(); i++) {
				g2d.setColor(Color.decode("#84CA50"));
				if ((int) accumulatedSpdRatio[i][0] == 0) {
					drawRoadEdgeOnImage(g2d, actualDrawingArea, edgeObjects.get(i), EdgeEndMarkerType.none);
				} else if ((accumulatedSpdRatio[i][1] / accumulatedSpdRatio[i][0]) > 0.75) {
					drawRoadEdgeOnImage(g2d, actualDrawingArea, edgeObjects.get(i), EdgeEndMarkerType.none);
				}
			}
			// Draw other edges on top of the green ones
			g2d.setStroke(thickStroke);
			for (int i = 0; i < edgeObjects.size(); i++) {
				// Determine color of edge based on relative speed
				if ((int) accumulatedSpdRatio[i][0] == 0) {
					continue;
				} else if ((accumulatedSpdRatio[i][1] / accumulatedSpdRatio[i][0]) > 0.75) {
					continue;
				} else if ((accumulatedSpdRatio[i][1] / accumulatedSpdRatio[i][0]) > 0.5) {
					g2d.setColor(Color.decode("#F07D02"));
				} else if ((accumulatedSpdRatio[i][1] / accumulatedSpdRatio[i][0]) > 0.25) {
					g2d.setColor(Color.decode("#E60000"));
				} else {
					g2d.setColor(Color.decode("#9E1313"));
				}
				drawRoadEdgeOnImage(g2d, actualDrawingArea, edgeObjects.get(i), EdgeEndMarkerType.none);
			}

		}

		isComposingObjectImage = false;
	}

	/**
	 * Draw image containing blocked edges, selected areas, etc.
	 */
	void prepareOverlayImage() {
		overlayImage = new BufferedImage(displayPanelDimension.width, displayPanelDimension.height,
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = overlayImage.createGraphics();
		g2d.setFont(new Font(g2d.getFont().getFontName(), Font.BOLD, 20));
		final Rectangle actualDrawingArea = new Rectangle(displayPanelDimension.width, displayPanelDimension.height);
		/*
		 * Draw source/destination window
		 */
		g2d.setColor(Color.magenta);
		final float dash[] = { 4.0f };
		g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));
		for (final double[] window : settings.guiSourceWindowsForInternalVehicle) {
			drawQueryRectangle(g2d, window[0], window[1], window[2], window[3]);
			g2d.drawString("S", convertLonToX(window[0]) - 20, convertLatToY(window[1]) + 20);
		}
		for (final double[] window : settings.guiDestinationWindowsForInternalVehicle) {
			drawQueryRectangle(g2d, window[0], window[1], window[2], window[3]);
			g2d.drawString("D", convertLonToX(window[0]) - 20, convertLatToY(window[1]) + 20);
		}
		for (final double[] window : settings.guiSourceDestinationWindowsForInternalVehicle) {
			drawQueryRectangle(g2d, window[0], window[1], window[2], window[3]);
			g2d.drawString("S/D", convertLonToX(window[0]) - 40, convertLatToY(window[1]) + 20);
		}
		g2d.setColor(Color.black);

		/*
		 * Draw blocked lanes
		 */
		g2d.setColor(Color.magenta);
		g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10.0f },
				0.0f));
		final ArrayList<EdgeObject> roadEdgesToDraw = new ArrayList<>(roadEdgesWithBlockingSign.values());
		for (final EdgeObject re : roadEdgesToDraw) {
			drawRoadLaneOnImage(g2d, actualDrawingArea, re, EdgeEndMarkerType.start);
		}
		g2d.setColor(Color.black);

		/*
		 * Edge at mouse point
		 */
		if (roadEdgeAtMousePoint != null) {
			g2d.setColor(Color.cyan);
			g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			drawRoadEdgeOnImage(g2d, actualDrawingArea, roadEdgeAtMousePoint, EdgeEndMarkerType.start);
			g2d.setColor(Color.black);
			g2d.drawString(roadEdgeAtMousePoint.note, mousePoint.x, mousePoint.y);
		}

		/*
		 * Intersection at mouse point
		 */
		if (!controlPanel.isDuringSimulation) {
			g2d.setColor(Color.cyan);
			if (roadIntersectionAtMousePoint != null) {
				g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				drawIntersectionOnImage(g2d, roadIntersectionAtMousePoint);
			}

			/*
			 * Highlight window at mouse point
			 */
			if (sdWindowAtMousePoint != null) {
				drawQueryRectangle(g2d, sdWindowAtMousePoint[0], sdWindowAtMousePoint[1], sdWindowAtMousePoint[2],
						sdWindowAtMousePoint[3]);
			}
			g2d.setColor(Color.black);
		}
		/*
		 * Draw current query window
		 */
		g2d.setColor(Color.blue);
		g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (validateQueryWindow()) {
			drawQueryRectangle(g2d, queryWindowTopLeft.x, queryWindowTopLeft.y, queryWindowBottomRight.x,
					queryWindowBottomRight.y);
		}
		g2d.setColor(Color.black);

	}

	/**
	 * Draw the road network to an image, which will be used as the background
	 * to draw vehicles
	 */
	void prepareRoadNetworkImage() {
		roadNetworkImage = new BufferedImage(displayPanelDimension.width, displayPanelDimension.height,
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = roadNetworkImage.createGraphics();
		g2d.setColor(new Color(224, 224, 235, 255));
		int laneWidthPixel = (int) Math.round((settings.laneWidthInMeters/getMetersPerPixel())*0.95);
		final Rectangle actualDrawingArea = new Rectangle(displayPanelDimension.width, displayPanelDimension.height);
		g2d.setStroke(new BasicStroke(laneWidthPixel, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		for (final EdgeObject e : edgeObjects) {
			drawRoadEdgeOnImage(g2d, actualDrawingArea, e, EdgeEndMarkerType.none);
		}
		g2d.setStroke(new BasicStroke((float) Math.ceil(0.1/getMetersPerPixel())));
		g2d.setColor(Color.black);
		for (DrawingObject.NodeObject nodeObject : nodeObjects) {
			drawNodeOnImage(g2d, actualDrawingArea, nodeObject);
		}
		g2d.setStroke(new BasicStroke(laneWidthPixel, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		g2d.setColor(Color.BLUE);
		for (final TramStopObject t : tramStopObjects) {
			drawTramStopOnImage(g2d, actualDrawingArea, t);
		}

	}

	/**
	 * Download static map image.
	 */
	void prepareStaticMapImage() {
		needNewStaticMapImage = false;
		if (!staticMapImageOn) {
			return;
		}
		isLoadingStaticMapImage = true;
		final Thread thread = new Thread() {
			@Override
			public void run() {
				// Max number of tiles on two axis 
				int tileCountHorizontal = 1;
				while (tileCountHorizontal * 256 < displayPanelDimension.width) {
					tileCountHorizontal++;
				}
				tileCountHorizontal += 1;

				int tileCountVertical = 1;
				while (tileCountVertical * 256 < displayPanelDimension.height) {
					tileCountVertical++;
				}
				tileCountVertical += 1;

				// The static image will hold the tiles
				staticMapImage = new BufferedImage(tileCountHorizontal * 256, tileCountVertical * 256,
						BufferedImage.TYPE_INT_ARGB);

				// Get the x/y of the top-left tile
				double lonOfPanelCorner = convertXToLon(0);
				double latOfPanelCorner = convertYToLat(0);
				String tileNumberOfFirstTile = openStreetMapTile.getTileNumber(latOfPanelCorner, lonOfPanelCorner,
						currentZoom);
				String[] partsInFirstTileNumber = tileNumberOfFirstTile.split("/");
				int xFirstTileNumber = Integer.parseInt(partsInFirstTileNumber[1]);
				int yFirstTileNumber = Integer.parseInt(partsInFirstTileNumber[2]);

				// Update the offset of the whole static map image, relative to the top-left corner of panel
				OpenStreetMapTile.BoundingBox boxOfFirstTile = openStreetMapTile.tile2boundingBox(currentZoom,
						xFirstTileNumber, yFirstTileNumber);
				offsetStaticMapImageTopLeftX = (int) (-1 * (256.0 * (lonOfPanelCorner - boxOfFirstTile.west)
						/ (boxOfFirstTile.east - boxOfFirstTile.west)));
				offsetStaticMapImageTopLeftY = (int) (-1 * (256.0 * (boxOfFirstTile.north - latOfPanelCorner)
						/ (boxOfFirstTile.north - boxOfFirstTile.south)));

				// Download all the tiles and draw them on the buffered image	
				final ArrayList<Integer> completedTiles = new ArrayList<Integer>();
				for (int i = 0; i <= tileCountHorizontal; i++) {
					for (int j = 0; j <= tileCountVertical; j++) {
						// tile number
						int xInTileNumber = xFirstTileNumber + i;
						int yInTileNumber = yFirstTileNumber + j;
						String tileNumber = "" + currentZoom + "/" + xInTileNumber + "/" + yInTileNumber;
						// coordinate of tile's top-left corner on panel
						int xInPanel = i * 256;
						int yInPanel = j * 256;

						new Thread(new Runnable() {
							int tileId;
							int totalNumTiles;

							public Runnable init(int tileId, int totalNumTiles) {
								this.tileId = tileId;
								this.totalNumTiles = totalNumTiles;
								return this;
							}

							public void run() {
								try {
									if (currentZoom <= maxZoomStaticMapImage) {
										String[] prefixes = new String[] { "a", "b", "c" };
										String randomPrefix = prefixes[random.nextInt(prefixes.length)];
										String urlString = "http://" + randomPrefix + ".tile.openstreetmap.org/"
												+ tileNumber + ".png";
										final URL url = new URL(urlString);
										final BufferedImage in = ImageIO.read(url);
										final Graphics2D g = staticMapImage.createGraphics();
										g.drawImage(in, xInPanel, yInPanel, null);
										g.dispose();
									} else {
										BufferedImage in = ImageIO.read(getClass()
												.getResourceAsStream((settings.inputBuiltinResource + "osmZoom.png")));
										final Graphics2D g = staticMapImage.createGraphics();
										g.drawImage(in, xInPanel, yInPanel, null);
										g.dispose();
									}
									repaint();
									completedTiles.add(this.tileId);
									if (completedTiles.size() == totalNumTiles) {
										isLoadingStaticMapImage = false;
									}
								} catch (Exception e) {
								}
							}
						}.init(i * j, tileCountHorizontal * tileCountVertical)).start();
					}
				}
			}
		};
		thread.start();
	}

	void removeSourceDestinationWindow(final double[] windowToRemove) {
		removeWindow(settings.guiSourceWindowsForInternalVehicle, windowToRemove);
		removeWindow(settings.guiDestinationWindowsForInternalVehicle, windowToRemove);
		removeWindow(settings.guiSourceDestinationWindowsForInternalVehicle, windowToRemove);
	}

	void removeWindow(final List<double[]> windowList, final double[] windowToRemove) {
		for (int i = 0; i < windowList.size(); i++) {
			boolean isMatch = true;
			for (int j = 0; j < 4; j++) {
				if (windowList.get(i)[j] != windowToRemove[j]) {
					isMatch = false;
					break;
				}
			}
			if (isMatch) {
				windowList.remove(i);
				return;
			}
		}
	}

	/**
	 * Reset map's position and zoom. Similar to "change zoom" method except
	 * that map area offset is calculated from scratch.
	 */
	void resetMap() {
		mousePoint.x = (int) (0.5 * displayPanelDimension.width);
		mousePoint.y = (int) (0.5 * displayPanelDimension.height);
		currentZoom = 16;
		mapWidthInLonDegree = Math.abs(maxLon - minLon);
		mapHeightInLonDegree = Math.abs(maxLat - minLat) * settings.lonVsLat;
		updateMapAreaDimension();
		updateOffsetMapAreaToDisplayPanel(mousePoint, 0.5, 0.5);
		staticMapImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		prepareStaticMapImage();
		prepareRoadNetworkImage();
		prepareObjectsImage();
		prepareOverlayImage();
		repaint();
	}

	void setControlPanel(final ControlPanel controlPanel) {
		this.controlPanel = controlPanel;
	}

	void setCursorBasedOnZoom() {
		if (currentZoom >= minZoomExceptStaticMapImage) {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		} else {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	void showPopupMenu(final String toDo, final int x, final int y) {
		popup = new JPopupMenu();
		if (controlPanel.isDuringSimulation) {
			if (toDo.equals("edge")) {
				final EdgeObject edgeSelected = roadEdgeAtMousePoint;
				final ActionListener menuListener = new ActionListener() {
					public void actionPerformed(final ActionEvent event) {
						final String[] words = event.getActionCommand().split(" ");
						if (words[0].equals("Unblock")) {
							blockUnblockLane(edgeSelected, Integer.parseInt(words[2]), false);
						} else if (words[0].equals("Block")) {
							blockUnblockLane(edgeSelected, Integer.parseInt(words[2]), true);
						} else if (words[0].equals("Change")) {
							changeLaneDirection(edgeSelected);
						} else if (words[0].equals("Close")) {
							hidePopupMenu();
						}
					}
				};
				// Add blocking options based on lanes' status
				for (int i = 0; i < edgeSelected.laneBlocks.length; i++) {
					if (edgeSelected.laneBlocks[i]) {
						final JMenuItem item = new JMenuItem("Unblock lane " + i);
						item.addActionListener(menuListener);
						popup.add(item);
					} else {
						final JMenuItem item = new JMenuItem("Block lane " + i);
						item.addActionListener(menuListener);
						popup.add(item);
					}
				}	
				final JMenuItem itemC = new JMenuItem("Change Direction of Last Lane");
				itemC.addActionListener(menuListener);
				popup.add(itemC);
				
				// Add close menu option
				final JMenuItem item = new JMenuItem("Close menu");
				item.addActionListener(menuListener);
				popup.add(item);

			}
			else if (toDo.equals("lane")){

			}
		} else {
			if (toDo.equals("intersection")) {
				final Node nodeSelected = getSelectedRoadIntersectionNode();
				final ActionListener menuListener = new ActionListener() {

					public void actionPerformed(final ActionEvent event) {
						// If there is light, remove it, vice versa
						processor.setLightChangeNode(nodeSelected);
						// Change light object list
						if (event.getActionCommand().equalsIgnoreCase("Add light")) {
							final Serializable_GUI_Light lightObject = new Serializable_GUI_Light(nodeSelected.lon,
									nodeSelected.lat, "G");
							lightObjects.add(lightObject);
							// Update object image layer
							prepareObjectsImage();
						} else if (event.getActionCommand().equalsIgnoreCase("Delete light")) {
							for (final Serializable_GUI_Light lightObject : lightObjects) {
								if ((lightObject.longitude == nodeSelected.lon)
										&& (lightObject.latitude == nodeSelected.lat)) {
									lightObjects.remove(lightObject);
									break;
								}
							}
							// Update object image layer
							prepareObjectsImage();
						} else if (event.getActionCommand().equalsIgnoreCase("Close menu")) {
							hidePopupMenu();
						}

					}
				};

				final JMenuItem itemAddLight = new JMenuItem("Add light");
				itemAddLight.setEnabled(!nodeSelected.light);
				itemAddLight.addActionListener(menuListener);
				popup.add(itemAddLight);
				final JMenuItem itemDeleteLight = new JMenuItem("Delete light");
				itemDeleteLight.setEnabled(nodeSelected.light);
				itemDeleteLight.addActionListener(menuListener);
				popup.add(itemDeleteLight);
				// Add close menu option
				final JMenuItem item = new JMenuItem("Close menu");
				item.addActionListener(menuListener);
				popup.add(item);
			} else if (toDo.equals("setup")) {
				final double[] windowSelected = sdWindowAtMousePoint;
				final ActionListener menuListener = new ActionListener() {
					public void actionPerformed(final ActionEvent event) {
						if (event.getActionCommand().equalsIgnoreCase("Set as source")) {
							settings.guiSourceWindowsForInternalVehicle.add(getCoordOfQueryWindow());
							// Overlay image
							prepareOverlayImage();
						} else if (event.getActionCommand().equalsIgnoreCase("Set as destination")) {
							settings.guiDestinationWindowsForInternalVehicle.add(getCoordOfQueryWindow());
							// Overlay image
							prepareOverlayImage();
						} else if (event.getActionCommand().equalsIgnoreCase("Set as source and destination")) {
							settings.guiSourceDestinationWindowsForInternalVehicle.add(getCoordOfQueryWindow());
							// Overlay image
							prepareOverlayImage();
						} else if (event.getActionCommand().equalsIgnoreCase("Download road network")) {
							new GuiUtil.OSMDownloader(MonitorPanel.this, gui, settings).execute();
						} else if (event.getActionCommand().equalsIgnoreCase("Remove source/destination window")) {
							removeSourceDestinationWindow(windowSelected);
							// Overlay image
							prepareOverlayImage();
						} else if (event.getActionCommand().equalsIgnoreCase("Close menu")) {
							hidePopupMenu();
						}
					}
				};

				if (windowSelected == null) {
					final JMenuItem itemAddSrcArea = new JMenuItem("Set as source");
					itemAddSrcArea.addActionListener(menuListener);
					popup.add(itemAddSrcArea);
					final JMenuItem itemAddDstArea = new JMenuItem("Set as destination");
					itemAddDstArea.addActionListener(menuListener);
					popup.add(itemAddDstArea);
					final JMenuItem itemAddSrcDstArea = new JMenuItem("Set as source and destination");
					itemAddSrcDstArea.addActionListener(menuListener);
					popup.add(itemAddSrcDstArea);
				} else {
					final JMenuItem itemRemoveSdArea = new JMenuItem("Remove source/destination window");
					itemRemoveSdArea.addActionListener(menuListener);
					popup.add(itemRemoveSdArea);
				}
				if (!isDownloadingOSM) {
					final JMenuItem itemDownloadOSM = new JMenuItem("Download road network");
					itemDownloadOSM.addActionListener(menuListener);
					popup.add(itemDownloadOSM);
				}
				// Add close menu option
				final JMenuItem item = new JMenuItem("Close menu");
				item.addActionListener(menuListener);
				popup.add(item);
			}
		}
		// Show
		popup.show(this, x, y);
	}

	public void startSetupProgress() {
		clearObjectsFromLastSimulation();
		lblSetupProgress.setText("Building road network at workers...");
		lblSetupProgress.setVisible(true);
	}

	/**
	 * Switch on/off road network graph
	 */
	public void switchRoadNetworkGraph(final boolean onOff) {
		roadNetworkOn = onOff;
		repaint();
	}

	/**
	 * Switch on/off static map image
	 */
	public void switchStaticMapImage(final boolean onOff) {
		staticMapImageOn = onOff;
		if (staticMapImageOn) {
			needNewStaticMapImage = true;
		}
		repaint();
	}

	/**
	 * Switch on/off vehicle details
	 */
	public void switchVehicleDetail(final boolean onOff) {
		vehicleDetailOn = onOff;
		if (!isReceivingData) {
			prepareObjectsImage();
			repaint();
		}
	}

	/**
	 * Redraw the scene using new data.
	 *
	 * @param realTimeFactor
	 *
	 *
	 */
	public void update(final HashMap<String, ArrayList<Serializable_GUI_Vehicle>> guiVehicleList,
			final HashMap<String, ArrayList<Serializable_GUI_Light>> guiLightList, final double realTimeFactor, double resolution) {
		if (isComposingObjectImage) {
			return;
		}

		if (draggingMap) {
			return;
		}

		isReceivingData = true;

		/*
		 * Reset values from last step.
		 */
		vehicleObjects.clear();
		lightObjects.clear();
		/*
		 * Vehicles.
		 */
		for (final ArrayList<Serializable_GUI_Vehicle> list : guiVehicleList.values()) {
			vehicleObjects.addAll(list);
		}
		// Update statistics
		guiStatistic.setVehicleData(vehicleObjects);
		guiStatistic.calculateAverageSpeed();
		statisticLabel.setText(guiStatistic.getResult());
		/*
		 * Traffic lights.
		 */
		for (final ArrayList<Serializable_GUI_Light> list : guiLightList.values()) {
			lightObjects.addAll(list);
		}

		/*
		 * Prepare objects image.
		 */
		prepareObjectsImage();
		/*
		 * Update time label
		 */
		updateTimeLabel(false, gui.stepToDraw, realTimeFactor,resolution);
		/*
		 * Re-draw
		 */
		repaint();

		isReceivingData = false;
	}

	/*
	 * Calculate the dimension of virtual panel that holds the whole map. Then
	 * calculate the distance offsets in X/Y that need to be added to everything
	 * on the virtual panel.
	 *
	 * It seems that the online maps are based on distance measured by longitude
	 * degrees. That means the vertical distance is also calculated using the
	 * distance unit for horizontal distance. That is why we use a conversion
	 * parameter when calculating the dimension of the virtual panel.
	 */
	void updateMapAreaAfterZoom(final Point point) {
		final double pointXvsMapAreaWidth = (-offset_MapAreaToDisplayPanel_TopLeftX + point.x) / mapAreaWidthInPixels;
		final double pointYvsMapAreaHeight = (-offset_MapAreaToDisplayPanel_TopLeftY + point.y) / mapAreaHeightInPixels;

		updateMapAreaDimension();

		updateOffsetMapAreaToDisplayPanel(point, pointXvsMapAreaWidth, pointYvsMapAreaHeight);
	}

	void updateMapAreaDimension() {
		final double mapWidthInMeters = RoadUtil.getDistInMeters(maxLat, minLon, maxLat, maxLon);
		final double mapHeightInMeters = RoadUtil.getDistInMeters(minLat, minLon, maxLat, minLon);
		final double metersPerPixel = getMetersPerPixel();
		mapAreaWidthInPixelsOld = mapAreaWidthInPixels;
		mapAreaWidthInPixels = mapWidthInMeters / metersPerPixel;
		mapAreaHeightInPixels = mapHeightInMeters / metersPerPixel;
	}

	void updateOffsetMapAreaToDisplayPanel(final Point point, final double pointXvsMapAreaWidth,
			final double pointYvsMapAreaHeight) {
		offset_MapAreaToDisplayPanel_TopLeftX = point.x - (pointXvsMapAreaWidth * mapAreaWidthInPixels);
		offset_MapAreaToDisplayPanel_TopLeftY = point.y - (pointYvsMapAreaHeight * mapAreaHeightInPixels);
	}

	public void updateSetupProgress(final double createdVehicleRatio) {
		final int perc = (int) (createdVehicleRatio * 100);
		lblSetupProgress.setText("Creating vehicles: " + perc + "% completed");
	}

	/**
	 * Update time label
	 *
	 * @param realTimeFactor
	 */
	void updateTimeLabel(final boolean reset, final int stepCurrent, final double realTimeFactor, double resolution) {
		if (reset) {
			statusLabel.setText("");
		} else {
			final long totalSeconds = (long) (stepCurrent / resolution);
			final long s = totalSeconds % 60;
			final long m = (totalSeconds / 60) % 60;
			final long h = (totalSeconds / (60 * 60)) % 24;
			final long d = totalSeconds / (60 * 60 * 24);
			String speedUp = "N/A";
			if (realTimeFactor > 0) {
				speedUp = String.valueOf(Math.round(realTimeFactor * 100.0) / 100.0);
			}
			statusLabel.setText("Time: " + d + " day " + h + " hours " + m + " minutes " + String.format("%02d", s)
					+ " seconds. Speed-up: " + speedUp + ".");
		}
	}

	/**
	 * Update virtual panel position after dragging
	 */
	void updateVirtualPanelAfterDragging(final int gapX, final int gapY) {
		offsetImageTopLeftX = 0;
		offsetImageTopLeftY = 0;

		offset_MapAreaToDisplayPanel_TopLeftX += gapX;
		offset_MapAreaToDisplayPanel_TopLeftY += gapY;

		// Revert change if off-limit
		if (!validDisplayY() || !validDisplayX()) {
			offset_MapAreaToDisplayPanel_TopLeftX -= gapX;
			offset_MapAreaToDisplayPanel_TopLeftY -= gapY;
		}
	}

	/**
	 * Update virtual panel position during dragging
	 */
	void updateVirtualPanelDuringDragging(final MouseEvent e) {
		final Point newPoint = e.getPoint();
		final int gapX = newPoint.x - mousePoint.x;
		final int gapY = newPoint.y - mousePoint.y;
		// New offset of virtual panel to the top-left corner of display panel
		offset_MapAreaToDisplayPanel_TopLeftX += gapX;
		offset_MapAreaToDisplayPanel_TopLeftY += gapY;
		// Revert change if off-limit
		if (!validDisplayX() || !validDisplayY()) {
			offset_MapAreaToDisplayPanel_TopLeftX -= gapX;
			offset_MapAreaToDisplayPanel_TopLeftY -= gapY;
		} else {
			// New offset of temporary image to the top-left corner of display
			// panel
			offsetImageTopLeftX += gapX;
			offsetImageTopLeftY += gapY;
			offsetStaticMapImageTopLeftX += gapX;
			offsetStaticMapImageTopLeftY += gapY;
			// Remember new point
			mousePoint = newPoint;
		}
	}

	/*
	 * Validate query window.
	 */
	boolean validateQueryWindow() {
		if ((queryWindowTopLeft == null) || (queryWindowBottomRight == null)) {
			return false;
		}
		if ((queryWindowTopLeft.x == queryWindowBottomRight.x) || (queryWindowTopLeft.y == queryWindowBottomRight.y)) {
			return false;
		}
		if (queryWindowTopLeft.x > queryWindowBottomRight.x) {
			return false;
		}
		return !(queryWindowTopLeft.y < queryWindowBottomRight.y);
	}

	boolean validDisplayX() {
		final double newMinLonDisplayArea = convertXToLon(0);
		final double newMaxLonDisplayArea = convertXToLon(displayPanelDimension.getWidth());
		if (newMinLonDisplayArea < -180) {
			return false;
		} else return !(newMaxLonDisplayArea > 180);
	}

	boolean validDisplayY() {
		final double newMaxLatDisplayArea = convertYToLat(0);
		final double newMinLatDisplayArea = convertYToLat(displayPanelDimension.getHeight());
		if (newMaxLatDisplayArea > 80) {
			return false;
		} else return !(newMinLatDisplayArea < -80);
	}

	void updateEdgeObject(Edge edge){
		final EdgeObject dummyEdge = new EdgeObject(0, 0, 0, 0, edge.index,
				0, "", 0, null, null);
		EdgeObject edgeObj = edgeObjects.get(Collections.binarySearch(edgeObjects, dummyEdge, new EdgeObjectComparator()));



		List<DrawingObject.LaneObject> laneObjs = new ArrayList<>();
		for (Lane lane : edge.getLanes()) {
			DrawingObject.LaneObject laneObject = new DrawingObject.LaneObject(lane.latStart, lane.lonStart, lane.latEnd, lane.lonEnd);
			laneObjs.add(laneObject);
		}

		edgeObj.numLanes = edge.getLaneCount();
		edgeObj.lanes = laneObjs;

		edgeObj.laneBlocks = new boolean[edgeObj.numLanes];
		for (int i = 0; i < edgeObj.laneBlocks.length; i++) {
			edgeObj.laneBlocks[i] = false;
		}


	}
}

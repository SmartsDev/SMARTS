package processor.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import common.Settings;
import processor.server.FileOutput;
import processor.server.Server;
import traffic.light.LightUtil;
import traffic.light.TrafficLightTiming;
import traffic.routing.RouteUtil;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JToggleButton;
import javax.swing.JRadioButton;

public class ControlPanel_MiscConfig extends JPanel {
	GUI gui;
	private final JButton btnSetupWorkers;
	private final JTextField textField_numRandomBackgroundPrivateVehicles;
	private final JTextField textField_numRandomBackgroundTrams;
	private final JTextField textField_numRandomBackgroundBuses;
	private final JTextField textField_TotalNumSteps;
	private final JTextField textField_NumStepsPerSec;
	private final JTextField textField_lookAheadDist;
	private final JFileChooser fc = new JFileChooser();
	private final JComboBox comboBoxTrafficLight;
	private final JButton btnLoadForegroundVehicles;
	private final JButton btnLoadBackgroundVehicles;
	private final JComboBox comboBoxRouting;
	private MonitorPanel monitor;
	private final JLabel lblnumRandomTrams;
	private final JLabel lblnumRandomBuses;
	private final JCheckBox chckbxExternalReroute;
	private final JCheckBox chckbxServerbased;
	private JLabel lblBackgroundRouteFile;
	private JLabel lblForegroundRouteFile;
	private JTextField textField_ForegroundRouteFile;
	private JTextField textField_BackgroundRouteFile;
	private JRadioButton rdbtnLeftDrive;
	private JRadioButton rdbtnRightDrive;
	private JLabel lblOutputTravelTimes;
	private JComboBox comboBoxOutputTravelTime;
	private JComboBox comboBoxOutputTrajectory;
	private JComboBox comboBoxOutputRoute;
	private JCheckBox chckbxAllowTurnFromAnyLane;

	public ControlPanel_MiscConfig(final GUI gui) {
		this.gui = gui;
		setPreferredSize(new Dimension(428, 706));

		// Set default directory of file chooser
		final File workingDirectory = new File(System.getProperty("user.dir"));
		fc.setCurrentDirectory(workingDirectory);

		btnLoadForegroundVehicles = new JButton("Change");
		btnLoadForegroundVehicles.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btnLoadForegroundVehicles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					Settings.inputForegroundVehicleFile = file.getPath();
				}
				if (returnVal == JFileChooser.CANCEL_OPTION) {
					Settings.inputForegroundVehicleFile = "";
				}
				refreshFileLabels();
			}
		});

		lblForegroundRouteFile = new JLabel("Foreground route file");

		textField_ForegroundRouteFile = new JTextField();
		textField_ForegroundRouteFile.setEditable(false);
		textField_ForegroundRouteFile.setColumns(10);

		btnLoadBackgroundVehicles = new JButton("Change");
		btnLoadBackgroundVehicles.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btnLoadBackgroundVehicles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					Settings.inputBackgroundVehicleFile = file.getPath();
				}
				if (returnVal == JFileChooser.CANCEL_OPTION) {
					Settings.inputBackgroundVehicleFile = "";
				}
				refreshFileLabels();
			}
		});

		lblBackgroundRouteFile = new JLabel("Background route file");

		textField_BackgroundRouteFile = new JTextField();
		textField_BackgroundRouteFile.setEditable(false);
		textField_BackgroundRouteFile.setColumns(10);

		final JLabel lblnumRandomPrivateVehicles = new JLabel(
				"No. of random background private vehicles");
		lblnumRandomPrivateVehicles
				.setHorizontalAlignment(SwingConstants.RIGHT);
		lblnumRandomPrivateVehicles.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblnumRandomPrivateVehicles.setToolTipText("");

		textField_numRandomBackgroundPrivateVehicles = new JTextField();
		textField_numRandomBackgroundPrivateVehicles.setFont(new Font("Tahoma",
				Font.PLAIN, 13));
		textField_numRandomBackgroundPrivateVehicles
				.setToolTipText("Non-negative integer");
		textField_numRandomBackgroundPrivateVehicles.setText("100");
		textField_numRandomBackgroundPrivateVehicles
				.setInputVerifier(new GuiUtil.NonNegativeIntegerVerifier());

		lblnumRandomTrams = new JLabel(
				"No. of random background trams (if applicable)");
		lblnumRandomTrams.setToolTipText("");
		lblnumRandomTrams.setHorizontalAlignment(SwingConstants.RIGHT);
		lblnumRandomTrams.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_numRandomBackgroundTrams = new JTextField();
		textField_numRandomBackgroundTrams
				.setToolTipText("Non-negative integer");
		textField_numRandomBackgroundTrams.setText("5");
		textField_numRandomBackgroundTrams.setFont(new Font("Tahoma",
				Font.PLAIN, 13));

		lblnumRandomBuses = new JLabel(
				"No. of random background buses (if applicable)");
		lblnumRandomBuses.setToolTipText("");
		lblnumRandomBuses.setHorizontalAlignment(SwingConstants.RIGHT);
		lblnumRandomBuses.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_numRandomBackgroundBuses = new JTextField();
		textField_numRandomBackgroundBuses
				.setToolTipText("Non-negative integer");
		textField_numRandomBackgroundBuses.setText("5");
		textField_numRandomBackgroundBuses.setFont(new Font("Tahoma",
				Font.PLAIN, 13));

		final JLabel lblNumberOfSteps = new JLabel("Max number of steps");
		lblNumberOfSteps.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNumberOfSteps.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_TotalNumSteps = new JTextField();
		textField_TotalNumSteps.setToolTipText("Non-negative integer");
		textField_TotalNumSteps.setFont(new Font("Tahoma", Font.PLAIN, 13));
		textField_TotalNumSteps.setText("18000");

		final JLabel lblNumberOfSteps_1 = new JLabel(
				"Number of steps per second");
		lblNumberOfSteps_1.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNumberOfSteps_1.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_NumStepsPerSec = new JTextField();
		textField_NumStepsPerSec
				.setToolTipText("Real number between 0.1 and 1000");
		textField_NumStepsPerSec.setFont(new Font("Tahoma", Font.PLAIN, 13));
		textField_NumStepsPerSec.setText("5");

		final JLabel lblLookAheadDist = new JLabel(
				"Look-ahead distance in metres");
		lblLookAheadDist.setHorizontalAlignment(SwingConstants.RIGHT);
		lblLookAheadDist.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_lookAheadDist = new JTextField();
		textField_lookAheadDist.setToolTipText("Non-negative integer");
		textField_lookAheadDist.setFont(new Font("Tahoma", Font.PLAIN, 13));
		textField_lookAheadDist.setText("50");
		textField_lookAheadDist.setColumns(10);

		final JLabel lblTrafficLights = new JLabel("Traffic light timing");
		lblTrafficLights.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTrafficLights.setFont(new Font("Tahoma", Font.PLAIN, 13));

		comboBoxTrafficLight = new JComboBox(new Object[] {});
		comboBoxTrafficLight.setFont(new Font("Tahoma", Font.PLAIN, 13));
		comboBoxTrafficLight.setModel(new DefaultComboBoxModel(
				new String[] { TrafficLightTiming.DYNAMIC.name(),
						TrafficLightTiming.FIXED.name(),
						TrafficLightTiming.NONE.name() }));
		comboBoxTrafficLight.setSelectedIndex(1);

		final JLabel lblRouting = new JLabel("Routing algorithm for new routes");
		lblRouting.setHorizontalAlignment(SwingConstants.RIGHT);
		lblRouting.setFont(new Font("Tahoma", Font.PLAIN, 13));

		comboBoxRouting = new JComboBox(new Object[] {});
		comboBoxRouting.setModel(new DefaultComboBoxModel(new String[] {
				"DIJKSTRA", "RANDOM_A_STAR", "SIMPLE" }));
		comboBoxRouting.setSelectedIndex(0);
		comboBoxRouting.setFont(new Font("Tahoma", Font.PLAIN, 13));
		final GridBagConstraints gbc_chckbxIncludePublicVehicles = new GridBagConstraints();
		gbc_chckbxIncludePublicVehicles.fill = GridBagConstraints.BOTH;
		gbc_chckbxIncludePublicVehicles.insets = new Insets(0, 10, 5, 5);
		gbc_chckbxIncludePublicVehicles.gridwidth = 2;
		gbc_chckbxIncludePublicVehicles.gridx = 1;
		gbc_chckbxIncludePublicVehicles.gridy = 19;

		chckbxServerbased = new JCheckBox("Server-based synchronization");
		chckbxServerbased.setSelected(true);
		chckbxServerbased.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxServerbased.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				Settings.isServerBased = chckbxServerbased.isSelected();

			}
		});

		final GridBagConstraints gbc_chckbxTramGiveway = new GridBagConstraints();
		gbc_chckbxTramGiveway.gridwidth = 2;
		gbc_chckbxTramGiveway.fill = GridBagConstraints.BOTH;
		gbc_chckbxTramGiveway.insets = new Insets(0, 10, 5, 5);
		gbc_chckbxTramGiveway.gridx = 1;
		gbc_chckbxTramGiveway.gridy = 14;

		final JCheckBox chckbxOutputLog = new JCheckBox(
				"Output simulation log (average travel speed, etc.)");
		chckbxOutputLog.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxOutputLog.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Settings.isOutputSimulationLog = chckbxOutputLog.isSelected();
			}
		});

		btnSetupWorkers = new JButton("Run Simulation");
		btnSetupWorkers.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnSetupWorkers.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				// Set up
				if (verifyParameterInput()) {
					setupNewSim();
				}
			}
		});

		chckbxExternalReroute = new JCheckBox(
				"Route-change/vehicle removal in heavy congestions");
		chckbxExternalReroute
				.setToolTipText("A vehicle can change route in heavy congestion. The vehicle can end its trip early if it had changed the route for a number of times.");
		chckbxExternalReroute.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				Settings.isAllowReroute = chckbxExternalReroute.isSelected();
			}
		});
		chckbxExternalReroute.setFont(new Font("Tahoma", Font.PLAIN, 13));

		rdbtnLeftDrive = new JRadioButton("Drive on left");
		rdbtnLeftDrive.setSelected(true);
		rdbtnLeftDrive.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (rdbtnLeftDrive.isSelected()) {
					if (!Settings.isDriveOnLeft) {
						Settings.isDriveOnLeft = true;
						gui.changeMap();
					}
					rdbtnRightDrive.setSelected(false);
				} else {
					rdbtnLeftDrive.setSelected(true);
				}
			}
		});

		rdbtnRightDrive = new JRadioButton("Drive on right");
		rdbtnRightDrive.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (rdbtnRightDrive.isSelected()) {
					if (Settings.isDriveOnLeft) {
						Settings.isDriveOnLeft = false;
						gui.changeMap();
					}
					rdbtnLeftDrive.setSelected(false);
				} else {
					rdbtnRightDrive.setSelected(true);
				}
			}
		});

		comboBoxOutputTrajectory = new JComboBox();
		comboBoxOutputTrajectory.setModel(new DefaultComboBoxModel(
				new String[] { "NONE", "FOREGROUND", "BACKGROUND", "ALL" }));

		JLabel lblOutputTrajectories = new JLabel("Output trajectories");

		JLabel lblOutputRoutes = new JLabel("Output initial routes");

		comboBoxOutputRoute = new JComboBox();
		comboBoxOutputRoute.setModel(new DefaultComboBoxModel(new String[] {
				"NONE", "FOREGROUND", "BACKGROUND", "ALL" }));

		lblOutputTravelTimes = new JLabel("Output travel times");

		comboBoxOutputTravelTime = new JComboBox();
		comboBoxOutputTravelTime.setModel(new DefaultComboBoxModel(
				new String[] { "NONE", "FOREGROUND", "BACKGROUND", "ALL" }));

		chckbxAllowTurnFromAnyLane = new JCheckBox("Allow turn from any lane");
		chckbxAllowTurnFromAnyLane.setSelected(true);
		chckbxAllowTurnFromAnyLane
				.setToolTipText("Check this if vehicles can make turn in any lane in a multi-lane road. Uncheck if vehicles can only make turn from certain lanes, e.g., turn left from the left-most lane.");
		chckbxAllowTurnFromAnyLane.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				Settings.isUseAnyLaneToTurn = chckbxAllowTurnFromAnyLane.isSelected();
			}
		});
		
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout
				.setHorizontalGroup(groupLayout
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								groupLayout
										.createSequentialGroup()
										.addGap(20)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.LEADING)
														.addGroup(
																groupLayout
																		.createSequentialGroup()
																		.addComponent(
																				chckbxAllowTurnFromAnyLane)
																		.addContainerGap())
														.addGroup(
																groupLayout
																		.createParallelGroup(
																				Alignment.LEADING)
																		.addGroup(
																				groupLayout
																						.createSequentialGroup()
																						.addComponent(
																								btnSetupWorkers,
																								GroupLayout.PREFERRED_SIZE,
																								144,
																								GroupLayout.PREFERRED_SIZE)
																						.addContainerGap())
																		.addGroup(
																				groupLayout
																						.createParallelGroup(
																								Alignment.LEADING)
																						.addGroup(
																								groupLayout
																										.createSequentialGroup()
																										.addGroup(
																												groupLayout
																														.createParallelGroup(
																																Alignment.LEADING)
																														.addComponent(
																																chckbxOutputLog)
																														.addComponent(
																																chckbxExternalReroute))
																										.addContainerGap())
																						.addGroup(
																								groupLayout
																										.createParallelGroup(
																												Alignment.LEADING)
																										.addGroup(
																												groupLayout
																														.createSequentialGroup()
																														.addComponent(
																																chckbxServerbased)
																														.addContainerGap())
																										.addGroup(
																												groupLayout
																														.createParallelGroup(
																																Alignment.LEADING)
																														.addGroup(
																																groupLayout
																																		.createSequentialGroup()
																																		.addComponent(
																																				lblOutputTrajectories)
																																		.addPreferredGap(
																																				ComponentPlacement.RELATED)
																																		.addComponent(
																																				comboBoxOutputTrajectory,
																																				GroupLayout.PREFERRED_SIZE,
																																				GroupLayout.DEFAULT_SIZE,
																																				GroupLayout.PREFERRED_SIZE)
																																		.addContainerGap())
																														.addGroup(
																																groupLayout
																																		.createSequentialGroup()
																																		.addGroup(
																																				groupLayout
																																						.createParallelGroup(
																																								Alignment.LEADING)
																																						.addComponent(
																																								lblOutputRoutes,
																																								GroupLayout.DEFAULT_SIZE,
																																								126,
																																								Short.MAX_VALUE)
																																						.addComponent(
																																								lblOutputTravelTimes,
																																								GroupLayout.PREFERRED_SIZE,
																																								126,
																																								GroupLayout.PREFERRED_SIZE))
																																		.addPreferredGap(
																																				ComponentPlacement.RELATED)
																																		.addGroup(
																																				groupLayout
																																						.createParallelGroup(
																																								Alignment.LEADING,
																																								false)
																																						.addComponent(
																																								comboBoxOutputTravelTime,
																																								0,
																																								GroupLayout.DEFAULT_SIZE,
																																								Short.MAX_VALUE)
																																						.addComponent(
																																								comboBoxOutputRoute,
																																								0,
																																								119,
																																								Short.MAX_VALUE))
																																		.addGap(199))
																														.addGroup(
																																groupLayout
																																		.createSequentialGroup()
																																		.addGroup(
																																				groupLayout
																																						.createParallelGroup(
																																								Alignment.LEADING)
																																						.addGroup(
																																								groupLayout
																																										.createParallelGroup(
																																												Alignment.LEADING)
																																										.addGroup(
																																												groupLayout
																																														.createSequentialGroup()
																																														.addComponent(
																																																lblRouting)
																																														.addPreferredGap(
																																																ComponentPlacement.RELATED)
																																														.addComponent(
																																																comboBoxRouting,
																																																GroupLayout.PREFERRED_SIZE,
																																																142,
																																																GroupLayout.PREFERRED_SIZE))
																																										.addGroup(
																																												groupLayout
																																														.createParallelGroup(
																																																Alignment.LEADING)
																																														.addGroup(
																																																groupLayout
																																																		.createParallelGroup(
																																																				Alignment.LEADING)
																																																		.addGroup(
																																																				groupLayout
																																																						.createParallelGroup(
																																																								Alignment.LEADING)
																																																						.addGroup(
																																																								groupLayout
																																																										.createSequentialGroup()
																																																										.addComponent(
																																																												lblNumberOfSteps_1)
																																																										.addPreferredGap(
																																																												ComponentPlacement.RELATED)
																																																										.addComponent(
																																																												textField_NumStepsPerSec,
																																																												GroupLayout.PREFERRED_SIZE,
																																																												46,
																																																												GroupLayout.PREFERRED_SIZE))
																																																						.addGroup(
																																																								groupLayout
																																																										.createSequentialGroup()
																																																										.addComponent(
																																																												rdbtnLeftDrive,
																																																												GroupLayout.DEFAULT_SIZE,
																																																												144,
																																																												Short.MAX_VALUE)
																																																										.addPreferredGap(
																																																												ComponentPlacement.RELATED)
																																																										.addComponent(
																																																												rdbtnRightDrive,
																																																												GroupLayout.PREFERRED_SIZE,
																																																												148,
																																																												GroupLayout.PREFERRED_SIZE)
																																																										.addGap(90))
																																																						.addGroup(
																																																								groupLayout
																																																										.createSequentialGroup()
																																																										.addComponent(
																																																												lblnumRandomPrivateVehicles)
																																																										.addPreferredGap(
																																																												ComponentPlacement.RELATED)
																																																										.addComponent(
																																																												textField_numRandomBackgroundPrivateVehicles,
																																																												GroupLayout.PREFERRED_SIZE,
																																																												51,
																																																												GroupLayout.PREFERRED_SIZE))
																																																						.addGroup(
																																																								groupLayout
																																																										.createSequentialGroup()
																																																										.addGroup(
																																																												groupLayout
																																																														.createParallelGroup(
																																																																Alignment.TRAILING)
																																																														.addComponent(
																																																																lblBackgroundRouteFile)
																																																														.addComponent(
																																																																lblForegroundRouteFile))
																																																										.addGap(18)
																																																										.addGroup(
																																																												groupLayout
																																																														.createParallelGroup(
																																																																Alignment.LEADING,
																																																																false)
																																																														.addComponent(
																																																																textField_ForegroundRouteFile)
																																																														.addComponent(
																																																																textField_BackgroundRouteFile,
																																																																GroupLayout.DEFAULT_SIZE,
																																																																134,
																																																																Short.MAX_VALUE))
																																																										.addPreferredGap(
																																																												ComponentPlacement.RELATED)
																																																										.addGroup(
																																																												groupLayout
																																																														.createParallelGroup(
																																																																Alignment.LEADING)
																																																														.addComponent(
																																																																btnLoadBackgroundVehicles)
																																																														.addComponent(
																																																																btnLoadForegroundVehicles))))
																																																		.addGroup(
																																																				groupLayout
																																																						.createParallelGroup(
																																																								Alignment.LEADING,
																																																								false)
																																																						.addGroup(
																																																								groupLayout
																																																										.createSequentialGroup()
																																																										.addComponent(
																																																												lblTrafficLights)
																																																										.addPreferredGap(
																																																												ComponentPlacement.RELATED)
																																																										.addComponent(
																																																												comboBoxTrafficLight,
																																																												0,
																																																												GroupLayout.DEFAULT_SIZE,
																																																												Short.MAX_VALUE))
																																																						.addGroup(
																																																								groupLayout
																																																										.createSequentialGroup()
																																																										.addComponent(
																																																												lblLookAheadDist)
																																																										.addPreferredGap(
																																																												ComponentPlacement.RELATED)
																																																										.addComponent(
																																																												textField_lookAheadDist,
																																																												GroupLayout.PREFERRED_SIZE,
																																																												51,
																																																												GroupLayout.PREFERRED_SIZE))))
																																														.addGroup(
																																																groupLayout
																																																		.createParallelGroup(
																																																				Alignment.LEADING,
																																																				false)
																																																		.addGroup(
																																																				groupLayout
																																																						.createSequentialGroup()
																																																						.addComponent(
																																																								lblnumRandomBuses)
																																																						.addPreferredGap(
																																																								ComponentPlacement.RELATED)
																																																						.addComponent(
																																																								textField_numRandomBackgroundBuses))
																																																		.addGroup(
																																																				groupLayout
																																																						.createSequentialGroup()
																																																						.addComponent(
																																																								lblnumRandomTrams)
																																																						.addPreferredGap(
																																																								ComponentPlacement.RELATED)
																																																						.addComponent(
																																																								textField_numRandomBackgroundTrams,
																																																								GroupLayout.PREFERRED_SIZE,
																																																								35,
																																																								GroupLayout.PREFERRED_SIZE)))))
																																						.addGroup(
																																								groupLayout
																																										.createSequentialGroup()
																																										.addComponent(
																																												lblNumberOfSteps)
																																										.addPreferredGap(
																																												ComponentPlacement.RELATED)
																																										.addComponent(
																																												textField_TotalNumSteps,
																																												GroupLayout.PREFERRED_SIZE,
																																												76,
																																												GroupLayout.PREFERRED_SIZE)))
																																		.addGap(67)))))))));
		groupLayout
				.setVerticalGroup(groupLayout
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								groupLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																rdbtnLeftDrive)
														.addComponent(
																rdbtnRightDrive))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																textField_ForegroundRouteFile,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																btnLoadForegroundVehicles)
														.addComponent(
																lblForegroundRouteFile))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblBackgroundRouteFile)
														.addComponent(
																textField_BackgroundRouteFile,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																btnLoadBackgroundVehicles))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblnumRandomPrivateVehicles,
																GroupLayout.PREFERRED_SIZE,
																22,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																textField_numRandomBackgroundPrivateVehicles,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblnumRandomTrams)
														.addComponent(
																textField_numRandomBackgroundTrams,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblnumRandomBuses)
														.addComponent(
																textField_numRandomBackgroundBuses,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblNumberOfSteps,
																GroupLayout.PREFERRED_SIZE,
																22,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																textField_TotalNumSteps,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblNumberOfSteps_1,
																GroupLayout.PREFERRED_SIZE,
																22,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																textField_NumStepsPerSec,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblLookAheadDist,
																GroupLayout.PREFERRED_SIZE,
																22,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																textField_lookAheadDist,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																comboBoxTrafficLight,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE)
														.addComponent(
																lblTrafficLights,
																GroupLayout.PREFERRED_SIZE,
																22,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblRouting)
														.addComponent(
																comboBoxRouting,
																GroupLayout.PREFERRED_SIZE,
																22,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.UNRELATED)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.LEADING)
														.addComponent(
																lblOutputTrajectories)
														.addComponent(
																comboBoxOutputTrajectory,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addGap(10)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblOutputRoutes)
														.addComponent(
																comboBoxOutputRoute,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addGap(16)
										.addGroup(
												groupLayout
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(
																lblOutputTravelTimes)
														.addComponent(
																comboBoxOutputTravelTime,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addGap(12)
										.addComponent(chckbxServerbased)
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addComponent(chckbxOutputLog)
										.addPreferredGap(
												ComponentPlacement.UNRELATED)
										.addComponent(chckbxExternalReroute)
										.addPreferredGap(
												ComponentPlacement.UNRELATED)
										.addComponent(
												chckbxAllowTurnFromAnyLane)
										.addGap(21)
										.addComponent(btnSetupWorkers)
										.addContainerGap(107, Short.MAX_VALUE)));
		setLayout(groupLayout);

	}

	void refreshFileLabels() {
		textField_ForegroundRouteFile
				.setText(Settings.inputForegroundVehicleFile);
		textField_BackgroundRouteFile
				.setText(Settings.inputBackgroundVehicleFile);
	}

	boolean verifyParameterInput() {
		boolean isParametersValid = true;
		GuiUtil.NonNegativeIntegerVerifier nonNegativeIntegerVerifier = new GuiUtil.NonNegativeIntegerVerifier();
		if (!nonNegativeIntegerVerifier
				.verify(textField_numRandomBackgroundPrivateVehicles)) {
			textField_numRandomBackgroundPrivateVehicles
					.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numRandomBackgroundPrivateVehicles
					.setBackground(Color.WHITE);
		}
		if (!nonNegativeIntegerVerifier
				.verify(textField_numRandomBackgroundTrams)) {
			textField_numRandomBackgroundTrams.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numRandomBackgroundTrams.setBackground(Color.WHITE);
		}
		if (!nonNegativeIntegerVerifier
				.verify(textField_numRandomBackgroundBuses)) {
			textField_numRandomBackgroundBuses.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numRandomBackgroundBuses.setBackground(Color.WHITE);
		}

		GuiUtil.PositiveIntegerVerifier positiveIntegerVerifier = new GuiUtil.PositiveIntegerVerifier();
		if (!positiveIntegerVerifier.verify(textField_TotalNumSteps)) {
			textField_TotalNumSteps.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_TotalNumSteps.setBackground(Color.WHITE);
		}
		if (!positiveIntegerVerifier.verify(textField_lookAheadDist)) {
			textField_lookAheadDist.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_lookAheadDist.setBackground(Color.WHITE);
		}

		GuiUtil.NumStepsPerSecond positiveDoubleVerifier = new GuiUtil.NumStepsPerSecond();
		if (!positiveDoubleVerifier.verify(textField_NumStepsPerSec)) {
			textField_NumStepsPerSec.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_NumStepsPerSec.setBackground(Color.WHITE);
		}

		return isParametersValid;
	}

	public void setMonitorPanel(final MonitorPanel monitor) {
		this.monitor = monitor;
	}

	void setupNewSim() {
		// Disable setup panel
		GuiUtil.setEnabledStatusOfComponents(this, false);

		Settings.numGlobalRandomBackgroundPrivateVehicles = Integer
				.parseInt(textField_numRandomBackgroundPrivateVehicles
						.getText());
		Settings.numGlobalBackgroundRandomTrams = Integer
				.parseInt(textField_numRandomBackgroundTrams.getText());
		Settings.numGlobalBackgroundRandomBuses = Integer
				.parseInt(textField_numRandomBackgroundBuses.getText());
		Settings.maxNumSteps = Integer.parseInt(textField_TotalNumSteps
				.getText());
		Settings.numStepsPerSecond = Double
				.parseDouble(textField_NumStepsPerSec.getText());
		Settings.lookAheadDistance = Double.parseDouble(textField_lookAheadDist
				.getText());
		Settings.trafficLightTiming = LightUtil
				.getLightTypeFromString((String) comboBoxTrafficLight
						.getSelectedItem());
		Settings.routingAlgorithm = RouteUtil
				.getRoutingAlgorithmFromString((String) comboBoxRouting
						.getSelectedItem());
		Settings.outputRouteScope = FileOutput
				.getScopeFromString((String) comboBoxOutputRoute
						.getSelectedItem());
		Settings.outputTrajectoryScope = FileOutput
				.getScopeFromString((String) comboBoxOutputTrajectory
						.getSelectedItem());
		Settings.outputTravelTimeScope = FileOutput
				.getScopeFromString((String) comboBoxOutputTravelTime
						.getSelectedItem());

		gui.server.setupNewSim();
		monitor.startSetupProgress();
	}
}

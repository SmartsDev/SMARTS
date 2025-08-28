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
	private final JTextField textField_numRandomPrivateVehicles;
	private final JTextField textField_numRandomTrams;
	private final JTextField textField_numRandomBuses;
	private final JTextField textField_TotalNumSteps;
	private final JTextField textField_NumStepsPerSec;
	private final JTextField textField_lookAheadDist;
	private final JFileChooser fc = new JFileChooser();
	private final JComboBox comboBoxTrafficLight;
	private final JButton btnLoadForegroundVehicles;
	private final JButton btnLoadBackgroundVehicles;
	private final JComboBox comboBoxRouting;
	private final JCheckBox chckbxDumpInitialRoutes;
	private final JCheckBox chckbxOutputTrajectory;

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

	private JLabel lblRunMultipleSimulations;
    private JTextField txt_runMultipleSimulations;
    private JButton btn_changeMultipleSimulationFile;
    private JButton btn_runMultipleSimulation;
    private Settings settings;

	public ControlPanel_MiscConfig(Settings settings, final GUI gui) {
		this.settings = settings;
		this.gui = gui;
		setPreferredSize(new Dimension(428, 641));

		// Set default directory of file chooser
		final File workingDirectory = new File(System.getProperty("user.dir"));
		fc.setCurrentDirectory(workingDirectory);

		lblRunMultipleSimulations = new JLabel("Multiple Simulation Script File");
		txt_runMultipleSimulations = new JTextField();
		txt_runMultipleSimulations.setEditable(false);
		txt_runMultipleSimulations.setColumns(10);
		btn_changeMultipleSimulationFile = new JButton("Change");
		btn_changeMultipleSimulationFile.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btn_changeMultipleSimulationFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					settings.inputSimulationScript = file.getPath();
				}
				if (returnVal == JFileChooser.CANCEL_OPTION) {
					settings.inputSimulationScript = "";
				}
				txt_runMultipleSimulations.setText(settings.inputSimulationScript);
			}
		});
		btn_runMultipleSimulation = new JButton("Run Multiple Simulation");
		btn_runMultipleSimulation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gui.processor.setupMultipleSim();
			}
		});

		btnLoadForegroundVehicles = new JButton("Change");
		btnLoadForegroundVehicles.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btnLoadForegroundVehicles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					settings.inputForegroundVehicleFile = file.getPath();
				}
				if (returnVal == JFileChooser.CANCEL_OPTION) {
					settings.inputForegroundVehicleFile = "";
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
					settings.inputBackgroundVehicleFile = file.getPath();
				}
				if (returnVal == JFileChooser.CANCEL_OPTION) {
					settings.inputBackgroundVehicleFile = "";
				}
				refreshFileLabels();
			}
		});

		lblBackgroundRouteFile = new JLabel("Background route file");

		textField_BackgroundRouteFile = new JTextField();
		textField_BackgroundRouteFile.setEditable(false);
		textField_BackgroundRouteFile.setColumns(10);

		final JLabel lblnumRandomPrivateVehicles = new JLabel("Number of random private vehicles");
		lblnumRandomPrivateVehicles.setHorizontalAlignment(SwingConstants.RIGHT);
		lblnumRandomPrivateVehicles.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblnumRandomPrivateVehicles.setToolTipText("");

		textField_numRandomPrivateVehicles = new JTextField();
		textField_numRandomPrivateVehicles.setFont(new Font("Tahoma", Font.PLAIN, 13));
		textField_numRandomPrivateVehicles.setToolTipText("Non-negative integer");
		textField_numRandomPrivateVehicles.setText("100");
		textField_numRandomPrivateVehicles.setInputVerifier(new GuiUtil.NonNegativeIntegerVerifier());

		lblnumRandomTrams = new JLabel("Number of random trams (if applicable)");
		lblnumRandomTrams.setToolTipText("");
		lblnumRandomTrams.setHorizontalAlignment(SwingConstants.RIGHT);
		lblnumRandomTrams.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_numRandomTrams = new JTextField();
		textField_numRandomTrams.setToolTipText("Non-negative integer");
		textField_numRandomTrams.setText("0");
		textField_numRandomTrams.setFont(new Font("Tahoma", Font.PLAIN, 13));

		lblnumRandomBuses = new JLabel("Number of random buses (if applicable)");
		lblnumRandomBuses.setToolTipText("");
		lblnumRandomBuses.setHorizontalAlignment(SwingConstants.RIGHT);
		lblnumRandomBuses.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_numRandomBuses = new JTextField();
		textField_numRandomBuses.setToolTipText("Non-negative integer");
		textField_numRandomBuses.setText("0");
		textField_numRandomBuses.setFont(new Font("Tahoma", Font.PLAIN, 13));

		final JLabel lblNumberOfSteps = new JLabel("Max number of steps");
		lblNumberOfSteps.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNumberOfSteps.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_TotalNumSteps = new JTextField();
		textField_TotalNumSteps.setToolTipText("Non-negative integer");
		textField_TotalNumSteps.setFont(new Font("Tahoma", Font.PLAIN, 13));
		textField_TotalNumSteps.setText("18000");

		final JLabel lblNumberOfSteps_1 = new JLabel("Number of steps per second");
		lblNumberOfSteps_1.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNumberOfSteps_1.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_NumStepsPerSec = new JTextField();
		textField_NumStepsPerSec.setToolTipText("Real number between 0.1 and 1000");
		textField_NumStepsPerSec.setFont(new Font("Tahoma", Font.PLAIN, 13));
		textField_NumStepsPerSec.setText("5");

		final JLabel lblLookAheadDist = new JLabel("Look-ahead distance in metres");
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
		comboBoxTrafficLight.setModel(new DefaultComboBoxModel(new String[] { TrafficLightTiming.DYNAMIC.name(),
				TrafficLightTiming.FIXED.name(), TrafficLightTiming.NONE.name() }));
		comboBoxTrafficLight.setSelectedIndex(1);

		final JLabel lblRouting = new JLabel("Routing algorithm for new routes");
		lblRouting.setHorizontalAlignment(SwingConstants.RIGHT);
		lblRouting.setFont(new Font("Tahoma", Font.PLAIN, 13));

		comboBoxRouting = new JComboBox(new Object[] {});
		comboBoxRouting.setModel(new DefaultComboBoxModel(new String[] { "DIJKSTRA", "RANDOM_A_STAR", "DIJKSTRA_LPF" }));
		comboBoxRouting.setSelectedIndex(0);
		comboBoxRouting.setFont(new Font("Tahoma", Font.PLAIN, 13));
		final GridBagConstraints gbc_chckbxIncludePublicVehicles = new GridBagConstraints();
		gbc_chckbxIncludePublicVehicles.fill = GridBagConstraints.BOTH;
		gbc_chckbxIncludePublicVehicles.insets = new Insets(0, 10, 5, 5);
		gbc_chckbxIncludePublicVehicles.gridwidth = 2;
		gbc_chckbxIncludePublicVehicles.gridx = 1;
		gbc_chckbxIncludePublicVehicles.gridy = 19;

		chckbxOutputTrajectory = new JCheckBox("Output vehicle trajectories");
		chckbxOutputTrajectory.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
				settings.isOutputTrajectory = chckbxOutputTrajectory.isSelected();
			}
		});
		chckbxOutputTrajectory.setFont(new Font("Tahoma", Font.PLAIN, 13));

		chckbxDumpInitialRoutes = new JCheckBox("Output initial routes of vehicles");
		chckbxDumpInitialRoutes.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
				settings.isOutputInitialRoutes = chckbxDumpInitialRoutes.isSelected();
			}
		});

		chckbxServerbased = new JCheckBox("Server-based synchronization");
		chckbxServerbased.setSelected(true);
		chckbxServerbased.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxServerbased.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				settings.isServerBased = chckbxServerbased.isSelected();

			}
		});

		final GridBagConstraints gbc_chckbxTramGiveway = new GridBagConstraints();
		gbc_chckbxTramGiveway.gridwidth = 2;
		gbc_chckbxTramGiveway.fill = GridBagConstraints.BOTH;
		gbc_chckbxTramGiveway.insets = new Insets(0, 10, 5, 5);
		gbc_chckbxTramGiveway.gridx = 1;
		gbc_chckbxTramGiveway.gridy = 14;

		chckbxDumpInitialRoutes.setFont(new Font("Tahoma", Font.PLAIN, 13));

		final JCheckBox chckbxOutputLog = new JCheckBox("Output simulation log");
		chckbxOutputLog.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxOutputLog.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				settings.isOutputSimulationLog = chckbxOutputLog.isSelected();
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

		chckbxExternalReroute = new JCheckBox("Allow vehicles change routes");
		chckbxExternalReroute.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				settings.isAllowReroute = chckbxExternalReroute.isSelected();
			}
		});
		chckbxExternalReroute.setFont(new Font("Tahoma", Font.PLAIN, 13));

		rdbtnLeftDrive = new JRadioButton("Drive on left");
		rdbtnLeftDrive.setSelected(true);
		rdbtnLeftDrive.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (rdbtnLeftDrive.isSelected()) {
					if (!settings.isDriveOnLeft) {
						settings.isDriveOnLeft = true;
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
					if (settings.isDriveOnLeft) {
						settings.isDriveOnLeft = false;
						gui.changeMap();
					}
					rdbtnLeftDrive.setSelected(false);
				} else {
					rdbtnRightDrive.setSelected(true);
				}
			}
		});

		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
				.createSequentialGroup().addGap(20)
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup().addComponent(chckbxExternalReroute)
								.addPreferredGap(ComponentPlacement.RELATED, 154,
										GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
								.createSequentialGroup()
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
										.addGroup(groupLayout.createSequentialGroup().addComponent(lblnumRandomBuses)
												.addPreferredGap(ComponentPlacement.RELATED)
												.addComponent(textField_numRandomBuses))
										.addGroup(groupLayout.createSequentialGroup().addComponent(lblnumRandomTrams)
												.addPreferredGap(ComponentPlacement.RELATED).addComponent(
														textField_numRandomTrams, GroupLayout.PREFERRED_SIZE, 35,
														GroupLayout.PREFERRED_SIZE)))
								.addPreferredGap(ComponentPlacement.RELATED, 74, GroupLayout.PREFERRED_SIZE))
								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
										.addGroup(groupLayout.createSequentialGroup()
												.addComponent(chckbxOutputTrajectory).addPreferredGap(
														ComponentPlacement.RELATED, 84, GroupLayout.PREFERRED_SIZE))
										.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
												.addGroup(groupLayout
														.createSequentialGroup().addComponent(chckbxOutputLog)
														.addPreferredGap(ComponentPlacement.RELATED, 192,
																GroupLayout.PREFERRED_SIZE))
												.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)

														.addGroup(groupLayout.createSequentialGroup()
																.addComponent(chckbxDumpInitialRoutes)
																.addPreferredGap(ComponentPlacement.RELATED, 94,
																		GroupLayout.PREFERRED_SIZE))
														.addComponent(chckbxServerbased)
														.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
																.addGroup(groupLayout.createSequentialGroup()
																		.addComponent(lblRouting)
																		.addPreferredGap(ComponentPlacement.RELATED)
																		.addComponent(comboBoxRouting,
																				GroupLayout.PREFERRED_SIZE, 142,
																				GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(ComponentPlacement.RELATED, 5,
																				GroupLayout.PREFERRED_SIZE))
																.addGroup(groupLayout
																		.createParallelGroup(Alignment.TRAILING)
																		.addGroup(groupLayout.createSequentialGroup()
																				.addGroup(groupLayout
																						.createParallelGroup(
																								Alignment.LEADING)
																						.addGroup(groupLayout
																								.createSequentialGroup()
																								.addComponent(
																										lblnumRandomPrivateVehicles)
																								.addPreferredGap(
																										ComponentPlacement.RELATED)
																								.addComponent(
																										textField_numRandomPrivateVehicles,
																										54, 54, 54))
																						.addGroup(groupLayout
																								.createParallelGroup(
																										Alignment.LEADING,
																										false)
																								.addGroup(groupLayout
																										.createSequentialGroup()
																										.addComponent(
																												lblNumberOfSteps_1)
																										.addPreferredGap(
																												ComponentPlacement.RELATED)
																										.addComponent(
																												textField_NumStepsPerSec))
																								.addGroup(groupLayout
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
																						.addGroup(groupLayout
																								.createSequentialGroup()
																								.addGroup(groupLayout
																										.createParallelGroup(
																												Alignment.LEADING)
																										.addComponent(lblRunMultipleSimulations)
																										.addComponent(
																												lblBackgroundRouteFile)
																										.addComponent(
																												lblForegroundRouteFile))
																								.addPreferredGap(
																										ComponentPlacement.RELATED)
																								.addGroup(groupLayout
																										.createParallelGroup(
																												Alignment.LEADING)
																										.addComponent(txt_runMultipleSimulations, GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
																										.addComponent(
																												textField_ForegroundRouteFile,
																												GroupLayout.DEFAULT_SIZE,
																												133,
																												Short.MAX_VALUE)
																										.addComponent(
																												textField_BackgroundRouteFile,
																												GroupLayout.DEFAULT_SIZE,
																												133,
																												Short.MAX_VALUE)))
																						.addGroup(Alignment.TRAILING,
																								groupLayout
																										.createSequentialGroup()
																										.addComponent(
																												rdbtnLeftDrive,
																												GroupLayout.DEFAULT_SIZE,
																												147,
																												Short.MAX_VALUE)
																										.addPreferredGap(
																												ComponentPlacement.UNRELATED)
																										.addComponent(
																												rdbtnRightDrive,
																												GroupLayout.PREFERRED_SIZE,
																												148,
																												GroupLayout.PREFERRED_SIZE)
																										.addPreferredGap(
																												ComponentPlacement.RELATED)))
																				.addGap(7)
																				.addGroup(groupLayout
																						.createParallelGroup(
																								Alignment.LEADING)
																						.addComponent(
																								btnLoadForegroundVehicles)
																						.addComponent(
																								btnLoadBackgroundVehicles)
																						.addComponent(btn_changeMultipleSimulationFile)))
																		.addGroup(groupLayout.createSequentialGroup()
																				.addPreferredGap(
																						ComponentPlacement.RELATED)
																				.addComponent(btn_runMultipleSimulation, GroupLayout.PREFERRED_SIZE, 144, GroupLayout.PREFERRED_SIZE)
																				.addComponent(btnSetupWorkers,
																						GroupLayout.PREFERRED_SIZE, 144,
																						GroupLayout.PREFERRED_SIZE)))
																.addGroup(groupLayout.createSequentialGroup()
																		.addGroup(groupLayout.createParallelGroup(
																				Alignment.TRAILING, false)
																				.addGroup(groupLayout
																						.createSequentialGroup()
																						.addComponent(lblTrafficLights)
																						.addPreferredGap(
																								ComponentPlacement.RELATED)
																						.addComponent(
																								comboBoxTrafficLight, 0,
																								GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE))
																				.addGroup(Alignment.LEADING, groupLayout
																						.createSequentialGroup()
																						.addComponent(lblLookAheadDist)
																						.addPreferredGap(
																								ComponentPlacement.RELATED)
																						.addComponent(
																								textField_lookAheadDist,
																								GroupLayout.PREFERRED_SIZE,
																								51,
																								GroupLayout.PREFERRED_SIZE)))
																		.addPreferredGap(ComponentPlacement.RELATED,
																				110, GroupLayout.PREFERRED_SIZE))))))))
				.addGap(67)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(groupLayout
				.createSequentialGroup().addContainerGap()

				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblRunMultipleSimulations)
						.addComponent(txt_runMultipleSimulations, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btn_changeMultipleSimulationFile))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(btn_runMultipleSimulation)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(rdbtnLeftDrive)
						.addComponent(rdbtnRightDrive))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblForegroundRouteFile)
						.addComponent(textField_ForegroundRouteFile, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnLoadForegroundVehicles))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblBackgroundRouteFile)
						.addComponent(textField_BackgroundRouteFile, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnLoadBackgroundVehicles))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblnumRandomPrivateVehicles, GroupLayout.PREFERRED_SIZE, 22,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_numRandomPrivateVehicles, GroupLayout.PREFERRED_SIZE,
								GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblnumRandomTrams)
						.addComponent(textField_numRandomTrams, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblnumRandomBuses)
						.addComponent(textField_numRandomBuses, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNumberOfSteps, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_TotalNumSteps, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNumberOfSteps_1, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_NumStepsPerSec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblLookAheadDist, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_lookAheadDist, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(comboBoxTrafficLight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(lblTrafficLights, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE).addComponent(lblRouting)
						.addComponent(comboBoxRouting, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chckbxServerbased)
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chckbxDumpInitialRoutes)
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chckbxOutputLog)
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chckbxOutputTrajectory)
				.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(chckbxExternalReroute).addGap(9)
				.addComponent(btnSetupWorkers).addContainerGap(97, Short.MAX_VALUE)));
		setLayout(groupLayout);

	}

	void refreshFileLabels() {
		textField_ForegroundRouteFile.setText(settings.inputForegroundVehicleFile);
		textField_BackgroundRouteFile.setText(settings.inputBackgroundVehicleFile);
	}

	boolean verifyParameterInput() {
		boolean isParametersValid = true;
		GuiUtil.NonNegativeIntegerVerifier nonNegativeIntegerVerifier = new GuiUtil.NonNegativeIntegerVerifier();
		if (!nonNegativeIntegerVerifier.verify(textField_numRandomPrivateVehicles)) {
			textField_numRandomPrivateVehicles.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numRandomPrivateVehicles.setBackground(Color.WHITE);
		}
		if (!nonNegativeIntegerVerifier.verify(textField_numRandomTrams)) {
			textField_numRandomTrams.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numRandomTrams.setBackground(Color.WHITE);
		}
		if (!nonNegativeIntegerVerifier.verify(textField_numRandomBuses)) {
			textField_numRandomBuses.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numRandomBuses.setBackground(Color.WHITE);
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

		settings.numGlobalRandomPrivateVehicles = Integer.parseInt(textField_numRandomPrivateVehicles.getText());
		settings.numGlobalRandomTrams = Integer.parseInt(textField_numRandomTrams.getText());
		settings.numGlobalRandomBuses = Integer.parseInt(textField_numRandomBuses.getText());
		settings.maxNumSteps = Integer.parseInt(textField_TotalNumSteps.getText());
		settings.numStepsPerSecond = Double.parseDouble(textField_NumStepsPerSec.getText());
		settings.lookAheadDistance = Double.parseDouble(textField_lookAheadDist.getText());
		settings.trafficLightTiming = LightUtil.getLightTypeFromString((String) comboBoxTrafficLight.getSelectedItem());
		//settings.routingAlgorithm = RouteUtil.getRoutingAlgorithmFromString((String) comboBoxRouting.getSelectedItem()); TODO: Temp disable from GUI

		gui.processor.setupNewSim();
		monitor.startSetupProgress();
	}

	void updateGuiComps(){
		rdbtnLeftDrive.setSelected(settings.isDriveOnLeft);
		rdbtnRightDrive.setSelected(!settings.isDriveOnLeft);
		textField_ForegroundRouteFile.setText(settings.inputForegroundVehicleFile);
		//chckbxLoadOnlyPreviousODPairForegroundVehicles.setSelected(Settings.inputOnlyODPairsOfForegroundVehicleFile);
		textField_BackgroundRouteFile.setText(settings.inputBackgroundVehicleFile);
		textField_numRandomPrivateVehicles.setText(String.valueOf(settings.numGlobalRandomPrivateVehicles));
		textField_numRandomBuses.setText(String.valueOf(settings.numGlobalRandomBuses));
		textField_numRandomTrams.setText(String.valueOf(settings.numGlobalRandomTrams));
		textField_TotalNumSteps.setText(String.valueOf(settings.maxNumSteps));
		textField_NumStepsPerSec.setText(String.valueOf(settings.numStepsPerSecond));
		textField_lookAheadDist.setText(String.valueOf((int)settings.lookAheadDistance));
		comboBoxTrafficLight.setSelectedItem(settings.trafficLightTiming.name());
		comboBoxRouting.setSelectedItem(settings.routingAlgorithm.name());
		chckbxServerbased.setSelected(settings.isServerBased);
		chckbxDumpInitialRoutes.setSelected(settings.isOutputInitialRoutes);
		chckbxOutputTrajectory.setSelected(settings.isOutputTrajectory);
		chckbxExternalReroute.setSelected(settings.isAllowReroute);
	}

	public JButton getBtnSetupWorkers() {
		return btnSetupWorkers;
	}
}

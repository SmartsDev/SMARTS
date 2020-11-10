package processor.server.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import common.Settings;
import processor.server.Server;
import processor.server.gui.GUI.VehicleDetailType;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class ControlPanel_Realtime extends JPanel {
	private final JButton btnPause, btnStop;
	private final JSlider sldSimStepPause;
	private final Server server;
	private final JCheckBox chckbxAdditionalVehicleDetails;
	private final JComboBox comboBoxVehicleDetails;
	private MonitorPanel monitor;
	private ControlPanel controlPanel;
	private final JComboBox comboBoxTrafficDrawingMethod;
	private final JLabel lblDrawTrafficAs;
	private final ControlPanel cp;

	public ControlPanel_Realtime(final Server server, ControlPanel cp) {
		this.server = server;
		setPreferredSize(new Dimension(445, 250));

		btnPause = new JButton("PAUSE");
		btnPause.setFont(new Font("Tahoma", Font.BOLD, 13));

		btnStop = new JButton("STOP");
		btnStop.setFont(new Font("Tahoma", Font.BOLD, 13));

		final JLabel lblSlow = new JLabel("1000ms");
		lblSlow.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblSlow.setHorizontalAlignment(SwingConstants.LEFT);

		final JLabel lblFast = new JLabel("0ms");
		lblFast.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblFast.setHorizontalAlignment(SwingConstants.RIGHT);

		sldSimStepPause = new JSlider();
		sldSimStepPause.setSnapToTicks(true);
		sldSimStepPause.setMaximum(1000);
		sldSimStepPause.setPaintTicks(true);
		sldSimStepPause.setValue(0);
		sldSimStepPause.setMajorTickSpacing(100);
		sldSimStepPause.setMinorTickSpacing(100);

		chckbxAdditionalVehicleDetails = new JCheckBox("Show vehicle detail");
		chckbxAdditionalVehicleDetails.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxAdditionalVehicleDetails.setHorizontalAlignment(SwingConstants.LEFT);
		comboBoxVehicleDetails = new JComboBox();
		VehicleDetailType[] vehicleDetailTypes = GUI.VehicleDetailType.values();
		String[] typeStrings = new String[vehicleDetailTypes.length];
		for (int i = 0; i < vehicleDetailTypes.length; i++) {
			typeStrings[i] = vehicleDetailTypes[i].toString();
		}
		comboBoxVehicleDetails.setModel(new DefaultComboBoxModel(typeStrings));
		comboBoxVehicleDetails.setSelectedIndex(0);

		lblDrawTrafficAs = new JLabel("Traffic display");
		lblDrawTrafficAs.setHorizontalAlignment(SwingConstants.LEFT);
		lblDrawTrafficAs.setFont(new Font("Tahoma", Font.PLAIN, 13));

		comboBoxTrafficDrawingMethod = new JComboBox(new Object[] {});
		comboBoxTrafficDrawingMethod.setModel(new DefaultComboBoxModel(new String[] { "Vehicles", "Flow" }));
		comboBoxTrafficDrawingMethod.setSelectedIndex(0);

		JLabel lblStepPause = new JLabel("Additional delay between steps");
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(30)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblDrawTrafficAs)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(comboBoxTrafficDrawingMethod, GroupLayout.PREFERRED_SIZE, 88, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(chckbxAdditionalVehicleDetails)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(comboBoxVehicleDetails, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(btnPause, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addGap(30)
									.addComponent(btnStop, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE))
								.addComponent(lblStepPause, Alignment.LEADING))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
								.addGroup(groupLayout.createSequentialGroup()
									.addComponent(lblFast)
									.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(lblSlow))
								.addComponent(sldSimStepPause, GroupLayout.PREFERRED_SIZE, 205, GroupLayout.PREFERRED_SIZE))))
					.addGap(9))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnPause, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnStop, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblStepPause)
							.addGap(25))
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblSlow, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblFast, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
							.addGap(5)
							.addComponent(sldSimStepPause, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(13)))
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(chckbxAdditionalVehicleDetails)
						.addComponent(comboBoxVehicleDetails, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(comboBoxTrafficDrawingMethod, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblDrawTrafficAs))
					.addContainerGap(25, Short.MAX_VALUE))
		);
		setLayout(groupLayout);

		this.cp = cp;
	}

	public void addListener() {

		chckbxAdditionalVehicleDetails.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				monitor.switchVehicleDetail(chckbxAdditionalVehicleDetails.isSelected());
			}
		});

		comboBoxVehicleDetails.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitor.changeVehicleDetailType((String) comboBoxVehicleDetails.getSelectedItem());
			}
		});

		comboBoxTrafficDrawingMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitor.changeTrafficDrawingMethod((String) comboBoxTrafficDrawingMethod.getSelectedItem());
			}
		});

		btnPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				if (btnPause.getText().equals("PAUSE")) {
					pauseSim();
				} else if (btnPause.getText().equals("RESUME")) {
					resumeSim();
				}
			}
		});

		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				stopSim();
			}
		});

		sldSimStepPause.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent arg0) {
				if (!sldSimStepPause.getValueIsAdjusting()) {
					server.changeSpeed(sldSimStepPause.getValue());
				}
			}
		});
	}

	public void getReadyToSimulate() {
		btnPause.setEnabled(true);
		btnPause.setText("PAUSE");
		btnStop.setEnabled(true);
	}

	void pauseSim() {
		server.pauseSim();
		btnPause.setText("RESUME");
	}

	void resumeSim() {
		if (!Settings.isServerBased) {
			//Only start drawing at the next worker-reporting step to preventing UI freezing
			cp.gui.stepToDraw += Settings.trafficReportStepGapInServerlessMode;
			cp.gui.clearObjectData();//Clear received data
		}
		server.resumeSim();
		btnPause.setEnabled(true);
		btnPause.setText("PAUSE");
		btnStop.setEnabled(true);
	}

	public void setMonitorPanelAndControlPanel(final MonitorPanel monitor, final ControlPanel controlPanel) {
		this.monitor = monitor;
		this.controlPanel = controlPanel;
	}

	void stopSim() {
		controlPanel.isDuringSimulation = false;
		server.stopSim();
		btnPause.setEnabled(false);
		btnStop.setEnabled(false);
		cp.cpMap.btnCenterMapToSelectedPlace.setEnabled(true);
		cp.cpMap.btnLoadOpenstreetmapFile.setEnabled(true);
		cp.cpMap.lblSelectablePlaces.setEnabled(true);
		cp.cpMap.listCountryName.setEnabled(true);
	}
}

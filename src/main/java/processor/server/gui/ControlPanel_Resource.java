package processor.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import common.Settings;
import processor.Simulator;
import processor.server.Server;

import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class ControlPanel_Resource extends JPanel {
	private final JLabel lblNumberOfWorkers;
	private final JButton btnApply;
	private final JTextField textField_numWorkerRequired;
	final JLabel lblConnectedWorkers;
	private JTextField textField_numConnectedWorkers;

	private JRadioButton rdbtnSpacePart;
	private JRadioButton rdbtnGridGraphPart;

	public ControlPanel_Resource(final Server server, final ControlPanel parentPanel) {
		setPreferredSize(new Dimension(450, 97));

		btnApply = new JButton("Apply");
		btnApply.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (verifyParameterInput()) {
					GuiUtil.setEnabledStatusOfComponents(parentPanel.cpMiscConfig, false);
					server.killConnectedWorkers();
					updateNumConnectedWorkers(0);

					Settings.numWorkers = Integer.parseInt(textField_numWorkerRequired.getText());
					server.resetConnectionOfWorkers();
					if (Settings.isSharedJVM) {
						Simulator.createWorkers();
					}
				}
			}
		});

		lblNumberOfWorkers = new JLabel("Number of workers required");
		lblNumberOfWorkers.setToolTipText("");
		lblNumberOfWorkers.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNumberOfWorkers.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_numWorkerRequired = new JTextField();
		textField_numWorkerRequired.setToolTipText("");
		textField_numWorkerRequired.setText(String.valueOf(Settings.numWorkers));
		textField_numWorkerRequired.setFont(new Font("Tahoma", Font.PLAIN, 13));

		lblConnectedWorkers = new JLabel("Number of connected workers");
		lblConnectedWorkers.setToolTipText("");
		lblConnectedWorkers.setHorizontalAlignment(SwingConstants.RIGHT);
		lblConnectedWorkers.setFont(new Font("Tahoma", Font.PLAIN, 13));

		textField_numConnectedWorkers = new JTextField();
		textField_numConnectedWorkers.setEditable(false);
		textField_numConnectedWorkers.setColumns(10);
		btnApply.setFont(new Font("Tahoma", Font.PLAIN, 13));

		rdbtnSpacePart = new JRadioButton("Space-grid Partitioning ");
		rdbtnSpacePart.setSelected(true);
		rdbtnSpacePart.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (rdbtnSpacePart.isSelected()) {
					Settings.partitionType = "Space-grid";
					rdbtnGridGraphPart.setSelected(false);
				} else {
					rdbtnSpacePart.setSelected(true);
				}
			}
		});
		rdbtnGridGraphPart = new JRadioButton("GridGraph Partitioning");
		rdbtnGridGraphPart.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (rdbtnGridGraphPart.isSelected()) {
					Settings.partitionType = "GridGraph";
					rdbtnSpacePart.setSelected(false);
				} else {
					rdbtnGridGraphPart.setSelected(true);
				}
			}
		});
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(22)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblNumberOfWorkers)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textField_numWorkerRequired, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnApply, GroupLayout.PREFERRED_SIZE, 104, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblConnectedWorkers)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(textField_numConnectedWorkers, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
								.addComponent(rdbtnSpacePart, GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(rdbtnGridGraphPart, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE)
								)).addGap(90)

					.addContainerGap(81, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNumberOfWorkers, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_numWorkerRequired, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnApply))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblConnectedWorkers, GroupLayout.PREFERRED_SIZE, 22, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_numConnectedWorkers, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
							.addComponent(rdbtnSpacePart)
							.addComponent(rdbtnGridGraphPart))
						.addGap(90)
					)

		);
		setLayout(groupLayout);
	}

	public void updateNumConnectedWorkers(final int num) {
		textField_numConnectedWorkers.setText(String.valueOf(num));
	}

	boolean verifyParameterInput() {
		boolean isParametersValid = true;

		GuiUtil.PositiveIntegerVerifier positiveIntegerVerifier = new GuiUtil.PositiveIntegerVerifier();
		if (!positiveIntegerVerifier.verify(textField_numWorkerRequired)) {
			textField_numWorkerRequired.setBackground(Color.RED);
			isParametersValid = false;
		} else {
			textField_numWorkerRequired.setBackground(Color.WHITE);
		}

		return isParametersValid;
	}
}

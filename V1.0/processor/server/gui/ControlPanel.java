package processor.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import processor.server.Server;

public class ControlPanel extends JPanel {
	ControlPanel_Resource cpRes;
	ControlPanel_MiscConfig cpMiscConfig;
	ControlPanel_Realtime cpRealtime;
	ControlPanel_Map cpMap;
	boolean isDuringSimulation = false;
	GUI gui;

	public ControlPanel(final Server server, final GUI gui, final int topLeftX, final int topLeftY,
			final Dimension displayPanelDimension) {
		this.gui = gui;
		setBorder(null);
		setBackground(Color.WHITE);
		cpRes = new ControlPanel_Resource(server, this);
		cpMiscConfig = new ControlPanel_MiscConfig(gui);
		GuiUtil.setEnabledStatusOfComponents(cpMiscConfig, false);
		cpMap = new ControlPanel_Map(gui);
		cpRealtime = new ControlPanel_Realtime(server, this);
		GuiUtil.setEnabledStatusOfComponents(cpRealtime, false);
		addSubPanel(cpRes, cpRes.getPreferredSize(), displayPanelDimension, "Computing Resource", true);
		addSubPanel(cpMap, cpMap.getPreferredSize(), displayPanelDimension, "Map", true);
		addSubPanel(cpMiscConfig, cpMiscConfig.getPreferredSize(), displayPanelDimension, "Miscellaneous Settings",
				false);
		addSubPanel(cpRealtime, cpRealtime.getPreferredSize(), displayPanelDimension, "Realtime Control", false);

		setPreferredSize(getPreferredDimension(displayPanelDimension));
	}

	public void addListener() {
		cpMap.addListener();
		cpRealtime.addListener();
	}

	void addSubPanel(final JPanel subPanel, final Dimension expandSize, final Dimension displayPanelDimension,
			final String name, final boolean isCollapsed) {
		final JButton button = new JButton(name);
		button.setFont(new Font("Tahoma", Font.PLAIN, 15));
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setPreferredSize(new Dimension(displayPanelDimension.width, 20));
		button.setHorizontalAlignment(SwingConstants.LEFT);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				// TODO Auto-generated method stub
				if (subPanel.isVisible()) {
					subPanel.setVisible(false);
					subPanel.setPreferredSize(new Dimension(0, 0));
					button.setText("+ " + name);
				} else {
					subPanel.setVisible(true);
					subPanel.setPreferredSize(new Dimension(displayPanelDimension.width, expandSize.height));
					button.setText("- " + name);
				}
				// Re-calculate parent panel size
				setPreferredSize(getPreferredDimension(displayPanelDimension));
			}
		});
		add(button);
		add(subPanel);
		// Collapse sub panel
		if (isCollapsed) {
			button.setText("+ " + name);
			subPanel.setVisible(false);
			subPanel.setPreferredSize(new Dimension(0, 0));
		} else {
			button.setText("- " + name);
			subPanel.setVisible(true);
			subPanel.setPreferredSize(new Dimension(displayPanelDimension.width, expandSize.height));
		}
	}

	Dimension getPreferredDimension(final Dimension displayPanelDimension) {
		final int width = displayPanelDimension.width;
		final int height = 200 + cpRes.getPreferredSize().height + cpMiscConfig.getPreferredSize().height
				+ cpMap.getPreferredSize().height + cpRealtime.getPreferredSize().height;
		return new Dimension(width, height);
	}

	public void prepareToSetup() {
		GuiUtil.setEnabledStatusOfComponents(cpRealtime, false);
		GuiUtil.setEnabledStatusOfComponents(cpMiscConfig, true);
		GuiUtil.setEnabledStatusOfComponents(cpRes, true);
		cpMap.btnCenterMapToSelectedPlace.setEnabled(true);
		cpMap.btnLoadOpenstreetmapFile.setEnabled(true);
		cpMap.lblSelectablePlaces.setEnabled(true);
		cpMap.listCountryName.setEnabled(true);
		isDuringSimulation = false;
	}

	public void setMonitorPanel(final MonitorPanel monitor) {
		cpRealtime.setMonitorPanelAndControlPanel(monitor, this);
		cpMap.setMonitorPanel(monitor);
		cpMiscConfig.setMonitorPanel(monitor);
	}

	public void startSimulation() {
		GuiUtil.setEnabledStatusOfComponents(cpRealtime, true);
		cpRealtime.getReadyToSimulate();
		GuiUtil.setEnabledStatusOfComponents(cpMiscConfig, false);
		GuiUtil.setEnabledStatusOfComponents(cpRes, false);
		isDuringSimulation = true;
		cpMap.btnCenterMapToSelectedPlace.setEnabled(false);
		cpMap.btnLoadOpenstreetmapFile.setEnabled(false);
		cpMap.lblSelectablePlaces.setEnabled(false);
		cpMap.listCountryName.setEnabled(false);
	}

	public void updateNumConnectedWorkers(final int num) {
		cpRes.updateNumConnectedWorkers(num);
	}

}

package processor.server.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import common.Settings;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class ControlPanel_Map extends JPanel {
	class CountryCentroid {
		String name;
		double latitude, longitude;

		public CountryCentroid(final String name, final double latitude, final double longitude) {
			this.name = name;
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

	private final JCheckBox chckbxRoadNetworkGraph;
	private final JCheckBox chckbxStaticMapImage;
	private MonitorPanel monitorPanel;
	public final JButton btnLoadOpenstreetmapFile;
	private final JFileChooser fc = new JFileChooser();
	public final JList listCountryName = new JList();
	private final ArrayList<CountryCentroid> regionCentroids = new ArrayList<>();
	public final JButton btnCenterMapToSelectedPlace;
	public final JLabel lblSelectablePlaces;

	private final GUI gui;
	private Settings settings;

	public ControlPanel_Map(final GUI gui, Settings settings) {
		this.gui = gui;
		this.settings = settings;
		setPreferredSize(new Dimension(450, 466));

		// Set default directory of file chooser
		final File workingDirectory = new File(System.getProperty("user.dir"));
		fc.setCurrentDirectory(workingDirectory);

		chckbxRoadNetworkGraph = new JCheckBox("Show road network graph");
		chckbxRoadNetworkGraph.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxRoadNetworkGraph.setSelected(true);

		chckbxStaticMapImage = new JCheckBox("Show map image in background");
		chckbxStaticMapImage.setFont(new Font("Tahoma", Font.PLAIN, 13));

		btnLoadOpenstreetmapFile = new JButton("Import roads from OpenStreetMap file");
		btnLoadOpenstreetmapFile.setFont(new Font("Tahoma", Font.PLAIN, 13));
		loadRegionCentroids(settings.inputBuiltinAdministrativeRegionCentroid);
		fillCountryNameToList();

		lblSelectablePlaces = new JLabel("Road map areas");

		listCountryName.setFont(new Font("Tahoma", Font.PLAIN, 13));
		listCountryName.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(listCountryName);
		final JScrollPane listScroller = new JScrollPane(listCountryName);

		btnCenterMapToSelectedPlace = new JButton("Locate");
		btnCenterMapToSelectedPlace.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btnCenterMapToSelectedPlace.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
				if (monitorPanel.currentZoom != monitorPanel.zoomAtRelocation) {
					monitorPanel.changeZoomFromCenter(monitorPanel.zoomAtRelocation);
				}
				// Coordinates of selected place
				final String name = (String) listCountryName.getSelectedValue();
				if (name != null) {
					for (final CountryCentroid cc : regionCentroids) {
						if (cc.name.equals(name)) {
							/*
							 * Update positions
							 */
							final int gapX = (int) (0.5 * monitorPanel.displayPanelDimension.width)
									- monitorPanel.convertLonToX(cc.longitude);
							final int gapY = (int) (0.5 * monitorPanel.displayPanelDimension.height)
									- monitorPanel.convertLatToY(cc.latitude);

							monitorPanel.moveMapCenterToPoint(gapX, gapY);
							break;
						}
					}
				}

			}
		});
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(20)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(chckbxRoadNetworkGraph, GroupLayout.PREFERRED_SIZE, 283, GroupLayout.PREFERRED_SIZE)
						.addComponent(chckbxStaticMapImage, GroupLayout.PREFERRED_SIZE, 283, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createSequentialGroup()
							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
								.addComponent(listScroller, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
								.addComponent(lblSelectablePlaces))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnCenterMapToSelectedPlace, GroupLayout.PREFERRED_SIZE, 79, GroupLayout.PREFERRED_SIZE))
						.addComponent(btnLoadOpenstreetmapFile, GroupLayout.PREFERRED_SIZE, 261, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(44, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(7)
					.addComponent(chckbxRoadNetworkGraph, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
					.addGap(3)
					.addComponent(chckbxStaticMapImage, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lblSelectablePlaces, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
					.addGap(5)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(listScroller, GroupLayout.PREFERRED_SIZE, 239, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnCenterMapToSelectedPlace))
					.addGap(18)
					.addComponent(btnLoadOpenstreetmapFile, GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		setLayout(groupLayout);
	}

	public void addListener() {
		btnLoadOpenstreetmapFile.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					settings.inputOpenStreetMapFile = file.getPath();
					gui.changeMap();
				}
			}
		});

		chckbxRoadNetworkGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitorPanel.switchRoadNetworkGraph(chckbxRoadNetworkGraph.isSelected());
			}
		});

		chckbxStaticMapImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitorPanel.switchStaticMapImage(chckbxStaticMapImage.isSelected());
			}
		});

	}

	void fillCountryNameToList() {
		final DefaultListModel listModel = new DefaultListModel();
		for (final CountryCentroid cc : regionCentroids) {
			listModel.addElement(cc.name);
		}
		listCountryName.setModel(listModel);
	}

	void loadRegionCentroids(String inputBuiltinAdministrativeRegionCentroid) {
		try {
			final InputStream inputStream = getClass()
					.getResourceAsStream(inputBuiltinAdministrativeRegionCentroid);
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			String line = bufferedReader.readLine();// Skip first line
			while ((line = bufferedReader.readLine()) != null) {
				final String[] items = line.split(",");
				final CountryCentroid cc = new CountryCentroid(items[2], Double.parseDouble(items[0]),
						Double.parseDouble(items[1]));
				regionCentroids.add(cc);
			}
			bufferedReader.close();
			inputStream.close();
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setMonitorPanel(final MonitorPanel monitor) {
		monitorPanel = monitor;
	}

}

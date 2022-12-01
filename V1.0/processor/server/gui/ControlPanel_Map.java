package processor.server.gui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
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
import javax.swing.SwingConstants;

public class ControlPanel_Map extends JPanel {
	class CountryCentroid {
		String name;
		double latitude, longitude;

		public CountryCentroid(final String name, final double latitude,
				final double longitude) {
			this.name = name;
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

	private final JCheckBox chckbxRoadNetworkGraph;
	private final JCheckBox chckbxStaticMapImage;
	private final JCheckBox chckbxPlaceNames;
	private MonitorPanel monitorPanel;
	public final JButton btnLoadOpenstreetmapFile;
	private final JFileChooser fc = new JFileChooser();
	public final JList listCountryName = new JList();
	private final ArrayList<CountryCentroid> regionCentroids = new ArrayList<>();
	public final JButton btnCenterMapToSelectedPlace;
	public final JLabel lblSelectablePlaces;

	private final GUI gui;
	private JPanel panel;
	private JLabel lblPlaceNamesAre;
	private JLabel lblCities;
	private JLabel lblBy;
	private JLabel lblLicence;

	public ControlPanel_Map(final GUI gui) {
		this.gui = gui;
		setPreferredSize(new Dimension(450, 472));

		// Set default directory of file chooser
		final File workingDirectory = new File(System.getProperty("user.dir"));
		fc.setCurrentDirectory(workingDirectory);

		chckbxRoadNetworkGraph = new JCheckBox("Show road network graph");
		chckbxRoadNetworkGraph.setBounds(20, 7, 283, 23);
		chckbxRoadNetworkGraph.setFont(new Font("Tahoma", Font.PLAIN, 13));
		chckbxRoadNetworkGraph.setSelected(true);

		chckbxPlaceNames = new JCheckBox(
				"Show place names from GeoNames.org");
		chckbxPlaceNames.setBounds(20, 69, 283, 23);
		chckbxPlaceNames.setFont(new Font("Tahoma", Font.PLAIN, 13));

		btnLoadOpenstreetmapFile = new JButton(
				"Import roads from OpenStreetMap file");
		btnLoadOpenstreetmapFile.setBounds(20, 421, 261, 34);
		btnLoadOpenstreetmapFile.setFont(new Font("Tahoma", Font.PLAIN, 13));
		loadRegionCentroids();
		fillCountryNameToList();

		lblSelectablePlaces = new JLabel("Road map areas");
		lblSelectablePlaces.setBounds(20, 146, 94, 25);
		setLayout(null);
		listCountryName.setBounds(1, 1, 298, 237);

		listCountryName.setFont(new Font("Tahoma", Font.PLAIN, 13));
		listCountryName.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		add(listCountryName);
		final JScrollPane listScroller = new JScrollPane(listCountryName);
		listScroller.setBounds(20, 173, 300, 239);

		btnCenterMapToSelectedPlace = new JButton("Locate");
		btnCenterMapToSelectedPlace.setBounds(332, 171, 79, 25);
		btnCenterMapToSelectedPlace.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btnCenterMapToSelectedPlace.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent arg0) {
				if (monitorPanel.currentZoom != monitorPanel.zoomAtRelocation) {
					monitorPanel
							.changeZoomFromCenter(monitorPanel.zoomAtRelocation);
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
		add(chckbxRoadNetworkGraph);
		add(chckbxPlaceNames);
		add(listScroller);
		add(lblSelectablePlaces);
		add(btnCenterMapToSelectedPlace);
		add(btnLoadOpenstreetmapFile);


		lblPlaceNamesAre = new JLabel("Place names are from ");
		lblPlaceNamesAre.setFont(new Font("Arial", Font.ITALIC, 13));
		lblPlaceNamesAre.setBounds(45, 96, 132, 16);
		lblPlaceNamesAre.setHorizontalAlignment(SwingConstants.LEFT);
		add(lblPlaceNamesAre);
		
		lblCities = new JLabel("cities500");
		lblCities.setFont(new Font("Arial", Font.ITALIC, 13));
		lblCities.setBounds(177, 87,  55, 34);
		lblCities.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseEntered(MouseEvent e) {
		        lblCities.setText("<html><a href=''>cities500</a></html>");
		    }
			
			@Override
			public void mouseClicked(MouseEvent e) {
			    try {
			         
			        Desktop.getDesktop().browse(new URI("http://download.geonames.org/export/dump/"));
			         
			    } catch (IOException | URISyntaxException e1) {
			        e1.printStackTrace();
			    }
			}
			
			@Override
		    public void mouseExited(MouseEvent e) {
				lblCities.setText("cities500");
		    }
		});
		add(lblCities);
		
		lblBy = new JLabel("by GeoNames.org");//https://www.geonames.org/
		lblBy.setFont(new Font("Arial", Font.ITALIC, 13));
		lblBy.setBounds(234, 87, 132, 34);
		lblBy.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseEntered(MouseEvent e) {
				lblBy.setText("<html><a href=''>by GeoNames.org</a></html>");
		    }
			
			@Override
			public void mouseClicked(MouseEvent e) {
			    try {
			         
			        Desktop.getDesktop().browse(new URI("https://www.geonames.org/"));
			         
			    } catch (IOException | URISyntaxException e1) {
			        e1.printStackTrace();
			    }
			}
			
			@Override
		    public void mouseExited(MouseEvent e) {
				lblBy.setText("by GeoNames.org");
		    }
		});
		add(lblBy);
		
		lblLicence = new JLabel("licensed under a Creative Commons Attribution 4.0 License");
		lblLicence.setFont(new Font("Arial", Font.ITALIC, 13));
		lblLicence.setBounds(45, 112, 367, 34);
		lblLicence.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseEntered(MouseEvent e) {
				lblLicence.setText("<html><a href=''>licensed under a Creative Commons Attribution 4.0 License</a></html>");
		    }
			
			@Override
			public void mouseClicked(MouseEvent e) {
			    try {
			         
			        Desktop.getDesktop().browse(new URI("https://creativecommons.org/licenses/by/4.0/"));
			         
			    } catch (IOException | URISyntaxException e1) {
			        e1.printStackTrace();
			    }
			}
			
			@Override
		    public void mouseExited(MouseEvent e) {
				lblLicence.setText("licensed under a Creative Commons Attribution 4.0 License");
		    }
		});
		add(lblLicence);
		
		chckbxStaticMapImage = new JCheckBox("Show map image in background");
		chckbxStaticMapImage.setBounds(20, 35, 212, 25);
		add(chckbxStaticMapImage);
		
	}

	public void addListener() {

		btnLoadOpenstreetmapFile.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				final int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					final File file = fc.getSelectedFile();
					Settings.inputOpenStreetMapFile = file.getPath();
					gui.changeMap();
				}
			}
		});

		chckbxRoadNetworkGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitorPanel.switchRoadNetworkGraph(chckbxRoadNetworkGraph
						.isSelected());
			}
		});

		chckbxStaticMapImage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitorPanel.switchStaticMapImage(chckbxStaticMapImage.isSelected());
			}
		});
		
		
		chckbxPlaceNames.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				monitorPanel.switchPlaceNameImage(chckbxPlaceNames
						.isSelected());
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

	void loadRegionCentroids() {
		try {
			final InputStream inputStream = getClass().getResourceAsStream(
					Settings.inputBuiltinAdministrativeRegionCentroid);
			final BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(inputStream, "UTF-8"));
			String line = bufferedReader.readLine();// Skip first line
			while ((line = bufferedReader.readLine()) != null) {
				final String[] items = line.split(",");
				final CountryCentroid cc = new CountryCentroid(items[2],
						Double.parseDouble(items[0]),
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

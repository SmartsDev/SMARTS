package processor.server.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import common.Settings;
import processor.server.Server;
import traffic.road.RoadType;

public class GuiUtil {

	static class OSMDownloader extends SwingWorker<String, String> {
		MonitorPanel monitor;
		GUI gui;
		Server server;
		String nameDownloadedFile = "download.osm";

		OSMDownloader(final MonitorPanel monitor, final GUI gui, final Server server) {
			this.monitor = monitor;
			this.gui = gui;
			this.server = server;
		}

		@Override
		public String doInBackground() {
			getOsmForQueryWindow();
			return "success";
		}

		String download(final String queryString) {
			final StringBuilder sb = new StringBuilder();
			URLConnection connection;
			try {
				connection = new URL(queryString).openConnection();
				connection.setRequestProperty("Accept-Charset", "UTF-8");
				final InputStream response = connection.getInputStream();

				/*
				 * Start a timer for updating downloading status
				 */
				final Timer timerDownloadStatus = new Timer(500, new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent arg0) {
						monitor.lblDownloading.setText(sb.length() + " characters downloaded");
					}
				});
				timerDownloadStatus.start();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(response, "UTF-8"))) {
					for (String line; (line = reader.readLine()) != null;) {
						sb.append(line);
					}
				}
				timerDownloadStatus.stop();

			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return sb.toString();
		}

		/*
		 * Download OpenStreetMap XML data based on user-defined query window.
		 */
		void getOsmForQueryWindow() {
			monitor.isDownloadingOSM = true;
			// Show progress label
			monitor.lblDownloading.setVisible(true);
			// Get coordinate bounds of query window
			double coordLeft = monitor.queryWindowTopLeft.x;
			double coordRight = monitor.queryWindowBottomRight.x;
			if (coordLeft > coordRight) {
				final double temp = coordLeft;
				coordLeft = coordRight;
				coordRight = temp;
			}
			double coordTop = monitor.queryWindowTopLeft.y;
			double coordBottom = monitor.queryWindowBottomRight.y;
			if (coordTop < coordBottom) {
				final double temp = coordTop;
				coordTop = coordBottom;
				coordBottom = temp;
			}
			// Query string
			final String queryStringPrefix = "http://overpass-api.de/api/interpreter?data=";
			final String boundingBoxString = "(" + coordBottom + "," + coordLeft + "," + coordTop + "," + coordRight
					+ ")";
			// Select way and relations intersecting with query window except
			// buildings
			String queryString = queryStringPrefix + "(";
			queryString += getOverpassWithRoadType(boundingBoxString);
			queryString += "relation[\"route\"]" + boundingBoxString + ";";// Including route information such as tram routes
			queryString += ");out%20meta;";// Not including route information
			System.out.println("Download URL is " + queryString);
			// Download data
			final String data = download(queryString);
			// Write data to file
			if (data.length() == 0) {
				return;
			}
			initOutputFile();
			outputStringToFile(data);
			// Parse downloaded data
			if (JOptionPane.showConfirmDialog(null,
					"Data is saved to " + nameDownloadedFile + ". Reset all the settings and load the map?", "Confirm",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				gui.changeMap();
			}
			// Hide progress label
			monitor.lblDownloading.setVisible(false);
			monitor.isDownloadingOSM = false;
		}
		
		String getOverpassWithRoadType(String boundingBoxString) {
			String string = "";
			for (RoadType type : RoadType.values()) {
				String typeString = type.name();
				String segment = "(way[\"highway\"=\"";
				if (typeString.equals("tram")) {
					segment = "(way[\"railway\"=\"";
				}
				segment += typeString;
				segment += "\"]";
				segment += boundingBoxString;
				segment += ";>;);";
				string += segment;
			}
			return string;
		}
		
		/**
		 * Initialize output file
		 */
		void initOutputFile() {
			try {
				final File outputFile = new File(nameDownloadedFile);
				if (outputFile.exists()) {
					outputFile.delete();
				}
				outputFile.createNewFile();
				Settings.inputOpenStreetMapFile = outputFile.getPath();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * Save a string to file
		 */
		void outputStringToFile(final String str) {
			BufferedWriter out;
			try {
				out = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(Settings.inputOpenStreetMapFile), "UTF-8"));
				out.write(str);
				out.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static int getWorkingScreenHeight() {
		return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
	}

	public static int getWorkingScreenWidth() {
		return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
	}

	

	public static void setEnabledStatusOfComponents(final Container container, final boolean status) {
		final Component[] components = container.getComponents();
		for (final Component c : components) {
			c.setEnabled(status);
		}
	}

	public static class NonNegativeIntegerVerifier extends InputVerifier {
		@Override
		public boolean verify(JComponent input) {
			String text = ((JTextField) input).getText();
			try {
				int intValue = Integer.parseInt(text);
				double doubleValue = Double.parseDouble(text);
				if (intValue < 0)
					return false;
				else if (intValue == 0 && doubleValue != 0)
					return false;
				else if (intValue > 0 && doubleValue / intValue != 1.0)
					return false;
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
	}

	public static class PositiveIntegerVerifier extends InputVerifier {
		@Override
		public boolean verify(JComponent input) {
			String text = ((JTextField) input).getText();
			try {
				int intValue = Integer.parseInt(text);
				double doubleValue = Double.parseDouble(text);
				if (intValue <= 0)
					return false;
				else if (doubleValue / intValue != 1.0)
					return false;
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
	}

	public static class NumStepsPerSecond extends InputVerifier {
		@Override
		public boolean verify(JComponent input) {
			String text = ((JTextField) input).getText();
			try {
				double doubleValue = Double.parseDouble(text);
				if (doubleValue < 0.1 || doubleValue > 1000)
					return false;
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
	}

}

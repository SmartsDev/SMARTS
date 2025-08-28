package processor.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.Settings;
import common.SysUtil;
import processor.communication.message.SerializableRouteDump;
import processor.communication.message.SerializableRouteDumpPoint;
import processor.communication.message.SerializableTrajectory;
import processor.communication.message.Serializable_Finished_Vehicle;

public class FileOutput {
	FileOutputStream fosLog;
	FileOutputStream fosTrajectory;
	FileOutputStream fosRoute;
	FileOutputStream trjFos;
	FileOutputStream vdFos;
	FileOutputStream bestTTFos;
	FileOutputStream expSettings;
	private Settings settings;

	public FileOutput(Settings settings) {
		this.settings = settings;
	}

	/**
	 * Close output file
	 */
	public void close() {
		try {
			if (fosLog != null) {
				fosLog.close();
			}
			if (fosTrajectory != null) {
				fosTrajectory.close();
			}
			if (fosRoute != null) {
				outputStringToFile(fosRoute, "</data>");
				fosRoute.close();
			}
			if(trjFos != null){
				trjFos.close();
			}
			if(vdFos != null){
				vdFos.close();
			}
			if(bestTTFos != null){
				bestTTFos.close();
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void init() {
		if (settings.isOutputTrajectory) {
			initTrajectoryOutputFile();
		}
		if (settings.isOutputInitialRoutes) {
			initRouteOutputFile();
			initBestTTOutputFile();
		}
		if (settings.isOutputSimulationLog) {
			initSimLogOutputFile();
		}
		//initTrjOutputFile(); TODO: Removed by Udesh
		initVDOutputFile();
		saveSettingsToXML();
	}


	public void saveSettingsToXML(){
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
		String str = gson.toJson(settings);
		System.out.println("Settings: " + str);
		try {
			final File fileTrj = getNewFile("Settings_"+settings.getOutputPrefix());
			// Print column titles
			expSettings = new FileOutputStream(fileTrj, true);
			outputStringToFile(expSettings,
					str + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	File getNewFile(String prefix, String extension) {
		File downloadDir = new File(settings.downloadDirectory);
		if(!downloadDir.exists()){
			System.out.println("Download directory not exist. Creating a new directory");
			downloadDir.mkdirs();
		}
		String testName = settings.testName;
		if(testName == null){
			testName = prefix + SysUtil.getTimeStampString();
		}else{
			testName = prefix + testName;
		}
		String fileName = settings.downloadDirectory + "/" + testName + "." +extension;
		File file = new File(fileName);
		int counter = 0;
		while (file.exists()) {
			counter++;
			fileName = settings.downloadDirectory + "/" + testName + "_" + counter + ".txt";
			file = new File(fileName);
		}
		return file;
	}

	File getNewFile(String prefix) {
		return getNewFile(prefix ,"txt");
	}

	void initRouteOutputFile() {
		try {
			final File file = getNewFile(settings.prefixOutputRoutePlan);
			fosRoute = new FileOutputStream(file, true);
			outputStringToFile(fosRoute,
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator"));
			outputStringToFile(fosRoute, "<data>" + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void initSimLogOutputFile() {

		try {
			final File file = getNewFile(settings.prefixOutputSimLog);
			// Print column titles
			fosLog = new FileOutputStream(file, true);
			outputStringToFile(fosLog, "Time Stamp, Real Time(s), Simulation Time(s), # of Worker-Worker Connections"
					+ System.getProperty("line.separator"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	void initTrajectoryOutputFile() {
		try {
			final File file = getNewFile(settings.prefixOutputTrajectory);
			// Print column titles
			fosTrajectory = new FileOutputStream(file, true);
			outputStringToFile(fosTrajectory,
					"Trajectory ID,Vehicle ID,Time Stamp,Displacement, Latitude,Longitude" + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void initTrjOutputFile() {
		try {
			final File fileTrj = getNewFile("Trj_", "trj");
			// Print column titles
			trjFos = new FileOutputStream(fileTrj, true);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void initVDOutputFile() {
		try {
			final File fileTrj = getNewFile("VD_"+settings.getOutputPrefix());
			// Print column titles
			vdFos = new FileOutputStream(fileTrj, true);
			outputStringToFile(vdFos,
					"ID,VID,Source,Destination,BestTravelTime,ActualTravelTime,timeStamp,RouteLength,TimeOnDirectional,TimeOnDirectionalSpeed,Route" + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void initBestTTOutputFile() {
		try {
			final File fileTrj = getNewFile("BestTT_");
			// Print column titles
			bestTTFos = new FileOutputStream(fileTrj, true);
			outputStringToFile(bestTTFos,
					"ID,VID,Source,Destination,BestTravelTime,ActualTravelTime,RouteLength,Route" + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Output trajectory of individual vehicles
	 */
	public void outputTrajectories(HashMap<String, TreeMap<Double, double[]>> allTrajectories) {
		if (settings.isOutputTrajectory) {
			int trajectoryId = 0;
			for (String vehicleId : allTrajectories.keySet()) {
				// Trajectory counter	
				trajectoryId++;
				outputStringToFile(fosTrajectory, String.valueOf(trajectoryId));
				outputStringToFile(fosTrajectory, ",");
				outputStringToFile(fosTrajectory, vehicleId);
				outputStringToFile(fosTrajectory, ",");
				TreeMap<Double, double[]> points = allTrajectories.get(vehicleId);
				ArrayList<Double> timeStamps = new ArrayList<Double>(points.keySet());
				for (int i = 0; i < timeStamps.size(); i++) {
					if (i > 0) {
						outputStringToFile(fosTrajectory, ",,");
					}
					double timeStamp = timeStamps.get(i);
					outputStringToFile(fosTrajectory, String.valueOf(timeStamp));
					outputStringToFile(fosTrajectory, ",");
					double[] point = points.get(timeStamp);
					outputStringToFile(fosTrajectory, String.valueOf(point[0]));
					outputStringToFile(fosTrajectory, ",");
					outputStringToFile(fosTrajectory, String.valueOf(point[1]));
					outputStringToFile(fosTrajectory, ",");
					outputStringToFile(fosTrajectory, String.valueOf(point[2]));

					outputStringToFile(fosTrajectory, System.getProperty("line.separator"));

				}
			}
		}
	}

	public void outputRoutes(final ArrayList<SerializableRouteDump> routes) {
		if (settings.isOutputInitialRoutes) {
			try {
				final StringBuilder sb = new StringBuilder();
				for (final SerializableRouteDump route : routes) {
					sb.append("<vehicle ");
					sb.append("id=\"" + route.vehicleId + "\" vid=\"" + route.vid +"\" type=\"" + route.type + "\" start_time=\""
							+ route.startTime + "\" driverProfile=\"" + route.driverProfile + "\">"
							+ System.getProperty("line.separator"));
					for (final SerializableRouteDumpPoint point : route.routeDumpPoints) {
						sb.append("<node ");
						if (point.stopDuration == 0) {
							sb.append("id=\"" + point.nodeId + "\"/>" + System.getProperty("line.separator"));
						} else {
							sb.append("id=\"" + point.nodeId + "\" stopover=\"" + point.stopDuration + "\"/>"
									+ System.getProperty("line.separator"));
						}
					}
					sb.append("</vehicle>" + System.getProperty("line.separator"));

				}
				outputStringToFile(fosRoute, sb.toString());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void outputBestTTData(List<SerializableRouteDump> routes){
		for (SerializableRouteDump route : routes) {
			StringBuilder sb = new StringBuilder();
			sb.append(route.vehicleId+",");
			sb.append(route.vid+",");
			sb.append(route.s+",");
			sb.append(route.d+",");
			sb.append(route.spTime+",");
			sb.append(route.spTime+",");
			sb.append(route.spLength+",");
			sb.append(route.route+ System.getProperty("line.separator"));
			outputStringToFile(bestTTFos, sb.toString());
		}
	}

	public void outputSimLog(final int stepCurrent, final double simulationTimeCounter, final int totalNumFellowsOfWorker, int resolution) {
		final Date date = new Date();

		if (settings.isOutputSimulationLog && (fosLog != null)) {
			outputStringToFile(fosLog, date.toString());
			outputStringToFile(fosLog, ",");
			outputStringToFile(fosLog, String.valueOf(stepCurrent / resolution));
			outputStringToFile(fosLog, ",");
			outputStringToFile(fosLog, String.valueOf(simulationTimeCounter));
			outputStringToFile(fosLog, ",");
			outputStringToFile(fosLog, String.valueOf(totalNumFellowsOfWorker));
			outputStringToFile(fosLog, System.getProperty("line.separator"));
		}
	}

	void outputStringToFile(final FileOutputStream fos, final String str) {

		final byte[] dataInBytes = str.getBytes();

		try {
			fos.write(dataInBytes);
			fos.flush();
		} catch (final IOException e) {
		}
	}

	public FileOutputStream getTrjFos() {
		return trjFos;
	}

	public FileOutputStream getVdFos() {
		return vdFos;
	}
}

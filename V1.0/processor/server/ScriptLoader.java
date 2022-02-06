package processor.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import common.Settings;
import traffic.light.TrafficLightTiming;
import traffic.routing.Routing;

/**
 * This class loads simulation setups from a file.
 */
public class ScriptLoader {
	private final ArrayList<ArrayList<String>> simSetups = new ArrayList<>();

	boolean isEmpty() {
		return simSetups.size() == 0;
	}

	/*
	 * Load setups from a file and save the setups to a list.
	 */
	boolean loadScriptFile() {
		simSetups.clear();
		File file = new File(Settings.inputSimulationScript);
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			ArrayList<String> oneSimSetup = new ArrayList<>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("###")) {
					if (oneSimSetup.size() > 0) {
						simSetups.add(oneSimSetup);
						oneSimSetup = new ArrayList<>();
					}
				} else {
					oneSimSetup.add(line);
				}
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Get simulation setup from the imported setup list. The setup will be sent
	 * to workers when server informs the workers to set up simulation. The
	 * retrieved setup will be removed from the list.
	 */
	boolean retrieveOneSimulationSetup() {
		boolean isNewMap = false;
		final ArrayList<String> newSettings = simSetups.get(0);

		for (final String string : newSettings) {
			if (string.length() >= 2 && string.substring(0, 2).equals("//"))
				continue;
			String[] fields = string.split(" ");
			if (fields.length < 2)
				continue;
			switch (fields[0]) {
			case "driveOnLeft": {
				Settings.isDriveOnLeft = Boolean.parseBoolean(fields[1]);
				break;
			}
			case "maxNumSteps": {
				Settings.maxNumSteps = Integer.parseInt(fields[1]);
				break;
			}
			case "numRandomBackgroundPrivateVehicles": {
				Settings.numGlobalRandomBackgroundPrivateVehicles = Integer.parseInt(fields[1]);
				break;
			}
			case "numRandomBackgroundTrams": {
				Settings.numGlobalBackgroundRandomTrams = Integer.parseInt(fields[1]);
				break;
			}
			case "numRandomBackgroundBuses": {
				Settings.numGlobalBackgroundRandomBuses = Integer.parseInt(fields[1]);
				break;
			}
			case "foregroundVehicleFile": {
				if (fields[1].equals("-")) {
					Settings.inputForegroundVehicleFile = "";
				} else {
					Settings.inputForegroundVehicleFile = fields[1];
				}
				break;
			}
			case "backgroundVehicleFile": {
				if (fields[1].equals("-")) {
					Settings.inputBackgroundVehicleFile = "";
				} else {
					Settings.inputBackgroundVehicleFile = fields[1];
				}
				break;
			}
			case "outputSimulationLog": {
				Settings.isOutputSimulationLog = Boolean.parseBoolean(fields[1]);
				break;
			}
			case "outputTrajectory": {
				try {
					DataOutputScope scope = DataOutputScope.valueOf(fields[1]);
					Settings.outputTrajectoryScope = scope;
				} catch (Exception e) {
					System.out.println("Output trajectory value is invalid.");
				}
				break;
			}
			case "outputInitialRoute": {
				try {
					DataOutputScope scope = DataOutputScope.valueOf(fields[1]);
					Settings.outputRouteScope = scope;
				} catch (Exception e) {
					System.out.println("Output route value is invalid.");
				}
				break;
			}
			case "outputTravelTime": {
				try {
					DataOutputScope scope = DataOutputScope.valueOf(fields[1]);
					Settings.outputTravelTimeScope = scope;
				} catch (Exception e) {
					System.out.println("Output travel time value is invalid.");
				}
				break;
			}
			case "numRuns": {
				// Create settings for repeat simulations.
				final int numRuns = Integer.parseInt(fields[1]);
				for (int i = 0; i < (numRuns - 1); i++) {
					simSetups.add(1, new ArrayList<String>());
				}
			}
			case "lookAheadDistance": {
				Settings.lookAheadDistance = Double.parseDouble(fields[1]);
				break;
			}
			case "numStepsPerSecond": {
				Settings.numStepsPerSecond = Double.parseDouble(fields[1]);
				break;
			}
			case "serverBased": {
				Settings.isServerBased = Boolean.parseBoolean(fields[1]);
				break;
			}
			case "openStreetMapFile": {
				if (fields[1].equals("-")) {
					Settings.inputOpenStreetMapFile = "";
				} else {
					Settings.inputOpenStreetMapFile = fields[1];
				}
				isNewMap = true;
				break;
			}
			case "trafficLightTiming": {
				try {
					TrafficLightTiming newTiming = TrafficLightTiming.valueOf(fields[1]);
					Settings.trafficLightTiming = newTiming;
				} catch (Exception e) {
					System.out.println("Traffic light timing value is invalid.");
				}
				break;
			}
			case "routingAlgorithm": {
				try {
					Routing.Algorithm newAlgorithm = Routing.Algorithm.valueOf(fields[1]);
					Settings.routingAlgorithm = newAlgorithm;
				} catch (Exception e) {
					System.out.println("Routing algorithm value is invalid.");
				}
				break;
			}
			case "trafficReportStepGapInServerlessMode": {
				Settings.trafficReportStepGapInServerlessMode = Integer.parseInt(fields[1]);
				break;
			}
			case "allowReroute": {
				Settings.isAllowReroute = Boolean.parseBoolean(fields[1]);
				break;
			}

			case "partitionType": {
				Settings.partitionType = fields[1];
				break;
			}
			case "turnFromAnyLane":{
				Settings.isUseAnyLaneToTurn=Boolean.parseBoolean(fields[1]);
				break;
			}
			}

		}
		simSetups.remove(0);
		return isNewMap;
	}
}

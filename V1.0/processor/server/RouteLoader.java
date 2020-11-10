package processor.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import common.Settings;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableRouteLeg;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.VehicleType;

/**
 * This class loads vehicle routes from external files and create vehicles based
 * on the routes.
 *
 */
public class RouteLoader {
	class NodeIdComparator implements Comparator<NodeInfo> {
		@Override
		public int compare(final NodeInfo v1, final NodeInfo v2) {
			// TODO Auto-generated method stub
			return v1.osmId > v2.osmId ? -1 : v1.osmId == v2.osmId ? 0 : 1;
		}
	}

	class NodeInfo {
		long osmId;

		int index;

		NodeInfo(final long osmId, final int index) {
			super();
			this.osmId = osmId;
			this.index = index;
		}
	}

	ArrayList<String> vehicles = new ArrayList<>(150000);
	ArrayList<NodeInfo> idMappers = new ArrayList<>(300000);
	RoadNetwork roadNetwork;
	NodeIdComparator nodeIdComparator = new NodeIdComparator();

	ArrayList<WorkerMeta> workers;

	Server server;

	public RouteLoader(final Server server, final ArrayList<WorkerMeta> workers) {
		roadNetwork = server.roadNetwork;
		this.workers = workers;
		this.server = server;
	}

	/**
	 * Append route of vehicles to the workers whose work area covers the first
	 * node of the route.
	 */
	void assignVehicleToWorker() {
		// Clear routes from previous loading
		for (final WorkerMeta worker : workers) {
			worker.externalRoutes.clear();
		}
		for (final String vehicle : vehicles) {
			final String[] fields = vehicle.split(Settings.delimiterItem);
			final boolean foreground = Boolean.parseBoolean(fields[0]);
			final String id = fields[1];
			final double start_time = Double.parseDouble(fields[2]);
			final String type = fields[3];
			final String driverProfile = fields[4];
			final double repeatRate = Double.parseDouble(fields[5]);
			final ArrayList<SerializableRouteLeg> route = getRouteFromString(fields[6]);
			final Node routeStartNode = roadNetwork.edges.get(route.get(0).edgeIndex).startNode;
			final WorkerMeta routeStartWorker = server.getWorkerAtRouteStart(routeStartNode);
			routeStartWorker.externalRoutes.add(new SerializableExternalVehicle(foreground, id, start_time, type,
					driverProfile, repeatRate, route));
		}
	}

	ArrayList<SerializableRouteLeg> getRouteFromString(final String routeString) {

		final String[] routeLegs = routeString.split(Settings.delimiterSubItem);
		final ArrayList<SerializableRouteLeg> route = new ArrayList<>();
		for (int i = 0; i < (routeLegs.length - 1); i++) {
			final String[] currentLegDetails = routeLegs[i].split("#");
			final String[] nextLegDetails = routeLegs[i + 1].split("#");
			final long osmIdNd1 = Long.parseLong(currentLegDetails[0]);
			final int mapperIndexNd1 = Collections.binarySearch(idMappers, new NodeInfo(osmIdNd1, -1),
					nodeIdComparator);
			// Stop processing further if node cannot be found
			if (mapperIndexNd1 < 0) {
				System.out.println("Cannot find node: " + osmIdNd1 + ". ");
				break;
			}

			final int nodeIndexNd1 = idMappers.get(mapperIndexNd1).index;
			final Node nd1 = roadNetwork.nodes.get(nodeIndexNd1);

			// Get the edge and add it to a list
			final long osmIdNd2 = Long.parseLong(nextLegDetails[0]);

			for (final Edge e : nd1.outwardEdges) {
				if (e.endNode.osmId == osmIdNd2) {
					route.add(new SerializableRouteLeg(e.index, Double.parseDouble(currentLegDetails[1])));
					break;
				}
			}
		}

		return route;
	}

	void loadRoutes() {

		if (Settings.inputForegroundVehicleFile.length() > 0) {
			scanXML(Settings.inputForegroundVehicleFile, true);
		}
		if (Settings.inputBackgroundVehicleFile.length() > 0) {
			scanXML(Settings.inputBackgroundVehicleFile, false);
		}

		for (final Node node : roadNetwork.nodes) {
			idMappers.add(new NodeInfo(node.osmId, node.index));
		}
		Collections.sort(idMappers, nodeIdComparator);

		for (final WorkerMeta worker : workers) {
			worker.externalRoutes.clear();
		}

		assignVehicleToWorker();
	}

	/**
	 * Scan the given XML file to get routes of vehicles.
	 */
	void scanXML(final String fileName, final boolean foreground) {
		try {

			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser saxParser = factory.newSAXParser();

			final DefaultHandler handler = new DefaultHandler() {

				StringBuilder sbOneV = new StringBuilder();

				@Override
				public void characters(final char ch[], final int start, final int length) throws SAXException {
				}

				@Override
				public void endElement(final String uri, final String localName, final String qName)
						throws SAXException {

					if (qName.equals("vehicle")) {
						vehicles.add(sbOneV.toString());
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName,
						final Attributes attributes) throws SAXException {

					if (qName.equals("vehicle")) {
						sbOneV.delete(0, sbOneV.length());
						// Vehicle is foreground or background?
						sbOneV.append(String.valueOf(foreground) + Settings.delimiterItem);
						// Vehicle ID
						sbOneV.append(attributes.getValue("id") + Settings.delimiterItem);
						// Vehicle start time (earliest time the vehicle could be released from parking)
						String start_time = attributes.getValue("start_time");
						if (start_time == null) {
							start_time = "0";
						}
						sbOneV.append(start_time + Settings.delimiterItem);
						// Vehicle type
						String type = attributes.getValue("type");
						if (type == null) {
							type = VehicleType.CAR.name();
						}
						sbOneV.append(type + Settings.delimiterItem);
						// Vehicle driver profile
						String driverProfile = attributes.getValue("driverProfile");
						if (driverProfile == null) {
							driverProfile = DriverProfile.NORMAL.name();
						}
						sbOneV.append(driverProfile + Settings.delimiterItem);
						// Repeat rate of this vehicle
						String repeatRate = attributes.getValue("repeatPerSecond");
						if (repeatRate == null) {
							repeatRate = "0";
						}
						sbOneV.append(repeatRate + Settings.delimiterItem);
					}

					if (qName.equals("node")) {
						// Node ID on vehicle's route
						sbOneV.append(attributes.getValue("id") + "#");
						// Time length the vehicle needs to park at the node
						String stopover = attributes.getValue("stopover");
						if (stopover == null) {
							stopover = "0";
						}
						sbOneV.append(stopover + Settings.delimiterSubItem);
					}
				}

			};

			saxParser.parse(fileName, handler);

		} catch (final Exception exception) {
			System.out.println(exception);
		}
	}

}

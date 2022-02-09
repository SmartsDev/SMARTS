/*******************************************************************************
 * Copyright (C)  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package osm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import common.Settings;
import traffic.road.RoadType;

/**
 * This class extracts road map from OpenStreetMap XML data, which consists of
 * nodes, ways and relations. Nodes not referenced by way elements are not
 * extracted. Routes of public transport are extracted by scanning the
 * relations, which consist of way elements.
 *
 */
public class OSM {
	/*
	 * Comparator based on node id.
	 */
	class OSMNodeComparator implements Comparator<OSMNode> {
		@Override
		public int compare(final OSMNode n1, final OSMNode n2) {
			if (n1.id < n2.id) {
				return -1;
			} else if (n1.id > n2.id) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	/*
	 * Comparator based on element id.
	 */
	class XMLElementIdComparator implements Comparator<Element> {
		@Override
		public int compare(final Element e1, final Element e2) {

			final long id1 = Long.valueOf(e1.getAttribute("id"));
			final long id2 = Long.valueOf(e2.getAttribute("id"));

			if (id1 < id2) {
				return -1;
			} else if (id1 > id2) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	/**
	 * Processes an OSM XML file as specified by user.
	 *
	 * @param args
	 *            Directory of the file containing OSM data
	 */
	public static void main(final String[] args) {
		final OSM osm = new OSM();
		if (args.length > 0) {
			Settings.inputOpenStreetMapFile = args[0];
		} else {
			if (Settings.inputOpenStreetMapFile.length() == 0) {
				System.out.println("You need to specify the path of the file for processing.");
				return;
			}
		}
		osm.processOSM(Settings.inputOpenStreetMapFile, false);
	}

	FileOutputStream fosOutputFile;
	ArrayList<Element> elements_node = new ArrayList<>(1000000);
	ArrayList<Element> elements_way = new ArrayList<>(1000000);
	ArrayList<Element> elements_relation = new ArrayList<>(300);

	ArrayList<OSMNode> nodes = new ArrayList<>(1000000);

	ArrayList<OSMEdge> edges = new ArrayList<>(1000000);

	double minLon, maxLon, minLat, maxLat;

	/*
	 * Attaches route reference (e.g., route number of tram) to way elements.
	 * The route numbers are from relation elements.
	 */
	void attachRouteRefToWays() {
		final XMLElementIdComparator xC = new XMLElementIdComparator();
		for (final Element e : elements_relation) {
			final String type = e.getAttribute("type");
			final String routeRef = e.getAttribute("ref");
			final NodeList ways = e.getChildNodes();

			for (int i = 0; i < ways.getLength(); i++) {
				final Element member = (Element) ways.item(i);
				final int wayIndex = Collections.binarySearch(elements_way, member, xC);
				if (wayIndex < 0) {
					continue;
				}
				if (type.equals("tram")) {
					final String value = elements_way.get(wayIndex).getAttribute("tramRef");
					elements_way.get(wayIndex).setAttribute("tramRef", value + routeRef + "-");
				}
				if (type.equals("bus")) {
					final String value = elements_way.get(wayIndex).getAttribute("busRef");
					elements_way.get(wayIndex).setAttribute("busRef", value + routeRef + "-");
				}
			}
		}
	}

	/*
	 * Creates edges based on the way elements, each of which consists of a
	 * sequence of nodes. Each pair of adjacent nodes corresponds to an edge or
	 * two edges (if the way element represents a segment on a "two-way" road).
	 */
	void buildEdges() {
		final OSMNodeComparator nodeComp = new OSMNodeComparator();
		for (final Element e : elements_way) {
			final int lanes = Integer.parseInt(e.getAttribute("lanes"));
			final String type = e.getAttribute("type");
			final String name = e.getAttribute("name");
			final boolean oneway = Boolean.parseBoolean(e.getAttribute("oneway"));
			final int maxspeed = Integer.parseInt(e.getAttribute("maxspeed"));
			final boolean roundabout = Boolean.parseBoolean(e.getAttribute("roundabout"));
			final String busRef = e.getAttribute("busRef");
			final String tramRef = e.getAttribute("tramRef");
			final int rightLanes = Integer.parseInt(e.getAttribute("rightLanes"));//Read number of 'straight or right turn' lanes if OSM data contains this customized field
			final int leftLanes = Integer.parseInt(e.getAttribute("leftLanes"));//Read number of 'straight or left turn' lanes if OSM data contains this customized field
			final int rightOnlyLanes = Integer.parseInt(e.getAttribute("rightOnlyLanes"));//Read number of right only lanes if OSM data contains this customized field
			final int leftOnlyLanes = Integer.parseInt(e.getAttribute("leftOnlyLanes"));//Read number of left only lanes if OSM data contains this customized field

			final NodeList children = e.getChildNodes();
			for (int i = 0; i < (children.getLength() - 1); i++) {
				// Index of the starting node of the edge
				final Element startElement = (Element) children.item(i);
				final OSMNode startOSMNode = new OSMNode(Long.parseLong(startElement.getAttribute("id")), 0, 0, false,
						false, false, "");
				final int indexStartNode = Collections.binarySearch(nodes, startOSMNode, nodeComp);

				// Stop as the node cannot be found although it is reference by
				// the way element
				if (indexStartNode < 0) {
					break;
				}

				// Index of the ending node of the edge
				final Element endElement = (Element) children.item(i + 1);
				final OSMNode endOSMNode = new OSMNode(Long.parseLong(endElement.getAttribute("id")), 0, 0, false,
						false, false, "");
				final int indexEndNode = Collections.binarySearch(nodes, endOSMNode, nodeComp);
				if (indexEndNode < 0) {
					break;
				}

				if ((indexStartNode >= 0) && (indexEndNode >= 0)) {
					final OSMEdge edge1 = new OSMEdge(indexStartNode, indexEndNode, lanes, type, name, oneway, maxspeed,
							roundabout, busRef, tramRef, rightLanes, leftLanes, rightOnlyLanes, leftOnlyLanes);
					edges.add(edge1);

					if (!oneway) {
						final OSMEdge edge2 = new OSMEdge(indexEndNode, indexStartNode, lanes, type, name, oneway,
								maxspeed, roundabout, busRef, tramRef, rightLanes, leftLanes, rightOnlyLanes,
								leftOnlyLanes);
						edges.add(edge2);
					}
				}
			}
		}
	}

	/*
	 * Builds nodes based on node elements in OSM file. Only the node elements
	 * that are referenced by the way elements are considered.
	 */
	void buildNodes() {

		try {

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db;
			db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();

			// Get id of nodes used by way elements
			final HashSet<Long> hsNodes = new HashSet<>();
			for (final Element e : elements_way) {
				final NodeList children = e.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					final Element child = (Element) children.item(i);
					hsNodes.add(Long.parseLong(child.getAttribute("id")));
				}
			}

			// Sort the node ids
			final List<Long> sortedNodeIdsUsedByWays = new ArrayList<>(hsNodes);
			Collections.sort(sortedNodeIdsUsedByWays);

			final XMLElementIdComparator xC = new XMLElementIdComparator();
			for (final long id : sortedNodeIdsUsedByWays) {
				final Element nodeToSearch = document.createElement("nd");
				nodeToSearch.setAttribute("id", Long.toString(id));
				// Find the actual node element referenced by a way element
				final int index = Collections.binarySearch(elements_node, nodeToSearch, xC);
				// Nodes may not be in the OSM file although they are referenced
				// by some way elements.
				if (index < 0) {
					continue;
				}
				final Element element = elements_node.get(index);

				// Construct a node based on the found node element
				nodes.add(new OSMNode(id, Double.parseDouble(element.getAttribute("lat")),
						Double.parseDouble(element.getAttribute("lon")),
						Boolean.parseBoolean(element.getAttribute("traffic_signals")),
						Boolean.parseBoolean(element.getAttribute("tram_stop")),
						Boolean.parseBoolean(element.getAttribute("bus_stop")), element.getAttribute("name")));
			}
		} catch (final ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Closes output file
	 */
	void closeOutputFile() {
		try {
			fosOutputFile.close();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Gets the bounding box of the spatial data
	 */
	void getBoundingBox() {
		try {
			OSMNode e = nodes.get(0);
			minLat = e.lat;
			maxLat = minLat;
			minLon = e.lon;
			maxLon = minLon;

			double lat, lon;
			for (int i = 1; i < nodes.size(); i++) {
				e = nodes.get(i);
				lat = e.lat;
				lon = e.lon;
				if (lat < minLat) {
					minLat = lat;
				}
				if (lat > maxLat) {
					maxLat = lat;
				}
				if (lon < minLon) {
					minLon = lon;
				}
				if (lon > maxLon) {
					maxLon = lon;
				}
			}

		} catch (final Exception e) {
		}
	}

	/*
	 * Initializes output file
	 */
	void initOutputFile() {
		try {
			final File outputFile = new File("roads.txt");
			outputFile.delete();
			outputFile.createNewFile();
			fosOutputFile = new FileOutputStream(outputFile, true);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	boolean isPositiveInteger(final String str) {
		if (str == null) {
			return false;
		}
		final int length = str.length();
		if (length == 0) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if ((c < '0') || (c > '9')) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Links edges to their start nodes.
	 */
	void linkEdgesToNodes() {
		for (final OSMEdge edge : edges) {
			nodes.get(edge.startNodeIndex).edges.add(edge);
		}
	}

	/**
	 * Output road network data to a string. The data contains a list of nodes.
	 * For each node on the list, the data contains the information of outward
	 * edges that start from the node. For each outward edge, the end node of
	 * the edge is saved in the data. Additional information about the edge,
	 * such as public transport reference, is also saved with the end node.
	 * 
	 * A row contains one node and one or more outward edges that extend from
	 * the node. A node starts with a <Node> tag. An edge starts with an <Edge>
	 * tag. A node or an edge contains a number of fields. Some of the fields
	 * are read from OpenStreetMap data. Others are computed values. A typical
	 * row shows <Node> ... <Edge> ... <Edge> ... <Edge> ...
	 * 
	 * A node contains the following fields: index of the node in the list of
	 * nodes, OpenStreetMap node ID, name, latitude, longitude, whether the node
	 * has a traffic signal, whether the node has a tram, whether the node has a
	 * bus stop, number of outward edges extended from the node.
	 * 
	 * An edge contains the following fields: index of the edge in the list of
	 * edge, total number of lanes, number of lanes that allow right turn,
	 * number of lanes that allow left turn, number of lanes that only allow
	 * right turn, number of lanes that only allow left turn, road type,
	 * name,max speed, whether the edge is part of a roundabout, references to
	 * trams that use this edge, references to buses that use this edge.
	 * 
	 * 
	 *
	 */
	String outputMapToString() {

		final StringBuilder sb = new StringBuilder();
		try {
			// Output bounding box
			sb.append("minLat" + Settings.delimiterSubItem + minLat + Settings.delimiterSubItem);
			sb.append("minLon" + Settings.delimiterSubItem + minLon + Settings.delimiterSubItem);
			sb.append("maxLat" + Settings.delimiterSubItem + maxLat + Settings.delimiterSubItem);
			sb.append("maxLon" + Settings.delimiterSubItem + maxLon + Settings.delimiterSubItem);
			sb.append("#nodes" + Settings.delimiterSubItem + nodes.size() + Settings.delimiterSubItem);
			sb.append(Settings.delimiterItem);

			// Output GraphNode with GraphEdge
			for (int i = 0; i < nodes.size(); i++) {
				final OSMNode gn = nodes.get(i);
				sb.append("<Node>" + Settings.delimiterSubItem);

				// Index of the current node in the output list of all nodes
				sb.append(Integer.toString(i) + Settings.delimiterSubItem);

				// Original node id in OSM data
				sb.append(gn.id + Settings.delimiterSubItem);

				// Name of the current node
				sb.append(removeInvalidCharacters(gn.name) + Settings.delimiterSubItem);

				// Coordinates
				sb.append(gn.lat + Settings.delimiterSubItem);
				sb.append(gn.lon + Settings.delimiterSubItem);

				// Traffic signal
				sb.append(gn.isTrafficLight + Settings.delimiterSubItem);

				// Tram stop
				sb.append(gn.isTramStop + Settings.delimiterSubItem);

				// Bus stop
				sb.append(gn.isBusStop + Settings.delimiterSubItem);

				// Number of edges started from this node (i.e., outward edges)
				final ArrayList<OSMEdge> edges = gn.edges;
				sb.append(edges.size() + Settings.delimiterSubItem);

				// Edges
				for (final OSMEdge ge : edges) {
					sb.append("<Edge>" + Settings.delimiterSubItem);
					sb.append(ge.endNodeIndex + Settings.delimiterSubItem);
					sb.append(ge.lanes + Settings.delimiterSubItem);
					sb.append(ge.rightLanes + Settings.delimiterSubItem);
					sb.append(ge.leftLanes + Settings.delimiterSubItem);
					sb.append(ge.rightOnlyLanes + Settings.delimiterSubItem);
					sb.append(ge.leftOnlyLanes + Settings.delimiterSubItem);
					sb.append(removeInvalidCharacters(ge.type) + Settings.delimiterSubItem);
					sb.append(removeInvalidCharacters(ge.name) + Settings.delimiterSubItem);
					sb.append(ge.maxspeed + Settings.delimiterSubItem);
					sb.append(ge.roundabout + Settings.delimiterSubItem);
					sb.append(removeInvalidCharacters(ge.tramRef) + Settings.delimiterSubItem);
					sb.append(removeInvalidCharacters(ge.busRef) + Settings.delimiterSubItem);
				}
				// Separator
				sb.append(Settings.delimiterItem);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	String removeInvalidCharacters(String toCheck) {
		String result = new String(toCheck);
		result.replace(Settings.delimiterSubItem, " ");
		result.replace(Settings.delimiterItem, " ");
		return result;
	}

	/*
	 * Saves a string to result file.
	 */
	synchronized void outputStringToFile(final String str) {
		final byte[] dataInBytes = str.getBytes();
		try {
			fosOutputFile.write(dataInBytes);
			fosOutputFile.flush();
		} catch (final IOException e) {
		}
	}

	/**
	 * Processes a given OSM file. Road network is extracted from the file. The
	 * result is saved to a global variable in memory or a file on disk.
	 *
	 * @param inputFilePath
	 *            Directory of OSM file
	 * @param isToMemory
	 *            Whether the result is saved to global variable in memory or a
	 *            file on disk
	 * @return
	 */
	public void processOSM(final String inputFilePath, final boolean isToMemory) {
		System.out.println("Building road network graph based on OSM file: " + Settings.inputOpenStreetMapFile);
		if (scanXML(inputFilePath)) {
			Collections.sort(elements_node, new XMLElementIdComparator());
			Collections.sort(elements_way, new XMLElementIdComparator());
			attachRouteRefToWays();
			buildNodes();
			buildEdges();
			linkEdgesToNodes();
			getBoundingBox();

			if (isToMemory) {
				Settings.roadGraph = outputMapToString();
			} else {
				initOutputFile();
				outputStringToFile(outputMapToString());
				closeOutputFile();
			}
			System.out.println("Done");
		}
	}

	/*
	 * Scans the XML file to get the lists of nodes, ways and relations.
	 */
	boolean scanXML(final String filePath) {
		try {

			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser saxParser = factory.newSAXParser();

			final DefaultHandler handler = new DefaultHandler() {

				// Create a dummy document for creating temporary nodes
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document document = db.newDocument();

				boolean b_node = false;// Is reading a node?
				boolean b_way = false;// Is reading a way?
				boolean b_relation = false;// Is reading a relation?

				Element e_node;// Temporary node element
				Element e_way;// Temporary way element
				Element e_relation;// Temporary way element
				ArrayList<Element> list_nd = new ArrayList<>(10000);//Node reference in way element
				ArrayList<Element> list_member = new ArrayList<>(1000);//Way reference in relation element

				@Override
				public void characters(final char ch[], final int start, final int length) throws SAXException {

				}

				@Override
				public void endElement(final String uri, final String localName, final String qName)
						throws SAXException {

					if (qName.equals("node")) {
						elements_node.add(e_node);
						b_node = false;
					}

					if (qName.equals("way")) {
						// Only process way elements that have type attribute
						if (e_way.hasAttribute("type")) {
							for (final Element e : list_nd) {
								e_way.appendChild(e);
							}
							elements_way.add(e_way);
						}
						b_way = false;
					}

					if (qName.equals("relation")) {
						if (e_relation.hasAttribute("type")) {
							for (final Element e : list_member) {
								e_relation.appendChild(e);
							}
							elements_relation.add(e_relation);
						}
						b_relation = false;
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName,
						final Attributes attributes) throws SAXException {

					if (qName.equals("node")) {
						b_node = true;
						e_node = document.createElement("node");
						e_node.setAttribute("id", attributes.getValue("id"));
						e_node.setAttribute("lat", attributes.getValue("lat"));
						e_node.setAttribute("lon", attributes.getValue("lon"));
					}

					if (qName.equals("way")) {
						b_way = true;
						e_way = document.createElement("way");
						e_way.setAttribute("id", attributes.getValue("id"));
						list_nd.clear();
					}

					if (qName.equals("nd")) {
						final Element nd = document.createElement("nd");
						nd.setAttribute("id", attributes.getValue("ref"));
						list_nd.add(nd);
					}

					if (qName.equals("relation")) {
						b_relation = true;
						e_relation = document.createElement("relation");
						list_member.clear();
					}

					if (qName.equals("member")) {
						final Element member = document.createElement("member");
						member.setAttribute("id", attributes.getValue("ref"));
						list_member.add(member);
					}

					// Set the attributes of elements by reading the attribute
					// values from OSM file. If the value is not shown in the
					// file, set the default values.
					if (qName.equals("tag")) {
						if (b_node) {
							final String k = attributes.getValue("k");
							final String v = attributes.getValue("v");

							// Traffic signals.
							if (k.equals("highway") && v.equals("traffic_signals")) {
								e_node.setAttribute("traffic_signals", "true");
							}
							// Tram stop.
							if (k.equals("railway") && v.equals("tram_stop")) {
								e_node.setAttribute("tram_stop", "true");
							}
							// Name
							if (k.equals("name")) {
								e_node.setAttribute("name", v);
							}

							// Bus stop.
							if (k.equals("highway") && v.equals("bus_stop")) {
								e_node.setAttribute("bus_stop", "true");
							}
							/*
							 * Complete attributes if they are missing in
							 * original data
							 */
							if (!e_node.hasAttribute("traffic_signals")) {
								e_node.setAttribute("traffic_signals", "false");
							}
							if (!e_node.hasAttribute("tram_stop")) {
								e_node.setAttribute("tram_stop", "false");
							}
							if (!e_node.hasAttribute("name")) {
								e_node.setAttribute("name", "");
							}
							if (!e_node.hasAttribute("bus_stop")) {
								e_node.setAttribute("bus_stop", "false");
							}
						}

						if (b_way) {
							final String k = attributes.getValue("k");
							final String v = attributes.getValue("v");

							// Road type.
							if (k.equals("highway") || k.equals("railway")) {
								for (final RoadType t : RoadType.values()) {
									if (v.equals(t.name())) {
										e_way.setAttribute("type", v);
										break;
									}
								}
							}
							// Max speed.
							if (k.equals("maxspeed")) {
								if (isPositiveInteger(attributes.getValue("v"))) {
									e_way.setAttribute("maxspeed", v);
								}
							}

							// Name of the road.
							if (k.equals("name")) {
								e_way.setAttribute("name", v);
							}
							// One-way or not.
							if (k.equals("oneway")) {
								e_way.setAttribute("oneway", "true");
							}
							// Make sure roundabout is one-way
							if (k.equals("junction") && v.equals("roundabout")) {
								e_way.setAttribute("oneway", "true");
								e_way.setAttribute("roundabout", "true");
							}
							// Number of lanes
							if ((Settings.numLanesPerEdge == 0) && k.equals("lanes")) {
								try {
									final int numLanes = Integer.parseInt(v);
									if (numLanes > 0) {
										e_way.setAttribute("lanes", String.valueOf(numLanes));
									}
								} catch (final Exception e) {

								}
							}
							// Number of right-turn/left-turn lanes
							if (k.equals("rightLanes")) {
								e_way.setAttribute("rightLanes", v);
							}
							if (k.equals("leftLanes")) {
								e_way.setAttribute("leftLanes", v);
							}
							if (k.equals("rightOnlyLanes")) {
								e_way.setAttribute("rightOnlyLanes", v);
							}
							if (k.equals("leftOnlyLanes")) {
								e_way.setAttribute("leftOnlyLanes", v);
							}

							// Make sure road type is one of the specified types
							final String typeStr = e_way.getAttribute("type");
							RoadType type = null;
							for (final RoadType t : RoadType.values()) {
								if (typeStr.equals(t.name())) {
									type = t;
									break;
								}
							}
							// Set default attributes to way elements if they
							// are missing
							if (type != null) {
								if (!e_way.hasAttribute("maxspeed")) {
									e_way.setAttribute("maxspeed", String.valueOf(type.maxSpeed));
								}
								if (!e_way.hasAttribute("name")) {
									e_way.setAttribute("name", "");
								}
								if (!e_way.hasAttribute("oneway")) {
									e_way.setAttribute("oneway", "false");
								}
								if (!e_way.hasAttribute("roundabout")) {
									e_way.setAttribute("roundabout", "false");
								}
								if (!e_way.hasAttribute("lanes")) {
									if (Settings.numLanesPerEdge == 0) {
										e_way.setAttribute("lanes", String.valueOf(type.numLanes));
									} else {
										e_way.setAttribute("lanes", String.valueOf(Settings.numLanesPerEdge));
									}
								}
								if (!e_way.hasAttribute("rightLanes")) {
									e_way.setAttribute("rightLanes", "1");//Set to 1 if only one lane can be used for straight or right turn
									//e_way.setAttribute("rightLanes", e_way.getAttribute("lanes"));//Set to total number of lanes if vehicle can turn right from any lane
								}
								if (!e_way.hasAttribute("leftLanes")) {
									e_way.setAttribute("leftLanes", "1");//Set to 1 if only one lane can be used for straight or left turn
									//e_way.setAttribute("leftLanes", e_way.getAttribute("lanes"));//Set to total number of lanes if vehicle can turn left from any lane
								}
								if (!e_way.hasAttribute("rightOnlyLanes")) {
									e_way.setAttribute("rightOnlyLanes", "0");//Set to a positive number if there are right-only (no straight) lanes
								}
								if (!e_way.hasAttribute("leftOnlyLanes")) {
									e_way.setAttribute("leftOnlyLanes", "0");//Set to a positive number if there are left-only (no straight) lanes
								}
							}
						}

						if (b_relation) {
							final String k = attributes.getValue("k");
							final String v = attributes.getValue("v");

							if (k.equals("route")) {
								if (v.equals("tram")) {
									e_relation.setAttribute("type", "tram");
								} else if (v.equals("bus")) {
									e_relation.setAttribute("type", "bus");
								}
							}

							if (k.equals("ref")) {
								e_relation.setAttribute("ref", v);
							}
						}
					}
				}

			};

			InputStream inputStream = null;
			final File file = new File(filePath);
			inputStream = new FileInputStream(file);

			final Reader reader = new InputStreamReader(inputStream, "UTF-8");
			final InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			saxParser.parse(is, handler);
			return true;
		} catch (final Exception exception) {
			return false;
		}
	}

}

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

import java.util.ArrayList;

/**
 * Node in road network graph. A node is the end point of an edge.
 *
 */
public class OSMNode {
	long id;
	double lat, lon;
	boolean isTrafficLight;
	boolean isTramStop;
	boolean isBusStop;
	ArrayList<OSMEdge> edges = new ArrayList<>();
	String name;

	/**
	 *
	 * @param id
	 *            Node id in OSM data.
	 * @param lat
	 *            Latitude.
	 * @param lon
	 *            Longitude.
	 * @param has_traffic_light
	 *            Whether there is traffic light at this node.
	 * @param has_tram_stop
	 *            Whether there is a tram stop at this node.
	 * @param has_bus_stop
	 *            Whether there is a bus stop at this node.
	 * @param name
	 *            Name associated with this node.
	 */
	public OSMNode(final long id, final double lat, final double lon, final boolean has_traffic_light,
			final boolean has_tram_stop, final boolean has_bus_stop, final String name) {
		super();
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		isTrafficLight = has_traffic_light;
		isTramStop = has_tram_stop;
		isBusStop = has_bus_stop;
		this.name = name;
	}
}

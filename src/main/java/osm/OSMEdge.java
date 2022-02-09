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

/**
 * Edge on a road network graph. An edge has two nodes, one at the start and one
 * at the end.
 *
 */
public class OSMEdge {
	int startNodeIndex;
	int endNodeIndex;
	int lanes;
	String type;
	String name;
	boolean oneway;
	int maxspeed;
	boolean roundabout;
	String busRef;
	String tramRef;
	int rightLanes;
	int leftLanes;
	int rightOnlyLanes;
	int leftOnlyLanes;

	/**
	 *
	 * @param startNodeIndex
	 *            Index of the start node in the list of all the nodes on the
	 *            map.
	 * @param endNodeIndex
	 *            Index of the end node in the list of all the nodes on the map.
	 * @param lanes
	 *            Number of lanes.
	 * @param type
	 *            Road type.
	 * @param name
	 *            Name associated with the road.
	 * @param oneway
	 *            Whether the edge is on a one-way road.
	 * @param maxspeed
	 *            Max speed.
	 * @param roundabout
	 *            Whether the edge is in a roundabout.
	 * @param busRef
	 *            The string containing all the references to the bus routes
	 *            passing this edge.
	 * @param tramRef
	 *            The string containing all the references to the tram routes
	 *            passing this edge.
	 * @param rightLanes
	 *            Number of lanes where vehicles can turn right or go straight
	 * @param leftLanes
	 *            Number of lanes where vehicles can turn left or go straight
	 * @param rightOnlyLanes
	 *            Number of lanes that only allow right-turn vehicles
	 * @param leftOnlyLanes
	 *            Number of lanes that only allow left-turn vehicles
	 */
	public OSMEdge(final int startNodeIndex, final int endNodeIndex, final int lanes, final String type,
			final String name, final boolean oneway, final int maxspeed, final boolean roundabout, final String busRef,
			final String tramRef, final int rightLanes, final int leftLanes, final int rightOnlyLanes,
			final int leftOnlyLanes) {
		super();
		this.startNodeIndex = startNodeIndex;
		this.endNodeIndex = endNodeIndex;
		this.lanes = lanes;
		this.type = type;
		this.name = name;
		this.oneway = oneway;
		this.maxspeed = maxspeed;
		this.roundabout = roundabout;
		this.busRef = busRef;
		this.tramRef = tramRef;
		this.rightLanes = rightLanes;
		this.leftLanes = leftLanes;
		this.rightOnlyLanes = rightOnlyLanes;
		this.leftOnlyLanes = leftOnlyLanes;
	}

}

package processor.server.gui;

public class OpenStreetMapTile {
	/**
	 * Get tile number for downloading OpenStreetMap static image. Methods in
	 * this class are from
	 * https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java.
	 * 
	 * @param lat
	 * @param lon
	 * @param zoom
	 * @return
	 */
	String getTileNumber(final double lat, final double lon, final int zoom) {
		int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
		int ytile = (int) Math
				.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2
						* (1 << zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);
		return ("" + zoom + "/" + xtile + "/" + ytile);
	}

	class BoundingBox {
		double north;
		double south;
		double east;
		double west;
	}

	BoundingBox tile2boundingBox(final int zoom, final int x, final int y) {
		BoundingBox bb = new BoundingBox();
		bb.north = tile2lat(y, zoom);
		bb.south = tile2lat(y + 1, zoom);
		bb.west = tile2lon(x, zoom);
		bb.east = tile2lon(x + 1, zoom);
		return bb;
	}

	static double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

}

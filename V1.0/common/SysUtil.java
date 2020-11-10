package common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Random;

public class SysUtil {

	public static String getMyIpV4Addres() {
		if (Settings.isSharedJVM) {
			return "127.0.0.1"; // Fix to localhost (might be necessary for computer without Internet)
		} else {
			try {
				Enumeration en;

				// Return first external IPv4 address
				en = NetworkInterface.getNetworkInterfaces();
				while (en.hasMoreElements()) {
					NetworkInterface ni = (NetworkInterface) en.nextElement();
					Enumeration ee = ni.getInetAddresses();
					while (ee.hasMoreElements()) {
						final InetAddress ia = (InetAddress) ee.nextElement();
						if (!ia.isLoopbackAddress() && !ia.isSiteLocalAddress() && (ia instanceof Inet4Address)) {
							return ia.getHostAddress();
						}
					}
					// If there is no external IPv4 address, return first local address
					en = NetworkInterface.getNetworkInterfaces();
					while (en.hasMoreElements()) {
						ni = (NetworkInterface) en.nextElement();
						ee = ni.getInetAddresses();
						while (ee.hasMoreElements()) {
							final InetAddress ia = (InetAddress) ee.nextElement();
							if ((ia.isLoopbackAddress() || ia.isSiteLocalAddress()) && (ia instanceof Inet4Address)) {
								return ia.getHostAddress();
							}
						}
					}
				}
			} catch (final SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String getRandomID(final int numCharacters) {
		final char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		final StringBuilder sb = new StringBuilder();
		final Random random = new Random();
		for (int i = 0; i < numCharacters; i++) {
			final char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		return sb.toString();
	}

	public static String getTimeStampString() {
		return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
	}

}

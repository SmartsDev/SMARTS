package common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterOutputStream;

public class SysUtil {

	public static String getMyIpV4Addres() {
		if (Settings.isSharedJVM) {
			return "127.0.0.1"; // Fix to localhost (might be necessary for
								// computer without Internet)
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
						if (!ia.isLoopbackAddress() && !ia.isSiteLocalAddress()
								&& (ia instanceof Inet4Address)) {
							return ia.getHostAddress();
						}
					}
					// If there is no external IPv4 address, return first local
					// address
					en = NetworkInterface.getNetworkInterfaces();
					while (en.hasMoreElements()) {
						ni = (NetworkInterface) en.nextElement();
						ee = ni.getInetAddresses();
						while (ee.hasMoreElements()) {
							final InetAddress ia = (InetAddress) ee
									.nextElement();
							if ((ia.isLoopbackAddress() || ia
									.isSiteLocalAddress())
									&& (ia instanceof Inet4Address)) {
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
		return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar
				.getInstance().getTime());
	}

	public static String compressString(String str) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] input = str.getBytes();
			GZIPOutputStream gzip = new GZIPOutputStream(bos);
			gzip.write(input, 0, input.length);
			gzip.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] compressed = bos.toByteArray();
		String result = new String(Base64.getEncoder().encode(compressed));
		return result;
	}

	public static String decompressString(String str) {
		String result = "";
		try {
			byte[] compressed = Base64.getDecoder().decode(str);
			ByteArrayInputStream bos = new ByteArrayInputStream(compressed);

			GZIPInputStream os = new GZIPInputStream(bos);
			ByteArrayOutputStream byteout = new java.io.ByteArrayOutputStream();

			int res = 0;
			byte buf[] = new byte[1024];
			while (res >= 0) {
				res = os.read(buf, 0, buf.length);
				if (res > 0) {
					byteout.write(buf, 0, res);
				}
			}

			byte[] decompressed = byteout.toByteArray();
			result = new String(decompressed);
		} catch (Exception e) {

		}
		return result;

	}
}

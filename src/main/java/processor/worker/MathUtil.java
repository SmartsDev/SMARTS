package processor.worker;

import java.util.ArrayList;

public class MathUtil {
	public static double computeAccuracy(final double v, final double base) {
		if (Math.abs(v - base) < base) {
			return 1 - (Math.abs(v - base) / base);
		} else {
			return 0;
		}
	}

	public static double computeAvg(final ArrayList<Double> data) {
		double accumulated = 0;
		for (final double acc : data) {
			accumulated += acc;
		}
		return accumulated / data.size();
	}

	public static double computeStdev(final ArrayList<Double> data) {
		double average = 0;
		double stdev = 0;

		if (data.size() > 0) {
			average = computeAvg(data);
			double devSquare = 0;
			for (final double acc : data) {
				devSquare += Math.pow(acc - average, 2);
			}
			stdev = Math.sqrt(devSquare / data.size());
			return stdev;
		} else {
			return 0;
		}
	}
}

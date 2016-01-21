package mgbtest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LevenWER {

	public static int getLevenshteinDistance(String s, String t) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}

		int n = s.length(); // length of s
		int m = t.length(); // length of t

		if (n == 0) {
			return m;
		} else if (m == 0) {
			return n;
		}

		if (n > m) {
			// swap the input strings to consume less memory
			String tmp = s;
			s = t;
			t = tmp;
			n = m;
			m = t.length();
		}

		int p[] = new int[n + 1]; // 'previous' cost array, horizontally
		int d[] = new int[n + 1]; // cost array, horizontally
		int _d[]; // placeholder to assist in swapping p and d

		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t

		char t_j; // jth character of t

		int cost; // cost

		for (i = 0; i <= n; i++) {
			p[i] = i;
		}

		for (j = 1; j <= m; j++) {
			t_j = t.charAt(j - 1);
			d[0] = j;

			for (i = 1; i <= n; i++) {
				cost = s.charAt(i - 1) == t_j ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left
				// and up +cost
				d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
			}

			// copy current distance counts to 'previous row' distance counts
			_d = p;
			p = d;
			d = _d;
		}

		// our last action in the above loop was to switch d and p, so p now
		// actually has the most recent cost counts
		return p[n];
	}

	public static double getWordErrorRate(int levenshteinDistance, int numOfSegmentWords) {
		return round((double) levenshteinDistance / (double) numOfSegmentWords, 2);
	}

	/*
	 * This method returns the AWD as a double data type, rounding it off to two
	 * numbers after decimal: Average Word Duration
	 */
	public static double calculateAWD(double duration, double numOfSegmentWords) {

		return round(numOfSegmentWords / duration, 2);

	}

	/*
	 * Method to round a double number to upto two numbers after the decimal
	 * E.g. 89.34
	 */
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	/*
	 * Get the time in seconds. E.g. 56.89 seconds. Argument is the start or the
	 * end time array
	 */
	public static double getTime(double[] timeArr) {
		double time = timeArr[0] * 3600 + timeArr[1] * 60 + timeArr[2];
		return time;

	}

	/*
	 * Converts a string array to a double
	 */
	public static double[] getTimeArr(String[] timeStr) throws NumberFormatException {
		double[] timeArr = new double[3];
		for (int i = 0; i < timeStr.length; i++) {
			timeArr[i] = Double.parseDouble(timeStr[i]);

		}
		return timeArr;
	}

	public static void main(String[] args) {

		/*
		 * Levenshtein distance test, String converted to bukwalter
		 */
		String s = "s$wvvq";
		String t = "A$wyg";
		System.out.println("++++ Calculated Levenshteing Distance++++ " + getLevenshteinDistance(s, t));
		System.out.println("++++++ Calculated the Word Error Rate++++ "
				+ (getWordErrorRate(getLevenshteinDistance(s, t), s.length())) * 100 + "%");

		/*
		 * AWD calculation: Assuming that one line in the ALL.tra is of the form
		 * as given below
		 */

		String startTime = "00:12:05,94";
		String endTime = "00:12:11,41";
		String segmentString = "يلجأ النظام السوري إلى استخدام ذخائر عنقودية لقمع الاحتجاجا";
		// String segmentStringBuk = ArabicUtils.utf82buck(segmentString);
		double[] startTimeArr = getTimeArr(startTime.replace(",", ".").split(":"));
		double[] endTimeArr = getTimeArr(endTime.replace(",", ".").split(":"));
		double startTimeD = getTime(startTimeArr);
		double endTimeD = getTime(endTimeArr);
		double duration = round(Math.abs((endTimeD - startTimeD)), 2);
		double awd = calculateAWD(duration, segmentString.split(" ").length);
		System.out.println("+++ Segment Duration++ " + duration);
		System.out.println("+++++ Segment String length+++ " + segmentString.split(" ").length);
		System.out.println("+++++AWD is Given by+++++ " + awd);
	}

}

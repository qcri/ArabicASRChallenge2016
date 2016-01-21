package mgbtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mgbbeans.SegmentBean;

public class TestAlignment {

	public static void main(String[] args) {
		SegmentBean segmentBean = new SegmentBean();
		setSegmentAttributes(
				"00152FDA-5AB7-4B47-993C-AC8AC4D8C3AB.xml_xdyjp-bn-qnp_00:00:05,94_00:00:11,41 يلجأ النظام السوري إلى استخدام ذخائر عنقودية لقمع الاحتجاجات	Words:9 Correct:7	Correct%:77%	Ins:1	Del:3	WMER:66.0%	PMER:24.3%	AWD:0.15	Start:0 End:1",
				segmentBean);
		System.out.println(segmentBean);

	}

	private static void setSegmentAttributes(String currentSegment, SegmentBean segmentBean) {
		String segmentID = null;
		String speakerName = null;
		String startTime = null;
		String endTime = null;
		String[] words = null;
		String wordMatchErrorRate = null;
		String phonemeMatchErrorRate = null;
		String awd = null;
		String transcriptString = null;

		String[] tabSplittedSegment = currentSegment.split("\t");
		segmentID = tabSplittedSegment[0].split("\\.xml")[0].replaceAll("-", "_");
		wordMatchErrorRate = tabSplittedSegment[5].split(":")[1];
		phonemeMatchErrorRate = tabSplittedSegment[6].split(":")[1];
		awd = tabSplittedSegment[7].split(":")[1];

		String segment = tabSplittedSegment[0].split("\\.xml")[1];
		String line = segment;
		String pattern = "(.*?)(\\d\\d:.*?)\\s(.*)";
		Pattern r = Pattern.compile(pattern);

		Matcher m = r.matcher(line);

		if (m.find()) {
			speakerName = m.group(1).replaceAll("_", " ").trim().replaceAll("-", " ");
			startTime = getTime(getTimeArrDouble(m.group(2).split("_")[0].replace(",", ".").split(":")));
			endTime = getTime(getTimeArrDouble(m.group(2).split("_")[1].replace(",", ".").split(":")));
			transcriptString = m.group(3);
			words = m.group(3).split(" ");
		}

		segmentBean.setId(segmentID);
		segmentBean.setSpeakerName(speakerName);
		segmentBean.setStartTime(startTime);
		segmentBean.setEndTime(endTime);

		segmentBean.setTranscriptString(transcriptString);
		segmentBean.setWordMatchErrorRate(wordMatchErrorRate);
		segmentBean.setGraphemeMatchErrorRate(phonemeMatchErrorRate);
		segmentBean.setAwd(awd);

	}

	private static String getTime(double[] timeArr) {
		double time = round((timeArr[0] * 3600 + timeArr[1] * 60 + timeArr[2]), 2);
		return Double.toString(time).trim();
	}

	private static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	private static double[] getTimeArrDouble(String[] timeStr) throws NumberFormatException {
		double[] timeArr = new double[3];
		for (int i = 0; i < timeStr.length; i++) {

			timeArr[i] = Double.parseDouble(timeStr[i]);

		}
		return timeArr;
	}

	public static String[] charAlign(String a, String b) {
		int[][] T = new int[a.length() + 1][b.length() + 1];

		for (int i = 0; i <= a.length(); i++)
			T[i][0] = i;

		for (int i = 0; i <= b.length(); i++)
			T[0][i] = i;

		for (int i = 1; i <= a.length(); i++) {
			for (int j = 1; j <= b.length(); j++) {
				if (a.charAt(i - 1) == b.charAt(j - 1))
					T[i][j] = T[i - 1][j - 1];
				else
					T[i][j] = Math.min(T[i - 1][j], T[i][j - 1]) + 1;
			}
		}

		StringBuilder aa = new StringBuilder(), bb = new StringBuilder();

		for (int i = a.length(), j = b.length(); i > 0 || j > 0;) {
			if (i > 0 && T[i][j] == T[i - 1][j] + 1) {
				aa.append(a.charAt(--i));
				bb.append("-");
			} else if (j > 0 && T[i][j] == T[i][j - 1] + 1) {
				bb.append(b.charAt(--j));
				aa.append("-");
			} else if (i > 0 && j > 0 && T[i][j] == T[i - 1][j - 1]) {
				aa.append(a.charAt(--i));
				bb.append(b.charAt(--j));
			}
		}

		return new String[] { aa.reverse().toString(), bb.reverse().toString() };
	}

}

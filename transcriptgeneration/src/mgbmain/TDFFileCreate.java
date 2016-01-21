package mgbmain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import mgbbeans.SegmentBean;

public class TDFFileCreate {

	private static ListMultimap<String, SegmentBean> segmentMap;
	private static List<SegmentBean> badSegmentsList;
	private static int nfe;
	private static int i;

	static {
		badSegmentsList = new ArrayList<SegmentBean>();
		segmentMap = ArrayListMultimap.create();
	}

	public static void main(String... args) {
		BufferedReader br = null;
		SegmentBean segmentBean = null;
		String sCurrentLine;

		try {
			br = new BufferedReader(new FileReader("/Users/alt-sameerk/Documents/tra-2016-01-04/ALL.tra"));
			while ((sCurrentLine = br.readLine()) != null) {
				segmentBean = new SegmentBean();
				try {

					setSegmentBeanAttributes(segmentBean, sCurrentLine.split("\t")[0].split(".xml")[0],
							sCurrentLine.split("\t")[0].split(".xml")[1], sCurrentLine.split("\t"));

				} catch (NumberFormatException ne) {
					// TODO
				}

				if (checkSegmentBean(segmentBean) == 1) {
					// System.out.println(segmentBean.toString());
					populateMultiMap(segmentBean);
				} else {
					// System.out.println(sCurrentLine);
					badSegmentsList.add(segmentBean);
				}

				// break;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// removeBadSegmentsFromMap();
		serializeSegmentsMapObject();
		createTDFFiles();
		writeBadSegmentsToFile(badSegmentsList);
	}

	private static void writeBadSegmentsToFile(List<SegmentBean> badSegmentsList) {
		BufferedWriter bw = null;
		try {

			for (SegmentBean badBean : badSegmentsList) {
				bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/badSegments.txt", true));
				bw.write(badBean.getId().trim() + "_speaker_" + badBean.getSpeakerName() + "_align "
						+ badBean.getStartTime() + " " + badBean.getEndTime() + " "
						+ badBean.getTranscriptString().trim() + "\n");
				bw.close();
			}
		} catch (Exception e) {

		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void createTDFFiles() {

		ObjectInputStream ooi = null;
		BufferedWriter bw = null;
		List<SegmentBean> segmentBeanList = null;
		ListMultimap<String, SegmentBean> segmentsMap = ArrayListMultimap.create();

		try {
			ooi = new ObjectInputStream(new FileInputStream(System.getProperty("user.dir") + "/segmentMap.ser"));
			segmentsMap = (ListMultimap<String, SegmentBean>) ooi.readObject();
			for (String key : segmentsMap.keySet()) {
				segmentBeanList = segmentsMap.get(key);
				for (SegmentBean segmentBean : segmentBeanList) {
					try {
						bw = new BufferedWriter(new FileWriter(
								"/Users/alt-sameerk/Documents/tra_normalized/" + segmentBean.getId() + ".tra", true));
						String writeString = segmentBean.getId().trim() + "\t" + segmentBean.getSpeakerName().trim()
								+ "\t" + segmentBean.getStartTime() + "\t" + segmentBean.getEndTime() + "\t"
								+ segmentBean.getWordMatchErrorRate() + "\t" + segmentBean.getTranscriptString().trim()
								+ "\n";
						// System.out.println(writeString);
						bw.write(writeString);
					} finally {
						bw.close();
					}
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	private static void serializeSegmentsMapObject() {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.dir") + "/segmentMap.ser"));
			oos.writeObject(segmentMap);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (oos != null) {
				try {
					oos.flush();
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	// private static void removeBadSegmentsFromMap() {
	// for (String badPrograms : badSegmentsList) {
	// segmentMap.removeAll(badPrograms);
	// }
	//
	// }

	private static int checkSegmentBean(SegmentBean segmentBean) {
		if (segmentBean.getTranscriptString() != null && segmentBean.getStartTime() != null
				&& segmentBean.getTranscriptString() != null) {
			return 1;
		} else {
			return 0;
		}

	}

	private static void populateMultiMap(SegmentBean segmentBean) {
		// System.out.println(segmentMap);
		// System.out.println(segmentBean.getId());
		segmentMap.put(segmentBean.getId(), segmentBean);
	}

	private static void setSegmentBeanAttributes(SegmentBean segmentBean, String id, String infoString,
			String[] strings) throws NumberFormatException {
		setMatchRate(strings, segmentBean);
		String[] splittedInfoString = splitInfoString(infoString);
		if (id.equals("A7FD87DD-5824-4AD6-96B1-8B61AFA1577D")) {
			// System.out.println(id);
		}
		segmentBean.setId(id);
		String[] attributeInfo = getAttributeInformation(splittedInfoString);

		if (!Arrays.asList(attributeInfo).contains("") && !Arrays.asList(attributeInfo).contains(null)) {
			segmentBean.setSpeakerName(attributeInfo[0]);
			segmentBean.setStartTime(attributeInfo[1]);
			segmentBean.setEndTime(attributeInfo[2]);

			segmentBean.setTranscriptString(attributeInfo[4]);
		}
	}

	private static void setMatchRate(String[] strings, SegmentBean segmentBean) {
		for (String s : strings) {
			if (s.contains("Match%")) {
				segmentBean.setWordMatchErrorRate((Double.toString(
						round(Double.parseDouble(s.split(":")[1].substring(0, s.split(":")[1].indexOf("%"))), 2))));
			}
		}

	}

	private static String[] getAttributeInformation(String[] splittedInfoString) throws NumberFormatException {

		String[] attributeInfo = new String[5];
		String speakerName = null;
		String startTime = null;
		String endTime = null;
		String transcription = null;
		String duration = null;

		if (splittedInfoString[0] != null && !splittedInfoString[0].isEmpty()) {
			speakerName = splittedInfoString[0].replaceAll("_", " ").trim().replaceAll(" ", "_");
			attributeInfo[0] = speakerName;
		}

		if (splittedInfoString[1] != null && !splittedInfoString[1].isEmpty()) {

			startTime = getTime(getTimeArr(splittedInfoString[1].split("_")[0].replace(",", ".").split(":")));
			endTime = getTime(getTimeArr(splittedInfoString[1].split("_")[1].replace(",", ".").split(":")));
			duration = getDuration(startTime, endTime);

		}

		if (splittedInfoString[1] != null && !splittedInfoString[2].isEmpty()) {
			BufferedWriter bw = null;
			BufferedWriter bwEng = null;

			transcription = getNormalisedTranscription(splittedInfoString[2]);
			if (!transcription.equals(splittedInfoString[2]) && !transcription.equals(" ")) {
				// System.out.println("Inside other if *******");
				// System.out.println(transcription);
				try {
					bw = new BufferedWriter(
							new FileWriter(System.getProperty("user.dir") + "/normalizedSegments", true));
					bw.write(splittedInfoString[2] + "\t" + transcription + "\n");
				} catch (IOException e) {

					e.printStackTrace();
				} finally {
					try {
						bw.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (transcription.equals(" ")) {
				System.out.println("inside transcription empty *****");
				try {
					bwEng = new BufferedWriter(
							new FileWriter(System.getProperty("user.dir") + "/englishSegments", true));
					bwEng.write(splittedInfoString[2] + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						bwEng.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (transcription != null && !transcription.equals(" ") && duration != null
				&& Double.parseDouble(duration) > 0.0 && !transcription.isEmpty()) {
			attributeInfo[1] = startTime;
			attributeInfo[2] = endTime;
			attributeInfo[3] = duration;
			attributeInfo[4] = transcription;
		} else {
			i++;
		}
		return attributeInfo;
	}

	private static String getNormalisedTranscription(String segmentTranscription) {
		String pattern = "[A-Za-z]";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(segmentTranscription);

		/*
		 * if we have english we do not want to include the segment
		 */
		if (m.find()) {
			System.out.println("english" + segmentTranscription);
			return " ";
		} else {
			return segmentTranscription.replaceAll("[^\\sء-ي0-9]", "");
		}
	}

	private static String getDuration(String startTime, String endTime) {

		return Double.toString(round((Double.parseDouble(endTime) - Double.parseDouble(startTime)), 2));
	}

	private static String getTime(double[] timeArr) {

		double time = round((timeArr[0] * 3600 + timeArr[1] * 60 + timeArr[2]), 2);
		return Double.toString(time).trim();

	}

	/*
	 * http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-
	 * places
	 */
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	private static double[] getTimeArr(String[] timeStr) throws NumberFormatException {
		double[] timeArr = new double[3];
		for (int i = 0; i < timeStr.length; i++) {

			timeArr[i] = Double.parseDouble(timeStr[i]);

		}
		return timeArr;
	}

	private static String[] splitInfoString(String infoString) {
		String[] matchedStrings = new String[3];
		String pattern = "(.*?)(\\d.*?)\\s(.*)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(infoString);
		// System.out.println(m.groupCount());
		if (m.find() && (m.groupCount() == 3)) {
			matchedStrings[0] = m.group(1);
			matchedStrings[1] = m.group(2);
			matchedStrings[2] = m.group(3);
		}

		return matchedStrings;

	}

}

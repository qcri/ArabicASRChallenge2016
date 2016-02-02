package mgbutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedHashMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import mgbbeans.ProgramBean;
import mgbbeans.SegmentBean;

/**
 * This class is used to create the xml files corresponding to the wav files
 * 
 * @author Sameer Khurana (skhurana@qf.org.qa)
 *
 */
public class GenerateXMLTranscription {

	private static ListMultimap<String, SegmentBean> segmentMap;
	private static ListMultimap<String, String> speakersMap;
	private static LinkedHashMap<String, ProgramBean> programInfoMap;

	static {
		segmentMap = ArrayListMultimap.create();
		speakersMap = ArrayListMultimap.create();
		programInfoMap = MGBUtil.getALJProgramInformation("/Users/alt-sameerk/Downloads/aja_tdf");

	}

	public static void createTranscript(String textTranscriptFilePath, String mgbFolder)
			throws IllegalArgumentException, IllegalAccessException {
		BufferedReader br = null;
		String currentSegment;
		try {

			br = new BufferedReader(new FileReader(textTranscriptFilePath));
			while ((currentSegment = br.readLine()) != null) {

				SegmentBean segmentBean = new SegmentBean();

				MGBUtil.setSegmentAttributes(currentSegment, segmentBean);

				if (MGBUtil.checkSegmentBean(segmentBean) == 1) {
					populateSegmentMap(segmentBean);
				} else {
					MGBUtil.writeSegmentsToFile(segmentBean, System.getProperty("user.dir") + "/badSegments");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		speakersMap = MGBUtil.populateSpeakersMap(segmentMap);

		MGBUtil.generateXML(segmentMap, speakersMap, programInfoMap, mgbFolder);
	}

	private static void populateSegmentMap(SegmentBean segmentBean) {
		segmentMap.put(segmentBean.getId(), segmentBean);
	}

}

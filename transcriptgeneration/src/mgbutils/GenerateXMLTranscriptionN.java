package mgbutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import mgbbeans.ProgramBean;
import mgbbeans.SegmentBean;

/**
 * Logic to generate the transcript xmls
 * 
 * @author Sameer Khurana (skhurana@qf.org.qa)
 *
 */
public class GenerateXMLTranscriptionN {

	/**
	 * Object that stores the information about each segment in the ALL.tra.
	 * Information like starttime, endtime, transcript, etc.
	 */
	private ListMultimap<String, SegmentBean> segmentMap = ArrayListMultimap.create();
	/**
	 * Stores the information about the speakers in a particular program
	 */
	private ListMultimap<String, String> speakersMap = ArrayListMultimap.create();
	/**
	 * Information about the programs like publishing date etc.
	 */
	private LinkedHashMap<String, ProgramBean> programInfoMap;

	/**
	 * Method to generate the xml transcript files given the ALL.tra and the
	 * program info tdf file
	 * 
	 * @param textTranscriptFilePath
	 *            - ALL.tra
	 * @param mgbFolder
	 *            - the folder where xml transcript files are to be stored
	 * @param programInfoFilePath
	 *            - the tdf which contains ALJ program information
	 * @param removeDoubleHash
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public void createTranscript(String textTranscriptFilePath, String mgbFolder, String programInfoFilePath,
			String normalizeFlag, String bukwalterFlag, String mapNumbersFlag, String transcriptType,
			String removeDiacritics, String annotateLatinWords, String removeBadSegments, String removeHesitation,
			String removeDoubleHash) throws IllegalArgumentException, IllegalAccessException, IOException {

		/*
		 * Getting the program info map, to get program date and some other info
		 */
		programInfoMap = MGBUtil.getALJProgramInformation(programInfoFilePath, bukwalterFlag);

		BufferedReader br = null;
		String currentSegment;
		try {

			br = new BufferedReader(new FileReader(textTranscriptFilePath));
			while ((currentSegment = br.readLine()) != null) {

				SegmentBean segmentBean = new SegmentBean();

				MGBUtil.setSegmentAttributes(currentSegment, segmentBean, normalizeFlag, removeHesitation);

				if (MGBUtil.checkSegmentBean(segmentBean) == 1) {
					populateSegmentMap(segmentBean);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		speakersMap = MGBUtil.populateSpeakersMap(segmentMap);

		MGBUtil.generateXML(segmentMap, speakersMap, programInfoMap, mgbFolder, bukwalterFlag, mapNumbersFlag,
				transcriptType, removeDiacritics, annotateLatinWords, removeHesitation, removeDoubleHash);
	}

	private void populateSegmentMap(SegmentBean segmentBean) {
		segmentMap.put(segmentBean.getId(), segmentBean);
	}

}

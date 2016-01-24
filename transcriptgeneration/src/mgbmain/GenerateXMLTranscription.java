package mgbmain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import mgbbeans.ProgramBean;
import mgbbeans.SegmentBean;
import mgbutils.MGBUtil;

/**
 * This class is used to create the xml files corresponding to the wav files
 * 
 * @author alt-sameerk
 *
 */
public class GenerateXMLTranscription {

	private static ListMultimap<String, SegmentBean> segmentMap;
	private static ListMultimap<String, String> speakersMap;
	private static LinkedHashMap<String, ProgramBean> programInfoMap;

	static {
		segmentMap = ArrayListMultimap.create();
		speakersMap = ArrayListMultimap.create();
		programInfoMap = MGBUtil.getALJProgramInformation("D:\\Speech\\ArabicASRChallenge\\exp-2015-11-10\\aja_tdf.txt"); // "/Users/alt-sameerk/Downloads/aja_tdf");

	}

	public static void createTranscript(String textTranscriptFilePath)
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		speakersMap = MGBUtil.populateSpeakersMap(segmentMap);

		MGBUtil.generateXML(segmentMap, speakersMap, programInfoMap, "D:\\Speech\\ArabicASRChallenge\\ArabicASRChallenge\\"); // /Users/alt-sameerk/Documents/tra_xml/xml_hamdy/");
	}

	private static void populateSegmentMap(SegmentBean segmentBean) {
		segmentMap.put(segmentBean.getId(), segmentBean);
	}

}

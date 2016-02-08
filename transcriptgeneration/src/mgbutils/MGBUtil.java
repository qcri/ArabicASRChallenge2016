package mgbutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import mgbbeans.ProgramBean;
import mgbbeans.SegmentBean;


public class MGBUtil {

	public static LinkedHashMap<String, ProgramBean> getALJProgramInformation(String ajFileURL) {

		System.out.println("+++CREATING ALJ PROGRAM MAP++++");
		LinkedHashMap<String, ProgramBean> programInfoMap = new LinkedHashMap<String, ProgramBean>();
		BufferedReader reader = null;
		ProgramBean programBean = null;

		try {
                        //Serial, EntityID, ArticleGuid, ArticleTitle, ProgramName, FriendlyURL
			
                    reader = new BufferedReader(new FileReader(new File(ajFileURL)));
			String currentLine;
			currentLine = reader.readLine();
                        while ((currentLine = reader.readLine()) != null) {
				programBean = new ProgramBean();
				String[] splittedTDF = currentLine.split("\t");
				programBean.setTitle(ArabicUtils.utf82buck(splittedTDF[4]));
				programBean.setName(splittedTDF[3]);
				programBean.setDate(getDate(splittedTDF[5]));
				programInfoMap.put(splittedTDF[2].replaceAll("-", "_"), programBean);
			}

			System.out.println("+++Done with the Program Map+++");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return programInfoMap;

	}

	public static String getDate(String programURL) {
		String date = null;
		String pattern = "/[0-9].*/";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(programURL);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		if (m.find()) {
			try {
				date = formatter.format(formatter.parse(m.group(0).replaceAll("/", " ").trim().replaceAll(" ", "/")));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return date;
	}

	public static void writeSegmentsToFile(SegmentBean segmentBean, String newFileLocation) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(new File(newFileLocation), true));
			bw.write(segmentBean.toString() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void generateXML(ListMultimap<String, SegmentBean> segmentMap,
			ListMultimap<String, String> speakersMap, LinkedHashMap<String, ProgramBean> programInfoMap,
			String xmlTranscriptDestinationFolderPath) {

		try {
                        int size;

                        size = segmentMap.keySet().size();
			for (String programID : segmentMap.keySet()) {

				List<SegmentBean> segmentBeans = segmentMap.get(programID);
				List<String> speakerNames = speakersMap.get(programID);

				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

				Document doc = docBuilder.newDocument();
				Element rootElement = doc.createElement("transcript");
				rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				rootElement.setAttribute("xsi:noNamespaceSchemaLocation", "transcript.xsd");

				doc.appendChild(rootElement);

				Element head = doc.createElement("head");
				rootElement.appendChild(head);

				Element recording = doc.createElement("recording");
				ProgramBean programBean = programInfoMap.get(programID);
				recording.setAttribute("title", programBean.getTitle());
				recording.setAttribute("episode", "unknown");
				recording.setAttribute("date", programBean.getDate());
				recording.setAttribute("time", "unknown");
				recording.setAttribute("service_name", "Al Jazeera");
				recording.setAttribute("type", "unknown");
				recording.setAttribute("genre", "unknown");
				recording.setAttribute("filename", programID.replaceAll("_", "-"));
				recording.setAttribute("detail", "unknown");

				head.appendChild(recording);

				Element annotations = doc.createElement("annotations");
				head.appendChild(annotations);

				Element annotation = doc.createElement("annotation");
				annotation.setAttribute("id", "transcript_align");
				annotation.setAttribute("type", "automatic");
				annotation.setAttribute("date", "unknown");
				annotation.setAttribute("detail", "forced-alignment of the original transcription");
				annotations.appendChild(annotation);

				Element annotation1 = doc.createElement("annotation");
				annotation1.setAttribute("id", "transcript_orig");
				annotation1.setAttribute("type", "unknown");
				annotation1.setAttribute("date", "unknown");
				annotation1.setAttribute("detail",
						"Output of the parsing of the original transcription files provided by the AL Jazeera");
				annotations.appendChild(annotation1);

				Element annotation2 = doc.createElement("annotation");
				annotation2.setAttribute("id", "transcript_lsdecode");
				annotation2.setAttribute("type", "automatic");
				annotation2.setAttribute("date", "unknown");
				annotation2.setAttribute("detail", "output of the lightly supervised decoding");
				annotations.appendChild(annotation2);

				Element speakers = doc.createElement("speakers");
				head.appendChild(speakers);

				for (int i = 0; i < speakerNames.size(); i++) {
					Element speaker = doc.createElement("speaker");
					speakers.appendChild(speaker);
					speaker.setAttribute("id", programID + "_speaker" + (i + 1) + "_align");
					speaker.setAttribute("name", speakerNames.get(i));
				}

				Element body = doc.createElement("body");
				rootElement.appendChild(body);

				Element segments = doc.createElement("segments");
				segments.setAttribute("annotation_id", "transcript_align");
				body.appendChild(segments);

				int wordCount = 1;
				int utteranceNumber = 1;
				for (int k = 0; k < segmentBeans.size(); k++) {
					SegmentBean segmentBean = segmentBeans.get(k);
					Element segment = doc.createElement("segment");
					segment.setAttribute("id", segmentBean.getId() + "_utt_" + utteranceNumber + "_align");
					segment.setAttribute("who", segmentBean.getId() + "_speaker"
							+ (speakerNames.indexOf(segmentBean.getSpeakerName()) + 1) + "_align");
					segment.setAttribute("WMER", segmentBean.getWordMatchErrorRate());
					segment.setAttribute("PMER", segmentBean.getGraphemeMatchErrorRate());
					segment.setAttribute("AWD", segmentBean.getAwd());
					segment.setAttribute("starttime", segmentBean.getStartTime());
					segment.setAttribute("endtime", segmentBean.getEndTime());
					segments.appendChild(segment);

					String[] words = segmentBean.getTranscriptString().split(" ");
					for (int j = 0; j < words.length; j++) {
						Element element = doc.createElement("element");
						element.setAttribute("type", "word");
						element.setAttribute("id", segmentBean.getId() + "_w" + wordCount + "_align");
                                                if(words[j]!=null && !words[j].isEmpty())
                                                {
                                                  //System.out.println(words[j]);
						  element.appendChild(doc.createTextNode(words[j]));
						  segment.appendChild(element);
						  wordCount++;
                                                }
					}

					utteranceNumber++;
				}

				try {
					prettyPrint(doc, programID, xmlTranscriptDestinationFolderPath);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

	}

	public static final void prettyPrint(Document doc, String programID, String xmlTraDestinationFolderPath)
			throws Exception {

		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new FileWriter(new File(xmlTraDestinationFolderPath + programID.replaceAll("_", "-") + ".xml"));
		tf.transform(new DOMSource(doc), new StreamResult(out));

	}

	public static ListMultimap<String, String> populateSpeakersMap(ListMultimap<String, SegmentBean> segmentMap) {
		ListMultimap<String, String> speakersMap = ArrayListMultimap.create();
		for (String segmentID : segmentMap.keySet()) {
			List<SegmentBean> segmentBeans = segmentMap.get(segmentID);
			for (SegmentBean segmentBean : segmentBeans) {
				String speaker = segmentBean.getSpeakerName();
				List<String> speakers = speakersMap.get(segmentID);
				if (speakers != null && !speakers.contains(speaker)) {
					speakersMap.put(segmentID, speaker);
				}
			}

		}

		return speakersMap;

	}

	public static void setSegmentAttributes(String currentSegment, SegmentBean segmentBean) {
		String segmentID = null;
		String speakerName = null;
		String startTime = null;
		String endTime = null;
		String wordMatchErrorRate = null;
		String phonemeMatchErrorRate = null;
		String awd = null;
		String transcriptString = null;

		String[] tabSplittedSegment = currentSegment.split("\t");
		segmentID = tabSplittedSegment[0].split("\\.xml")[0].replaceAll("-", "_");
		wordMatchErrorRate = tabSplittedSegment[5].split(":")[1];
		phonemeMatchErrorRate = tabSplittedSegment[6].split(":")[1];
		if (tabSplittedSegment[7].split(":").length > 1) {
			awd = tabSplittedSegment[7].split(":")[1];
		}

		String segment = tabSplittedSegment[0].split("\\.xml")[1];
		String line = segment;
		String pattern = "(.*?)(\\d\\d:.*?)\\s(.*)";
		Pattern r = Pattern.compile(pattern);

		Matcher m = r.matcher(line);

		if (m.find()) {
			speakerName = m.group(1).replaceAll("_", " ").trim().replaceAll("-", " ");
			String startTimeStr = m.group(2).split("_")[0];
			startTime = Double.toString(getTime(startTimeStr));
			String endTimeStr = m.group(2).split("_")[1];
			endTime = Double.toString(getTime(endTimeStr));
			transcriptString = m.group(3);
		}

		segmentBean.setId(segmentID);
		segmentBean.setSpeakerName(speakerName);
		segmentBean.setStartTime(startTime);
		segmentBean.setEndTime(endTime);
		if (transcriptString.matches("[A-Za-z]")) {
			segmentBean.setTranscriptString("");
		} else {
			segmentBean.setTranscriptString(getNormaliseBukTranscriptString(transcriptString));
		}
		segmentBean.setWordMatchErrorRate(wordMatchErrorRate);
		segmentBean.setGraphemeMatchErrorRate(phonemeMatchErrorRate);
		segmentBean.setAwd(awd);

	}

	public static String getNormaliseBukTranscriptString(String transcriptString) {
		// String normalised = transcriptString.replaceAll("[،؟:/!,؛\"]",
		// "").replaceAll("[^0-9٠-٩]\\.[^0-9٠-٩]", "")
		// .replaceAll("[\\(\\)\\[\\]_]", "").replaceAll("\\s\\s^\n", "
		// ").replaceAll("[-\\.$]", "")
		// .replaceAll("[A-Za-z]", "").replaceAll("(?m)^[\t]*\r?\n", "");
		String normalised = transcriptString.replaceAll("[،؟:/!,؛\"]", "").replaceAll("[^0-9٠-٩]\\.[^0-9٠-٩]", "")
				.replaceAll("[\\(\\)\\[\\]_]", "").replaceAll("\\s\\s^\n", " ").replaceAll("[-\\.$]", "")
				.replaceAll("[A-Za-z]", "").replaceAll("(?m)^[\t]*\r?\n", "");
		String buk = ArabicUtils.utf82buck(normalised);
		return buk;
	}

	public static double getTime(String timeString) {
            
            double[] timeArr = new double[3];
            try
            {
                boolean breakpoint;
		String[] timeStr = timeString.replace(",", ".").split(":");

		for (int i = 0; i < timeStr.length; i++) {
                            timeArr[i] = Double.parseDouble(timeStr[i]);
		}
            }
            catch (NumberFormatException e)
            {
                System.out.println(timeString);
                e.printStackTrace();

            }
            /*catch (Exception e)
            {
                System.out.println(timeString);
                //e.printStackTrace();
            }*/
            
		double time = round((timeArr[0] * 3600 + timeArr[1] * 60 + timeArr[2]), 2);
		return time;
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static int checkSegmentBean(SegmentBean segmentBean)
			throws IllegalArgumentException, IllegalAccessException {
		int flag = 1;

		for (Field f : segmentBean.getClass().getDeclaredFields()) {
			f.setAccessible(true);
			if (f.get(segmentBean) == null) {
				flag = 0;
			} else if (f.get(segmentBean).toString().isEmpty()) {
				flag = 0;
			}
		}

		return flag;
	}

	public static void fixTraSpeakers(String badSpeakerNamesTraFilePath, String newTraFilePath) {

		BufferedReader br = null;
		String sCurrentLine;

		try {
			String modifiedLine = null;
			br = new BufferedReader(new FileReader(badSpeakerNamesTraFilePath));
			while ((sCurrentLine = br.readLine()) != null) {
				String modifiedSegmentID = null;
				String segmentID = sCurrentLine.split("\t")[0];
				String[] restInfo = Arrays.copyOfRange(sCurrentLine.split("\t"), 1, sCurrentLine.split("\t").length);

				String pattern = "(.*?)(\\d\\d:.*?)\\s(.*)";
				Pattern r = Pattern.compile(pattern);
				Matcher m = r.matcher(segmentID.split("\\.xml")[1]);

				String modifiedSpeaker = null;
				if (m.find()) {
					modifiedSpeaker = m.group(1).replaceAll("_", " ").trim().replaceAll(" ", "-") + "_" + m.group(2)
							+ " " + m.group(3);
					modifiedSegmentID = segmentID.split(".xml")[0] + ".xml" + "_" + modifiedSpeaker;
					modifiedLine = modifiedSegmentID;
					for (String s : restInfo) {

						modifiedLine = modifiedLine + "\t" + s;
					}
					writeStringToFile(modifiedLine, newTraFilePath);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void writeStringToFile(String modifiedLine, String filePath) {

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(filePath, true));
			bw.write(modifiedLine + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public static String calculateAWD(String startTime, String endTime, int numOfSegmentWords) {

		double startTimeD = MGBUtil.getTime(startTime);
		double endTimeD = MGBUtil.getTime(endTime);
		double duration = MGBUtil.round(Math.abs((endTimeD - startTimeD)), 2);
		if (duration > 0.0) {
			return Double.toString(MGBUtil.round((double) numOfSegmentWords / duration, 2));
		} else {
			return "";
		}

	}

	public static String normalizeWord(String t) {
		String s;

		s = t;

		s = s.replaceAll("أ", "ا");
		s = s.replaceAll("إ", "ا");
		s = s.replaceAll("آ", "ا");
		s = s.replaceAll("ى", "ي");
		s = s.replaceAll("ة", "ه");
		s = s.replaceAll("ؤ", "ء");
		s = s.replaceAll("ئ", "ء");

		s = ArabicUtils.removeDiacritics(s);

		return s;
	}

}

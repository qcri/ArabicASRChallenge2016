/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mgbmain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mgbutils.ArabicUtils;
import mgbutils.MGBUtil;
import mgbutils.WordSequenceAligner;
import mgbutils.WordSequenceAligner.Alignment;

/**
 *
 * @author hmubarak
 */
public class MGBTranscriptionComponent {
	final int MAX_SRT_WORDS_PER_LINE = 10;

	final boolean SPLIT_ON_SILENCE_ONLY = true;
	final boolean SPLIT_ON_MAX_LEN = false;
	final boolean SAVE_SPEAKER_NAME = false;

	final int MAX_SRT_CHAR_PER_LINE = 60; // -1
	final int EXTRA_WORDS_PER_LINE = 4;
	final float MAX_SILENCE_BETWEEN_WORDS = 0.15f; // 150 ms
	final float MIN_SILENCE_BETWEEN_SEGMENTS = 0.3f; // 300 ms
	final float MIN_SEGMENT_DURATION = 3.0f;
	final float MAX_SEGMENT_DURATION = 10.0f;
	final float MAX_WORD_DURATION = 1.0f;
	final int MAX_SPEAKER_NOF_WORDS = 15;

	final boolean VALIDATE_AJ_ONLY = false;// true;
	final boolean SAVE_EXTRACTED_CONTENTS = true;
	final boolean SAVE_DEBUG_INFO = true;
	final boolean CONSIDER_MP4_FILES_ONLY = false;// true;
	final boolean USE_REVISED_SPEAKERS = true;
	final int MAX_NOF_FILES = -1;// 100;//-1;//100;
	final float MIN_FILE_DURATION = 20.0f;
	final boolean USE_COLORS = false;
	final boolean GRAPHEME_ALIGN_FILES = true;
	final String[] SPEAKERS_COLORS = { "yellow", "green", "lightblue", "orange", "cyan", "indianred", "gold",
			"lightgray", "violet", "aliceblue" }; // http://www.w3schools.com/html/html_colornames.asp

	// Matching level
	final int ML_NO_MATCH = 0;
	final int ML_EXACT_MATCH = 1;
	final int ML_APPROX_MATCH = 2;

	int gNofErrors = 0, gNofSpecialChar = 0, gNofAllSegments = 0, gAllSegmentsLen = 0;
	int gProgramName = 0, gEpisodeTitle = 0, gEpisodeSpeaker = 0, gEpisodeGuest = 0, gEpisodeDate = 0,
			gEpisodeDirections = 0;

	public class SpeakerInfo {
		public String speakerName;
		public int count;

		public SpeakerInfo() {
			speakerName = "";
			count = 0;
		}
	}

	HashMap<String, Integer> NO_STOP_LIST_MAP = new HashMap<String, Integer>();

	HashMap<String, String> TIME_SORTED_LIST_MAP = new HashMap<String, String>();

	// Speakers after revision
	HashMap<String, String> SPEAKERS_LIST_MAP = new HashMap<String, String>();

	public class AjAsrWordInfo {
		String word, cleanWord, speaker;
		float startTime, duration, endTime;
		float startTimeSILBefore, durationSILBefore, endTimeSILBefore;
		boolean punct;
		int wordLen, mappingIndex, distance;
		int matchLevel;

		public AjAsrWordInfo() {
			word = "";
			cleanWord = "";
			speaker = "";

			startTime = 0.0f;
			duration = 0.0f;
			endTime = 0.0f;

			startTimeSILBefore = 0.0f;
			durationSILBefore = 0.0f;
			endTimeSILBefore = 0.0f;

			punct = false;
			wordLen = 0;
			mappingIndex = -1;
			distance = 0;

			matchLevel = ML_NO_MATCH;
		}

		public AjAsrWordInfo(AjAsrWordInfo w) {
			word = w.word;
			cleanWord = w.cleanWord;
			speaker = w.speaker;

			startTime = w.startTime;
			duration = w.duration;
			endTime = w.endTime;

			startTimeSILBefore = w.startTimeSILBefore;
			durationSILBefore = w.durationSILBefore;
			endTimeSILBefore = w.endTimeSILBefore;

			punct = w.punct;
			wordLen = w.wordLen;
			mappingIndex = w.mappingIndex;

			distance = w.distance;

			matchLevel = w.matchLevel;
		}
	}

	// Character types
	public final int SPRT = 0;
	public final int PNCT = 1;
	public final int NMBR = 2;
	public final int ELTR = 3;
	public final int ALTR = 4;
	public final int DIAC = 5;

	public final int[] iarMsCharacterType = {
			/* 0 1 2 3 4 5 6 7 8 9 A B C D E F */
			/* 0 */ SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT,
			SPRT, /* 0 */
			/* 1 */ SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT, SPRT,
			SPRT, /* 1 */
			/* 2 */ SPRT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT,
			PNCT, /* 2 */
			/* 3 */ NMBR, NMBR, NMBR, NMBR, NMBR, NMBR, NMBR, NMBR, NMBR, NMBR, PNCT, PNCT, PNCT, PNCT, PNCT,
			PNCT, /* 3 */
			/* 4 */ PNCT, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR,
			ELTR, /* 4 */
			/* 5 */ ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, PNCT, PNCT, PNCT, PNCT,
			PNCT, /* 5 */
			/* 6 */ PNCT, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR,
			ELTR, /* 6 */
			/* 7 */ ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, ELTR, PNCT, PNCT, PNCT, PNCT,
			PNCT, /* 7 */
			/* 8 */ SPRT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, SPRT, PNCT, PNCT, PNCT, PNCT,
			SPRT, /* 8 */
			/* 9 */ PNCT, PNCT, ELTR, PNCT, PNCT, PNCT, PNCT, PNCT, SPRT, PNCT, SPRT, PNCT, PNCT, SPRT, SPRT,
			SPRT, /* 9 */
			/* A */ SPRT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, SPRT, PNCT, PNCT, PNCT, PNCT,
			PNCT, /* A */
			/* B */ PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT, PNCT,
			PNCT, /* B */
			/* C */ PNCT, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR,
			ALTR, /* C */
			/* D */ ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, ALTR, PNCT, ALTR, ALTR, ALTR, ALTR, PNCT, ALTR, ALTR,
			ALTR, /* D */
			/* E */ PNCT, ALTR, PNCT, ALTR, ALTR, ALTR, ALTR, PNCT, PNCT, PNCT, PNCT, PNCT, ALTR, ALTR, PNCT,
			PNCT, /* E */
			/* F */ DIAC, DIAC, DIAC, DIAC, PNCT, DIAC, DIAC, PNCT, DIAC, -PNCT, DIAC, PNCT, PNCT, SPRT, SPRT,
			SPRT /* F */
			/* 0 1 2 3 4 5 6 7 8 9 A B C D E F */
	};

	public class AlignSegment {
		int start1, end1;
		int start2, end2;

		int nofSegWords, nofIns, nofDel, nofMatchWords, nofMatchChar, correctSegWords, asrStartTimeExists,
				asrEndTimeExists;
		int nofChar, charDistance;
	}

	int maxNofLines = -1;// 1000000;//-1;
	final static int infinity = 1234567890;

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		String[] aligned = charAlign("MKNLASREVNIYVNGKLV", "QMASREVNIYVNGKL");
		System.out.println(aligned[0]);
		System.out.println(aligned[1]);

		MGBTranscriptionComponent asr = new MGBTranscriptionComponent();

		// arge: "D:\\Speech\\ArabicASRChallenge\\exp-2015-10-25\\" "html" "xml"
		// "ctm" "align" "srt" "tra" "dfxp" "clean" "trs"
		// args[0] = "D:\\Speech\\ArabicASRChallenge\\exp-2015-11-10\\" //
		// Folder name
		// args[1] = "html"; //sunfolder name
		// args[2] = "xml"; //Extension of Aljazeera transcription
		// args[3] = "ctm"; // Extension of .ctm file
		// args[4] = "align"; // Extension of .align file (open with Excel)
		// args[5] = "srt"; // Extension of .srt file
		// args[6] = "tra"; // Extension of .tra file
		// args[7] = "dxfp"; // Extension of .dfxp file
		// args[8] = "clean; // Extension of .clean file
		// args[9] = "trs; // Extension of .trs file

		int dist;
		List<String> s = new ArrayList<String>();
		List<String> t = new ArrayList<String>();

		try {
			s.add("أهلا");
			s.add("بكم");
			s.add("مشاهدينا");
			s.add("الأعزاء");
			s.add("‘");
			s.add("اليوم");

			t.add("أهلا");
			t.add("بكم");
			// t.add("مشاهدينا");
			t.add("الأعزاء");
			t.add("نحدثكم");
			t.add("الآن");

			dist = asr.DTWDistance(s, t);

			if (false) {
				asr.generateNoStopList(args);
			}

			String traFileName = asr.parseASRFiles(args);

			// Sameer
			String newTraFilePath = System.getProperty("user.dir") + "/ALL_MOD.tra";
			MGBUtil.fixTraSpeakers(traFileName, newTraFilePath);
			Class.forName("mgbmain.GenerateXMLTranscription");
			MGBGenerateXMLComponent.createTranscript(newTraFilePath);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// http://stackoverflow.com/questions/15042879/java-characters-alignment-algorithm
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

	// https://en.wikipedia.org/wiki/Dynamic_time_warping
	public int DTWDistance(List<String> s, List<String> t) // s: array[1..n], t:
															// array[1..m]
	{
		int i, j, n, m, cost, min;

		n = s.size();
		m = t.size();

		int[][] DTW = new int[n + 1][m + 1];

		for (i = 1; i <= n; i++) {
			DTW[i][0] = infinity;
		}

		for (i = 1; i <= m; i++) {
			DTW[0][i] = infinity;
		}

		DTW[0][0] = 0;

		for (i = 1; i <= n; i++) {
			for (j = 1; j <= m; j++) {
				cost = 0;
				if (s.get(i - 1).compareTo(t.get(j - 1)) != 0) {
					cost = 1;
				}

				min = Math.min(DTW[i - 1][j], DTW[i][j - 1]); // insertion,
																// deletion
				min = Math.min(min, DTW[i - 1][j - 1]); // match

				DTW[i][j] = cost + min;
			}
		}

		return DTW[n][m];
	}

	public int DTWDistance(String s, String t) // s: array[1..n], t: array[1..m]
	{
		int i, j, n, m, cost, min;
		char[] sChar, tChar;

		n = s.length();
		m = t.length();

		sChar = s.toCharArray();
		tChar = t.toCharArray();

		int[][] DTW = new int[n + 1][m + 1];

		for (i = 1; i <= n; i++) {
			DTW[i][0] = infinity;
		}

		for (i = 1; i <= m; i++) {
			DTW[0][i] = infinity;
		}

		DTW[0][0] = 0;

		for (i = 1; i <= n; i++) {
			for (j = 1; j <= m; j++) {
				cost = 0;
				if (sChar[i - 1] != tChar[j - 1]) {
					cost = 1;
				}

				min = Math.min(DTW[i - 1][j], DTW[i][j - 1]); // insertion,
																// deletion
				min = Math.min(min, DTW[i - 1][j - 1]); // match

				DTW[i][j] = cost + min;
			}
		}

		return DTW[n][m];
	}

	public int LevenshteinDistance(CharSequence lhs, CharSequence rhs) {
		int len0 = lhs.length() + 1;
		int len1 = rhs.length() + 1;

		// the array of distances
		int[] cost = new int[len0];
		int[] newcost = new int[len0];

		// initial cost of skipping prefix in String s0
		for (int i = 0; i < len0; i++)
			cost[i] = i;

		// dynamically computing the array of distances

		// transformation cost for each letter in s1
		for (int j = 1; j < len1; j++) {
			// initial cost of skipping prefix in String s1
			newcost[0] = j;

			// transformation cost for each letter in s0
			for (int i = 1; i < len0; i++) {
				// matching current letters in both strings
				int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;

				// computing cost for each transformation
				int cost_replace = cost[i - 1] + match;
				int cost_insert = cost[i] + 1;
				int cost_delete = newcost[i - 1] + 1;

				// keep minimum cost
				newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
			}

			// swap cost/newcost arrays
			int[] swap = cost;
			cost = newcost;
			newcost = swap;
		}

		// the distance is the cost for transforming all letters in both strings
		return cost[len0 - 1];
	}

	int GetDTWDistance(List<String> s, List<String> asrLines2, int start, int len) {
		int j, m, dist;
		String s2;
		List<String> t = new ArrayList<String>();
		String[] fields2;

		dist = infinity;
		t.clear();

		for (j = start; j < start + len; j++) {
			s2 = asrLines2.get(j);
			fields2 = s2.split(" ");

			t.add(fields2[2]);
			m = t.size();
		}

		dist = DTWDistance(s, t);

		return dist;
	}

	public int LoadFile(String filename, List<String> list) {
		int nofLines, dbgLineNo, breakpoint, errors, i;
		String strLine, msg;

		nofLines = 0;
		dbgLineNo = 1226;
		errors = 0;

		try {
			list.clear();
			java.io.File file = new java.io.File(filename);
			if (file.exists()) {
				FileInputStream fstream = new FileInputStream(filename);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if (nofLines == dbgLineNo) {
						breakpoint = 1;
					}

					if (nofLines == maxNofLines) {
						break;
					}

					if ((nofLines > 0) && (nofLines % 10000) == 0) {
						msg = String.format("LoadFile(). Reading: %s, Line:%d", filename, nofLines);
						System.out.println(msg);
					}

					list.add(strLine);

					nofLines++;
				}
				// Close the input stream
				br.close();
			} else {
				nofLines = -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return nofLines;
	}

	public String cleanText(String text) {
		int i, c, charType, breakpoint;
		String s;
		char[] charInput = text.toCharArray();

		s = "";
		for (i = 0; i < charInput.length; i++) {
			c = charInput[i];
			if ((c <= 32) || (c == 127) || (c >= 194128 && c <= 194160)) {
				s += " ";
			} else {
				charType = SPRT;

				if ((c >= 0) && (c <= 255)) {
					charType = iarMsCharacterType[c];
				}

				if (charType == PNCT) {
					s += " ";
					s += charInput[i];
					s += " ";
				} else if ((charType == DIAC) || (charInput[i] == 'ـ')) {
					breakpoint = 1;
				} else {
					s += charInput[i];
				}
			}
		}

		// Normalize text
		s = normalizeWord(s);

		/*
		 * int nofTokens; ArrayList<String> tokens;
		 * 
		 * tokens = ArabicUtils.tokenizeText(s); nofTokens = tokens.size();
		 * 
		 * s = ""; for (i = 0; i < nofTokens; i++) { s += tokens.get(i); s +=
		 * " "; }
		 */

		s = s.replaceAll("  ", " ");
		s = s.trim();

		return s;
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

	public String splitPunct(String s) {
		int i, len;
		char ch;
		String out;

		/*
		 * out = ""; len = s.length(); for (i = 0; i < len; i++) { ch =
		 * s.charAt(i);
		 * 
		 * //if ((ch == ':') || (ch == '[') || (ch == ']') || (ch == '،') || (ch
		 * == '.') || (ch == '?') || (ch == '؟') || (ch == '-') || (ch == '_')
		 * || (ch == '\\') || (ch == '/') || (ch == '(') || (ch == ')') || (ch
		 * == '{') || (ch == '}') || (ch == '\'') || (ch == '\"') || (ch ==
		 * '*')) if ((ch >= 0) && (ch <= 255) && iarMsCharacterType[(int)ch] ==
		 * PNCT) { out += String.format(" %c ", ch); } else { out += ch; } }
		 */

		out = s;
		out = out.replaceAll(":", " : ");
		out = out.replaceAll("\\[", " \\[ ");
		out = out.replaceAll("\\]", " \\] ");
		out = out.replaceAll("،", " ، ");
		out = out.replaceAll("\\.", " \\. ");
		out = out.replaceAll("؟", " ؟ ");
		out = out.replaceAll("\\?", " \\? ");

		out = out.replaceAll("-", " - ");
		out = out.replaceAll("/", " / ");
		out = out.replaceAll("\\(", " \\( ");
		out = out.replaceAll("\\)", " \\) ");
		out = out.replaceAll("\\{", " \\{ ");
		out = out.replaceAll("}", " } ");

		if (out.indexOf("\\\\") > 0) {
			out = out.replaceAll("\\\\", " \\\\ ");
		}

		out = out.replaceAll("  ", " ");
		out = out.trim();

		return out;
	}

	public int ExtractContentFromXMLFile(String ajFilename, List<String> ajLines, BufferedWriter outClean,
			BufferedWriter outStatus, boolean extractedFromPDF) {
		int i, j, len, nofAjLines, test, nofLines, len1, len2, breakpoint;
		char ch, ch2;
		Element contentElement1, contentElement2;
		String cleanContent1, cleanContent2, cleanContent, s, msg, sClean, savedLine;
		String[] lines;

		nofAjLines = 0;
		ajLines.clear();

		msg = String.format("ExtractContentFromXMLFile(), start");
		System.out.println(msg);

		try {
			cleanContent1 = "";
			cleanContent2 = "";

			Document doc = Jsoup.parse(new File(ajFilename), "utf-8");

			if (!extractedFromPDF) {
				contentElement1 = doc.select("ArticleEpisode").first();
				contentElement2 = doc.select("SecondTapContent").first();
			} else {
				contentElement1 = doc;
				contentElement2 = null;
			}

			if (contentElement1 != null) {
				cleanContent1 = getCleanContent(contentElement1, extractedFromPDF);
			}
			if (contentElement2 != null) {
				cleanContent2 = getCleanContent(contentElement2, extractedFromPDF);
			}

			len1 = cleanContent1.length();
			len2 = cleanContent2.length();

			if ((len1 > 0) && (len2 > 0)) {
				gNofErrors++;
				breakpoint = 1;
			}

			if (len1 >= len2) {
				cleanContent = cleanContent1;
			} else {
				cleanContent = cleanContent2;
			}

			lines = cleanContent.split("\t");
			nofLines = lines.length;

			sClean = "";
			for (i = 0; i < nofLines; i++) {
				lines[i] = lines[i].replaceAll("\\n", " ");
				lines[i] = lines[i].replaceAll("\n", " ");

				lines[i] = lines[i].replaceAll("\\s+", " ");
				lines[i] = lines[i].trim();

				if (!lines[i].isEmpty()) {
					if (extractedFromPDF) {
						// Arabic letetrs in the pdf files are saved in revser
						// order
						savedLine = new StringBuffer(lines[i]).reverse().toString();
					} else {
						savedLine = lines[i];
					}
					ajLines.add(savedLine);

					if (SAVE_EXTRACTED_CONTENTS) {
						s = String.format("%s\r\n", savedLine);
						sClean += s;
					}

					if (savedLine.contains("&")) {
						gNofSpecialChar++;
						msg = String.format("SPECIAL_CHAR\tFilename:%s\t%s\r\n", ajFilename, savedLine);
						outStatus.write(msg);

					}
				}
			}
			if (SAVE_EXTRACTED_CONTENTS) {
				outClean.write(sClean);
			}

			nofAjLines = ajLines.size();

			/*
			 * test = -1;
			 * 
			 * if (test == 1) { Document doc = Jsoup.parse(new File(
			 * "D:\\Speech\\ArabicASRChallenge\\exp-2015-11-10\\html\\xml\\register.xml"
			 * ),"utf-8");
			 * 
			 * Element content2 = doc.select("content").first(); s =
			 * content2.text(); Element subtitle = doc.select("h2").first();
			 * Elements listItems = doc.select("ul.list > li"); for(Element
			 * item: listItems) { s = item.text(); // print list's items one
			 * after another
			 * 
			 * i = 0; }
			 * 
			 * Element content = doc.getElementById("content"); Elements tests =
			 * content.getElementsByTag("tests"); for (Element testElement :
			 * tests) {
			 * System.out.println(testElement.getElementsByTag("test")); }
			 * 
			 * i = 0; } else if (test == 2) { String html =
			 * "<p>An <a href='http://example.com/'><b>example</b></a> link.</p>"
			 * ; Document doc = Jsoup.parse(html); Element link =
			 * doc.select("a").first();
			 * 
			 * String text = doc.body().text(); // "An example link" String
			 * linkHref = link.attr("href"); // "http://example.com/" String
			 * linkText = link.text(); // "example""
			 * 
			 * String linkOuterH = link.outerHtml(); // "<a href="
			 * http://example.com"><b>example</b></a>" String linkInnerH =
			 * link.html(); // "<b>example</b>" } else if (test == 3) { Document
			 * doc = Jsoup.parse(new File(
			 * "D:\\Speech\\ArabicASRChallenge\\exp-2015-11-10\\html\\xml\\register.html"
			 * ),"utf-8"); String title = doc.title();
			 * 
			 * Element loginform = doc.getElementById("registerform");
			 * 
			 * Elements inputElements = loginform.getElementsByTag("input"); for
			 * (Element inputElement : inputElements) { String type =
			 * inputElement.attr("type"); String name =
			 * inputElement.attr("name"); String value =
			 * inputElement.attr("value"); System.out.println("Param type: " +
			 * type + " name: " + name + " value: " + value); }
			 * 
			 * Element contents = doc.getElementById("Contents"); Elements
			 * contentsElements =
			 * loginform.getElementsByTag("ContentsSubField1"); for (Element
			 * inputElement : contentsElements) { String name =
			 * inputElement.attr("name"); String id = inputElement.attr("id");
			 * 
			 * System.out.println("Param name: " + name + " id: " + id); } }
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("ExtractContentFromXMLFile(), end");
		System.out.println(msg);

		return nofAjLines;
	}

	public String getCleanContent(Element contentElement, boolean extractedFromPDF) {
		int i, j, len, nofLines;
		char ch, ch2;
		String content, cleanContent, s, msg;

		msg = String.format("getCleanContent(), start");
		System.out.println(msg);

		cleanContent = "";
		try {
			if (contentElement != null) {
				if (!extractedFromPDF) {
					content = contentElement.text();
				} else {
					content = contentElement.toString();
				}

				content = content.replaceAll("\t", " ");

				content = content.replaceAll("<br />", "\t");
				content = content.replaceAll("<br/>", "\t");
				content = content.replaceAll("<br>", "\t");
				content = content.replaceAll("<p>", "\t");
				content = content.replaceAll("</p>", "\t");

				content = content.replace(System.getProperty("line.separator"), "\t");

				content = content.replaceAll("\\r\\n", "\t");
				content = content.replaceAll("\\r", "\t");
				content = content.replaceAll("\\n", "\t");
				content = content.replaceAll("(\r\n|\n)", "\t");

				nofLines = 0;
				if (VALIDATE_AJ_ONLY) {
					len = content.length();
					for (i = 0; i < len; i++) {
						ch = content.charAt(i);

						if (ch == '\t') {
							nofLines++;
						}
					}
				}
				// content = content.replaceAll("\\r\\n|\\r|\\n", "\t");
				// content = content.replaceAll("[\\n\\r]+", "\t");

				if (extractedFromPDF) {
					content = content.replaceAll("</test>", "\t");
				}

				content = content.replaceAll("\t\t", "\t");

				len = content.length();
				cleanContent = "";
				for (i = 0; i < len; i++) {
					ch = content.charAt(i);

					if (ch == '\\' && (i < len - 1) && content.charAt(i + 1) == 'n') {
						cleanContent += '\t';
						i++;
						continue;
					}
					if (ch != '<') {
						cleanContent += ch;
					} else {
						for (j = i + 1; j < len; j++) {
							ch2 = content.charAt(j);

							if (ch2 == '>') {
								cleanContent += ' ';
								i = j;
								break;
							}
						}
					}
				}

				cleanContent = cleanContent.replaceAll("  ", " ");
				cleanContent = cleanContent.replaceAll(" \t", "\t");
				cleanContent = cleanContent.replaceAll("\t ", "\t");

				cleanContent = cleanContent.replaceAll("\t\t", "\t");

				cleanContent = cleanContent.replaceAll("&nbsp;", " ");
				cleanContent = cleanContent.replaceAll("&lt;", "<");
				cleanContent = cleanContent.replaceAll("&gt;", ">");
				cleanContent = cleanContent.replaceAll("&amp;", "&");

				// http://www.thesauruslex.com/typo/eng/enghtml.htm,
				// https://www.utexas.edu/learn/html/spchar.html
				// http://www.econlib.org/library/asciicodes.html
				// http://www.w3schools.com/charsets/ref_utf_punctuation.asp
				// French
				// À &Agrave; à &agrave;
				// Â &Acirc; â &acirc;
				// Ç &Ccedil; ç &ccedil;
				// È &Egrave; è &egrave;
				// É &Eacute; é &eacute;
				// Ê &Ecirc; ê &ecirc;
				// Ë &Euml; ë &euml;
				// Î &Icirc; î &icirc;
				// Ï &Iuml; ï &iuml;
				// Ô &Ocirc; ô &ocirc;
				// Œ &OElig; œ &oeli7g;
				// Ù &Ugrave; ù &ugrave;
				// Û &Ucirc; û &ucirc;
				// Ü &Uuml; ü &uuml;
				// Ÿ &#376; ÿ &yuml;
				cleanContent = cleanContent.replaceAll("&Agrave;", "À");
				cleanContent = cleanContent.replaceAll("&agrave;", "à");
				cleanContent = cleanContent.replaceAll("&Acirc;", "Â");
				cleanContent = cleanContent.replaceAll("&acirc;", "â");
				cleanContent = cleanContent.replaceAll("&Ccedil", "Ç");
				cleanContent = cleanContent.replaceAll("&ccedil", "ç");
				cleanContent = cleanContent.replaceAll("&Egrave;", "È");
				cleanContent = cleanContent.replaceAll("&egrave;", "è");
				cleanContent = cleanContent.replaceAll("&Eacute;", "//É");
				cleanContent = cleanContent.replaceAll("&eacute;", "é");
				cleanContent = cleanContent.replaceAll("&Ecirc;", "Ê");
				cleanContent = cleanContent.replaceAll("&ecirc;", "ê");
				cleanContent = cleanContent.replaceAll("&Euml;", "Ë");
				cleanContent = cleanContent.replaceAll("&euml;", "ë");
				cleanContent = cleanContent.replaceAll("&Icirc;", "Î");
				cleanContent = cleanContent.replaceAll("&icirc;", "î");
				cleanContent = cleanContent.replaceAll("&Iuml;", "Ï");
				cleanContent = cleanContent.replaceAll("&iuml;", "ï");
				cleanContent = cleanContent.replaceAll("&Ocirc;", "Ô");
				cleanContent = cleanContent.replaceAll("&ocirc;", "ô");
				cleanContent = cleanContent.replaceAll("&OElig;", "Œ");
				cleanContent = cleanContent.replaceAll("&oelig;", "œ");
				cleanContent = cleanContent.replaceAll("&Ugrave;", "Ù");
				cleanContent = cleanContent.replaceAll("&ugrave;", "ù");
				cleanContent = cleanContent.replaceAll("&Ucirc;", "Û");
				cleanContent = cleanContent.replaceAll("&ucirc;", "û");
				cleanContent = cleanContent.replaceAll("&Uuml;", "Ü");
				cleanContent = cleanContent.replaceAll("&uuml;", "ü");
				cleanContent = cleanContent.replaceAll("&#376;", "Ÿ");
				cleanContent = cleanContent.replaceAll("&yuml;", "ÿ");

				// German
				// Ä &Auml; ä &auml;
				// Ö &Ouml; ö &ouml;
				// Ü &Uuml; ü &uuml;
				// ß &szlig;
				cleanContent = cleanContent.replaceAll("&Auml;", "Ä");
				cleanContent = cleanContent.replaceAll("&auml;", "ä");
				cleanContent = cleanContent.replaceAll("&Ouml;", "Ö");
				cleanContent = cleanContent.replaceAll("&ouml;", "ö");
				cleanContent = cleanContent.replaceAll("&Uuml;", "Ü");
				cleanContent = cleanContent.replaceAll("&uuml;", "ü");
				cleanContent = cleanContent.replaceAll("&szlig;", "ß");

				// Danish
				// Æ &AElig; æ &aelig;
				// Ø &Oslash; ø &oslash;
				// Å &Aring; å &aring;
				cleanContent = cleanContent.replaceAll("&AElig;", "Æ");
				cleanContent = cleanContent.replaceAll("&aelig;", "æ");
				cleanContent = cleanContent.replaceAll("&Oslash;", "Ø");
				cleanContent = cleanContent.replaceAll("&oslash;", "ø");
				cleanContent = cleanContent.replaceAll("&Aring;", "Å");
				cleanContent = cleanContent.replaceAll("&aring;", "å");

				// Miscellaneous characters
				// € &euro; £ &pound;
				// « &laquo; » &raquo;
				// • &bull; † &dagger;
				// © &copy; ® &reg;
				// ™ &trade; ° &deg;
				// ‰ &permil; µ &micro;
				// · &middot; – &ndash;
				// — &mdash; № &#8470;
				cleanContent = cleanContent.replaceAll("&euro;", "€");
				cleanContent = cleanContent.replaceAll("&pound;", "£");
				cleanContent = cleanContent.replaceAll("&laquo;", "«");
				cleanContent = cleanContent.replaceAll("&raquo;", "»");
				cleanContent = cleanContent.replaceAll("&raquo;", "»");
				cleanContent = cleanContent.replaceAll("&bull;", "•");
				cleanContent = cleanContent.replaceAll("&dagger;", "†");
				cleanContent = cleanContent.replaceAll("&copy;", "©");
				cleanContent = cleanContent.replaceAll("&reg;", "®");
				cleanContent = cleanContent.replaceAll("&trade;", "™");
				cleanContent = cleanContent.replaceAll("&deg;", "°");
				cleanContent = cleanContent.replaceAll("&permil;", "‰");
				cleanContent = cleanContent.replaceAll("&micro;", "µ");
				cleanContent = cleanContent.replaceAll("&middot;", "·");
				cleanContent = cleanContent.replaceAll("&ndash;", "–");
				cleanContent = cleanContent.replaceAll("&mdash;", "—");
				cleanContent = cleanContent.replaceAll("&#8470;", "№");

				// ¼ &#188; &frac14; case fraction: 1/4
				// ½ &#189; &frac12; case fraction: 1/2
				// ¾ &#190; &frac34; case fraction: 3/4
				cleanContent = cleanContent.replaceAll("&frac14;", "¼");
				cleanContent = cleanContent.replaceAll("&frac12;", "½");
				cleanContent = cleanContent.replaceAll("&frac34;", "¾");

				// “ &ldquo;
				// ” &rdquo;
				// ‘ &lsquo;
				// ’ &rsquo;
				// « &laquo;
				// » &raquo;
				// ÷ &divide;
				// … &hellip;
				// &zwj;
				// ‚ &sbquo;
				// soft hyphen &shy;
				// &lrm; kleft to right
				// &rlm; right to left
				cleanContent = cleanContent.replaceAll("&ldquo;", "“");
				cleanContent = cleanContent.replaceAll("&rdquo;", "”");
				cleanContent = cleanContent.replaceAll("&lsquo;", "‘");
				cleanContent = cleanContent.replaceAll("&rsquo;", "’");
				cleanContent = cleanContent.replaceAll("&laquo;", "«");
				cleanContent = cleanContent.replaceAll("&raquo;", "»");
				cleanContent = cleanContent.replaceAll("&divide;", "÷");
				cleanContent = cleanContent.replaceAll("&hellip;", "…");
				cleanContent = cleanContent.replaceAll("&zwj;", "");
				cleanContent = cleanContent.replaceAll("&sbquo;", "‚");
				cleanContent = cleanContent.replaceAll("&shy;", " ");
				cleanContent = cleanContent.replaceAll("&lrm;", " ");
				cleanContent = cleanContent.replaceAll("&rlm;", " ");

				cleanContent = cleanContent.replaceAll("  ", " ");
				cleanContent = cleanContent.replaceAll("\t\t", "\t");

				cleanContent = cleanContent.trim();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("getCleanContent(), end");
		System.out.println(msg);

		return cleanContent;
	}

	public String parseASRFiles(String[] args) {
		if (args.length != 10) {
			System.out.println(
					"Please enter orgFoldername subFoldername AjExt ctmExt alignExt srtExt traExt dfxpExt cleanExt trsExt. Ex: \"D:\\Speech\\ArabicASRChallenge\\exp-2015-10-25\\ html xml ctm align srt tra dfxp clean trs");
			return "fail";
		}

		int i, nofAjXmlLines, nofAjLines, nofASRLines, extIndex, fileIndex, nofFiles, validAjFiles, validSrtFiles,
				nofSrtLines, nofAllSrtLines, nofSpeakers;
		int NOF_NO_STOP_LIST = 0, NOF_TIME_SORTED_LIST = 0, NOF_SPEAKERS_LIST;
		boolean genCleanFilesForCtmFilesOnly, extractedFromPDF;
		float fileDuration, totalDuration;
		String orgFolderName, subFolderName, ajFolderName, ctmFolderName, alignFolderName, srtFolderName, trsFolderName,
				msg, s, dbgFilename, mp4Filename;
		String ajExt, ctmExt, alignExt, srtExt, traExt, dfxpExt, cleanExt, trsExt;
		String ajFilename, ctmFilename, alignFilename, srtFilename, cleanFilename, cleanFilename2, trsFilename,
				traFolderName, dfxpFolderName, cleanFolderName, filename, outFilename, ext, filenameNoExt,
				traFilename = null, speakersFilename, statusFilename, sFileDuration, normalizedSpeaker;
		String[] fields;
		File[] ajFiles, srtFiles;
		Set<String> speakersKeys;
		Iterator<String> itrSpeakers;
		SpeakerInfo speakerInfo;
		File tmpFile, mp4Ffile;
		List<String> NO_STOP_LIST = new ArrayList<String>();
		List<String> TIME_SORTED_LIST = new ArrayList<String>();
		List<String> SPEAKERS_LIST = new ArrayList<String>();
		List<String> ajXmlLines = new ArrayList<String>();
		List<String> ajLines = new ArrayList<String>();
		List<String> ajLines2 = new ArrayList<String>();

		List<String> asrLines = new ArrayList<String>();
		List<String> asrLines2 = new ArrayList<String>();

		List<String> srtLines = new ArrayList<String>();
		List<String> allSrtLines = new ArrayList<String>();
		HashMap<String, SpeakerInfo> speakersMap = new HashMap<String, SpeakerInfo>();
		BufferedWriter outAlign, outSrt, outTra, outClean, outClean2, outTrs, outSpeakers, outStatus;

		msg = String.format("parseASRFiles(), start");
		System.out.println(msg);

		orgFolderName = args[0];
		subFolderName = args[1];
		ajExt = args[2];
		ctmExt = args[3];
		alignExt = args[4];
		srtExt = args[5];
		traExt = args[6];
		dfxpExt = args[7];
		cleanExt = args[8];
		trsExt = args[9];

		ajFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, ajExt);
		ctmFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, ctmExt);
		alignFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, alignExt);
		srtFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, srtExt);

		traFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, traExt);
		dfxpFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, dfxpExt);
		cleanFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, cleanExt);
		trsFolderName = String.format("%s\\%s\\%s", orgFolderName, subFolderName, trsExt);

		genCleanFilesForCtmFilesOnly = false;// true; // To optimize time,
												// generate clean files for ctm
												// files only
		validAjFiles = 0;

		extractedFromPDF = false;
		if (subFolderName.compareToIgnoreCase("pdf") == 0) {
			extractedFromPDF = true;
		}

		try {
			filename = String.format("%s\\NO_STOP_LIST.txt", orgFolderName);
			NOF_NO_STOP_LIST = LoadFile(filename, NO_STOP_LIST);

			for (i = 0; i < NOF_NO_STOP_LIST; i++) {
				NO_STOP_LIST_MAP.put(NO_STOP_LIST.get(i), 1);
			}

			filename = String.format("%s\\%s\\timed_sorted_list.txt", orgFolderName, subFolderName);
			NOF_TIME_SORTED_LIST = LoadFile(filename, TIME_SORTED_LIST);

			for (i = 0; i < NOF_TIME_SORTED_LIST; i++) {
				fields = TIME_SORTED_LIST.get(i).split(" ");
				TIME_SORTED_LIST_MAP.put(fields[1], fields[0]);
			}

			// Load speakers after revision
			if (USE_REVISED_SPEAKERS) {
				filename = String.format("%s\\SpeakersAfterRevision.txt", orgFolderName);
				NOF_SPEAKERS_LIST = LoadFile(filename, SPEAKERS_LIST);

				// Skip header line
				fields = SPEAKERS_LIST.get(0).split("\t");
				for (i = 1; i < NOF_SPEAKERS_LIST; i++) {
					// # Speaker Count NormalizedSpeaker Unique
					fields = SPEAKERS_LIST.get(i).split("\t");
					s = String.format("%s\t%s\t%s", fields[2], fields[3], fields[4]);
					SPEAKERS_LIST_MAP.put(fields[1], s);
				}
			}

			// Generate .srt files
			ajFiles = new File(ajFolderName).listFiles();

			fileIndex = 0;
			nofFiles = ajFiles.length;

			traFilename = String.format("%s\\ALL.%s", traFolderName, traExt);
			outTra = new BufferedWriter(new FileWriter(traFilename));

			speakersFilename = String.format("%s\\Speakers.txt", traFolderName);
			outSpeakers = new BufferedWriter(new FileWriter(speakersFilename));

			statusFilename = String.format("%s\\Status.txt", traFolderName);
			outStatus = new BufferedWriter(new FileWriter(statusFilename));

			totalDuration = 0.0f;

			dbgFilename = "00C710F1-5611-4A7C-8A25-801BAEA5A5AD.xml";
			// dbgFilename = "2EC920B6-C18D-49A2-90D6-6678DF022B2D.xml";
			// dbgFilename = "49973211-017A-422F-AD15-28F08604476D.xml";
			// dbgFilename = "003A779F-EFDB-4FD6-9495-71698B6832CE.xml";
			// dbgFilename = "00C710F1-5611-4A7C-8A25-801BAEA5A5AD.xml";
			// dbgFilename = "32789770-D08F-4CC9-ABFD-ABB04C728EC2.xml";
			// dbgFilename = "3A0EB34D-A252-4EED-86BE-AC3F610E46D9.xml";
			// dbgFilename = "5E0FB765-A673-4CBF-BDFA-7C1BCF651C7C.xml";
			// dbgFilename = "36BE0575-165E-44F6-85B8-44B3FA158D68.xml";
			// dbgFilename = "F80DC210-92E2-4B3A-80C6-89ECB738B375.xml";
			// dbgFilename = "0EC62179-AC40-438B-94FB-51E1D92E8677.xml";
			// dbgFilename = "017E8618-8B64-4FA0-9251-5907DA6F6078.xml";
			// dbgFilename = "xxx.xml";
			for (File file : ajFiles) {
				if (fileIndex == MAX_NOF_FILES) {
					break;
				}
				if (file.isFile()) {
					filename = file.getName();

					if (true && filename.compareTo(dbgFilename) != 0) {
						continue;
					}

					extIndex = filename.lastIndexOf('.');
					if (extIndex > 0) {
						filenameNoExt = filename.substring(0, extIndex);
						ext = filename.substring(extIndex + 1);
						// ext = Files.getFileExtension(filename);

						if (CONSIDER_MP4_FILES_ONLY) {
							mp4Filename = String.format("%s\\%s\\%s\\%s.mp4", orgFolderName, subFolderName, srtExt,
									filenameNoExt);
							mp4Ffile = new java.io.File(mp4Filename);
							if (!mp4Ffile.exists()) {
								continue;
							}
						}

						msg = String.format("File: %d/%d, %s. validAjFile:%d, totalDuration:%f", fileIndex + 1,
								nofFiles, filename, validAjFiles, totalDuration);
						System.out.println(msg);

						fileDuration = 0.0f;
						ajFilename = "";
						if (ext.compareToIgnoreCase(ajExt) == 0) {
							sFileDuration = TIME_SORTED_LIST_MAP.get(filenameNoExt);
							if (sFileDuration != null) {
								fileDuration = Float.parseFloat(sFileDuration);

								if (fileDuration < MIN_FILE_DURATION) {
									fileDuration = 0.0f;
								}
							}

							if (fileDuration < MIN_FILE_DURATION) {
								fileIndex++;
								continue;
							}

							totalDuration += fileDuration;

							outClean2 = null;
							nofAjLines = 0;
							if (!genCleanFilesForCtmFilesOnly) {
								ajFilename = String.format("%s\\%s", ajFolderName, filename);

								cleanFilename = String.format("%s\\%s.%s", cleanFolderName, filenameNoExt, cleanExt);
								outClean = new BufferedWriter(new FileWriter(cleanFilename));

								cleanFilename2 = String.format("%s\\%s.%s2", cleanFolderName, filenameNoExt, cleanExt);
								outClean2 = new BufferedWriter(new FileWriter(cleanFilename2));

								nofAjLines = ExtractContentFromXMLFile(ajFilename, ajLines, outClean, outStatus,
										extractedFromPDF);

								outClean.close();

								if (VALIDATE_AJ_ONLY) {
									ajLines2 = addAljazeeraTags(ajLines, ajFilename, outStatus, outClean2, outSpeakers);

									outClean2.close();

									fileIndex++;
									continue;
								}
							}

							if (GRAPHEME_ALIGN_FILES == false) {
								ctmFilename = String.format("%s\\%s.%s", ctmFolderName, filenameNoExt, ctmExt);
							} else {
								ctmFilename = String.format("%s2\\%s.GraphemeAlign.%s", ctmFolderName, filenameNoExt,
										ctmExt);
							}

							tmpFile = new File(ctmFilename);
							if (tmpFile.exists()) {
								if (genCleanFilesForCtmFilesOnly) {
									ajFilename = String.format("%s\\%s", ajFolderName, filename);

									cleanFilename = String.format("%s\\%s.%s", cleanFolderName, filenameNoExt,
											cleanExt);
									outClean = new BufferedWriter(new FileWriter(cleanFilename));

									cleanFilename2 = String.format("%s\\%s.%s2", cleanFolderName, filenameNoExt,
											cleanExt);
									outClean2 = new BufferedWriter(new FileWriter(cleanFilename2));

									nofAjLines = ExtractContentFromXMLFile(ajFilename, ajLines, outClean, outStatus,
											extractedFromPDF);

									outClean.close();

									if (VALIDATE_AJ_ONLY) {
										ajLines2 = addAljazeeraTags(ajLines, ajFilename, outStatus, outClean2,
												outSpeakers);

										outClean2.close();

										fileIndex++;
										continue;
									}
								}

								nofASRLines = LoadFile(ctmFilename, asrLines);

								if ((nofAjLines > 0) && (nofASRLines > 0)) {
									validAjFiles++;
									msg = String.format(
											"********************File: %d/%d, %s. validAjFiles:%d, totalDuration:%f",
											fileIndex + 1, nofFiles, filename, validAjFiles, totalDuration);
									System.out.println(msg);

									alignFilename = String.format("%s\\%s.%s", alignFolderName, filenameNoExt,
											alignExt);
									outAlign = new BufferedWriter(new FileWriter(alignFilename));

									ajLines2 = addAljazeeraTags(ajLines, ajFilename, outStatus, outClean2, outSpeakers);

									srtFilename = String.format("%s\\%s.%s", srtFolderName, filenameNoExt, srtExt);
									outSrt = new BufferedWriter(new FileWriter(srtFilename));

									trsFilename = String.format("%s\\%s.%s", trsFolderName, filenameNoExt, trsExt);
									outTrs = new BufferedWriter(new FileWriter(trsFilename));

									// Header of .trs file
									msg = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
									msg += String.format("<!DOCTYPE Trans SYSTEM \"trans-14.dtd\">\r\n");
									msg += String.format(
											"<Trans scribe=\"mai_e\" audio_filename=\"%s\" version=\"2\" version_date=\"160119\">\r\n",
											filenameNoExt);
									msg += String.format("<Episode>\r\n");
									msg += String.format(
											"<Section type=\"report\" startTime=\"0.0\" endTime=\"3600.0\">\r\n");
									msg += String.format("<Turn startTime=\"0\" endTime=\"3600.0\">\r\n");

									outTrs.write(msg);

									asrLines2 = extractAsrInfo(asrLines, outSrt, outTrs);

									alignAljazeeraWithAsr(ajLines2, asrLines2, outAlign, outSrt, outTrs, filename,
											outTra, outStatus, speakersMap);

									outClean2.close();
									outAlign.close();

									msg = "</Turn>\r\n</Section>\r\n</Episode>\r\n</Trans>\r\n";
									outTrs.write(msg);

									outSrt.close();
									outTrs.close();

									ajXmlLines.clear();
									ajLines2.clear();
									asrLines2.clear();
								}

								if (nofAjLines > 0) {
									ajLines.clear();
								}

								if (nofASRLines > 0) {
									asrLines.clear();
								}
							}
						}
					}
				}
				fileIndex++;
			}

			s = String.format("#\tSpeaker\tCount\tNormalizedSpeaker\tUnique\r\n");
			outSpeakers.write(s);

			nofSpeakers = 0;
			speakersKeys = speakersMap.keySet();
			itrSpeakers = speakersKeys.iterator();
			while (itrSpeakers.hasNext()) {
				// Getting Key
				s = itrSpeakers.next();
				speakerInfo = speakersMap.get(s);

				normalizedSpeaker = getNormalizedSpeaker(speakerInfo.speakerName);
				s = String.format("%d\t%s\t%d\t%s\t1\r\n", nofSpeakers + 1, speakerInfo.speakerName, speakerInfo.count,
						normalizedSpeaker);
				outSpeakers.write(s);

				nofSpeakers++;
			}

			outStatus.close();
			outSpeakers.close();
			outTra.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("parseASRFiles(), end. nofFiles=%d, nofErrors=%d, gNofAllSegments=%d, gAllSegmentsLen=%d",
				validAjFiles, gNofErrors, gNofAllSegments, gAllSegmentsLen);
		System.out.println(msg);

		if (VALIDATE_AJ_ONLY) {
			msg = String.format(
					"gProgramName=%d, gEpisodeTitle=%d, gEpisodeSpeaker=%d, gEpisodeGuest=%d, gEpisodeDate=%d, gEpisodeDirections=%d, gNofSpecialChar=%d",
					gProgramName, gEpisodeTitle, gEpisodeSpeaker, gEpisodeGuest, gEpisodeDate, gEpisodeDirections,
					gNofSpecialChar);
			System.out.println(msg);
		}

		return traFilename;
	}

	public String getNormalizedSpeaker(String orgSpeaker) {
		int i, len;
		char ch;
		String speaker, normalizedSpeaker;

		normalizedSpeaker = "";
		speaker = normalizeWord(orgSpeaker);
		len = speaker.length();

		for (i = 0; i < len; i++) {
			ch = speaker.charAt(i);

			if ((ch == '/') || (ch == '-')) {
				break;
			}
			normalizedSpeaker += ch;
		}
		normalizedSpeaker = normalizedSpeaker.replaceAll("  ", " ");
		normalizedSpeaker = normalizedSpeaker.trim();

		return normalizedSpeaker;
	}

	public List<String> addAljazeeraTags(List<String> ajLines, String ajFilename, BufferedWriter outStatus,
			BufferedWriter outClean2, BufferedWriter outSpeakers) {
		boolean metadata, breakpoint, bDir, validSpeaker;
		int i, j, m, n, nofWords, start, colon, speakerStart, title, endLine, tmpStart, nofAjEpisodeDirections;
		int programNameStart = -1, episodeTitleStart = -1, episodeSpeakerStart = -1, episodeGuestStart = -1,
				episodeDateStart = -1, episodeDirectionsStart = -1;
		List<String> ajLines2 = new ArrayList<String>();
		List<String> ajEpisodeDirections = new ArrayList<String>();
		String s, s2, msg, w0, w1, sClean, speakerName, orgAjLine, sDir, tmpSpeakerInfo, tmpNormalizedSpeaker,
				tmpUniqueSpeaker;
		String[] fields, fields2;

		msg = String.format("addAljazeeraTags(), start");
		System.out.println(msg);

		try {
			n = ajLines.size();

			endLine = Math.min(n, 200);

			for (i = 0; i < endLine; i++) {
				s = ajLines.get(i);

				s = s.replaceAll("\\s+", " ");

				s = s.replaceAll(":", " : ");
				s = s.replaceAll("\\s+", " ");
				s = s.trim();
				fields = s.split(" ");

				nofWords = fields.length;
				if (s.isEmpty()) {
					nofWords = 0;
				}

				/*
				 * for (j = 0; j < nofWords; j++) { if (fields[j].equals(":") &&
				 * (nofWords >= 20)) { metadata = false; w0 =
				 * normalizeWord(fields[0]); w1 = normalizeWord(fields[1]); if
				 * (w0.equals("اسم") || w0.equals("عنوان") || w0.equals("مقدم")
				 * || w0.equals("مقدمة") || w0.equals("ضيف") ||
				 * w0.equals("ضيفة") || w0.equals("ضيفا") || w0.equals("ضيفتا")
				 * || w0.equals("ضيوف") || w0.equals("تاريخ") ||
				 * w0.equals("المحاور")) { metadata = true; }
				 * 
				 * if (!metadata) { start = i; } break; } } if (start >= 0) {
				 * break; }
				 */

				if (nofWords >= 3) {
					if (fields[0].equals("اسم") && fields[1].equals("البرنامج") && fields[2].equals(":")) {
						programNameStart = i;
					}

					if (fields[0].equals("عنوان") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
						episodeTitleStart = i;
					}

					if ((fields[0].equals("مقدم") || fields[0].equals("مقدمة")) && fields[1].equals("الحلقة")
							&& fields[2].equals(":")) {
						episodeSpeakerStart = i;
					}

					if ((fields[0].equals("ضيف") || fields[0].equals("ضيفة") || fields[0].equals("ضيفا")
							|| fields[0].equals("ضيفتا") || fields[0].equals("ضيوف")) && fields[1].equals("الحلقة")
							&& fields[2].equals(":")) {
						episodeGuestStart = i;
					}

					if (fields[0].equals("تاريخ") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
						episodeDateStart = i;
					}
				}
				if (nofWords >= 2) {
					if (fields[0].equals("المحاور") && fields[1].equals(":")) {
						episodeDirectionsStart = i;
					}
				}
			}

			tmpStart = 0;
			if (tmpStart < programNameStart) {
				tmpStart = programNameStart + 1;
			}
			if (tmpStart < episodeTitleStart) {
				tmpStart = episodeTitleStart + 1;
			}
			if (tmpStart < episodeSpeakerStart) {
				tmpStart = episodeSpeakerStart + 1;
			}
			if (tmpStart < episodeGuestStart) {
				tmpStart = episodeGuestStart + 1;
			}
			if (tmpStart < episodeDateStart) {
				tmpStart = episodeDateStart + 1;
			}
			if (tmpStart < episodeDirectionsStart) {
				tmpStart = episodeDirectionsStart + 1;
			}

			start = -1;
			for (i = tmpStart; i < endLine; i++) {
				s = ajLines.get(i);

				s = s.replaceAll("\\s+", " ");

				s = s.replaceAll(":", " : ");
				s = s.replaceAll("\\s+", " ");
				s = s.trim();
				fields = s.split(" ");

				nofWords = fields.length;
				if (s.isEmpty()) {
					nofWords = 0;
				}

				m = Math.min(nofWords, 7);
				for (j = 0; j < m; j++) {
					if (fields[j].equals(":")) {
						metadata = false;
						w0 = normalizeWord(fields[0]);
						w1 = normalizeWord(fields[1]);
						if (w0.equals("اسم") || w0.equals("عنوان") || w0.equals("مقدم") || w0.equals("مقدمة")
								|| w0.equals("ضيف") || w0.equals("ضيفة") || w0.equals("ضيفا") || w0.equals("ضيفتا")
								|| w0.equals("ضيوف") || w0.equals("تاريخ") || w0.equals("المحاور")) {
							metadata = true;
						}

						if (!metadata) {
							start = i;
						}
						break;
					}
				}
				if (start >= 0) {
					break;
				}
			}

			if (VALIDATE_AJ_ONLY) {
				int programName1 = 0, episodeTitle1 = 0, episodeSpeaker1 = 0, episodeGuest1 = 0, episodeDate1 = 0,
						episodeDirections1 = 0;
				int programName2 = 0, episodeTitle2 = 0, episodeSpeaker2 = 0, episodeGuest2 = 0, episodeDate2 = 0,
						episodeDirections2 = 0;
				for (i = 0; i < start; i++) {
					s = ajLines.get(i);

					s = s.replaceAll("\\s+", " ");

					s = s.replaceAll(":", " : ");
					s = s.replaceAll("\\s+", " ");
					s = s.trim();
					fields = s.split(" ");

					nofWords = fields.length;
					if (s.isEmpty()) {
						nofWords = 0;
					}

					if (nofWords >= 3) {
						if (fields[0].equals("اسم") && fields[1].equals("البرنامج") && fields[2].equals(":")) {
							programName1 = 1;
							gProgramName++;
						}

						if (fields[0].equals("عنوان") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							episodeTitle1 = 1;
							gEpisodeTitle++;
						}

						if ((fields[0].equals("مقدم") || fields[0].equals("مقدمة")) && fields[1].equals("الحلقة")
								&& fields[2].equals(":")) {
							episodeSpeaker1 = 1;
							gEpisodeSpeaker++;
						}

						if ((fields[0].equals("ضيف") || fields[0].equals("ضيفة") || fields[0].equals("ضيفا")
								|| fields[0].equals("ضيفتا") || fields[0].equals("ضيوف")) && fields[1].equals("الحلقة")
								&& fields[2].equals(":")) {
							episodeGuest1 = 1;
							gEpisodeGuest++;
						}

						if (fields[0].equals("تاريخ") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							episodeDate1 = 1;
							gEpisodeDate++;
						}
					}
					if (nofWords >= 2) {
						if (fields[0].equals("المحاور") && fields[1].equals(":")) {
							episodeDirections1 = 1;
							gEpisodeDirections++;
						}
					}
				}
				if ((programName1 == 0) || (episodeTitle1 == 0) || (episodeSpeaker1 == 0) || (episodeGuest1 == 0)
						|| (episodeDate1 == 0) || (episodeDirections1 == 0)) {
					breakpoint = true;
					msg = String.format(
							"File:%s\tprogram:%d\ttitle:%d\tspeaker:%d\tguest:%d\tdate:%d\tdirections:%d\r\n",
							ajFilename, programName1, episodeTitle1, episodeSpeaker1, episodeGuest1, episodeDate1,
							episodeDirections1);
					outStatus.write(msg);
				}

				// Make sure that no missing metadata by looping on the whole
				// lines
				for (i = 0; i < n; i++) {
					s = ajLines.get(i);

					s = s.replaceAll("\\s+", " ");

					s = s.replaceAll(":", " : ");
					s = s.replaceAll("\\s+", " ");
					s = s.trim();
					fields = s.split(" ");

					nofWords = fields.length;
					if (s.isEmpty()) {
						nofWords = 0;
					}

					if (nofWords >= 3) {
						if (fields[0].equals("اسم") && fields[1].equals("البرنامج") && fields[2].equals(":")) {
							programName2 = 1;
						}

						if (fields[0].equals("عنوان") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							episodeTitle2 = 1;
						}

						if ((fields[0].equals("مقدم") || fields[0].equals("مقدمة")) && fields[1].equals("الحلقة")
								&& fields[2].equals(":")) {
							episodeSpeaker2 = 1;
						}

						if ((fields[0].equals("ضيف") || fields[0].equals("ضيفة") || fields[0].equals("ضيفا")
								|| fields[0].equals("ضيفتا") || fields[0].equals("ضيوف")) && fields[1].equals("الحلقة")
								&& fields[2].equals(":")) {
							episodeGuest2 = 1;
						}

						if (fields[0].equals("تاريخ") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							episodeDate2 = 1;
						}
					}

					if (nofWords >= 2) {
						if (fields[0].equals("المحاور") && fields[1].equals(":")) {
							episodeDirections2 = 1;
						}
					}
				}
				if ((programName1 != programName2) || (episodeTitle1 != episodeTitle2)
						|| (episodeSpeaker1 != episodeSpeaker2) || (episodeGuest1 != episodeGuest2)
						|| (episodeDate1 != episodeDate2) || (episodeDirections1 != episodeDirections2)) {
					breakpoint = true;
					msg = String.format(
							"METADATA\tFile:%s\tprogram:%d,%d\ttitle:%d,%d\tspeaker:%d,%d\tguest:%d,%d\tdate:%d,%d\tdirections:%d,%d\r\n",
							ajFilename, programName1, programName2, episodeTitle1, episodeTitle2, episodeSpeaker1,
							episodeSpeaker2, episodeGuest1, episodeGuest2, episodeDate1, episodeDate2,
							episodeDirections1, episodeDirections2);
					outStatus.write(msg);
				}
			}

			// Get episode directions
			nofAjEpisodeDirections = 0;
			if (episodeDirectionsStart > 0) {
				for (i = episodeDirectionsStart + 1; i < start; i++) {
					s = ajLines.get(i);
					s.trim();

					if (!s.isEmpty()) {
						ajEpisodeDirections.add(s);
						nofAjEpisodeDirections++;
					}
				}
			}

			speakerStart = -1;
			for (i = 0; i < n; i++) {
				orgAjLine = ajLines.get(i);
				s = splitPunct(orgAjLine);

				fields = s.split(" ");

				nofWords = fields.length;
				if (s.isEmpty()) {
					nofWords = 0;
				}

				if (i < start) {
					if (nofWords > 0) {
						s2 = String.format("<METADATA %s >", orgAjLine);
					} else {
						s2 = String.format("%s", orgAjLine);
					}
					ajLines2.add(s2);
					continue;
				}

				bDir = false;
				for (j = 0; j < nofAjEpisodeDirections; j++) {
					sDir = ajEpisodeDirections.get(j);
					if (sDir.contains(orgAjLine)) {
						s2 = String.format("<TITLE %s >", orgAjLine);
						ajLines2.add(s2);
						bDir = true;
						break;
					}
				}
				if (bDir) {
					continue;
				}

				colon = -1;
				for (j = 0; j < nofWords; j++) {
					if (fields[j].equals(":") && (j <= MAX_SPEAKER_NOF_WORDS)) {
						colon = j;
						break;
					}
				}

				title = 0;
				if (fields[0].equals("]") || fields[0].equals("[")) {
					// Ignore titles
					title = 1;
					speakerStart = -1;
				}

				if ((colon > 0) && (title == 0)) {
					speakerStart = i;
					s2 = "<SPEAKER ";
					speakerName = "";

					for (j = 0; j < nofWords; j++) {
						s2 += String.format("%s ", fields[j]);

						if (j != colon) {
							speakerName += String.format("%s ", fields[j]);
						}
						if (j == colon) {
							speakerName = speakerName.trim();

							validSpeaker = true;
							// Check if valid unique speakers
							if (USE_REVISED_SPEAKERS) {
								tmpSpeakerInfo = SPEAKERS_LIST_MAP.get(speakerName);
								if (tmpSpeakerInfo != null) {
									fields2 = tmpSpeakerInfo.split("\t");
									// Count NormalizedSpeaker Unique
									tmpNormalizedSpeaker = fields2[1];
									tmpUniqueSpeaker = fields2[2];

									if (tmpUniqueSpeaker.equalsIgnoreCase("error")) {
										validSpeaker = false;
									}
								} else {
									breakpoint = true;
									msg = String.format(
											"SPEAKER_NOT_FOUND_IN_SPEAKERS_REVISION\tFile:%s\tspeaker:%s\r\n",
											ajFilename, speakerName);
									outStatus.write(msg);
								}
							}

							if (validSpeaker) {
								s2 += "> ";

								if (VALIDATE_AJ_ONLY) {
									msg = String.format("%s\t%s\r\n", ajFilename, speakerName);
									outSpeakers.write(msg);
								}
							} else {
								s2 = s2.replaceAll("<SPEAKER ", "");
							}
						}
					}

					s2 = s2.trim();
					ajLines2.add(s2);
				} else {
					if (nofWords > 0) {
						if ((title > 0) && (speakerStart == -1)) {
							s2 = String.format("<TITLE %s >", ajLines.get(i));
						} else {
							s2 = String.format("%s", ajLines.get(i));
						}
					} else {
						s2 = String.format("%s", ajLines.get(i));
					}
					ajLines2.add(s2);
				}
			}

			msg = String.format("addAljazeeraTags(), end");
			System.out.println(msg);

			if (SAVE_EXTRACTED_CONTENTS) {
				n = ajLines2.size();
				sClean = "";
				for (i = 0; i < n; i++) {
					sClean += String.format("%s\r\n", ajLines2.get(i));
				}
				outClean2.write(sClean);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ajLines2;
	}

	public List<String> extractAsrInfo(List<String> asrLines, BufferedWriter outSrt, BufferedWriter outTrs) {
		int i, j, n, nofWords;
		char ch;
		boolean breakpoint;
		float fDuration, startSIL, durationSIL;
		List<String> asrLines2 = new ArrayList<String>();
		String s, s2, utf8, msg, word, BIE, grapheme, start, duration;
		String[] fields;

		msg = String.format("extractAsrInfo(), start");
		System.out.println(msg);

		try {
			n = asrLines.size();

			if (GRAPHEME_ALIGN_FILES == false) {
				for (i = 0; i < n; i++) {
					s = asrLines.get(i);
					s = s.replaceAll("  ", " ");
					fields = s.split(" ");

					if (fields[4].equalsIgnoreCase("<unk>")) {
						utf8 = fields[4];
					} else {
						utf8 = ArabicUtils.buck2utf8(fields[4]);
					}
					s2 = String.format("%s %s %s", fields[2], fields[3], utf8);
					asrLines2.add(s2);
				}
			} else {
				word = "";
				start = "";
				duration = "";
				fDuration = 0;
				BIE = "";
				startSIL = 0;
				durationSIL = 0;

				for (i = 0; i < n; i++) {
					s = asrLines.get(i);
					s = s.replaceAll("  ", " ");
					fields = s.split(" ");

					if (fields[4].equals("SIL_S") || fields[4].equals("SIL")) {
						if (durationSIL == 0) {
							startSIL = Float.parseFloat(fields[2]);
						}
						durationSIL += Float.parseFloat(fields[3]);
					} else {
						j = fields[4].indexOf("_");
						if (j > 0) {
							grapheme = fields[4].substring(0, j);

							if (grapheme.equals("V")) {
								grapheme = "*"; // ذ
							}
							BIE = fields[4].substring(j + 1);
							ch = BIE.charAt(0);

							fDuration += Float.parseFloat(fields[3]);
							if ((ch == 'B') || (ch == 'S')) {
								word = grapheme;
								start = fields[2];
							}

							if (ch == 'I') {
								word += grapheme;
							}

							if ((ch == 'E') || (ch == 'S')) {
								if (ch != 'S') {
									word += grapheme;
								}
								duration = String.format("%f", fDuration);

								utf8 = ArabicUtils.buck2utf8(word);

								if (durationSIL <= MIN_SILENCE_BETWEEN_SEGMENTS) {
									s2 = String.format("%s %s %s", start, duration, utf8);
								} else {
									s2 = String.format("%s %s %s %f %f", start, duration, utf8, startSIL, durationSIL);
								}
								asrLines2.add(s2);

								fDuration = 0;

								startSIL = 0;
								durationSIL = 0;
							} else {
								// FIXME: Error trap
								breakpoint = true;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("extractAsrInfo(), end");
		System.out.println(msg);

		return asrLines2;
	}

	public String formatTime(float totalSecs, int format) {
		float totalSecs2;
		int hours, minutes, seconds, fraction;
		String timeString;

		hours = (int) (totalSecs / 3600);
		minutes = (int) ((totalSecs % 3600) / 60);
		seconds = (int) (totalSecs % 60);

		totalSecs2 = (float) (hours * 3600 + minutes * 60 + seconds);

		fraction = (int) ((totalSecs - totalSecs2) * 100);

		if (format == 1) {
			timeString = String.format("%02d:%02d:%02d,%02d", hours, minutes, seconds, fraction);
		} else {
			timeString = String.format("%d.%02d", (int) totalSecs2, fraction);
		}

		return timeString;
	}

	public boolean isPunct(String s) {
		boolean punct;
		int len, len2, i;
		char ch, ch2;
		String arabicPunct = "؟.،";

		punct = false;
		len = s.length();

		if (len == 1) {
			ch = s.charAt(0);

			if ((ch >= 0) && (ch <= 0XFF)) {
				if (iarMsCharacterType[ch] == PNCT) {
					punct = true;
				}
			} else {
				len2 = arabicPunct.length();
				for (i = 0; i < len2; i++) {
					ch2 = arabicPunct.charAt(i);

					if (ch == ch2) {
						punct = true;
						break;
					}
				}
			}
		}

		return punct;
	}

	// http://stackoverflow.com/questions/15042879/java-characters-alignment-algorithm
	// https://en.wikipedia.org/wiki/Smith%E2%80%93Waterman_algorithm
	public int wordAlign(List<AjAsrWordInfo> orga, List<AjAsrWordInfo> orgb, List<AjAsrWordInfo> outa,
			List<AjAsrWordInfo> outb) {
		int aLen, bLen, maxLen, i, j, m, n, sameWord, distance, currWordLen;
		String s, sa, sb, msg;
		String[] fieldsa, fieldsb;

		AjAsrWordInfo wordInfo, wordInfo2;
		List<AjAsrWordInfo> a = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> b = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> tmpa = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> tmpb = new ArrayList<AjAsrWordInfo>();

		msg = String.format("wordAlign(), start");
		System.out.println(msg);

		aLen = orga.size();
		bLen = orgb.size();
		for (i = 0; i < aLen; i++) {
			a.add(orga.get(i));
		}
		for (i = 0; i < bLen; i++) {
			b.add(orgb.get(i));
		}

		outa.clear();
		outb.clear();

		maxLen = infinity;// 10;
		aLen = Math.min(maxLen, aLen);
		bLen = Math.min(maxLen, bLen);

		int[][] T = new int[aLen + 1][bLen + 1];

		for (i = 0; i <= aLen; i++) {
			T[i][0] = i;
		}

		for (i = 0; i <= bLen; i++) {
			T[0][i] = i;
		}

		for (i = 0; i < aLen; i++) {
			wordInfo = a.get(i);
			s = wordInfo.cleanWord;

			wordInfo2 = new AjAsrWordInfo(wordInfo);
			wordInfo2.word = s;

			a.set(i, wordInfo2);
		}

		for (i = 0; i < bLen; i++) {
			wordInfo = b.get(i);
			s = wordInfo.cleanWord;

			wordInfo2 = new AjAsrWordInfo(wordInfo);
			wordInfo2.word = s;

			b.set(i, wordInfo2);
		}

		for (i = 1; i <= aLen; i++) {
			wordInfo = a.get(i - 1);
			currWordLen = wordInfo.wordLen;

			for (j = 1; j <= bLen; j++) {
				// Compare only the first word of the string
				wordInfo2 = b.get(j - 1);

				sameWord = 0;
				if (wordInfo.word.compareTo(wordInfo2.word) == 0) {
					sameWord = 1;
				} else {
					// Apply relaxation
					// distance = DTWDistance(wordInfo.word, wordInfo2.word);
					distance = LevenshteinDistance(wordInfo.word, wordInfo2.word);

					if (distance <= currWordLen / 2) // 1
					{
						sameWord = 2;
						wordInfo.distance = distance;
					}
				}

				if (sameWord != 0) {
					T[i][j] = T[i - 1][j - 1];
				} else {
					T[i][j] = Math.min(T[i - 1][j], T[i][j - 1]) + 1;
				}
			}
		}

		for (i = aLen, j = bLen; i > 0 || j > 0;) {
			if ((i > 0) && (T[i][j] == T[i - 1][j] + 1)) {
				wordInfo = orga.get(--i);
				tmpa.add(wordInfo);

				wordInfo2 = new AjAsrWordInfo();
				// wordInfo2.word = "<null>";
				tmpb.add(wordInfo2);
			} else if ((j > 0) && (T[i][j] == T[i][j - 1] + 1)) {
				wordInfo = orgb.get(--j);
				tmpb.add(wordInfo);

				wordInfo2 = new AjAsrWordInfo();
				// wordInfo2.word = "<null>";
				tmpa.add(wordInfo2);
			} else if ((i > 0) && (j > 0) && (T[i][j] == T[i - 1][j - 1])) {
				wordInfo = orga.get(--i);
				tmpa.add(wordInfo);

				wordInfo2 = orgb.get(--j);
				tmpb.add(wordInfo2);
			}
		}

		// Reverse arrays
		n = tmpa.size();
		for (i = 0; i < n; i++) {
			outa.add(tmpa.get(n - i - 1));
		}

		n = tmpb.size();
		for (i = 0; i < n; i++) {
			outb.add(tmpb.get(n - i - 1));
		}

		msg = String.format("wordAlign(), end");
		System.out.println(msg);

		return 1;
	}

	public int genAjAsrWordsLists(List<String> ajLines2, List<String> asrLines2, List<AjAsrWordInfo> allAjWords,
			List<AjAsrWordInfo> allAsrWords) {
		int i, j, k, n, ajNofLines, asrNofLines, nofWords, start, step;
		boolean breakpoint;
		String s2, speaker, lastSpeaker, msg;
		AjAsrWordInfo wordInfo;
		List<String> s = new ArrayList<String>();
		String[] fields, fields2;

		msg = String.format("genAjAsrWordsLists(), start");
		System.out.println(msg);

		lastSpeaker = "";
		ajNofLines = ajLines2.size();
		asrNofLines = asrLines2.size();

		allAsrWords.clear();
		for (i = 0; i < asrNofLines; i++) {
			fields2 = asrLines2.get(i).split(" ");

			wordInfo = new AjAsrWordInfo();

			wordInfo.word = fields2[2];
			wordInfo.wordLen = wordInfo.word.length();
			wordInfo.cleanWord = cleanText(wordInfo.word);

			wordInfo.startTime = Float.parseFloat(fields2[0]);
			wordInfo.duration = Float.parseFloat(fields2[1]);
			wordInfo.endTime = wordInfo.startTime + wordInfo.duration;

			if (fields2.length == 3) {
				// starttime endtime word
				wordInfo.startTimeSILBefore = 0;
				wordInfo.durationSILBefore = 0;
				wordInfo.endTimeSILBefore = 0;
			} else if (fields2.length == 5) {
				// starttime endtime word starttimeSILBefore endtimeSILBefore
				wordInfo.startTimeSILBefore = Float.parseFloat(fields2[3]);
				wordInfo.durationSILBefore = Float.parseFloat(fields2[4]);
				wordInfo.endTimeSILBefore = wordInfo.startTimeSILBefore + wordInfo.durationSILBefore;
			} else {
				// FIXME: Error trap
				breakpoint = true;
			}

			wordInfo.matchLevel = ML_NO_MATCH;
			wordInfo.distance = 0;

			allAsrWords.add(wordInfo);
		}

		allAjWords.clear();
		for (i = 0; i < ajNofLines; i++) {
			s2 = ajLines2.get(i);

			s2 = splitPunct(s2);

			if (s2.isEmpty()) {
				continue;
			}

			s2 = s2.replaceAll("\\s+", " ");
			fields = s2.split(" ");
			nofWords = fields.length;

			start = 0;
			for (j = 0; j < nofWords; j++) {
				if (fields[j].startsWith("<")) {
					for (k = j; k < nofWords; k++) {
						if (fields[k].startsWith(">")) {
							start = k + 1;
							break;
						}
					}
				} else {
					break;
				}
				if (start != -1) {
					break;
				}
			}

			s.clear();
			for (j = start; j < nofWords; j++) {
				s.add(fields[j]);
			}
			n = s.size();

			if (n == 0) {
				continue;
			}

			speaker = "";
			for (j = 1; j < start - 1; j++) {
				speaker += String.format("%s ", fields[j]);
			}
			speaker = speaker.replaceAll(":", "");
			speaker = speaker.trim();

			speaker = speaker.replaceAll("_", "-");
			speaker = ArabicUtils.utf82buck(speaker);
			speaker = speaker.replaceAll(" ", "_");

			if (speaker.isEmpty()) {
				speaker = lastSpeaker;
			} else {
				lastSpeaker = speaker;
			}

			for (j = start; j < nofWords; j++) {
				wordInfo = new AjAsrWordInfo();

				wordInfo.word = fields[j];

				wordInfo.punct = isPunct(wordInfo.word);
				if (wordInfo.punct) {
					// Merge all punctuations together in one word (ex: الخروج
					// ..)
					step = 0;
					for (k = j + 1; k < nofWords; k++) {
						if (isPunct(fields[k])) {
							wordInfo.word += fields[k];
							step++;
						} else {
							break;
						}
					}
					j += step;
				}

				wordInfo.wordLen = wordInfo.word.length();
				wordInfo.cleanWord = cleanText(wordInfo.word);

				wordInfo.speaker = speaker;
				wordInfo.matchLevel = ML_NO_MATCH;
				wordInfo.distance = 0;

				allAjWords.add(wordInfo);
			}
		}

		msg = String.format("genAjAsrWordsLists(), end");
		System.out.println(msg);

		return 1;
	}

	public int saveAjAsrAlign(String ajFilename, List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList,
			BufferedWriter outAlign, BufferedWriter outStatus, List<AlignSegment> alignSegments) {
		int i, j, n, nofWordsAsr, nofWordsAsr3, exactMatch, approxMatch, nofSegments, start1, end1;
		float startTime, endTime, startTime3, endTime3, duration;
		String s2, sAsr, sAsr2, sAsr3, sAsr4, sAsr42, sStartTime, sEndTime, sDuration, sStartTime2, sEndTime2,
				sDuration2, sStartTimeSILBefore, sEndTimeSILBefore, sDurationSILBefore, sStartTimeSILBefore2,
				sEndTimeSILBefore2, sDurationSILBefore2, msg, out, sApproxMatch, normAj, normAsr, segmentID;
		AjAsrWordInfo wordInfoAj, wordInfoAsr, wordInfoAsr2;
		String[] fieldsAsr, fieldsAsr3;
		AlignSegment currAlignSegment;
		int[] wordSegmentID;

		msg = String.format("saveAjAsrAlign(), start");
		System.out.println(msg);

		n = 0;
		nofSegments = alignSegments.size();
		try {
			exactMatch = 0;
			approxMatch = 0;

			n = outAjList.size();

			wordSegmentID = new int[n];
			for (i = 0; i < n; i++) {
				wordSegmentID[i] = -1;
			}
			for (i = 0; i < nofSegments; i++) {
				currAlignSegment = alignSegments.get(i);

				start1 = currAlignSegment.start1;
				end1 = currAlignSegment.end1;

				for (j = start1; j <= end1; j++) {
					wordSegmentID[j] = i;
				}
			}

			s2 = String.format(
					"#\tSegmentID\tAJ:word,speaker\tASR:start,duration,end,startSIL,durationSIL,endSIL\tApproxMatch\tSilenceAfter:duration\tSilenceAfter>Threshold\r\n");
			out = s2;

			for (i = 0; i < n; i++) {
				wordInfoAj = outAjList.get(i);

				wordInfoAsr = outAsrList.get(i);

				sAsr2 = "";
				sAsr4 = "";

				sStartTime = "";
				sDuration = "";
				sEndTime = "";

				sStartTimeSILBefore = "";
				sDurationSILBefore = "";
				sEndTimeSILBefore = "";

				if (!wordInfoAsr.word.isEmpty()) {
					sStartTime = formatTime(wordInfoAsr.startTime, 1);
					sDuration = formatTime(wordInfoAsr.duration, 1);
					sEndTime = formatTime(wordInfoAsr.endTime, 1);

					sStartTime2 = formatTime(wordInfoAsr.startTime, 2);
					sDuration2 = formatTime(wordInfoAsr.duration, 2);
					sEndTime2 = formatTime(wordInfoAsr.endTime, 2);

					if (i < n - 1) {
						wordInfoAsr2 = outAsrList.get(i + 1);

						if (!wordInfoAsr2.word.isEmpty()) {
							duration = wordInfoAsr2.startTime - wordInfoAsr.endTime;

							if (duration >= MAX_SILENCE_BETWEEN_WORDS) {
								sAsr4 = formatTime(duration, 1);
								sAsr42 = formatTime(duration, 2);
								sAsr4 += String.format("\t1");
							}
						}
					}

					if (wordInfoAsr.durationSILBefore > 0) {
						sStartTimeSILBefore = formatTime(wordInfoAsr.startTimeSILBefore, 1);
						sDurationSILBefore = formatTime(wordInfoAsr.durationSILBefore, 1);
						sEndTimeSILBefore = formatTime(wordInfoAsr.endTimeSILBefore, 1);

						sStartTimeSILBefore2 = formatTime(wordInfoAsr.startTimeSILBefore, 2);
						sDurationSILBefore2 = formatTime(wordInfoAsr.durationSILBefore, 2);
						sEndTimeSILBefore2 = formatTime(wordInfoAsr.endTimeSILBefore, 2);

						sAsr2 = String.format("%s  %s  %s  %s # %s  %s  %s", wordInfoAsr.word, sStartTime, sDuration,
								sEndTime, sStartTimeSILBefore, sDurationSILBefore, sEndTimeSILBefore);
					} else {
						sAsr2 = String.format("%s  %s  %s  %s", wordInfoAsr.word, sStartTime, sDuration, sEndTime);
					}
				}

				sApproxMatch = "";
				if (!wordInfoAj.word.isEmpty() && !wordInfoAsr.word.isEmpty()) {
					if (wordInfoAj.matchLevel == ML_APPROX_MATCH) {
						approxMatch++;
						sApproxMatch = "1";
					} else {
						exactMatch++;
					}
				}

				segmentID = "";
				// if (!wordInfoAj.word.isEmpty())
				{
					segmentID = String.format("%d", wordSegmentID[i]);
				}

				s2 = String.format("%d\t%s\t%s  %s\t%s\t%s\r\n", i, segmentID, wordInfoAj.word, wordInfoAj.speaker,
						sAsr2, sApproxMatch, sAsr4);
				out += s2;
			}
			outAlign.write(out);
			// outAlign.flush();

			out = String.format(
					"MATCH\tFilename:%s\tNofWords:%d\tExactMatch:%d\t%%:%d\tApproxMatch:%d\t%%:%d\tAnyMatch:%d\t%%:%d\r\n",
					ajFilename, n, exactMatch, (exactMatch * 100) / n, approxMatch, (approxMatch * 100) / n,
					(exactMatch + approxMatch), ((exactMatch + approxMatch) * 100) / n);
			outStatus.write(out);
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("saveAjAsrAlign(), end");
		System.out.println(msg);

		return n;
	}

	public int alignAljazeeraWithAsr(List<String> ajLines2, List<String> asrLines2, BufferedWriter outAlign,
			BufferedWriter outSrt, BufferedWriter outTrs, String ajFilename, BufferedWriter outTra,
			BufferedWriter outStatus, HashMap<String, SpeakerInfo> speakersMap) {
		boolean punct;
		int ajNofLines, asrNofLines, i, j, k, n, m, nofWords, nofWordsAj, nofWordsAj2, nofWordsAsr, nofWordsAsr0,
				nofWordsAsr2, nofWordsAsr3, start, start1, end1, start2, end2, srtIndex, breakpoint, punctIndex,
				maxWindowIndex, minWindowIndex, nofSegments;
		float startTime, endTime, startTime0, endTime0, startTime2, endTime2, startTime3, endTime3, duration, duration3;
		String s2, sAj, sAj2, sAsr, sAsr0, sAsr2, sAsr3, sAsr4, speaker, lastSpeaker, remainingAj, sStartTime, sEndTime,
				msg;
		String[] fields, fields2, fieldsAj, fieldsAj2, fieldsAsr, fieldsAsr0, fieldsAsr2, fieldsAsr3;

		List<AjAsrWordInfo> allAjWords = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> allAsrWords = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> outAjList = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> outAsrList = new ArrayList<AjAsrWordInfo>();

		List<AlignSegment> alignSegments = new ArrayList<AlignSegment>();
		AlignSegment alignSegment;

		msg = String.format("alignAljazeeraWithAsr(), start");
		System.out.println(msg);

		try {
			genAjAsrWordsLists(ajLines2, asrLines2, allAjWords, allAsrWords);

			wordAlign(allAjWords, allAsrWords, outAjList, outAsrList);

			assignTimetoAj(outAjList, outAsrList);

			nofSegments = getAlignSegments(outAjList, outAsrList, alignSegments);
			n = saveAjAsrAlign(ajFilename, outAjList, outAsrList, outAlign, outStatus, alignSegments);

			genSrtOutput(outAjList, outAsrList, alignSegments, outSrt, outTrs, ajFilename, outTra, outStatus,
					speakersMap);
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("alignAljazeeraWithAsr(), end");
		System.out.println(msg);

		return 1;
	}

	public int assignTimetoAj(List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList) {
		int i, j, nAj, nAsr, startIndex, endIndex, nofInWords, currNofInWords, breakpoint, debugLine;
		float startTime1, endTime1, startTime2, endTime2, duration, currStart;
		String msg;
		AjAsrWordInfo wordInfoAj, wordInfoAj2, wordInfoAsr, wordInfoAsr2;

		msg = String.format("assignTimetoAj(), start");
		System.out.println(msg);

		nAj = outAjList.size();

		for (i = 0; i < nAj; i++) {
			wordInfoAj = outAjList.get(i);

			if (!wordInfoAj.word.isEmpty()) {
				if (!wordInfoAj.punct && (i == nAj - 1)) {
					wordInfoAj.punct = true;
				}

				wordInfoAsr = outAsrList.get(i);

				if (!wordInfoAsr.word.isEmpty()) {
					wordInfoAj.startTime = wordInfoAsr.startTime;
					wordInfoAj.duration = wordInfoAsr.duration;
					wordInfoAj.endTime = wordInfoAsr.endTime;

					wordInfoAj.startTimeSILBefore = wordInfoAsr.startTimeSILBefore;
					wordInfoAj.durationSILBefore = wordInfoAsr.durationSILBefore;
					wordInfoAj.endTimeSILBefore = wordInfoAsr.endTimeSILBefore;

					wordInfoAj.mappingIndex = i;

					if (wordInfoAj.cleanWord.compareTo(wordInfoAsr.cleanWord) == 0) {
						wordInfoAj.matchLevel = ML_EXACT_MATCH;
					} else {
						wordInfoAj.matchLevel = ML_APPROX_MATCH;
					}
				}
			}
		}

		debugLine = 3400;
		for (i = 0; i < nAj; i++) {
			if (i == debugLine) {
				breakpoint = 1;
			}
			wordInfoAj = outAjList.get(i);

			if (!wordInfoAj.word.isEmpty() && wordInfoAj.mappingIndex < 0) {
				startIndex = -1;
				endIndex = -1;
				nofInWords = 0;

				for (j = i - 1; j >= 0; j--) {
					wordInfoAj2 = outAjList.get(j);
					if (wordInfoAj2.mappingIndex >= 0) {
						startIndex = j;
						break;
					}
				}
				if (startIndex < 0) {
					startIndex = 0;
				}

				for (j = i + 1; j < nAj; j++) {
					wordInfoAj2 = outAjList.get(j);
					if (wordInfoAj2.mappingIndex >= 0) {
						endIndex = j;
						break;
					}
				}

				if (endIndex < 0) {
					// Last words of Aj
					wordInfoAj2 = outAjList.get(startIndex);
					startTime1 = wordInfoAj2.startTime;
					endTime1 = wordInfoAj2.endTime;

					endIndex = nAj - 1;
					for (j = startIndex + 1; j <= endIndex; j++) {
						wordInfoAj2 = outAjList.get(j);

						wordInfoAj2.startTime = endTime1;
						wordInfoAj2.duration = 0;
						wordInfoAj2.endTime = wordInfoAj2.startTime + wordInfoAj2.duration;
					}
				} else {
					for (j = startIndex + 1; j <= endIndex - 1; j++) {
						wordInfoAj2 = outAjList.get(j);
						if (!wordInfoAj2.word.isEmpty()) {
							nofInWords++;
						}
					}

					if (nofInWords == 0) {
						// FIXME: Error trap
						breakpoint = 1;
					}

					wordInfoAj2 = outAjList.get(startIndex);
					startTime1 = wordInfoAj2.startTime;
					endTime1 = wordInfoAj2.endTime;

					if (wordInfoAj2.mappingIndex < 0) {
						wordInfoAsr = outAsrList.get(startIndex);
						if (!wordInfoAsr.word.isEmpty()) {
							// FIXME: Needs debugging
							startTime1 = wordInfoAsr.startTime;
							// endTime1 = wordInfoAsr.endTime;
							endTime1 = wordInfoAsr.startTime;
						} else {
							// FIXME: Error trap
							breakpoint = 1;
						}
					}

					wordInfoAj2 = outAjList.get(endIndex);
					startTime2 = wordInfoAj2.startTime;
					endTime2 = wordInfoAj2.endTime;

					if (wordInfoAj2.mappingIndex < 0) {
						wordInfoAsr = outAsrList.get(endIndex);
						if (!wordInfoAsr.word.isEmpty()) {
							// FIXME: Needs debugging
							startTime2 = wordInfoAsr.startTime;
							endTime2 = wordInfoAsr.endTime;
						} else {
							// FIXME: Error trap
							breakpoint = 1;
						}
					}

					if (endTime1 < 0) {
						// FIXME: Needs debugging
						startTime1 = startTime2;
						endTime1 = endTime2;
					}

					if (nofInWords == 0) {
						nofInWords = 1;
					}
					duration = (startTime2 - endTime1) / nofInWords;

					currNofInWords = 0;
					currStart = endTime1;
					for (j = startIndex + 1; j <= endIndex - 1; j++) {
						wordInfoAj2 = outAjList.get(j);
						if (!wordInfoAj2.word.isEmpty()) {
							wordInfoAj2.startTime = currStart;
							wordInfoAj2.duration = duration;
							wordInfoAj2.endTime = wordInfoAj2.startTime + wordInfoAj2.duration;

							currStart += duration;
							currNofInWords++;
						}
					}

					if (currNofInWords == 0) {
						// FIXME: Needs debugging
						for (j = startIndex; j <= endIndex - 1; j++) {
							wordInfoAj2 = outAjList.get(j);
							if (!wordInfoAj2.word.isEmpty()) {
								wordInfoAj2.startTime = startTime1;
								wordInfoAj2.duration = 0;
								wordInfoAj2.endTime = endTime1;
							}
						}
					}

					i += (nofInWords - 1);
				}
			}
		}

		msg = String.format("assignTimetoAj(), end");
		System.out.println(msg);

		return 1;
	}

	public int getAlignSegments(List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList,
			List<AlignSegment> alignSegments) {
		boolean noStop, punct, split, breakpoint;
		int i, j, k, m, n, nofSegments, start1, end1, start2, end2, punctIndex, maxWindowIndex, minWindowIndex,
				dbgIndex, nofExtraWords, nofSegWords, correctSegWords, nofIns, nofDel, nofMatchWords, nofMatchChar,
				len1, len2, segmentNofChar, ajWordLen, asrWordLen;
		float segmentDuration;
		String msg, segmentSpeaker, ajCleanCharSegment, asrCleanCharSegment;
		AjAsrWordInfo wordInfo1, wordInfo2, wordInfo3, wordInfo_1;
		AlignSegment alignSegment;

		msg = String.format("getAlignSegments(), start");
		System.out.println(msg);

		nofSegments = 0;

		start1 = -1;
		end1 = -1;
		dbgIndex = 250;
		segmentSpeaker = "";
		segmentNofChar = 0;

		n = outAjList.size();

		for (i = end1 + 1; i < n; i++) {
			if (i == dbgIndex) {
				breakpoint = true;
			}
			// Prefer to split on punctuations
			start1 = -1;
			end1 = -1;
			segmentDuration = 0;

			if (SPLIT_ON_SILENCE_ONLY == true) {
				for (j = i; j < n; j++) {
					wordInfo1 = outAjList.get(j);
					len1 = wordInfo1.wordLen;

					if (!wordInfo1.word.isEmpty() && !wordInfo1.punct) {
						if (start1 < 0) {
							start1 = j;
							segmentDuration = 0;
						}

						if (wordInfo1.durationSILBefore > 0) {
							if (segmentDuration >= MIN_SEGMENT_DURATION) {
								end1 = j - 1;
								break;
							}
						} else {
							breakpoint = true;
						}

						segmentDuration += wordInfo1.duration;
					}
				}
				if (end1 < 0) {
					end1 = n - 1;
				}
			} else {
				if (SPLIT_ON_MAX_LEN == false) {
					for (j = i; j < n; j++) {
						wordInfo1 = outAjList.get(j);
						len1 = wordInfo1.wordLen;

						if (start1 < 0) {
							if (!wordInfo1.word.isEmpty()) {
								start1 = j;
								segmentSpeaker = wordInfo1.speaker;
							}
						}

						if (wordInfo1.punct && (j - i >= (MAX_SRT_WORDS_PER_LINE / 2) - 1)) {
							end1 = j;
							break;
						}

						if ((start1 >= 0) && (j > start1) && !wordInfo1.word.isEmpty()) {
							if (wordInfo1.speaker.compareTo(segmentSpeaker) != 0) {
								// Switch to another speaker
								end1 = j - 1;
								break;
							}
						}

						if ((j - i == MAX_SRT_WORDS_PER_LINE - 1) || (j == n - 1)) {
							// Look ahead searching for the nearset punct
							punctIndex = -1;
							maxWindowIndex = n - 1;
							nofExtraWords = 0;
							for (k = j + 1; k < n; k++) {
								wordInfo2 = outAjList.get(k);
								len2 = wordInfo2.wordLen;

								if (!wordInfo2.word.isEmpty()) {
									if ((start1 >= 0) && (k > start1)
											&& wordInfo2.speaker.compareTo(segmentSpeaker) != 0) {
										// Switch to another speaker
										maxWindowIndex = k - 1;
										break;
									}

									nofExtraWords++;
									if (nofExtraWords == EXTRA_WORDS_PER_LINE) {
										maxWindowIndex = k;
										break;
									}
								}
							}
							// maxWindowIndex = Math.min(j +
							// EXTRA_WORDS_PER_LINE, n - 1);
							for (k = j; k <= maxWindowIndex; k++) {
								wordInfo2 = outAjList.get(k);

								if (wordInfo2.punct) {
									punctIndex = k;
									end1 = k;
									break;
								}
							}

							if (punctIndex < 0) {
								// Look for silence period
								minWindowIndex = Math.max(j - (MAX_SRT_WORDS_PER_LINE / 2) + 2, 1); // EXTRA_WORDS_PER_LINE
																									// +
																									// 2
								maxWindowIndex = Math.min(j + EXTRA_WORDS_PER_LINE, n - 1);

								for (k = minWindowIndex; k <= maxWindowIndex; k++) {
									wordInfo2 = outAjList.get(k);

									if ((start1 >= 0) && (k > start1) && !wordInfo2.word.isEmpty()
											&& wordInfo2.speaker.compareTo(segmentSpeaker) != 0) {
										// Switch to another speaker
										end1 = k - 1;
										break;
									}

									wordInfo_1 = outAjList.get(k - 1);

									if (!wordInfo2.word.isEmpty() && !wordInfo_1.word.isEmpty()) {
										if ((wordInfo2.startTime - wordInfo_1.endTime) >= MAX_SILENCE_BETWEEN_WORDS) {
											noStop = foundInNoStopList(wordInfo_1);

											if (noStop) {
												// Return to the nearest
												// non-stop
												for (m = k - 2; m >= minWindowIndex; m--) {
													wordInfo3 = outAjList.get(m);
													if (!wordInfo3.word.isEmpty()) {
														noStop = foundInNoStopList(wordInfo3);

														if (!noStop) {
															end1 = m;
															break;
														}
													}
												}
												if (end1 >= 0) {
													break;
												}
											} else {
												end1 = k - 1;
												break;
											}
										}
									}
								}
							}

							break;
						}
					}

					if (start1 < 0) {
						start1 = i;
					}
					if (end1 < 0) {
						maxWindowIndex = Math.min(start1 + MAX_SRT_WORDS_PER_LINE - 1, n - 1);
						for (k = start1; k <= maxWindowIndex; k++) {
							wordInfo2 = outAjList.get(k);

							if (!wordInfo2.word.isEmpty()) {

								if ((start1 >= 0) && (k > start1) && wordInfo2.speaker.compareTo(segmentSpeaker) != 0) {
									break;
								}

								end1 = k;
							}
						}

						if (end1 >= 0) {
							wordInfo2 = outAjList.get(end1);
							noStop = foundInNoStopList(wordInfo2);

							if (noStop) {
								// Return to the nearest non-stop
								for (m = end1 - 1; m >= start1 + (MAX_SRT_WORDS_PER_LINE / 2); m--) {
									wordInfo3 = outAjList.get(m);
									if (!wordInfo3.word.isEmpty()) {
										noStop = foundInNoStopList(wordInfo3);

										if (!noStop) {
											end1 = m;
											break;
										}
									}
								}
							}
						}

						if (end1 < 0) {
							// FIXME: error trap
							breakpoint = true;
						}
					}
				} else {
					for (j = i; j < n; j++) {
						wordInfo1 = outAjList.get(j);
						len1 = wordInfo1.wordLen;

						if (start1 < 0) {
							if (!wordInfo1.word.isEmpty()) {
								start1 = j;
								segmentSpeaker = wordInfo1.speaker;

								segmentNofChar = len1 + 1;
								end1 = j;
							}
						}

						if ((start1 >= 0) && (j > start1) && !wordInfo1.word.isEmpty()) {
							if (wordInfo1.speaker.compareTo(segmentSpeaker) != 0) {
								// Switch to another speaker
								end1 = j - 1;
								break;
							}

							if (USE_COLORS) {
								if ((segmentNofChar + len1 + 1) > MAX_SRT_CHAR_PER_LINE) {
									split = true;
									if (wordInfo1.punct) {
										split = false;
									}
									// if (j < n - 1)
									// {
									// wordInfo2 = outAjList.get(j + 1);
									// if (wordInfo2.punct)
									// {
									// split = false;
									// }
									// }

									if (split) {
										end1 = j - 1;
										break;
									}
								}
							}
							segmentNofChar += len1 + 1;
							end1 = j;
						}
					}
				}

				// Change the end to the nearest existing word
				if (end1 >= 0) {
					wordInfo1 = outAjList.get(end1);
					if (wordInfo1.word.isEmpty()) {
						// FIXME: Error trap
						for (j = end1 - 1; j >= start1; j--) {
							wordInfo1 = outAjList.get(j);

							if (!wordInfo1.word.isEmpty()) {
								end1 = j;
								break;
							}
						}
					}
				}
			}

			if ((start1 >= 0) && (end1 >= 0)) {
				nofSegWords = 0;
				correctSegWords = 0;
				nofIns = 0;
				nofDel = 0;
				nofMatchWords = 0;
				nofMatchChar = 0;
				start2 = -1;
				end2 = -1;
				for (k = start1; k <= end1; k++) {
					punct = false;
					wordInfo1 = outAjList.get(k);
					if (!wordInfo1.word.isEmpty()) {
						if (!wordInfo1.punct) {
							nofSegWords++;
						}
					}

					wordInfo2 = outAsrList.get(k);

					if (start2 < 0) {
						if (!wordInfo2.word.isEmpty()) {
							start2 = k;
						}
					}

					if (!wordInfo2.word.isEmpty()) {
						end2 = k;
					}

					if (!punct) {
						if (!wordInfo1.word.isEmpty()) {
							if (!wordInfo2.word.isEmpty()) {
								// if (wordInfo1.matchLevel == ML_EXACT_MATCH)
								// {
								// nofMatchChar += wordInfo1.wordLen;
								// }
								// else if (wordInfo1.matchLevel ==
								// ML_APPROX_MATCH)
								// {
								// nofMatchChar +=
								// getNofMatchedChar(wordInfo1.word,
								// wordInfo2.word);
								// }
								nofMatchWords++;
							} else {
								nofDel++;
							}
						} else if (!wordInfo2.word.isEmpty()) {
							nofIns++;
						}
					}
				}

				// correctSegWords = nofSegWords - Math.abs(nofDel - nofIns);
				correctSegWords = nofSegWords - (nofDel + nofIns);

				alignSegment = new AlignSegment();

				alignSegment.start1 = start1;
				alignSegment.end1 = end1;
				alignSegment.start2 = start2;
				alignSegment.end2 = end2;

				alignSegment.nofSegWords = nofSegWords;
				alignSegment.correctSegWords = correctSegWords;

				alignSegment.nofIns = nofIns;
				alignSegment.nofDel = nofDel;
				alignSegment.nofMatchWords = nofMatchWords;

				alignSegment.asrStartTimeExists = 0;
				alignSegment.asrEndTimeExists = 0;

				alignSegment.nofChar = 0;
				alignSegment.charDistance = 0;

				// Mark segments with existing ASR time for start and end
				ajCleanCharSegment = "";
				asrCleanCharSegment = "";
				nofSegWords = 0;
				for (k = start1; k <= end1; k++) {
					punct = false;
					wordInfo1 = outAjList.get(k);
					if (!wordInfo1.word.isEmpty()) {
						if (!wordInfo1.punct) {
							nofSegWords++;

							ajWordLen = wordInfo1.word.length();
							for (m = 0; m < ajWordLen; m++) {
								ajCleanCharSegment += String.format("%c ", wordInfo1.word.charAt(m));
							}
						}
					}

					wordInfo2 = outAsrList.get(k);

					if (!wordInfo2.word.isEmpty()) {
						asrWordLen = wordInfo2.word.length();
						for (m = 0; m < asrWordLen; m++) {
							asrCleanCharSegment += String.format("%c ", wordInfo2.word.charAt(m));
						}
					}

					if (!punct) {
						if (!wordInfo1.word.isEmpty()) {
							if (!wordInfo2.word.isEmpty()) {
								if (k == start1) {
									alignSegment.asrStartTimeExists = 1;
								}
								if (nofSegWords == alignSegment.nofSegWords) {
									alignSegment.asrEndTimeExists = 1;
								}
							}
						}
					}

				}

				ajCleanCharSegment = ajCleanCharSegment.trim();
				asrCleanCharSegment = asrCleanCharSegment.trim();

				alignSegment.nofChar = ajCleanCharSegment.length();
				alignSegment.charDistance = LevenshteinDistance(ajCleanCharSegment, asrCleanCharSegment);

				m = LevenshteinDistance("A h m e d", "M o h a m e d");

				if ((start1 < 0) || (end1 < 0) || (start2 < 0) || (end2 < 0)) {
					// FIXME: error trap
					breakpoint = true;
				}

				// msg = String.format("[i=%d] alignSegment:%d-%d to %d-%d,
				// nofSegWords:%d, nofCorrectSegWords:%d", i, start1, end1,
				// start2, end2, nofSegWords, correctSegWords);
				// System.out.println (msg);

				alignSegments.add(alignSegment);

				nofSegments++;
			} else {
				// FIXME: Error trap
				breakpoint = true;
			}

			if (end1 > i) {
				i = end1;
			} else {
				// FIXME
				breakpoint = true;
			}
		}

		msg = String.format("getAlignSegments(), end");
		System.out.println(msg);

		return nofSegments;
	}

	public int getNofMatchedChar(String word1, String word2) {
		int i, n, match = 0;

		n = word1.length();

		return match;
	}

	public int genSrtOutput(List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList,
			List<AlignSegment> alignSegments, BufferedWriter outSrt, BufferedWriter outTrs, String ajFilename,
			BufferedWriter outTra, BufferedWriter outStatus, HashMap<String, SpeakerInfo> speakersMap) {
		int i, j, k, c, nofSegments, lastUsedColorIndex, nofColors, segmentLen, segmentLineLen, startTimeIndex,
				endTimeIndex;
		float startTime, endTime, segmentDuration;
		String s2, s3, sAj, sAsr, speaker, lastSpeaker, buckwalterSpeaker, sStartTime, sEndTime, sStartTime2, sEndTime2,
				msg, allSrt, allTrs, allTra, allStatus, color, color2, normAj, normAsr;

		// Sameer
		List<String[]> sAjArr = new ArrayList<String[]>(), sAsrArr = new ArrayList<String[]>();
		List<String[]> sAjArrChar = new ArrayList<String[]>();
		List<String[]> sAsrArrChar = new ArrayList<String[]>();
		String sAjBuk = null;
		String sAsrBuk = null;

		SpeakerInfo foundSpeakerInfo;
		AjAsrWordInfo wordInfoAj, wordInfoAj2, wordInfoAj3, wordInfoAsr, wordInfo;
		AlignSegment alignSegment;
		WordSequenceAligner aligner = new WordSequenceAligner(2, 1, 1);
		HashMap<String, String> speakersColorsMap = new HashMap<String, String>();

		msg = String.format("genSrtOutput(), start");
		System.out.println(msg);

		try {
			nofSegments = alignSegments.size();

			speaker = "";
			lastSpeaker = "";
			allStatus = "";

			allSrt = "";
			allTrs = "";
			allTra = "";

			nofColors = SPEAKERS_COLORS.length;
			lastUsedColorIndex = 0;
			color = SPEAKERS_COLORS[0];

			for (i = 0; i < nofSegments; i++) {
				alignSegment = alignSegments.get(i);

				sStartTime = "";
				sEndTime = "";
				sStartTime2 = "";
				sEndTime2 = "";

				sAj = "";
				segmentLen = 0;
				segmentLineLen = 0;
				startTime = -1.0f;
				endTime = -1.0f;
				startTimeIndex = -1;
				endTimeIndex = -1;
				for (j = alignSegment.start1; j <= alignSegment.end1; j++) {
					wordInfoAj = outAjList.get(j);

					if (!wordInfoAj.word.isEmpty()) {
						if (SAVE_DEBUG_INFO) {
							wordInfoAsr = outAsrList.get(j);
							if (!wordInfoAsr.word.isEmpty()) {
								if (USE_COLORS) {
									if (wordInfoAj.matchLevel == ML_EXACT_MATCH) {
										sAj += String.format("<u><b>%s</b></u>&nbsp;", wordInfoAj.word);
									} else {
										sAj += String.format("<u>%s</u>&nbsp;", wordInfoAj.word);
									}
								} else {
									sAj += String.format("%s ", wordInfoAj.word);
								}
							} else {
								if (USE_COLORS) {
									sAj += String.format("%s&nbsp;", wordInfoAj.word);
								} else {
									sAj += String.format("%s ", wordInfoAj.word);
								}
							}
						} else {
							sAj += String.format("%s ", wordInfoAj.word);
						}

						segmentLen += wordInfoAj.wordLen + 1;
						segmentLineLen += wordInfoAj.wordLen + 1;

						gNofAllSegments++;
						gAllSegmentsLen += wordInfoAj.wordLen + 1;

						if ((segmentLineLen >= MAX_SRT_CHAR_PER_LINE) && (j < alignSegment.end1) && USE_COLORS) {
							for (k = j + 1; k <= alignSegment.end1; k++) {
								wordInfoAj2 = outAjList.get(k);
								if (!wordInfoAj2.word.isEmpty()) {
									if (!wordInfoAj2.punct) {
										sAj += "\r\n";
										segmentLineLen = 0;
									}
									break;
								}
							}
						}
					}

					if (j == alignSegment.start1) {
						speaker = wordInfoAj.speaker;
						startTime = wordInfoAj.startTime;

						if (SPLIT_ON_SILENCE_ONLY) {
							startTime -= (MIN_SILENCE_BETWEEN_SEGMENTS / 3);
							if (startTime < 0) {
								startTime = 0;
							}
						}

						sStartTime = formatTime(startTime, 1);
						sStartTime2 = formatTime(startTime, 2);

						startTimeIndex = j;
					}

					if (SPLIT_ON_SILENCE_ONLY) {
						if (j == alignSegment.end2) {
							wordInfoAsr = outAsrList.get(j);
							endTime = wordInfoAsr.endTime;

							endTime += (MIN_SILENCE_BETWEEN_SEGMENTS / 3);
							sEndTime = formatTime(endTime, 1);
							sEndTime2 = formatTime(endTime, 2);

							endTimeIndex = j;
						}
					} else {
						if (j == alignSegment.end1) {
							sEndTime = formatTime(wordInfoAj.endTime, 1);
							sEndTime2 = formatTime(wordInfoAj.endTime, 2);
							endTime = wordInfoAj.endTime;

							endTimeIndex = j;

							if (SAVE_DEBUG_INFO && wordInfoAj.punct && USE_COLORS) {
								sAj += "ـ";
							}
						}
					}
				}

				if ((startTime < 0.0f) || (endTime < 0.0f)) {
					allStatus += String.format(
							"START_OR_END_TIME_ERROR\tFile:%s\tStartTime:%s\tEndTime:%s\tText:%s\r\n", ajFilename,
							sStartTime, sEndTime, sAj);
				}
				segmentDuration = endTime - startTime;
				if (!SPLIT_ON_SILENCE_ONLY && (segmentDuration > MAX_SEGMENT_DURATION)) {
					wordInfoAj2 = outAjList.get(startTimeIndex);
					wordInfoAj3 = outAjList.get(endTimeIndex);

					if (wordInfoAj3.matchLevel != ML_EXACT_MATCH) {
						if (alignSegment.nofSegWords > ((int) MAX_SEGMENT_DURATION / (int) MAX_WORD_DURATION)) {
							endTime = startTime + MAX_SEGMENT_DURATION;
						} else {
							endTime = startTime + alignSegment.nofSegWords * MAX_WORD_DURATION;
						}
						sEndTime = formatTime(endTime, 1);
						sEndTime2 = formatTime(endTime, 2);
					} else {
						if (alignSegment.nofSegWords > ((int) MAX_SEGMENT_DURATION / (int) MAX_WORD_DURATION)) {
							startTime = endTime - MAX_SEGMENT_DURATION;
						} else {
							startTime = endTime - alignSegment.nofSegWords * MAX_WORD_DURATION;
						}
						sStartTime = formatTime(startTime, 1);
						sStartTime2 = formatTime(startTime, 2);
					}
				}

				sAj = sAj.trim();

				// Sameer
				sAjBuk = MGBUtil.getNormaliseBukTranscriptString(sAj);
				sAjArr.add(sAjBuk.split(" "));
				sAjArrChar.add(sAjBuk.replaceAll(" ", "").split(""));

				if (sAj.isEmpty()) {
					allStatus += String.format("EMPTY_SEGMENT\tFile:%s\tStartTime:%s\tEndTime:%s\tText:%s\r\n",
							ajFilename, sStartTime, sEndTime, sAj);
				}

				if (SAVE_DEBUG_INFO) {
					// sAj += String.format(" Len:%d", segmentLen);
					// if (alignSegment.nofSegWords > 0)
					// {
					// sAj += String.format(" Score:%d", (alignSegment.nofMatch
					// * 100 )/ alignSegment.nofSegWords);
					// }
				}

				// Sameer
				sAsr = "";
				for (int p = alignSegment.start2; p <= alignSegment.end2; p++) {
					wordInfo = null;
					if (p > -1) {
						wordInfo = outAjList.get(p);
					}
					if (wordInfo != null) {
						if (!wordInfo.word.isEmpty()) {
							sAsr += String.format("%s ", wordInfo.word);
						}
					}

				}

				sAsr = sAsr.trim();
				sAsrBuk = MGBUtil.getNormaliseBukTranscriptString(sAsr);
				sAsrArr.add(sAsrBuk.split(" "));
				sAsrArrChar.add(sAsrBuk.replaceAll(" ", "").split(""));

				buckwalterSpeaker = speaker;
				speaker = speaker.replaceAll("_", " ");
				speaker = ArabicUtils.buck2utf8Arb(speaker);

				if (speaker.compareTo(lastSpeaker) != 0) {
					color2 = speakersColorsMap.get(speaker);
					if (color2 != null) {
						color = color2;
					} else {
						c = lastUsedColorIndex;
						color = SPEAKERS_COLORS[c];
						lastUsedColorIndex++;
						if (lastUsedColorIndex == nofColors) {
							lastUsedColorIndex = 0;
						}
						speakersColorsMap.put(speaker, color);
					}

					s3 = "";
					if (USE_COLORS) {
						s2 = String.format(
								"%d\r\n%s --> %s\r\n<font color=\"%s\"><font color=\"darkgray\">:%s</font>\r\n %s</font>\r\n\r\n",
								i + 1, sStartTime, sEndTime, color, speaker, sAj);
						s3 = String.format("<Sync time=\"%s\"/>\r\n%s\r\n<Sync time=\"%s\"/>\r\n%s\r\n", sStartTime2,
								sAj, sEndTime2, "");
					} else {
						if (SAVE_SPEAKER_NAME) {
							s2 = String.format("%d\r\n%s --> %s\r\n%s: %s\r\n\r\n", i + 1, sStartTime, sEndTime,
									speaker, sAj);
						} else {
							s2 = String.format("%d\r\n%s --> %s\r\n%s\r\n\r\n", i + 1, sStartTime, sEndTime, sAj);
						}
						s3 = String.format("<Sync time=\"%s\"/>\r\n%s\r\n<Sync time=\"%s\"/>\r\n%s\r\n", sStartTime2,
								sAj, sEndTime2, "");
					}

					foundSpeakerInfo = speakersMap.get(speaker);
					if (foundSpeakerInfo == null) {
						foundSpeakerInfo = new SpeakerInfo();
						foundSpeakerInfo.speakerName = speaker;
						foundSpeakerInfo.count = 1;

						speakersMap.put(speaker, foundSpeakerInfo);
					} else {
						foundSpeakerInfo.count++;
					}
				} else {
					s3 = "";
					if (USE_COLORS) {
						s2 = String.format("%d\r\n%s --> %s\r\n<font color=\"%s\">%s</font>\r\n\r\n", i + 1, sStartTime,
								sEndTime, color, sAj);
					} else {
						s2 = String.format("%d\r\n%s --> %s\r\n%s\r\n\r\n", i + 1, sStartTime, sEndTime, sAj);
					}
					s3 = String.format("<Sync time=\"%s\"/>\r\n%s\r\n<Sync time=\"%s\"/>\r\n%s\r\n", sStartTime2, sAj,
							sEndTime2, "");
				}

				allSrt += s2;
				allTrs += s3;

				// s2 = String.format("%s_%s_%s_%s
				// %s\t%d\t%d(%d%%)\tIns:%d,Del:%d,Match:%d (%d%%)\r\n",
				// ajFilename, buckwalterSpeaker, sStartTime, sEndTime, sAj,
				// alignSegment.nofSegWords, alignSegment.correctSegWords,
				// (alignSegment.correctSegWords * 100) /
				// alignSegment.nofSegWords, alignSegment.nofIns,
				// alignSegment.nofDel, alignSegment.nofMatch,
				// alignSegment.nofMatch / alignSegment.nofSegWords);

				// Sameer
				List<Alignment> alignmentInfo = aligner.align(sAjArr, sAsrArr);
				List<Alignment> alignmentCharacter = aligner.align(sAjArrChar, sAsrArrChar);
				Alignment wordLevelALignment = alignmentInfo.get(i);
				Alignment characterLevelAlign = alignmentCharacter.get(i);
				double leveDistWordLevel = MGBUtil.round(((double) (wordLevelALignment.numDeletions
						+ wordLevelALignment.numInsertions + wordLevelALignment.numSubstitutions))
						/ ((double) wordLevelALignment.getReferenceLength()), 2);
				double leveDistanceCharLevel = MGBUtil.round(((double) (characterLevelAlign.numDeletions
						+ characterLevelAlign.numInsertions + characterLevelAlign.numSubstitutions))
						/ ((double) characterLevelAlign.getReferenceLength()), 2);

				if (alignSegment.nofSegWords > 0) {
					// s2 = String.format("%s_%s_%s_%s
					// %s\tWords:%d\tCorrect:%d\tCorrect%%:%d%%\tIns:%d\tDel:%d\tMatch:%d\tMatch%%:%d%%\tStart:%d\tEnd:%d\r\n",
					// ajFilename, buckwalterSpeaker, sStartTime, sEndTime, sAj,
					// alignSegment.nofSegWords, alignSegment.correctSegWords,
					// (alignSegment.correctSegWords * 100) /
					// alignSegment.nofSegWords, alignSegment.nofIns,
					// alignSegment.nofDel, alignSegment.nofMatchWords,
					// (alignSegment.nofMatchWords * 100 )/
					// alignSegment.nofSegWords,
					// alignSegment.asrStartTimeExists,
					// alignSegment.asrEndTimeExists);
					// Sameer
					s2 = String.format(
							"%s_%s_%s_%s %s\tWords:%d Correct:%d\tCorrect:%d\tIns:%d\tDel:%d\tWMER:%s\tPMER:%s\tAWD:%s\tStart:%d\tEnd:%d\r\n",
							ajFilename, buckwalterSpeaker, sStartTime, sEndTime, sAj, alignSegment.nofSegWords,
							alignSegment.correctSegWords,
							(alignSegment.correctSegWords * 100) / alignSegment.nofSegWords, alignSegment.nofIns,
							alignSegment.nofDel, leveDistWordLevel * 100.0, leveDistanceCharLevel * 100,
							MGBUtil.calculateAWD(sStartTime, sEndTime, alignSegment.nofSegWords),
							alignSegment.asrStartTimeExists, alignSegment.asrEndTimeExists);
				} else {
					// s2 = String.format("%s_%s_%s_%s
					// %s\tWords:%d\tCorrect:%d\tCorrect%%:%d%%\tIns:%d\tDel:%d\tMatch:%d\tMatch%%:%d%%\tStart:%d\tEnd:%d\r\n",
					// ajFilename, buckwalterSpeaker, sStartTime, sEndTime, sAj,
					// alignSegment.nofSegWords, alignSegment.correctSegWords,
					// 0, alignSegment.nofIns, alignSegment.nofDel,
					// alignSegment.nofMatchWords, 0,
					// alignSegment.asrStartTimeExists,
					// alignSegment.asrEndTimeExists);
					// Sameer
					s2 = String.format(
							"%s_%s_%s_%s %s\tWords:%d Correct:%d\tCorrect:%d\tIns:%d\tDel:%d\tWMER:%s\tPMER:%s\tAWD:%s\tStart:%d\tEnd:%d\r\n",
							ajFilename, buckwalterSpeaker, sStartTime, sEndTime, sAj, alignSegment.nofSegWords,
							alignSegment.correctSegWords, 0, alignSegment.nofIns, alignSegment.nofDel, "", "", "",
							alignSegment.asrStartTimeExists, alignSegment.asrEndTimeExists);
				}
				allTra += s2;

				if (buckwalterSpeaker.isEmpty()) {
					allStatus += String.format(
							"SPEAKER_ERROR\tFile:%s\tSpeaker:%s\tStartTime:%s\tEndTime:%s\tText:%s\r\n", ajFilename,
							buckwalterSpeaker, sStartTime, sEndTime, sAj);
				}

				lastSpeaker = speaker;
			}

			outSrt.write(allSrt);
			outTrs.write(allTrs);

			outTra.write(allTra);
			outStatus.write(allStatus);
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("genSrtOutput(), end");
		System.out.println(msg);

		return 1;
	}

	public boolean foundInNoStopList(AjAsrWordInfo wordInfo) {
		boolean stopWord = false;
		String word;
		/*
		 * int i, n;
		 * 
		 * n = NO_STOP_LIST.size(); for (i = 0; i < n; i++) { if
		 * (wordInfo.word.compareTo(NO_STOP_LIST.get(i)) == 0) { stopWord =
		 * true; break; } }
		 */

		word = ArabicUtils.removeDiacritics(wordInfo.word);
		if (NO_STOP_LIST_MAP.get(word) != null) {
			stopWord = true;
		}

		return stopWord;
	}

	public int generateNoStopList(String[] args) {
		File[] rdiFiles;
		int i, j, fileIndex, nofFiles, nofRdiLines, nofWords;
		String filename, s;
		List<String> rdiLines = new ArrayList<String>();
		String[] words;

		rdiFiles = new File("D:\\RDI\\AfterRevision").listFiles();

		fileIndex = 0;
		nofFiles = rdiFiles.length;

		for (File file : rdiFiles) {
			if (file.isFile()) {
				filename = file.getName();

				nofRdiLines = LoadFile(filename, rdiLines);

				for (i = 0; i < nofRdiLines; i++) {
					words = rdiLines.get(i).split(" ");
					nofWords = words.length;

					for (j = 0; j < nofWords; j++) {
						s = ArabicUtils.removeDiacritics(words[j]);
					}
				}

				fileIndex++;
			}
		}

		return 1;
	}
}
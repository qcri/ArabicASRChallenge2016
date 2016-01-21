/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mgbmain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;

import mgbutils.ArabicUtils;
import mgbutils.MGBUtil;
import mgbutils.WordSequenceAligner;
import mgbutils.WordSequenceAligner.Alignment;

/**
 *
 * @author hmubarak
 */
public class ArabicASRChallenge {

	int gNofErrors = 0, gNofSpecialChar = 0;
	boolean gValidateAJOnly = true;// false
	boolean gSaveExtractedContents = true;
	int gProgramName = 0, gSeriesTitle = 0, gSeriesSpeaker = 0, gSeriesGuest = 0, gSeriesDate = 0,
			gSeriesDirections = 0;
	int MAX_NOF_FILES = -1;// 100;

	public class SpeakerInfo {
		public String speakerName;
		public int count;

		public SpeakerInfo() {
			speakerName = "";
			count = 0;
		}
	}

	final int MAX_SRT_WORDS_PER_LINE = 10;
	final int EXTRA_WORDS_PER_LINE = 4;
	final float MAX_SILENCE_BETWEEN_WORDS = 0.15f; // 150 ms
	final float MIN_SEGMENT_DURATION = 2.0f;

	int NOF_NO_STOP_LIST = 0;
	List<String> NO_STOP_LIST = new ArrayList<String>();
	HashMap<String, Integer> NO_STOP_LIST_MAP = new HashMap<String, Integer>();

	public class AjAsrWordInfo {
		String word, speaker;
		float startTime, duration, endTime;
		boolean punct;
		int mappingIndex;

		public AjAsrWordInfo() {
			word = "";
			speaker = "";
			startTime = -1.0f;
			duration = -1.0f;
			endTime = -1.0f;

			punct = false;
			mappingIndex = -1;
		}

		public AjAsrWordInfo(AjAsrWordInfo w) {
			word = w.word;
			speaker = w.speaker;
			startTime = w.startTime;
			duration = w.duration;
			endTime = w.endTime;

			punct = w.punct;
			mappingIndex = w.mappingIndex;
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

		int nofSegWords, nofIns, nofDel, nofMatch, correctSegWords, asrStartTimeExists, asrEndTimeExists;
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

		ArabicASRChallenge asr = new ArabicASRChallenge();

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

			asr.DTWDistance(s, t);

			String traFileName = asr.parseASRFiles(args);
			String newTraFilePath = System.getProperty("user.dir") + "/ALL_MOD.tra";
			MGBUtil.fixTraSpeakers(traFileName, newTraFilePath);
			Class.forName("mgbmain.GenerateXMLTranscription");
			GenerateXMLTranscription.createTranscript(newTraFilePath);
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

	int GetDTWDistance(List<String> s, List<String> asrLines2, int start, int len) {
		int j, dist;
		String s2;
		List<String> t = new ArrayList<String>();
		String[] fields2;

		dist = infinity;
		t.clear();

		for (j = start; j < start + len; j++) {
			s2 = asrLines2.get(j);
			fields2 = s2.split(" ");

			t.add(fields2[2]);
		}

		dist = DTWDistance(s, t);

		return dist;
	}

	public int LoadFile(String filename, List<String> list) {
		int nofLines, dbgLineNo;
		String strLine, msg;

		nofLines = 0;
		dbgLineNo = 1226;
		try {
			list.clear();
			java.io.File file = new java.io.File(filename);
			if (file.exists()) {
				FileInputStream fstream = new FileInputStream(filename);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

				// Read File Line By Line
				while ((strLine = br.readLine()) != null) {
					if (nofLines == dbgLineNo) {
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
		int i, c, charType;
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
				} else {
					s += charInput[i];
				}
			}
		}

		// Normalize text
		s = MGBUtil.normalizeWord(s);

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

	public String splitPunct(String s) {
		String out;

		out = s;
		out = out.replaceAll(":", " : ");
		out = out.replaceAll("\\[", " \\[ ");
		out = out.replaceAll("\\]", " \\] ");
		out = out.replaceAll("،", " ، ");
		out = out.replaceAll("\\.", " \\. ");
		out = out.replaceAll("؟", " ؟ ");
		out = out.replaceAll("\\?", " \\? ");

		out = out.replaceAll("  ", " ");
		out = out.trim();

		return out;
	}

	public int ExtractContentFromXMLFile(String ajFilename, List<String> ajLines, BufferedWriter outClean,
			BufferedWriter outStatus, boolean extractedFromPDF) {
		int i, nofAjLines, nofLines, len1, len2;
		org.jsoup.nodes.Element contentElement1, contentElement2;
		String cleanContent1, cleanContent2, cleanContent, s, msg, sClean, savedLine;
		String[] lines;

		nofAjLines = 0;
		ajLines.clear();

		msg = String.format("ExtractContentFromXMLFile(), start");
		System.out.println(msg);

		try {
			cleanContent1 = "";
			cleanContent2 = "";

			org.jsoup.nodes.Document doc = Jsoup.parse(new File(ajFilename), "utf-8");

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

					if (gSaveExtractedContents) {
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
			if (gSaveExtractedContents) {
				outClean.write(sClean);
			}

			nofAjLines = ajLines.size();

		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("ExtractContentFromXMLFile(), end");
		System.out.println(msg);

		return nofAjLines;
	}

	public String getCleanContent(org.jsoup.nodes.Element contentElement, boolean extractedFromPDF) {
		int i, j, len;
		char ch, ch2;
		String content, cleanContent, msg;

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

				if (gValidateAJOnly) {
					len = content.length();
					for (i = 0; i < len; i++) {
						ch = content.charAt(i);

						if (ch == '\t') {
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

				cleanContent = cleanContent.replaceAll("&Auml;", "Ä");
				cleanContent = cleanContent.replaceAll("&auml;", "ä");
				cleanContent = cleanContent.replaceAll("&Ouml;", "Ö");
				cleanContent = cleanContent.replaceAll("&ouml;", "ö");
				cleanContent = cleanContent.replaceAll("&Uuml;", "Ü");
				cleanContent = cleanContent.replaceAll("&uuml;", "ü");
				cleanContent = cleanContent.replaceAll("&szlig;", "ß");

				cleanContent = cleanContent.replaceAll("&AElig;", "Æ");
				cleanContent = cleanContent.replaceAll("&aelig;", "æ");
				cleanContent = cleanContent.replaceAll("&Oslash;", "Ø");
				cleanContent = cleanContent.replaceAll("&oslash;", "ø");
				cleanContent = cleanContent.replaceAll("&Aring;", "Å");
				cleanContent = cleanContent.replaceAll("&aring;", "å");

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

				cleanContent = cleanContent.replaceAll("&frac14;", "¼");
				cleanContent = cleanContent.replaceAll("&frac12;", "½");
				cleanContent = cleanContent.replaceAll("&frac34;", "¾");
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
		if (args.length != 9) {
			System.out.println(
					"Please enter orgFoldername subFoldername AjExt ctmExt alignExt srtExt traExt dfxpExt cleanExt. Ex: \"D:\\Speech\\ArabicASRChallenge\\exp-2015-10-25\\ html xml ctm align srt tra dfxp clean");
			return "fail";
		}

		int i, nofAjLines, nofASRLines, extIndex, fileIndex, nofFiles, validAjFiles, nofSpeakers;
		boolean genCleanFilesForCtmFilesOnly, extractedFromPDF;
		String orgFolderName, subFolderName, ajFolderName, ctmFolderName, alignFolderName, srtFolderName, msg, s,
				dbgFilename;
		String ajExt, ctmExt, alignExt, srtExt, traExt, cleanExt;
		String ajFilename, ctmFilename, alignFilename, srtFilename, cleanFilename, cleanFilename2, traFolderName,
				cleanFolderName, filename, ext, filenameNoExt, traFilename = null, speakersFilename, statusFilename;
		File[] ajFiles;
		Set<String> speakersKeys;
		Iterator<String> itrSpeakers;
		SpeakerInfo speakerInfo;
		File tmpFile;

		List<String> ajXmlLines = new ArrayList<String>();
		List<String> ajLines = new ArrayList<String>();
		List<String> ajLines2 = new ArrayList<String>();

		List<String> asrLines = new ArrayList<String>();
		List<String> asrLines2 = new ArrayList<String>();

		new ArrayList<String>();
		new ArrayList<String>();
		HashMap<String, SpeakerInfo> speakersMap = new HashMap<String, SpeakerInfo>();
		BufferedWriter outAlign, outSrt, outTra, outClean, outClean2, outSpeakers, outStatus;

		msg = String.format("parseASRFiles(), start");
		System.out.println(msg);

		orgFolderName = args[0];
		subFolderName = args[1];
		ajExt = args[2];
		ctmExt = args[3];
		alignExt = args[4];
		srtExt = args[5];
		traExt = args[6];
		cleanExt = args[8];

		ajFolderName = String.format("%s/%s/%s", orgFolderName, subFolderName, ajExt);
		ctmFolderName = String.format("%s/%s/%s", orgFolderName, subFolderName, ctmExt);
		alignFolderName = String.format("%s/%s/%s", orgFolderName, subFolderName, alignExt);
		srtFolderName = String.format("%s/%s/%s", orgFolderName, subFolderName, srtExt);

		traFolderName = String.format("%s/%s/%s", orgFolderName, subFolderName, traExt);
		cleanFolderName = String.format("%s/%s/%s", orgFolderName, subFolderName, cleanExt);

		genCleanFilesForCtmFilesOnly = false;// true; // To optimize time,
												// generate clean files for ctm
												// files only
		validAjFiles = 0;

		extractedFromPDF = false;
		if (subFolderName.compareToIgnoreCase("pdf") == 0) {
			extractedFromPDF = true;
		}

		try {
			filename = String.format("%s/NO_STOP_LIST.txt", orgFolderName);
			NOF_NO_STOP_LIST = LoadFile(filename, NO_STOP_LIST);

			for (i = 0; i < NOF_NO_STOP_LIST; i++) {
				NO_STOP_LIST_MAP.put(NO_STOP_LIST.get(i), 1);
			}

			// Generate .srt files
			ajFiles = new File(ajFolderName).listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return !pathname.isHidden();
				}
			});

			fileIndex = 0;
			nofFiles = ajFiles.length;

			traFilename = String.format("%s/ALL.%s", traFolderName, traExt);
			outTra = new BufferedWriter(new FileWriter(traFilename));

			speakersFilename = String.format("%s/Speakers.txt", traFolderName);
			outSpeakers = new BufferedWriter(new FileWriter(speakersFilename));

			statusFilename = String.format("%s/Status.txt", traFolderName);
			outStatus = new BufferedWriter(new FileWriter(statusFilename));

			// dbgFilename = "00C710F1-5611-4A7C-8A25-801BAEA5A5AD.xml";
			// dbgFilename = "2EC920B6-C18D-49A2-90D6-6678DF022B2D.xml";
			// dbgFilename = "49973211-017A-422F-AD15-28F08604476D.xml";
			dbgFilename = "00152FDA-5AB7-4B47-993C-AC8AC4D8C3AB.xml";
			// dbgFilename = "xxx.xml";
			for (File file : ajFiles) {
				if (fileIndex == MAX_NOF_FILES) {
					break;
				}
				if (file.isFile()) {
					filename = file.getName() + ".xml";

					if (true && filename.compareTo(dbgFilename) != 0) {
						continue;
					}

					msg = String.format("File: %d/%d, %s. validAjFile:%d", fileIndex + 1, nofFiles, filename,
							validAjFiles);
					System.out.println(msg);

					extIndex = filename.lastIndexOf('.');
					if (extIndex > 0) {
						filenameNoExt = filename.substring(0, extIndex);
						ext = filename.substring(extIndex + 1);
						// ext = Files.getFileExtension(filename);

						ajFilename = "";
						if (ext.compareToIgnoreCase(ajExt) == 0) {
							outClean2 = null;
							nofAjLines = 0;
							if (!genCleanFilesForCtmFilesOnly) {
								ajFilename = String.format("%s/%s", ajFolderName, filename);

								cleanFilename = String.format("%s/%s.%s", cleanFolderName, filenameNoExt, cleanExt);
								outClean = new BufferedWriter(new FileWriter(cleanFilename));

								cleanFilename2 = String.format("%s/%s.%s2", cleanFolderName, filenameNoExt, cleanExt);
								outClean2 = new BufferedWriter(new FileWriter(cleanFilename2));

								nofAjLines = ExtractContentFromXMLFile(ajFilename.replace(".xml", ""), ajLines,
										outClean, outStatus, extractedFromPDF);

								outClean.close();
								//
								// if (gValidateAJOnly) {
								// ajLines2 = addAljazeeraTags(ajLines,
								// ajFilename, outStatus, outClean2);
								//
								// outClean2.close();
								//
								// fileIndex++;
								// continue;
								// }
							}

							ctmFilename = String.format("%s/%s.%s", ctmFolderName, filenameNoExt, "word.ctm");
							tmpFile = new File(ctmFilename);
							if (tmpFile.exists()) {
								if (genCleanFilesForCtmFilesOnly) {
									ajFilename = String.format("%s/%s", ajFolderName, filename);

									cleanFilename = String.format("%s/%s.%s", cleanFolderName, filenameNoExt, cleanExt);
									outClean = new BufferedWriter(new FileWriter(cleanFilename));

									cleanFilename2 = String.format("%s/%s.%s2", cleanFolderName, filenameNoExt,
											cleanExt);
									outClean2 = new BufferedWriter(new FileWriter(cleanFilename2));

									nofAjLines = ExtractContentFromXMLFile(ajFilename, ajLines, outClean, outStatus,
											extractedFromPDF);

									outClean.close();

									if (gValidateAJOnly) {
										ajLines2 = addAljazeeraTags(ajLines, ajFilename, outStatus, outClean2);

										outClean2.close();

										fileIndex++;
										continue;
									}
								}

								nofASRLines = LoadFile(ctmFilename, asrLines);

								if ((nofAjLines > 0) && (nofASRLines > 0)) {
									validAjFiles++;
									msg = String.format("********************File: %d/%d, %s. validAjFiles:%d",
											fileIndex + 1, nofFiles, filename, validAjFiles);
									System.out.println(msg);

									alignFilename = String.format("%s/%s.%s", alignFolderName, filenameNoExt, alignExt);
									outAlign = new BufferedWriter(new FileWriter(alignFilename));

									ajLines2 = addAljazeeraTags(ajLines, ajFilename, outStatus, outClean2);

									srtFilename = String.format("%s/%s.%s", srtFolderName, filenameNoExt, srtExt);
									outSrt = new BufferedWriter(new FileWriter(srtFilename));

									asrLines2 = extractAsrInfo(asrLines, outSrt);

									alignAljazeeraWithAsr(ajLines2, asrLines2, outAlign, outSrt, filename, outTra,
											outStatus, speakersMap);

									outClean2.close();
									outAlign.close();
									outSrt.close();

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

			s = String.format("#\tSpeaker\tCount\r\n");
			outSpeakers.write(s);

			nofSpeakers = 0;
			speakersKeys = speakersMap.keySet();
			itrSpeakers = speakersKeys.iterator();
			while (itrSpeakers.hasNext()) {
				// Getting Key
				s = itrSpeakers.next();

				speakerInfo = speakersMap.get(s);
				s = String.format("%d\t%s\t%d\r\n", nofSpeakers + 1, speakerInfo.speakerName, speakerInfo.count);
				outSpeakers.write(s);

				nofSpeakers++;
			}

			outStatus.close();
			outSpeakers.close();
			outTra.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("parseASRFiles(), end. nofFiles=%d, nofErrors=%d", validAjFiles, gNofErrors);
		System.out.println(msg);

		if (gValidateAJOnly) {
			msg = String.format(
					"gProgramName=%d, gSeriesTitle=%d, gSeriesSpeaker=%d, gSeriesGuest=%d, gSeriesDate=%d, gSeriesDirections=%d, gNofSpecialChar=%d",
					gProgramName, gSeriesTitle, gSeriesSpeaker, gSeriesGuest, gSeriesDate, gSeriesDirections,
					gNofSpecialChar);
			System.out.println(msg);
		}

		return traFilename;
	}

	public List<String> addAljazeeraTags(List<String> ajLines, String ajFilename, BufferedWriter outStatus,
			BufferedWriter outClean2) {
		boolean metadata;
		int i, j, n, nofWords, start, colon, speakerStart, title, endLine, tmpStart;
		int programNameStart = -1, seriesTitleStart = -1, seriesSpeakerStart = -1, seriesGuestStart = -1,
				seriesDateStart = -1, seriesDirectionsStart = -1;
		List<String> ajLines2 = new ArrayList<String>();
		String s, s2, msg, w0, sClean;
		String[] fields;

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

				if (nofWords >= 3) {
					if (fields[0].equals("اسم") && fields[1].equals("البرنامج") && fields[2].equals(":")) {
						programNameStart = i;
					}

					if (fields[0].equals("عنوان") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
						seriesTitleStart = i;
					}

					if ((fields[0].equals("مقدم") || fields[0].equals("مقدمة")) && fields[1].equals("الحلقة")
							&& fields[2].equals(":")) {
						seriesSpeakerStart = i;
					}

					if ((fields[0].equals("ضيف") || fields[0].equals("ضيفة") || fields[0].equals("ضيفا")
							|| fields[0].equals("ضيفتا") || fields[0].equals("ضيوف")) && fields[1].equals("الحلقة")
							&& fields[2].equals(":")) {
						seriesGuestStart = i;
					}

					if (fields[0].equals("تاريخ") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
						seriesDateStart = i;
					}
				}
				if (nofWords >= 2) {
					if (fields[0].equals("المحاور") && fields[1].equals(":")) {
						seriesDirectionsStart = i;
					}
				}
			}

			tmpStart = -1;
			if (tmpStart < programNameStart) {
				tmpStart = programNameStart;
			}
			if (tmpStart < seriesTitleStart) {
				tmpStart = seriesTitleStart;
			}
			if (tmpStart < seriesSpeakerStart) {
				tmpStart = seriesSpeakerStart;
			}
			if (tmpStart < seriesGuestStart) {
				tmpStart = seriesGuestStart;
			}
			if (tmpStart < seriesDateStart) {
				tmpStart = seriesDateStart;
			}
			if (tmpStart < seriesDirectionsStart) {
				tmpStart = seriesDirectionsStart;
			}

			if (tmpStart < 0) {
				tmpStart = 0;
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

				for (j = 0; j < nofWords; j++) {
					if (fields[j].equals(":") && (nofWords >= 10)) {
						metadata = false;
						w0 = MGBUtil.normalizeWord(fields[0]);
						MGBUtil.normalizeWord(fields[1]);
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

			if (gValidateAJOnly) {
				int programName1 = 0, seriesTitle1 = 0, seriesSpeaker1 = 0, seriesGuest1 = 0, seriesDate1 = 0,
						seriesDirections1 = 0;
				int programName2 = 0, seriesTitle2 = 0, seriesSpeaker2 = 0, seriesGuest2 = 0, seriesDate2 = 0,
						seriesDirections2 = 0;
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

					if (nofWords >= 2) {
						if (fields[0].equals("اسم") && fields[1].equals("البرنامج") && fields[2].equals(":")) {
							programName1 = 1;
							gProgramName++;
						}

						if (fields[0].equals("عنوان") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							seriesTitle1 = 1;
							gSeriesTitle++;
						}

						if ((fields[0].equals("مقدم") || fields[0].equals("مقدمة")) && fields[1].equals("الحلقة")
								&& fields[2].equals(":")) {
							seriesSpeaker1 = 1;
							gSeriesSpeaker++;
						}

						if ((nofWords >= 3)
								&& (fields[0].equals("ضيف") || fields[0].equals("ضيفة") || fields[0].equals("ضيفا")
										|| fields[0].equals("ضيفتا") || fields[0].equals("ضيوف"))
								&& fields[1].equals("الحلقة") && fields[2].equals(":")) {
							seriesGuest1 = 1;
							gSeriesGuest++;
						}

						if (fields[0].equals("تاريخ") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							seriesDate1 = 1;
							gSeriesDate++;
						}

						if (fields[0].equals("المحاور") && fields[1].equals(":")) {
							seriesDirections1 = 1;
							gSeriesDirections++;
						}
					}
				}
				if ((programName1 == 0) || (seriesTitle1 == 0) || (seriesSpeaker1 == 0) || (seriesGuest1 == 0)
						|| (seriesDate1 == 0) || (seriesDirections1 == 0)) {
					msg = String.format(
							"File:%s\tprogram:%d\ttitle:%d\tspeaker:%d\tguest:%d\tdate:%d\tdirections:%d\r\n",
							ajFilename, programName1, seriesTitle1, seriesSpeaker1, seriesGuest1, seriesDate1,
							seriesDirections1);
					outStatus.write(msg);
				}

				// Make sure that no missing metadata byu looping on the whole
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

					if (nofWords >= 2) {
						if (fields[0].equals("اسم") && fields[1].equals("البرنامج") && fields[2].equals(":")) {
							programName2 = 1;
						}

						if (fields[0].equals("عنوان") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							seriesTitle2 = 1;
						}

						if ((fields[0].equals("مقدم") || fields[0].equals("مقدمة")) && fields[1].equals("الحلقة")
								&& fields[2].equals(":")) {
							seriesSpeaker2 = 1;
						}

						if ((nofWords >= 3)
								&& (fields[0].equals("ضيف") || fields[0].equals("ضيفة") || fields[0].equals("ضيفا")
										|| fields[0].equals("ضيفتا") || fields[0].equals("ضيوف"))
								&& fields[1].equals("الحلقة") && fields[2].equals(":")) {
							seriesGuest2 = 1;
						}

						if (fields[0].equals("تاريخ") && fields[1].equals("الحلقة") && fields[2].equals(":")) {
							seriesDate2 = 1;
						}

						if (fields[0].equals("المحاور") && fields[1].equals(":")) {
							seriesDirections2 = 1;
						}
					}
				}
				if ((programName1 != programName2) || (seriesTitle1 != seriesTitle2)
						|| (seriesSpeaker1 != seriesSpeaker2) || (seriesGuest1 != seriesGuest2)
						|| (seriesDate1 != seriesDate2) || (seriesDirections1 != seriesDirections2)) {
					msg = String.format(
							"METADATA\tFile:%s\tprogram:%d,%d\ttitle:%d,%d\tspeaker:%d,%d\tguest:%d,%d\tdate:%d,%d\tdirections:%d,%d\r\n",
							ajFilename, programName1, programName2, seriesTitle1, seriesTitle2, seriesSpeaker1,
							seriesSpeaker2, seriesGuest1, seriesGuest2, seriesDate1, seriesDate2, seriesDirections1,
							seriesDirections2);
					outStatus.write(msg);
				}
			}

			speakerStart = -1;
			for (i = 0; i < n; i++) {
				s = ajLines.get(i);
				s = splitPunct(s);

				fields = s.split(" ");

				nofWords = fields.length;
				if (s.isEmpty()) {
					nofWords = 0;
				}

				if (i < start) {
					if (nofWords > 0) {
						s2 = String.format("<METADATA %s >", ajLines.get(i));
					} else {
						s2 = String.format("%s", ajLines.get(i));
					}
					ajLines2.add(s2);
					continue;
				}

				colon = -1;
				for (j = 0; j < nofWords; j++) {
					if (fields[j].equals(":") && (j <= 3)) {
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
					for (j = 0; j < nofWords; j++) {
						s2 += String.format("%s ", fields[j]);
						if (j == colon) {
							s2 += String.format("> ", fields[j]);
						}
					}
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

			if (gSaveExtractedContents) {
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

	public List<String> extractAsrInfo(List<String> asrLines, BufferedWriter outSrt) {
		int i, n;
		List<String> asrLines2 = new ArrayList<String>();
		String s, s2, utf8, msg;
		String[] fields;

		msg = String.format("extractAsrInfo(), start");
		System.out.println(msg);

		try {
			n = asrLines.size();

			for (i = 0; i < n; i++) {
				s = asrLines.get(i);
				s = s.replaceAll("  ", " ");
				fields = s.split(" ");

				utf8 = ArabicUtils.buck2utf8(fields[4]);
				s2 = String.format("%s %s %s", fields[2], fields[3], utf8);
				asrLines2.add(s2);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("extractAsrInfo(), end");
		System.out.println(msg);

		return asrLines2;
	}

	public String formatTime(float totalSecs) {
		float totalSecs2;
		int hours, minutes, seconds, fraction;
		String timeString;

		hours = (int) (totalSecs / 3600);
		minutes = (int) ((totalSecs % 3600) / 60);
		seconds = (int) (totalSecs % 60);

		totalSecs2 = (float) (hours * 3600 + minutes * 60 + seconds);

		fraction = (int) ((totalSecs - totalSecs2) * 100);

		timeString = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, fraction);

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
		int aLen, bLen, maxLen, i, j, n;
		String s, msg;
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
			s = cleanText(wordInfo.word);

			wordInfo2 = new AjAsrWordInfo(wordInfo);
			wordInfo2.word = s;

			a.set(i, wordInfo2);
		}

		for (i = 0; i < bLen; i++) {
			wordInfo = b.get(i);
			s = cleanText(wordInfo.word);

			wordInfo2 = new AjAsrWordInfo(wordInfo);
			wordInfo2.word = s;

			b.set(i, wordInfo2);
		}

		for (i = 1; i <= aLen; i++) {
			for (j = 1; j <= bLen; j++) {
				// Compare only the first word of the string
				wordInfo = a.get(i - 1);
				wordInfo2 = b.get(j - 1);

				if (wordInfo.word.compareTo(wordInfo2.word) == 0) {
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
		int i, j, k, n, ajNofLines, asrNofLines, nofWords, start;
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
			wordInfo.startTime = Float.parseFloat(fields2[0]);
			wordInfo.duration = Float.parseFloat(fields2[1]);
			wordInfo.endTime = wordInfo.startTime + wordInfo.duration;

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

			speaker = ArabicUtils.utf82buck(speaker);
			speaker = speaker.replaceAll(" ", "-");

			if (speaker.isEmpty()) {
				speaker = lastSpeaker;
			} else {
				lastSpeaker = speaker;
			}

			for (j = start; j < nofWords; j++) {
				wordInfo = new AjAsrWordInfo();
				wordInfo.word = fields[j];

				wordInfo.speaker = speaker;

				allAjWords.add(wordInfo);
			}
		}

		msg = String.format("genAjAsrWordsLists(), end");
		System.out.println(msg);

		return 1;
	}

	public int saveAjAsrAlign(String ajFilename, List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList,
			BufferedWriter outAlign, BufferedWriter outStatus) {
		int i, n, matchedWordCount;
		float duration;
		String s2, sAsr2, sAsr4, sStartTime, sEndTime, sDuration, msg, out;
		AjAsrWordInfo wordInfoAj, wordInfoAsr, wordInfoAsr2;
		msg = String.format("saveAjAsrAlign(), start");
		System.out.println(msg);

		n = 0;
		try {
			matchedWordCount = 0;
			n = outAjList.size();
			s2 = String.format(
					"#\tAJ:word,speaker\tASR:start,duration,end\tSilenceAfter:duration\tSilenceAfter>Threshold\r\n");
			out = s2;

			for (i = 0; i < n; i++) {
				wordInfoAj = outAjList.get(i);

				wordInfoAsr = outAsrList.get(i);

				if (!wordInfoAj.word.isEmpty() && !wordInfoAsr.word.isEmpty()) {
					matchedWordCount++;
				}
				sAsr2 = "";
				sAsr4 = "";

				sStartTime = "";
				sDuration = "";
				sEndTime = "";
				if (!wordInfoAsr.word.isEmpty()) {
					sStartTime = formatTime(wordInfoAsr.startTime);
					sDuration = formatTime(wordInfoAsr.duration);
					sEndTime = formatTime(wordInfoAsr.endTime);

					if (i < n - 1) {
						wordInfoAsr2 = outAsrList.get(i + 1);

						if (!wordInfoAsr2.word.isEmpty()) {
							duration = wordInfoAsr2.startTime - wordInfoAsr.endTime;

							if (duration >= MAX_SILENCE_BETWEEN_WORDS) {
								sAsr4 = formatTime(duration);
								sAsr4 += String.format("\t1");
							}
						}
					}

					sAsr2 = String.format("%s  %s  %s  %s", wordInfoAsr.word, sStartTime, sDuration, sEndTime);
				}

				s2 = String.format("%d\t%s  %s\t%s\t%s\r\n", i, wordInfoAj.word, wordInfoAj.speaker, sAsr2, sAsr4);
				out += s2;
			}
			outAlign.write(out);
			// outAlign.flush();

			out = String.format("MATCH\tFilename:%s\tNofWords:%d\tMatchedWords:%d\t%%:%d\r\n", ajFilename, n,
					matchedWordCount, (matchedWordCount * 100) / n);
			outStatus.write(out);
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("saveAjAsrAlign(), end");
		System.out.println(msg);

		return n;
	}

	public int alignAljazeeraWithAsr(List<String> ajLines2, List<String> asrLines2, BufferedWriter outAlign,
			BufferedWriter outSrt, String ajFilename, BufferedWriter outTra, BufferedWriter outStatus,
			HashMap<String, SpeakerInfo> speakersMap) {
		String msg;
		List<AjAsrWordInfo> allAjWords = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> allAsrWords = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> outAjList = new ArrayList<AjAsrWordInfo>();
		List<AjAsrWordInfo> outAsrList = new ArrayList<AjAsrWordInfo>();

		List<AlignSegment> alignSegments = new ArrayList<AlignSegment>();
		msg = String.format("alignAljazeeraWithAsr(), start");
		System.out.println(msg);

		try {
			genAjAsrWordsLists(ajLines2, asrLines2, allAjWords, allAsrWords);

			wordAlign(allAjWords, allAsrWords, outAjList, outAsrList);

			saveAjAsrAlign(ajFilename, outAjList, outAsrList, outAlign, outStatus);

			assignTimetoAj(outAjList, outAsrList);

			getAlignSegments(outAjList, outAsrList, alignSegments);

			genSrtOutput(outAjList, outAsrList, alignSegments, outSrt, ajFilename, outTra, outStatus, speakersMap);
		} catch (Exception e) {
			e.printStackTrace();
		}

		msg = String.format("alignAljazeeraWithAsr(), end");
		System.out.println(msg);

		return 1;
	}

	public int assignTimetoAj(List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList) {
		int i, j, nAj, startIndex, endIndex, nofInWords, currNofInWords, debugLine;
		float startTime1, endTime1, startTime2, endTime2, duration, currStart;
		String msg;
		AjAsrWordInfo wordInfoAj, wordInfoAj2, wordInfoAsr;

		msg = String.format("assignTimetoAj(), start");
		System.out.println(msg);

		nAj = outAjList.size();

		for (i = 0; i < nAj; i++) {
			wordInfoAj = outAjList.get(i);

			if (!wordInfoAj.word.isEmpty()) {
				wordInfoAj.punct = isPunct(wordInfoAj.word);

				if (!wordInfoAj.punct && (i == nAj - 1)) {
					wordInfoAj.punct = true;
				}

				wordInfoAsr = outAsrList.get(i);

				if (!wordInfoAsr.word.isEmpty()) {
					wordInfoAj.startTime = wordInfoAsr.startTime;
					wordInfoAj.duration = wordInfoAsr.duration;
					wordInfoAj.endTime = wordInfoAsr.endTime;

					wordInfoAj.mappingIndex = i;
				}
			}
		}

		debugLine = 3400;
		for (i = 0; i < nAj; i++) {
			if (i == debugLine) {
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
					}

					wordInfoAj2 = outAjList.get(startIndex);
					startTime1 = wordInfoAj2.startTime;
					endTime1 = wordInfoAj2.endTime;

					if (wordInfoAj2.mappingIndex < 0) {
						wordInfoAsr = outAsrList.get(startIndex);
						if (!wordInfoAsr.word.isEmpty()) {
							startTime1 = wordInfoAsr.startTime;
							endTime1 = wordInfoAsr.startTime;
						} else {
						}
					}

					wordInfoAj2 = outAjList.get(endIndex);
					startTime2 = wordInfoAj2.startTime;
					endTime2 = wordInfoAj2.endTime;

					if (wordInfoAj2.mappingIndex < 0) {
						wordInfoAsr = outAsrList.get(endIndex);
						if (!wordInfoAsr.word.isEmpty()) {

							startTime2 = wordInfoAsr.startTime;
							endTime2 = wordInfoAsr.endTime;
						} else {
						}
					}

					if (endTime1 < 0) {
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
		boolean noStop, punct;
		int i, j, k, m, n, nofSegments, start1, end1, start2, end2, punctIndex, maxWindowIndex, minWindowIndex,
				dbgIndex, nofExtraWords, nofSegWords, correctSegWords, nofIns, nofDel, nofMatch;
		String msg;
		AjAsrWordInfo wordInfo1, wordInfo2, wordInfo3, wordInfo_1;
		AlignSegment alignSegment;

		msg = String.format("getAlignSegments(), start");
		System.out.println(msg);

		nofSegments = 0;

		start1 = -1;
		end1 = -1;
		dbgIndex = 277;

		n = outAjList.size();

		for (i = end1 + 1; i < n; i++) {
			if (i == dbgIndex) {
			}
			// Prefer to split on punctuations
			start1 = -1;
			end1 = -1;
			for (j = i; j < n; j++) {
				wordInfo1 = outAjList.get(j);

				if (start1 < 0) {
					if (!wordInfo1.word.isEmpty()) {
						start1 = j;
					}
				}

				if (wordInfo1.punct && (j - i >= (MAX_SRT_WORDS_PER_LINE / 2) - 1)) {
					end1 = j;
					break;
				}

				if ((j - i == MAX_SRT_WORDS_PER_LINE - 1) || (j == n - 1)) {
					// Look ahead searching for the nearset punct
					punctIndex = -1;
					maxWindowIndex = n - 1;
					nofExtraWords = 0;
					for (k = j + 1; k < n; k++) {
						wordInfo2 = outAjList.get(k);
						if (!wordInfo2.word.isEmpty()) {
							nofExtraWords++;
							if (nofExtraWords == EXTRA_WORDS_PER_LINE) {
								maxWindowIndex = k;
								break;
							}
						}
					}
					// maxWindowIndex = Math.min(j + EXTRA_WORDS_PER_LINE, n -
					// 1);
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
							wordInfo_1 = outAjList.get(k - 1);

							if (!wordInfo2.word.isEmpty() && !wordInfo_1.word.isEmpty()) {
								if ((wordInfo2.startTime - wordInfo_1.endTime) >= MAX_SILENCE_BETWEEN_WORDS) {
									noStop = foundInNoStopList(wordInfo_1);

									if (noStop) {
										// Return to the nearest non-stop
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
				}
			}

			nofSegWords = 0;
			correctSegWords = 0;
			nofIns = 0;
			nofDel = 0;
			nofMatch = 0;
			start2 = -1;
			end2 = -1;
			for (k = start1; k <= end1; k++) {
				punct = false;
				wordInfo1 = outAjList.get(k);
				if (!wordInfo1.word.isEmpty()) {
					punct = isPunct(wordInfo1.word);
					if (!punct) {
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
							nofMatch++;
						} else {
							nofDel++;
						}
					} else if (!wordInfo2.word.isEmpty()) {
						nofIns++;
					}
				}
			}

			correctSegWords = nofSegWords - Math.abs(nofDel - nofIns);

			alignSegment = new AlignSegment();

			alignSegment.start1 = start1;
			alignSegment.end1 = end1;
			alignSegment.start2 = start2;
			alignSegment.end2 = end2;

			alignSegment.nofSegWords = nofSegWords;
			alignSegment.correctSegWords = correctSegWords;

			alignSegment.nofIns = nofIns;
			alignSegment.nofDel = nofDel;
			alignSegment.nofMatch = nofMatch;

			alignSegment.asrStartTimeExists = 0;
			alignSegment.asrEndTimeExists = 0;

			// Mark segments with existing ASR time for start and end
			nofSegWords = 0;
			for (k = start1; k <= end1; k++) {
				punct = false;
				wordInfo1 = outAjList.get(k);
				if (!wordInfo1.word.isEmpty()) {
					punct = isPunct(wordInfo1.word);
					if (!punct) {
						nofSegWords++;
					}
				}

				wordInfo2 = outAsrList.get(k);

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

			if ((start1 < 0) || (end1 < 0) || (start2 < 0) || (end2 < 0)) {
			}

			msg = String.format("[i=%d] alignSegment:%d-%d to %d-%d, nofSegWords:%d, nofCorrectSegWords:%d", i, start1,
					end1, start2, end2, nofSegWords, correctSegWords);
			System.out.println(msg);

			alignSegments.add(alignSegment);

			nofSegments++;

			if (end1 > i) {
				i = end1;
			} else {
			}
		}

		msg = String.format("getAlignSegments(), end");
		System.out.println(msg);

		return nofSegments;
	}

	public int genSrtOutput(List<AjAsrWordInfo> outAjList, List<AjAsrWordInfo> outAsrList,
			List<AlignSegment> alignSegments, BufferedWriter outSrt, String ajFilename, BufferedWriter outTra,
			BufferedWriter outStatus, HashMap<String, SpeakerInfo> speakersMap) {
		int i, j, nofSegments;
		String s2, sAj, sAsr, speaker, lastSpeaker, buckwalterSpeaker, sStartTime, sEndTime, msg, allSrt, allTra,
				allStatus;
		List<String[]> sAjArr = new ArrayList<String[]>(), sAsrArr = new ArrayList<String[]>();
		List<String[]> sAjArrChar = new ArrayList<String[]>();
		List<String[]> sAsrArrChar = new ArrayList<String[]>();
		String sAjBuk = null;
		String sAsrBuk = null;
		SpeakerInfo foundSpeakerInfo;
		AjAsrWordInfo wordInfo;
		AlignSegment alignSegment;
		WordSequenceAligner aligner = new WordSequenceAligner(1, 1, 1);

		msg = String.format("genSrtOutput(), start");
		System.out.println(msg);

		try {
			nofSegments = alignSegments.size();

			speaker = "";
			lastSpeaker = "";
			allStatus = "";

			allSrt = "";
			allTra = "";

			for (i = 0; i < nofSegments; i++) {
				alignSegment = alignSegments.get(i);

				sStartTime = "";
				sEndTime = "";
				sAj = "";
				for (j = alignSegment.start1; j <= alignSegment.end1; j++) {
					wordInfo = outAjList.get(j);

					if (!wordInfo.word.isEmpty()) {
						sAj += String.format("%s ", wordInfo.word);
					}

					if (j == alignSegment.start1) {
						speaker = wordInfo.speaker;
						sStartTime = formatTime(wordInfo.startTime);
					}

					if (j == alignSegment.end1) {
						sEndTime = formatTime(wordInfo.endTime);
					}
				}

				sAj = sAj.trim();
				sAjBuk = MGBUtil.getNormaliseBukTranscriptString(sAj);
				sAjArr.add(sAjBuk.split(" "));
				sAjArrChar.add(sAjBuk.replaceAll(" ", "").split(""));

				sAsr = "";
				for (int k = alignSegment.start2; k <= alignSegment.end2; k++) {
					wordInfo = null;
					if (k > -1) {
						wordInfo = outAjList.get(k);
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
				speaker = speaker.replaceAll("-", " ");
				speaker = ArabicUtils.buck2utf8Arb(speaker);

				if (speaker.compareTo(lastSpeaker) != 0) {
					s2 = String.format("%d\r\n%s --> %s\r\n<font color=\"#aaaaff\">%s: %s</font>\r\n\r\n", i + 1,
							sStartTime, sEndTime, speaker, sAj);

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
					s2 = String.format("%d\r\n%s --> %s\r\n<font color=\"#aaaaff\">%s</font>\r\n\r\n", i + 1,
							sStartTime, sEndTime, sAj);
				}

				allSrt += s2;

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
					s2 = String.format(
							"%s_%s_%s_%s %s\tWords:%d Correct:%d\tCorrect:%d\tIns:%d\tDel:%d\tWMER:%s\tPMER:%s\tAWD:%s\tStart:%d\tEnd:%d\r\n",
							ajFilename, buckwalterSpeaker, sStartTime, sEndTime, sAj, alignSegment.nofSegWords,
							alignSegment.correctSegWords,
							(alignSegment.correctSegWords * 100) / alignSegment.nofSegWords, alignSegment.nofIns,
							alignSegment.nofDel, leveDistWordLevel * 100.0, leveDistanceCharLevel * 100,
							MGBUtil.calculateAWD(sStartTime, sEndTime, alignSegment.nofSegWords),
							alignSegment.asrStartTimeExists, alignSegment.asrEndTimeExists);

				} else {
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
		/*
		 * int i, n;
		 * 
		 * n = NO_STOP_LIST.size(); for (i = 0; i < n; i++) { if
		 * (wordInfo.word.compareTo(NO_STOP_LIST.get(i)) == 0) { stopWord =
		 * true; break; } }
		 */

		if (NO_STOP_LIST_MAP.get(wordInfo.word) != null) {
			stopWord = true;
		}

		return stopWord;
	}

}
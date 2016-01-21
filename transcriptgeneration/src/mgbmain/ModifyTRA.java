package mgbmain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifyTRA {

	public static void main(String... args) {
		BufferedReader br = null;
		String sCurrentLine;

		try {
			String modifiedLine = null;
			br = new BufferedReader(new FileReader("/Users/alt-sameerk/Documents/tra-2016-01-04/ALL.tra"));
			while ((sCurrentLine = br.readLine()) != null) {
				String modifiedSegmentID = null;
				String segmentID = sCurrentLine.split("\t")[0];
				String[] restInfo = Arrays.copyOfRange(sCurrentLine.split("\t"), 1, sCurrentLine.split("\t").length);
				// System.out.println(segmentID);
				String pattern = "(.*?)(\\d\\d:.*?)\\s(.*)";
				Pattern r = Pattern.compile(pattern);
				Matcher m = r.matcher(segmentID.split("\\.xml")[1]);
				String modifiedSpeaker = null;
				// System.out.println(sCurrentLine);
				if (m.find()) {
					modifiedSpeaker = m.group(1).replaceAll("_", " ").trim().replaceAll(" ", "-") + "_" + m.group(2)
							+ " " + m.group(3);
				} else {
					System.out.println(sCurrentLine);
					System.out.println(segmentID);
					break;

				}
				modifiedSegmentID = segmentID.split(".xml")[0] + ".xml" + "_" + modifiedSpeaker;
				modifiedLine = modifiedSegmentID;
				// System.out.println(modifiedLine);
				for (String s : restInfo) {

					modifiedLine = modifiedLine + "\t" + s;
				}
				writeToFile(modifiedLine);

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void writeToFile(String modifiedLine) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter("/Users/alt-sameerk/Documents/tra-2016-01-04/ALL_MOD.tra", true));
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

}

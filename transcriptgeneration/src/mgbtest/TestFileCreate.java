package mgbtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TestFileCreate {

	public static void main(String... args) {
		BufferedReader br = null;
		BufferedWriter bw = null;
		String sCurrentLine;
		try {
			int i = 0;
			br = new BufferedReader(new FileReader("/Users/alt-sameerk/Downloads/SelectedAJProgramsForDevEval.txt"));
			while ((sCurrentLine = br.readLine()) != null) {
				if (i > 0) {
					System.out.println(sCurrentLine);
					String[] segmentInfo = sCurrentLine.split("\t");
					String progId = segmentInfo[2];
					/*
					 * For each line extract the program id and write to a file
					 */
					bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/test_list", true));
					bw.write(progId.replaceAll("-", "_"));
					bw.write("\n");
					bw.close();
				}
				
				i++;
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
}

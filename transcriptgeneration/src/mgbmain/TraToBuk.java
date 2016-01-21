package mgbmain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import mgbutils.ArabicUtils;

public class TraToBuk {

	public static void main(String... args) {
		BufferedReader br = null;
		File[] traFiles = new File("/Users/alt-sameerk/Documents/tra_normalized/").listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return !file.isHidden();
			}
		});
		for (File file : traFiles) {
			BufferedWriter bw = null;
			try {
				br = new BufferedReader(new FileReader(file));
				String currentLine;
				while ((currentLine = br.readLine()) != null) {
					bw = new BufferedWriter(
							new FileWriter("/Users/alt-sameerk/Documents/tra_buk_normalized/" + file.getName(), true));
					String[] splitTab = currentLine.split("\t");
					// System.out.println(currentLine);
					if (splitTab.length == 6) {
						String wordsBuk = ArabicUtils.utf82buck(splitTab[5]);
						bw.write(splitTab[0] + " " + splitTab[0] + "_" + splitTab[2].replace(".", "") + "_"
								+ splitTab[3].replace(".", "") + "\t" + splitTab[2] + "\t" + splitTab[3] + "\t" + wordsBuk
								+ "\n");
						bw.close();
					} else {
						System.out.println(currentLine);
						bw = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/badOnes", true));
						bw.write(currentLine + "\n");
						bw.close();
					}
					// System.out.println(wordsBuk + "\t" + splitTab[5]);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// break;
		}

	}
}
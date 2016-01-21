package mgbtest;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TestTmp {

	public static void main(String... args) throws IOException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/all.tmp50086"));
			String line;
			int count = 1;
			while ((line = br.readLine()) != null) {
				if (count == 93101) {
					System.out.println(line);
				}

				count++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

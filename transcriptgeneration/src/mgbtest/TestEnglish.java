package mgbtest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestEnglish {

	public static void main(String... args) {
		String s = "الشهر المقبل وربما تضطر إلى دفع ljlnasdlnGAG";
		System.out.println(s.toLowerCase());
		String newP = s.replaceAll("[^\\s%ء-ي0-9٠-٩]", "");
		String pattern = "[A-Za-z]";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(s);
		if (m.find()) {
			// System.out.println(m.group(0));
		} else {
			System.out.println("no english");
		}

		// System.out.println(newP);

	}

}

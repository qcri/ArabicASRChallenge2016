package mgbtest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestReg {

	public static void main(String[] args) {
		String segment = "06B1BB7B-4E57-4062-BDCB-E742F32151D0 06B1BB7B-4E57-4062-BDCB-E742F32151D0_110773_110943 1107.73 1109.43 fy <dlb >lys k*lk .";
		String line = segment;
		String newSegm = "0152FDA-5AB7-4B47-993C-AC8AC4D8C3AB.xml_xdyjp-bn-qnp_00:00:05,94_00:00:11,41 يلجأ النظام السوري إلى استخدام ذخائر عنقودية لقمع الاحتجاجات	Words:9	Correct:7	Correct%:77%	Ins:1	Del:3	Match:6	Match%:66%	Start:0	End:1";
		String matchPercent = newSegm.split("\t")[7].split(":")[1];
		String matchErrorRate = Double
				.toString(100.0 - Double.parseDouble(matchPercent.substring(0, matchPercent.indexOf("%"))));
		System.out.println(matchErrorRate);
		System.out.println(newSegm.split("\t")[7].split(":")[1]);
		// String pattern = "(.*?)(\\d\\d\\s?)\\w(.*)";
		String pattern = "\\<(.*?)\\>";
		String test = "sameer  is a good boy";
		System.out.println(test.replaceAll("\\s\\s", ""));

		Pattern r = Pattern.compile(pattern);

		Matcher m = r.matcher("<tag>sameer</tag>");
		if (m.find()) {
			System.out.println(m.group(1));
			// System.out.println(m.group(2));
			// System.out.println(m.group(3));
		}
	}
}

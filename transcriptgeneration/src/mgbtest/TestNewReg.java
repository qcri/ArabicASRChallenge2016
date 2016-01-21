package mgbtest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestNewReg {

	public static void main(String[] args) throws ParseException {
		String line = "www.aljazeera.net/programs/SpecialInterview/2015/2/20/رئيس-العراق-بقاء-الأسد-أقل-خطرا-من-سيطرة-الإرهابيين";
		String pattern = "/[0-9].*/";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(line);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		if (m.find()) {
			System.out.println(
					formatter.format(formatter.parse(m.group(0).replaceAll("/", " ").trim().replaceAll(" ", "/"))));
		}
	}

}

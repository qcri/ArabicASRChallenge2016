package mgbtest;

public class TestCharArray {

	public static void main(String[] args) {
		String s = "this is hamdy";
		String[] sA = s.replaceAll(" ", "").split("");
		System.out.println(sA);
		for (String s1 : sA) {
			System.out.println(s1);
		}
	}

}

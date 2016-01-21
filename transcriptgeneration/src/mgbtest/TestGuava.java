package mgbtest;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Testing if the insertion order is maintained
 * 
 * @author alt-sameerk
 *
 */
public class TestGuava {
	public static void main(String... args) {
		ListMultimap<String, String> multiMap = ArrayListMultimap.create();
		multiMap.put("A", "Sam");
		multiMap.put("B", "Ter");
		multiMap.put("A", "Aam");
		multiMap.put("B", "Ser");

		// System.out.println(multiMap.removeAll("A"));
		System.out.println(multiMap);
		System.out.println(System.getProperty("user.dir"));
	}
}

package mgbtest;

import java.lang.reflect.Field;

import mgbbeans.SegmentBean;

public class TestObjectFields {

	public static void main(String... args) throws IllegalArgumentException, IllegalAccessException {
		SegmentBean obj = new SegmentBean();
		obj.setAwd("t");
		obj.setEndTime("87");
		obj.setStartTime("");
		// obj.setGraphemeMatchErrorRate(null);
		for (Field f : obj.getClass().getDeclaredFields()) {
			f.setAccessible(true);
			if (f.get(obj) == null) {
				if (f.getName().equals("transcriptString")) {
					System.out.println("Null field " + f.getName().equals("transcriptString"));
				}
			}
		}
	}

}

package mgbbeans;

import java.util.LinkedHashMap;

public class SegmentsFileBean {

	private LinkedHashMap<String, SegmentsFileLineBean> segmentsFileMap;

	public LinkedHashMap<String, SegmentsFileLineBean> getSegmentsFileMap() {
		return segmentsFileMap;
	}

	public void setSegmentsFileMap(LinkedHashMap<String, SegmentsFileLineBean> segmentsFileMap) {
		this.segmentsFileMap = segmentsFileMap;
	}

}

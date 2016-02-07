package mgbbeans;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class CtmFileBean {

	private ListMultimap<String, SegmentsFileLineBean> ctmFileMap = ArrayListMultimap.create();

	public ListMultimap<String, SegmentsFileLineBean> getCtmFileMap() {
		return ctmFileMap;
	}

	public void setCtmFileMap(ListMultimap<String, SegmentsFileLineBean> ctmFileMap) {
		this.ctmFileMap = ctmFileMap;
	}

}

package mgbbeans;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A segment object that stores all the attributes of a segment like the
 * programID, Arabic transcription, duration etc.
 * 
 * @author alt-sameerk
 *
 */
public class SegmentBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	private String startTime;
	private String endTime;
	private String speakerName;
	private String transcriptString;
	private String awd;
	private String graphemeMatchErrorRate;
	private String wordMatchErrorRate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getSpeakerName() {
		return speakerName;
	}

	public void setSpeakerName(String speakerName) {
		this.speakerName = speakerName;
	}

	public String getTranscriptString() {
		return transcriptString;
	}

	public void setTranscriptString(String transcriptString) {
		this.transcriptString = transcriptString;
	}

	public String getAwd() {
		return awd;
	}

	public void setAwd(String awd) {
		this.awd = awd;
	}

	public String getGraphemeMatchErrorRate() {
		return graphemeMatchErrorRate;
	}

	public void setGraphemeMatchErrorRate(String graphemeMatchErrorRate) {
		this.graphemeMatchErrorRate = graphemeMatchErrorRate;
	}

	public String getWordMatchErrorRate() {
		return wordMatchErrorRate;
	}

	public void setWordMatchErrorRate(String wordMatchErrorRate) {
		this.wordMatchErrorRate = wordMatchErrorRate;
	}

	@Override
	public String toString() {
		return "SegmentBean [id=" + id + ",  startTime=" + startTime + ", endTime=" + endTime + ", speakerName="
				+ speakerName + ", transcriptString=" + transcriptString + ", awd=" + awd + ", graphemeMatchErrorRate="
				+ graphemeMatchErrorRate + ", wordMatchErrorRate=" + wordMatchErrorRate + "]";
	}

}

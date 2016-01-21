package mgbtest;

import java.util.ArrayList;
import java.util.List;

import mgbutils.WordSequenceAligner;
import mgbutils.WordSequenceAligner.Alignment;

public class TestWordSequenceAligner {

	public static void main(String[] args) {
		WordSequenceAligner wsa = new WordSequenceAligner(1, 1, 1);
		String[] referenceSentences = new String[] { "s", "a", "m", "e", " e", " r" };
		String[] hypoSentences = new String[] { "h", "a", "m", "d", "y" };
		List<String[]> references = new ArrayList<String[]>();
		references.add(referenceSentences);
		List<String[]> hypo = new ArrayList<String[]>();
		hypo.add(hypoSentences);
		List<Alignment> alignments = wsa.align(references, hypo);
		System.out.println(alignments.get(0));
		System.out.println(alignments.get(0).getNumCorrect());
	}

}

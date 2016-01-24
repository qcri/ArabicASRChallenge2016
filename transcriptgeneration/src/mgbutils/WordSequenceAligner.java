/*
Copyright 2011 Brian Romanowski. All rights reserved.
Portions Copyright 1999-2002 Carnegie Mellon University.  
Portions Copyright 2002 Sun Microsystems, Inc.  
Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
All Rights Reserved.  Use is subject to license terms.
Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:
   1. Redistributions of source code must retain the above copyright notice, this list of
      conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright notice, this list
      of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.
THIS SOFTWARE IS PROVIDED BY BRIAN ROMANOWSKI ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BRIAN ROMANOWSKI OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
The views and conclusions contained in the software and documentation are those of the
authors.
*/

/*
 * Notes on licensing:
 * This code was written while inspecting the Sphinx 4 edu.cmu.sphinx.util.NISTAlign source code and 
 * the NIST sclite documentation.  To the degree that the source code here is copied from NISTAlign, 
 * it is covered by the copyrights reproduced above.  Brian Romanowski holds the copyright to the rest.
 */

package mgbutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Computes the word error rate (WER) and other statistics available from an
 * alignment of a hypothesis string and a reference string. The alignment and
 * metrics are intended to be, by default, identical to those of the
 * <a href="http://www.icsi.berkeley.edu/Speech/docs/sctk-1.2/sclite.htm">NIST
 * SCLITE tool</a>.
 * 
 * <p>
 * This code was written while consulting the Sphinx 4
 * edu.cmu.sphinx.util.NISTAlign source code.
 * </p>
 * 
 * @author romanows
 */
public class WordSequenceAligner {

	/**
	 * Cost of a substitution string edit operation applied during alignment.
	 * From edu.cmu.sphinx.util.NISTAlign, which should be referencing the NIST
	 * sclite utility settings.
	 */
	public static final int DEFAULT_SUBSTITUTION_PENALTY = 100;

	/**
	 * Cost of an insertion string edit operation applied during alignment. From
	 * edu.cmu.sphinx.util.NISTAlign, which should be referencing the NIST
	 * sclite utility settings.
	 */
	public static final int DEFAULT_INSERTION_PENALTY = 75;

	/**
	 * Cost of a deletion string edit operation applied during alignment. From
	 * edu.cmu.sphinx.util.NISTAlign, which should be referencing the NIST
	 * sclite utility settings.
	 */
	public static final int DEFAULT_DELETION_PENALTY = 75;

	/** Substitution penalty for reference-hypothesis string alignment */
	private final int substitutionPenalty;

	/** Insertion penalty for reference-hypothesis string alignment */
	private final int insertionPenalty;

	/** Deletion penalty for reference-hypothesis string alignment */
	private final int deletionPenalty;

	/**
	 * Result of an alignment. Has a {@link #toString()} method that
	 * pretty-prints human-readable metrics.
	 * 
	 * @author romanows
	 */
	public class Alignment {
		/**
		 * Reference words, with null elements representing insertions in the
		 * hypothesis sentence and upper-cased words representing an alignment
		 * mismatch
		 */
		public final String[] reference;

		/**
		 * Hypothesis words, with null elements representing deletions (missing
		 * words) in the hypothesis sentence and upper-cased words representing
		 * an alignment mismatch
		 */
		public final String[] hypothesis;

		/**
		 * Number of word substitutions made in the hypothesis with respect to
		 * the reference
		 */
		public final int numSubstitutions;

		/**
		 * Number of word insertions (unnecessary words present) in the
		 * hypothesis with respect to the reference
		 */
		public final int numInsertions;

		/**
		 * Number of word deletions (necessary words missing) in the hypothesis
		 * with respect to the reference
		 */
		public final int numDeletions;

		/**
		 * Constructor.
		 * 
		 * @param reference
		 *            reference words, with null elements representing
		 *            insertions in the hypothesis sentence
		 * @param hypothesis
		 *            hypothesis words, with null elements representing
		 *            deletions (missing words) in the hypothesis sentence
		 * @param numSubstitutions
		 *            Number of word substitutions made in the hypothesis with
		 *            respect to the reference
		 * @param numInsertions
		 *            Number of word insertions (unnecessary words present) in
		 *            the hypothesis with respect to the reference
		 * @param numDeletions
		 *            Number of word deletions (necessary words missing) in the
		 *            hypothesis with respect to the reference
		 */
		public Alignment(String[] reference, String[] hypothesis, int numSubstitutions, int numInsertions,
				int numDeletions) {
			if (reference == null || hypothesis == null || reference.length != hypothesis.length || numSubstitutions < 0
					|| numInsertions < 0 || numDeletions < 0) {
				throw new IllegalArgumentException();
			}
			this.reference = reference;
			this.hypothesis = hypothesis;
			this.numSubstitutions = numSubstitutions;
			this.numInsertions = numInsertions;
			this.numDeletions = numDeletions;
		}

		/**
		 * Number of word correct words in the aligned hypothesis with respect
		 * to the reference.
		 * 
		 * @return number of word correct words
		 */
		public int getNumCorrect() {
			return getHypothesisLength() - (numSubstitutions + numInsertions); // Substitutions
																				// are
																				// mismatched
																				// and
																				// not
																				// correct,
																				// insertions
																				// are
																				// extra
																				// words
																				// that
																				// aren't
																				// correct
		}

		/** @return true when the hypothesis exactly matches the reference */
		public boolean isSentenceCorrect() {
			return numSubstitutions == 0 && numInsertions == 0 && numDeletions == 0;
		}

		/**
		 * Get the length of the original reference sequence. This is not the
		 * same as {@link #reference}.length(), because that member variable may
		 * have null elements inserted to mark hypothesis insertions.
		 * 
		 * @return the length of the original reference sequence
		 */
		public int getReferenceLength() {
			return reference.length - numInsertions;
		}

		/**
		 * Get the length of the original hypothesis sequence. This is not the
		 * same as {@link #hypothesis}.length(), because that member variable
		 * may have null elements inserted to mark hypothesis deletions.
		 * 
		 * @return the length of the original hypothesis sequence
		 */
		public int getHypothesisLength() {
			return hypothesis.length - numDeletions;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder ref = new StringBuilder();
			StringBuilder hyp = new StringBuilder();
			ref.append("REF:\t");
			hyp.append("HYP:\t");
			for (int i = 0; i < reference.length; i++) {
				if (reference[i] == null) {
					for (int j = 0; j < hypothesis[i].length(); j++) {
						ref.append("*");
					}
				} else {
					ref.append(reference[i]);
				}

				if (hypothesis[i] == null) {
					for (int j = 0; j < reference[i].length(); j++) {
						hyp.append("*");
					}
				} else {
					hyp.append(hypothesis[i]);
				}

				if (i != reference.length - 1) {
					ref.append("\t");
					hyp.append("\t");
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append("\t");
			sb.append("# seq").append("\t");
			sb.append("# ref").append("\t");
			sb.append("# hyp").append("\t");
			sb.append("# cor").append("\t");
			sb.append("# sub").append("\t");
			sb.append("# ins").append("\t");
			sb.append("# del").append("\t");
			sb.append("acc").append("\t");
			sb.append("WER").append("\t");
			sb.append("# seq cor").append("\t");

			sb.append("\n");
			sb.append("STATS:\t");
			sb.append(1).append("\t");
			sb.append(getReferenceLength()).append("\t");
			sb.append(getHypothesisLength()).append("\t");
			sb.append(getNumCorrect()).append("\t");
			sb.append(numSubstitutions).append("\t");
			sb.append(numInsertions).append("\t");
			sb.append(numDeletions).append("\t");
			sb.append(getNumCorrect() / (float) getReferenceLength()).append("\t");
			sb.append((numSubstitutions + numInsertions + numDeletions) / (float) getReferenceLength()).append("\t");
			sb.append(isSentenceCorrect() ? 1 : 0);

			sb.append("\n");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");
			sb.append("-----\t");

			sb.append("\n");
			sb.append(ref).append("\n").append(hyp);

			return sb.toString();
		}
	}

	/**
	 * Collects several alignment results. Has a {@link #toString()} method that
	 * pretty-prints a human-readable summary metrics for the collection of
	 * results.
	 * 
	 * @author romanows
	 */
	public class SummaryStatistics {
		/**
		 * Number of correct words in the aligned hypothesis with respect to the
		 * reference
		 */
		private int numCorrect;

		/**
		 * Number of word substitutions made in the hypothesis with respect to
		 * the reference
		 */
		private int numSubstitutions;

		/**
		 * Number of word insertions (unnecessary words present) in the
		 * hypothesis with respect to the reference
		 */
		private int numInsertions;

		/**
		 * Number of word deletions (necessary words missing) in the hypothesis
		 * with respect to the reference
		 */
		private int numDeletions;

		/** Number of hypotheses that exactly match the associated reference */
		private int numSentenceCorrect;

		/** Total number of words in the reference sequences */
		private int numReferenceWords;

		/** Total number of words in the hypothesis sequences */
		private int numHypothesisWords;

		/** Number of sentences */
		private int numSentences;

		/**
		 * Constructor.
		 * 
		 * @param alignments
		 *            collection of alignments
		 */
		public SummaryStatistics(Collection<Alignment> alignments) {
			for (Alignment a : alignments) {
				add(a);
			}
		}

		/**
		 * Add a new alignment result
		 * 
		 * @param alignment
		 *            result to add
		 */
		public void add(Alignment alignment) {
			numCorrect += alignment.getNumCorrect();
			numSubstitutions += alignment.numSubstitutions;
			numInsertions += alignment.numInsertions;
			numDeletions += alignment.numDeletions;
			numSentenceCorrect += alignment.isSentenceCorrect() ? 1 : 0;
			numReferenceWords += alignment.getReferenceLength();
			numHypothesisWords += alignment.getHypothesisLength();
			numSentences++;
		}

		public int getNumSentences() {
			return numSentences;
		}

		public int getNumReferenceWords() {
			return numReferenceWords;
		}

		public int getNumHypothesisWords() {
			return numHypothesisWords;
		}

		public float getCorrectRate() {
			return numCorrect / (float) numReferenceWords;
		}

		public float getSubstitutionRate() {
			return numSubstitutions / (float) numReferenceWords;
		}

		public float getDeletionRate() {
			return numDeletions / (float) numReferenceWords;
		}

		public float getInsertionRate() {
			return numInsertions / (float) numReferenceWords;
		}

		/** @return the word error rate of this collection */
		public float getWordErrorRate() {
			return (numSubstitutions + numDeletions + numInsertions) / (float) numReferenceWords;
		}

		/** @return the sentence error rate of this collection */
		public float getSentenceErrorRate() {
			return (numSentences - numSentenceCorrect) / (float) numSentences;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("# seq").append("\t");
			sb.append("# ref").append("\t");
			sb.append("# hyp").append("\t");
			sb.append("cor").append("\t");
			sb.append("sub").append("\t");
			sb.append("ins").append("\t");
			sb.append("del").append("\t");
			sb.append("WER").append("\t");
			sb.append("SER").append("\t");
			sb.append("\n");

			sb.append(numSentences).append("\t");
			sb.append(numReferenceWords).append("\t");
			sb.append(numHypothesisWords).append("\t");
			sb.append(getCorrectRate()).append("\t");
			sb.append(getSubstitutionRate()).append("\t");
			sb.append(getInsertionRate()).append("\t");
			sb.append(getDeletionRate()).append("\t");
			sb.append(getWordErrorRate()).append("\t");
			sb.append(getSentenceErrorRate());
			return sb.toString();
		}
	}

	/**
	 * Constructor. Creates an object with default alignment penalties.
	 */
	public WordSequenceAligner() {
		this(DEFAULT_SUBSTITUTION_PENALTY, DEFAULT_INSERTION_PENALTY, DEFAULT_DELETION_PENALTY);
	}

	/**
	 * Constructor.
	 * 
	 * @param substitutionPenalty
	 *            substitution penalty for reference-hypothesis string alignment
	 * @param insertionPenalty
	 *            insertion penalty for reference-hypothesis string alignment
	 * @param deletionPenalty
	 *            deletion penalty for reference-hypothesis string alignment
	 */
	public WordSequenceAligner(int substitutionPenalty, int insertionPenalty, int deletionPenalty) {
		this.substitutionPenalty = substitutionPenalty;
		this.insertionPenalty = insertionPenalty;
		this.deletionPenalty = deletionPenalty;
	}

	/**
	 * Produce alignment results for several pairs of sentences.
	 * 
	 * @see #align(String[], String[])
	 * @param references
	 *            reference sentences to align with the given hypotheses
	 * @param hypotheses
	 *            hypothesis sentences to align with the given references
	 * @return collection of per-sentence alignment results
	 */
	public List<Alignment> align(List<String[]> references, List<String[]> hypotheses) {
		if (references.size() != hypotheses.size()) {
			throw new IllegalArgumentException();
		}
		if (references.size() == 0) {
			return new ArrayList<WordSequenceAligner.Alignment>();
		}

		List<Alignment> alignments = new ArrayList<WordSequenceAligner.Alignment>();
		Iterator<String[]> refIt = references.iterator();
		Iterator<String[]> hypIt = hypotheses.iterator();
		while (refIt.hasNext()) {
			alignments.add(align(refIt.next(), hypIt.next()));
		}
		return alignments;
	}

	/**
	 * Produces {@link Alignment} results from the alignment of the hypothesis
	 * words to the reference words. Alignment is done via weighted string edit
	 * distance according to {@link #substitutionPenalty},
	 * {@link #insertionPenalty}, {@link #deletionPenalty}.
	 * 
	 * @param reference
	 *            sequence of words representing the true sentence; will be
	 *            evaluated as lowercase.
	 * @param hypothesis
	 *            sequence of words representing the hypothesized sentence; will
	 *            be evaluated as lowercase.
	 * @return results of aligning the hypothesis to the reference
	 */
	public Alignment align(String[] reference, String[] hypothesis) {
		// Values representing string edit operations in the backtrace matrix
		final int OK = 0;
		final int SUB = 1;
		final int INS = 2;
		final int DEL = 3;

		/*
		 * Next up is our dynamic programming tables that track the string edit
		 * distance calculation. The row address corresponds to an index within
		 * the sequence of reference words. The column address corresponds to an
		 * index within the sequence of hypothesis words. cost[0][0] addresses
		 * the beginning of two word sequences, and thus always has a cost of
		 * zero.
		 */

		/**
		 * cost[3][2] is the minimum alignment cost when aligning the first two
		 * words of the reference to the first word of the hypothesis
		 */
		int[][] cost = new int[reference.length + 1][hypothesis.length + 1];

		/**
		 * backtrace[3][2] gives information about the string edit operation
		 * that produced the minimum cost alignment between the first two words
		 * of the reference to the first word of the hypothesis. If a deletion
		 * operation is the minimum cost operation, then we say that the best
		 * way to get to hyp[1] is by deleting ref[2].
		 */
		int[][] backtrace = new int[reference.length + 1][hypothesis.length + 1];

		// Initialization
		cost[0][0] = 0;
		backtrace[0][0] = OK;

		// First column represents the case where we achieve zero hypothesis
		// words by deleting all reference words.
		for (int i = 1; i < cost.length; i++) {
			cost[i][0] = deletionPenalty * i;
			backtrace[i][0] = DEL;
		}

		// First row represents the case where we achieve the hypothesis by
		// inserting all hypothesis words into a zero-length reference.
		for (int j = 1; j < cost[0].length; j++) {
			cost[0][j] = insertionPenalty * j;
			backtrace[0][j] = INS;
		}

		// For each next column, go down the rows, recording the min cost edit
		// operation (and the cumulative cost).
		for (int i = 1; i < cost.length; i++) {
			for (int j = 1; j < cost[0].length; j++) {
				int subOp, cs; // it is a substitution if the words aren't
								// equal, but if they are, no penalty is
								// assigned.
				if (reference[i - 1].toLowerCase().equals(hypothesis[j - 1].toLowerCase())) {
					subOp = OK;
					cs = cost[i - 1][j - 1];
				} else {
					subOp = SUB;
					cs = cost[i - 1][j - 1] + substitutionPenalty;
				}
				int ci = cost[i][j - 1] + insertionPenalty;
				int cd = cost[i - 1][j] + deletionPenalty;

				int mincost = Math.min(cs, Math.min(ci, cd));
				if (cs == mincost) {
					cost[i][j] = cs;
					backtrace[i][j] = subOp;
				} else if (ci == mincost) {
					cost[i][j] = ci;
					backtrace[i][j] = INS;
				} else {
					cost[i][j] = cd;
					backtrace[i][j] = DEL;
				}
			}
		}

		// Now that we have the minimal costs, find the lowest cost edit to
		// create the hypothesis sequence
		LinkedList<String> alignedReference = new LinkedList<String>();
		LinkedList<String> alignedHypothesis = new LinkedList<String>();
		int numSub = 0;
		int numDel = 0;
		int numIns = 0;
		int i = cost.length - 1;
		int j = cost[0].length - 1;
		while (i > 0 || j > 0) {
			switch (backtrace[i][j]) {
			case OK:
				alignedReference.add(0, reference[i - 1].toLowerCase());
				alignedHypothesis.add(0, hypothesis[j - 1].toLowerCase());
				i--;
				j--;
				break;
			case SUB:
				alignedReference.add(0, reference[i - 1].toUpperCase());
				alignedHypothesis.add(0, hypothesis[j - 1].toUpperCase());
				i--;
				j--;
				numSub++;
				break;
			case INS:
				alignedReference.add(0, null);
				alignedHypothesis.add(0, hypothesis[j - 1].toUpperCase());
				j--;
				numIns++;
				break;
			case DEL:
				alignedReference.add(0, reference[i - 1].toUpperCase());
				alignedHypothesis.add(0, null);
				i--;
				numDel++;
				break;
			}
		}

		return new Alignment(alignedReference.toArray(new String[] {}), alignedHypothesis.toArray(new String[] {}),
				numSub, numIns, numDel);
	}
}
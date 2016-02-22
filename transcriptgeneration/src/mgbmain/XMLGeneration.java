package mgbmain;

import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import mgbutils.GenerateXMLTranscriptionN;

/**
 * 
 * @author Sameer Khurana (skhurana@qf.org.qa)
 *
 */
public class XMLGeneration {

	private static String traFilePath;
	private static String programFilePath;
	private static String mapNumbersFlag;
	private static String bukwalterFlag;
	private static String normalizeFlag;
	private static String transcriptType;
	private static String destinationFolder;
	private static String removeDiacritics;
	private static String annotateLatinWords;
	private static String removeBadSegments;
	private static String removeHesitation;
	private static String removeDoubleHash;
	private static Options options;

	static {
		options = new Options();
		options.addOption("tra", true, "Absolute Path: Give the ALL.tra file");
		options.addOption("p", true, "Absolute Path: program tdf file path");
		options.addOption("num", true, "Yes/No: flag for mapping the arabic indic numbers to Arabic numberals");
		options.addOption("bw", true, "Yes/No: Convert to bukwalter or not");
		options.addOption("t", true, "Train/Dev: Generate xml for training or dev set");
		options.addOption("norm", true, "Yes/No: normalize the arabic text or not, remove punctuations");
		options.addOption("d", true, "transcript xml files destination folder");
		options.addOption("a", true, "Yes/No: annotate latin words in the corpus with the tag @@LAT@@");
		options.addOption("rd", true, "Yes/No: remove diacritics");
		options.addOption("b", true, "Yes/No: remove segments from the dev set that have overlap speech");
		options.addOption("h", true, "Yes/No: remove hesitation mark or not");
		options.addOption("hh", true, "Yes/No: remove double ## at the start of a word");
	}

	public static void main(String[] args) {
		parse(args);
		// TODO Duplicate class added for now so an not to break Hamdy's code.
		// CLean it up
		GenerateXMLTranscriptionN gen = new GenerateXMLTranscriptionN();
		try {
			gen.createTranscript(traFilePath, destinationFolder, programFilePath, normalizeFlag, bukwalterFlag,
					mapNumbersFlag, transcriptType, removeDiacritics, annotateLatinWords, removeBadSegments,
					removeHesitation, removeDoubleHash);
		} catch (IllegalArgumentException | IllegalAccessException | IOException e) {
			e.printStackTrace();
		}
	}

	private static void parse(String[] args) {
		CommandLineParser parser = new BasicParser();

		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("tra") && cmd.hasOption("p") && cmd.hasOption("num") && cmd.hasOption("bw")
					&& cmd.hasOption("t") && cmd.hasOption("norm") && cmd.hasOption("a") && cmd.hasOption("p")
					&& cmd.hasOption("rd") && cmd.hasOption("b") && cmd.hasOption("h") && cmd.hasOption("hh")) {
				traFilePath = cmd.getOptionValue("tra");
				programFilePath = cmd.getOptionValue("p");
				mapNumbersFlag = cmd.getOptionValue("num");
				bukwalterFlag = cmd.getOptionValue("bw");
				normalizeFlag = cmd.getOptionValue("norm");
				transcriptType = cmd.getOptionValue("t");
				destinationFolder = cmd.getOptionValue("d");
				annotateLatinWords = cmd.getOptionValue("a");
				removeDiacritics = cmd.getOptionValue("rd");
				removeBadSegments = cmd.getOptionValue("b");
				removeHesitation = cmd.getOptionValue("h");
				removeDoubleHash = cmd.getOptionValue("hh");

			} else {
				help();
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	private static void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("mgbmain.XMLGeneration -cp \".:/path/to/lib/*\"", options);
		System.exit(0);
	}

}

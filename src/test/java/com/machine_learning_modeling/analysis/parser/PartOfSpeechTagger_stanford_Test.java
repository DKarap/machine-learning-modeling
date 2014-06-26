package com.machine_learning_modeling.analysis.parser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.machine_learning.core.analysis.tokenizer.Tokenizer;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning.utils.Helper;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger_stanford_implementation;

/**
 * Unit test for simple App.
 */
public class PartOfSpeechTagger_stanford_Test extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public PartOfSpeechTagger_stanford_Test(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(PartOfSpeechTagger_stanford_Test.class);
	}

	/**
	 * 
	 * @throws NoLanguageSupportException
	 * @throws IOException 
	 */
	public void testPOStagger() throws NoLanguageSupportException, IOException  {

		// Initialize the tagger
		String language = "en";
		final boolean skip_stopwords = true;
		final List<String> stopwords;
		if(language.equals("en"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/en/stopwords.txt");
		else if(language.equals("nl"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/nl/stopwords.txt");
		else
			stopwords=null;

		String pos_stanford_filepath = "./data/part_of_speech_stanford/models/english-left3words-distsim.tagger";
		
		int max_ngram_length = 3;
		PartOfSpeechTagger partOfSpeechTagger = new PartOfSpeechTagger_stanford_implementation(language,pos_stanford_filepath);
		Tokenizer tokenizer = new Tokenizer(skip_stopwords,stopwords);
		
		// The sample string
		String sample = "The election year politics are annoying for many people.";
 
		// The tagged string
		List<String> arr = tokenizer.simpleTokenize(sample, 1, 1,1,25, false, true,true);
		Map<String,Set<String>> wordToToTagSetMap =  partOfSpeechTagger.parseTokenizedSentence(arr.toArray(new String[arr.size()]), max_ngram_length);

		//test
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("election year politics").toString());
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("election year").toString());
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("year politics").toString());

	}
}

package com.machine_learning_modeling.analysis.parser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import opennlp.tools.util.InvalidFormatException;

import com.machine_learning.core.analysis.tokenizer.Tokenizer;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning.utils.Helper;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger_opennlp_implementation;

/**
 * Unit test for simple App.
 */
public class PartOfSpeechTagger_opennlp_Test extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public PartOfSpeechTagger_opennlp_Test(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(PartOfSpeechTagger_opennlp_Test.class);
	}

	/**
	 * 
	 * @throws NoLanguageSupportException
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	public void testPOStagger() throws NoLanguageSupportException, InvalidFormatException, IOException  {
		
		/**
		 * ENGLISH
		 */
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

		String pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/en-pos-maxent.bin";
		
		
		
		int max_ngram_length = 3;
		PartOfSpeechTagger partOfSpeechTagger = new PartOfSpeechTagger_opennlp_implementation(language,pos_maxent_opennlp_filepath);
		Tokenizer tokenizer = new Tokenizer(skip_stopwords,stopwords);
		
		// The sample string
		String sample = "The election year politics are annoying for many people.";
		//String output = "{election year politics=[NP], many=[JJ], politics=[NNS], The=[DT], election year=[NP], election=[NN], are=[VBP], for=[IN], year politics=[NP], year=[NN], annoying=[VBG], people=[NNS]}";

		// The tagged string
		List<String> arr = tokenizer.simpleTokenize(sample, 1, 1,1,25, false, true, true);
		Map<String,Set<String>> wordToToTagSetMap =  partOfSpeechTagger.parseTokenizedSentence(arr.toArray(new String[arr.size()]), max_ngram_length);
		System.out.println(wordToToTagSetMap);

		
		//test
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("election year politics").toString());
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("election year").toString());
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("year politics").toString());
		
		
		
		/**
		 * DUTCH
		 */
		// Initialize the tagger
		language = "nl";
		max_ngram_length = 3;
		pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/nl-pos-maxent.bin";
		partOfSpeechTagger = new PartOfSpeechTagger_opennlp_implementation(language,pos_maxent_opennlp_filepath);
		
		// The sample string
		sample = "Je hebt minimaal WO werk- en denkniveau en bij voorkeur een journalistieke of bedrijfseconomische opleiding genoten";
		//String output = "{election year politics=[NP], many=[JJ], politics=[NNS], The=[DT], election year=[NP], election=[NN], are=[VBP], for=[IN], year politics=[NP], year=[NN], annoying=[VBG], people=[NNS]}";

		// The tagged string
		arr = tokenizer.simpleTokenize(sample, 1, 1, 1,25,false, true, true);
		wordToToTagSetMap =  partOfSpeechTagger.parseTokenizedSentence(arr.toArray(new String[arr.size()]), max_ngram_length);
		System.out.println(wordToToTagSetMap);
		
		//test
		assertEquals("POS openNlp broke..", "[NP]", wordToToTagSetMap.get("wo werk").toString());


	}
}

package com.machine_learning_modeling.analysis.parser;

import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.machine_learning_modeling.core.analysis.parser.ParserPCFG_stanford_implementation;
import com.machine_learning.exception.NoLanguageSupportException;

/**
 * Unit test for simple App.
 */
public class ParserPCFG_stanford_Test extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ParserPCFG_stanford_Test(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ParserPCFG_stanford_Test.class);
	}

	
	
	/**
	 * 
	 * @throws com.machine_learning.exception.NoLanguageSupportException 
	 * @throws NoLanguageSupportException
	 */
	public void testPOStagger() throws com.machine_learning.exception.NoLanguageSupportException   {
		String language = "en";
		ParserPCFG_stanford_implementation parserPCFG = new ParserPCFG_stanford_implementation(language,"./data/parser_PCFG_stanford/lexparser/englishPCFG.ser.gz");

		//test sentence - simply tokenization
		String sentence = "Trainee Test Engineer.";
		sentence = sentence.replaceAll("\\W+", " ");
		String[] tokenArray = sentence.split("\\s+");

		int max_ngram_length = 3;
		Map<String,Set<String>> ngramToTagSet = parserPCFG.parseTokenizedSentence(tokenArray,max_ngram_length);
		String expectedParsing = "[NP]";
		assertEquals("Parser broke...", expectedParsing, ngramToTagSet.get("trainee test engineer").toString());
	}
}

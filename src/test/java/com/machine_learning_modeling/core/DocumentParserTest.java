package com.machine_learning_modeling.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import opennlp.tools.util.InvalidFormatException;

import com.cybozu.labs.langdetect.LangDetectException;
import com.machine_learning.core.analysis.SentenceDetector;
import com.machine_learning.core.analysis.tokenizer.Tokenizer;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning.exception.NoSemanticFieldSupportException;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger_opennlp_implementation;
import com.machine_learning_modeling.domain.Attribute;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.utils.Helper;

import de.l3s.boilerpipe.BoilerpipeProcessingException;

/**
 * Unit test for simple App.
 */
public class DocumentParserTest extends TestCase {
	public static String lang_profiles_filepath = "./data/language_profiles";

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public DocumentParserTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(DocumentParserTest.class);
	}


	/**
	 * Test extracting Features from a test document 
	 * @throws LangDetectException 
	 * @throws IOException 
	 * @throws NoLanguageSupportException 
	 * @throws InvalidFormatException 
	 * @throws com.machine_learning.exception.NoLanguageSupportException 
	 * @throws FieldDoesntExistInDocumentException 
	 * 
	 */
	public void testGetFeatures() throws LangDetectException, InvalidFormatException,  IOException, com.machine_learning.exception.NoLanguageSupportException, NoLanguageSupportException  {
		int min_ngram_length = 1;
		int max_ngram_length = 3;
		final List<String> stopwords;
		final boolean skip_stopwords = true;

		List<String> fieldToParseList = new ArrayList<String>(Arrays.asList("title","detail_page_text"));

		//create document
		Document doc = createTestDocument();
		String language = doc.getLanguage();
		
		if(language.equals("en"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/en/stopwords.txt");
		else if(language.equals("nl"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/nl/stopwords.txt");
		else
			stopwords=null;

		String pos_maxent_opennlp_filepath = null;
		if(language.equals("en"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/en-pos-maxent.bin";
		else if(language.equals("nl"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/nl-pos-maxent.bin";
		else if(language.equals("de"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/de-pos-maxent.bin";

		
		String sentence_model_filepath = null;
		if(language.equals("en"))
			sentence_model_filepath = "./data/sentence_detector_openNlp/en-sent.bin";
		else if(language.equals("nl"))
			sentence_model_filepath = "./data/sentence_detector_openNlp/nl-sent.bin";

		
		
		
		
		//INITIALIZER...BASED ON THE GIVEN LANGUAGE
		DocumentParser documentParser = new DocumentParser(lang_profiles_filepath);
		Tokenizer tokenizer = new Tokenizer(skip_stopwords,stopwords);
		SentenceDetector sentenceDetector = new SentenceDetector(doc.getLanguage(),sentence_model_filepath);
		PartOfSpeechTagger partOfSpeechTagger = new PartOfSpeechTagger_opennlp_implementation(doc.getLanguage(),pos_maxent_opennlp_filepath);
		boolean use_pos_tagger = true;
		boolean lowerCase = true;
		boolean ignorePunctuation = true;
		boolean ignoreDigits = true;

		int min_token_length = 1;
		int max_token_length = 25;
		
		//get documents features
		List<Feature> featureList = documentParser.getDocumentFeatures(doc,fieldToParseList, tokenizer, sentenceDetector, partOfSpeechTagger,min_ngram_length, max_ngram_length, min_token_length, max_token_length, use_pos_tagger,lowerCase, ignorePunctuation, ignoreDigits);
		
		Feature testFeat = new Feature("trainee test engineer","en");
		Feature featParsed = featureList.get(featureList.indexOf(testFeat));
		
		assertEquals("wrong doc freq:", 1, featParsed.getDoc_frequency());
		assertEquals("wrong ngram_length:", 3, featParsed.getNgram_length());
		
		List<Attribute> fieldAttList = featParsed.getFieldAttributeList();
		assertEquals("wrong field doc freq:", 1, fieldAttList.get(fieldAttList.indexOf(new Attribute("title"))).getDoc_frequency());

		List<Attribute> posAttList = featParsed.getPart_of_speechAttributeList();
		Attribute posAttr = posAttList.get(posAttList.indexOf(new Attribute("NP")));
		assertEquals("wrong pos doc freq:", 1, posAttr.getDoc_frequency());
		assertEquals("wrong pos total freq:", 2, posAttr.getTotal_frequency());

		//test number of detected features
		fieldToParseList = new ArrayList<String>(Arrays.asList("title"));
		featureList = documentParser.getDocumentFeatures(doc,fieldToParseList, tokenizer, sentenceDetector, partOfSpeechTagger,1, 1,min_token_length, max_token_length,use_pos_tagger,lowerCase, ignorePunctuation, ignoreDigits);
		assertEquals("wrong number of extracted features", 7, featureList.size());
		
		testFeat = new Feature("test","en");
		featParsed = featureList.get(featureList.indexOf(testFeat));
		fieldAttList = featParsed.getFieldAttributeList();
		assertEquals("wrong field total freq:", 2, fieldAttList.get(fieldAttList.indexOf(new Attribute("title"))).getTotal_frequency());


//		for(Feature feature : featureList)
//			System.out.println(feature.toString());

		//at the end close lang profiles
		documentParser.closeDocumentParser();
	}
	
	
	public void testDocumentSemanticParser() throws LangDetectException, BoilerpipeProcessingException, NoSemanticFieldSupportException{
		//create document
		Document doc = createTestDocument();
			
		
		//INITIALIZER...BASED ON THE GIVEN LANGUAGE
		DocumentParser documentParser = new DocumentParser(lang_profiles_filepath);
		List<String> semanticList = Arrays.asList("language","detail_page_main_content"); //fields for semantic processing
		documentParser.semanticParser(doc, semanticList);
		
		
		String lang = doc.getFieldValueByKey("language");
		assertEquals("wrong lang detection:", "unknown",lang);
		assertEquals("wrong main content detection:", null,doc.getFieldValueByKey("detail_page_main_content"));

		
		documentParser.closeDocumentParser();
	}
	
	
	private static Document createTestDocument(){
		String language = "en";
		Map<String,String> fieldToTextMap = new HashMap<String,String>();
		fieldToTextMap.put("title", "For our Test Team in the Leiden office, we are looking for a Trainee Test Engineer!");
		fieldToTextMap.put("detail_page_text", " <a>For our Test Team in the Leiden office, we are looking for a Trainee Test Engineer!</a>");
		fieldToTextMap.put("url", " http://www.shortoftheweek.com/2013/12/18/the-runners/");	
		fieldToTextMap.put("language", language);
		Document doc = new Document(1L,fieldToTextMap, language);
		return doc;
	}
}

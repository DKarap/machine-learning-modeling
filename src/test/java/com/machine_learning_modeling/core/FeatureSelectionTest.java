package com.machine_learning_modeling.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.machine_learning_modeling.domain.Attribute;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.exception.DictionaryDuplicationException;
import com.machine_learning_modeling.exception.SmoothingException;

/**
 * We dont test UTILITY MEASURES HERE>>>>thats why we dont get smoothing exeption...
 */
public class FeatureSelectionTest extends TestCase {

	
	public void testRulesFiltering() throws IOException, DictionaryDuplicationException, SmoothingException {

		/**
		 * Input....
		 */
		String dictionary_name = "dictionary_test";
		String document_type = "feature";
		String language = "en";
		boolean accept_only_noun_phrases = false;
		int min_ngram_length = 1;
		int max_ngram_length = 1;
		int min_token_length = 3;
		int max_token_length = 15;
		int min_document_frequency = 1;
		int min_source_frequency = 1;
		final List<String> accepted_fields = Arrays.asList("unknown_field"); //fields in where the accepted features must have been seen before in our corpus
		Map<String,Double> fieldToWeigthMap = new HashMap<String,Double>();
		
		
		Dictionary dictionary = new Dictionary(dictionary_name, document_type,1000,100); //dictionary doesnt include the test features
		
		
		/**
		 * ########### TEST FILTERING FEATURES PART
		 */
		Feature testFeature = createTestFeature("test feature",language);
		List<Feature> featureCandidateList = new ArrayList<Feature>(Arrays.asList(testFeature));
		FeatureSelection featureSelection = new FeatureSelection( min_ngram_length, max_ngram_length,  min_token_length, max_token_length, min_document_frequency,min_source_frequency,accept_only_noun_phrases,null);
		featureCandidateList = featureSelection.parser(featureCandidateList,dictionary,-1,fieldToWeigthMap);
		assertEquals("ngram length filter broke.. ", 0,featureCandidateList.size());

		
		featureCandidateList = new ArrayList<Feature>(Arrays.asList(createTestFeature("feature",language)));
		featureSelection = new FeatureSelection( min_ngram_length, max_ngram_length, min_token_length, max_token_length,2,min_source_frequency,accept_only_noun_phrases,null);
		featureCandidateList = featureSelection.parser(featureCandidateList,dictionary,-1,fieldToWeigthMap);
		assertEquals("min doc freq filter broke.. ", 0,featureCandidateList.size());

		
		featureCandidateList = new ArrayList<Feature>(Arrays.asList(createTestFeature("feature",language)));
		featureSelection = new FeatureSelection( min_ngram_length, max_ngram_length, min_token_length, max_token_length,min_document_frequency,20,accept_only_noun_phrases,null);
		featureCandidateList = featureSelection.parser(featureCandidateList,dictionary,-1,fieldToWeigthMap);
		assertEquals("min source freq filter broke.. ", 0,featureCandidateList.size());



		featureCandidateList = new ArrayList<Feature>(Arrays.asList(createTestFeature("test",language)));
		featureSelection = new FeatureSelection( min_ngram_length, max_ngram_length,min_token_length, max_token_length, min_document_frequency,min_source_frequency,true,null);
		featureCandidateList = featureSelection.parser(featureCandidateList,dictionary,-1,fieldToWeigthMap);
		assertEquals("part of speech filter broke.. ", 0,featureCandidateList.size());

		
		featureCandidateList = new ArrayList<Feature>(Arrays.asList(createTestFeature("tttttttttttttttttttttttt",language),createTestFeature("t",language)));
		featureSelection = new FeatureSelection( min_ngram_length, max_ngram_length,3, 10, min_document_frequency,min_source_frequency,accept_only_noun_phrases,null);
		featureCandidateList = featureSelection.parser(featureCandidateList,dictionary,-1,fieldToWeigthMap);
		assertEquals("token length filter broke.. ", 0,featureCandidateList.size());



		//test accepted fields that a good feature must exist 
		featureCandidateList = new ArrayList<Feature>(Arrays.asList(createTestFeature("test senior engineer",language)));
		featureSelection = new FeatureSelection( min_ngram_length, 3,min_token_length, max_token_length, min_document_frequency,min_source_frequency,accept_only_noun_phrases,accepted_fields);
		featureCandidateList = featureSelection.parser(featureCandidateList,dictionary,-1,fieldToWeigthMap);
		assertEquals("accept features that appear only in given fields filter broke.. ", 0,featureCandidateList.size());

		
		
		/**
		 * ########## TEST UTLITY MEASURES PART
		 */
	}
	
	private static Feature createTestFeature(String value,String language){
		Feature feature = new Feature(value, language);
		
		feature.addFieldAttr(new Attribute("title"));
		feature.addFieldAttr(new Attribute("url"));
		feature.addFieldAttr(new Attribute("content"));
		feature.addFieldAttr(new Attribute("detail_page_main_content"));
		
		feature.addPartOfSpeechAttr(new Attribute("JJ"));
		feature.addPartOfSpeechAttr(new Attribute("VV"));
		feature.addPartOfSpeechAttr(new Attribute("VB"));
		
		return feature;
	}

}

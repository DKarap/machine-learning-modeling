package com.machine_learning_modeling.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.elasticsearch.ElasticsearchException;

import com.machine_learning_modeling.domain.Attribute;
import com.machine_learning_modeling.domain.Feature;

/**
 * Unit test for simple App.
 */
public class DictionaryTest extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public DictionaryTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(DictionaryTest.class);
	}

	

	public void testDictionary() throws ElasticsearchException, Exception {

		final String dictionary_name = "dictionary_test";
		final String document_type = "feature";
		final String language = "en";
		boolean update_doc_freq = false;
		boolean update_source_freq = false;
		
		Dictionary dictionary = new Dictionary(dictionary_name, document_type,1000,100);

		// flush index
		dictionary.eraseDictionary();
		assertEquals("Empty Index at the start didnt work.. ", 0,dictionary.getNumberOfFeaturesInDictionary());
		assertEquals("Empty Index at the start didnt work.. ", 0,dictionary.getNumberOfParsedDocuments());
		assertEquals("Empty Index at the start didnt work.. ", 0,dictionary.getNumberOfParsedSources());
		assertEquals("Empty Index at the start didnt work.. ", 0,dictionary.getIndexStat("index_stats","total_docs"));
		assertEquals("Empty Index at the start didnt work.. ", 0,dictionary.getIndexStat("index_stats","total_sources"));
		
		
		
		
		
		//add test features
		List<String> testFeatures = new ArrayList<String>(Arrays.asList("test engineer","senior engineer","test senior","test senior engineer"));
		int nr_docs = 30;
		int Min = 0;int Max=testFeatures.size()-1;
		Set<String> uniqueFeatureThatWeAdd = new HashSet<String>();
		for (int i = 1; i <= nr_docs; i++) {
			int randomFeatureIndex = Min + (int)(Math.random() * ((Max - Min) + 1));
			System.out.println("i:" + i + "\trandomFeatureIndex:" + randomFeatureIndex);
			
			Feature featureToAdd = createTestFeature(testFeatures.get(randomFeatureIndex),language);
			dictionary.addFeature(featureToAdd, update_doc_freq, update_source_freq);
			uniqueFeatureThatWeAdd.add(testFeatures.get(randomFeatureIndex));
		}
		dictionary.flushMemoryFeaturesIntoIndex();
		
		
		
		// test nr of documents  dictionary
		System.out.println("in Dict:"+dictionary.getNumberOfFeaturesInDictionary()+"\tIm mempry:"+dictionary.getNumberOfFeaturesInMemory()+"\tuniqueFeatureThatWeAdd:"+uniqueFeatureThatWeAdd.size());
		assertEquals("inCorrect of nr of features in Dictionary", uniqueFeatureThatWeAdd.size(), dictionary.getNumberOfFeaturesInDictionary());

		
		//add number of parsed documents and sources
		dictionary.increaseNumberOfParsedDocuments(10);
		dictionary.increaseNumberOfParsedSources(10);
		dictionary.setIndexStats();
		assertEquals("NumberOfParsedDocumentsToIndex is not correct...", 10, dictionary.getNumberOfParsedDocuments());
		assertEquals("NumberOfParsedSourcesToIndex is not correct...", 10, dictionary.getNumberOfParsedSources());

		
		dictionary.computeUtilityMeasures();
		dictionary.refreshIndex();

		// test also the update frequencies part(total doc frequencies)
		String fieldToSearchName = "word";
		String fieldToSearchValue = "test senior engineer";
		Feature feature = dictionary.getFeature(fieldToSearchName,fieldToSearchValue);
		assertEquals("Wrong source frequency", 1, feature.getSource_frequency());
		Attribute attr = feature.getAttribute("field", "title");
		assertEquals("Wrong doc freqiuency;should be one", 1, attr.getDoc_frequency());
		
		
		assertEquals("Wrong idf computation..", 3.3219280948873626, feature.getIdf_doc());

		// close dictionary
		dictionary.closeDictionary();
	}
	
	


	private static Feature createTestFeature(String value,String language){
		Feature feature = new Feature(value, language);
		
		feature.addFieldAttr(new Attribute("title"));
		feature.addFieldAttr(new Attribute("url"));
		feature.addFieldAttr(new Attribute("content"));
		
		feature.addPartOfSpeechAttr(new Attribute("NN",10L,5L));
		feature.addPartOfSpeechAttr(new Attribute("NP"));
		feature.addPartOfSpeechAttr(new Attribute("VB"));
		
		return feature;
	}

}

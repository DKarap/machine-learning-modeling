package com.machine_learning_modeling.core.analysis.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.InvalidFormatException;

import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning_modeling.utils.Helper;

/**
 * Part of speech of tagger utilize Stanford core nlp library..
 * @author mimis
 *
 */

public class PartOfSpeechTagger_opennlp_implementation implements PartOfSpeechTagger{

	private final String language;
	private final POSTaggerME tagger;
	
	
	public PartOfSpeechTagger_opennlp_implementation(String language, String pos_maxent_opennlp_filepath)  throws NoLanguageSupportException, InvalidFormatException, IOException {
		super();
		this.language = language.toLowerCase();
	    InputStream  modelIn = new FileInputStream(pos_maxent_opennlp_filepath);		
		POSModel model = new POSModel(modelIn);
		this.tagger = new POSTaggerME(model);
	    modelIn.close();


	}
	
	

	public String getLanguage() {
		return language;
	}



	@Override	
	public Map<String,Set<String>> parseTokenizedSentence(String[] textArray, int max_ngram_length){
		String[] tagsArr = tagger.tag(textArray);
		Set<String> nounPhrasesSet = Helper.getNounPhrases(textArray, tagsArr);
		//create tagMap - lower case ngrams
		Map<String,Set<String>> ngramToTagSet = Helper.getNgramToTagSetMapSentence(textArray, tagsArr, nounPhrasesSet, max_ngram_length);
		return ngramToTagSet;
	}
	

}

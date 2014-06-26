package com.machine_learning_modeling.core.analysis.parser;

import java.util.Map;
import java.util.Set;

import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning_modeling.utils.Helper;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;


/**
 * Part of speech of tagger utilize Stanford core nlp library..
 * @author mimis
 *
 */

public class PartOfSpeechTagger_stanford_implementation implements PartOfSpeechTagger{

	private final String language;
	private final MaxentTagger tagger;
	
	
	public PartOfSpeechTagger_stanford_implementation(String language,String pos_stanford_filepath)  throws NoLanguageSupportException {
		super();
		this.language = language.toLowerCase();
		this.tagger = new MaxentTagger(pos_stanford_filepath);
		
	}

	

	public String getLanguage() {
		return language;
	}



	@Override
	public Map<String,Set<String>> parseTokenizedSentence(String[] textArray, int max_ngram_length) {
		String taggedString = tagger.tagString(edu.stanford.nlp.util.StringUtils.join(textArray," ")); //word_tag word2_tag2 ...wordN_tagN
		String[] tagsArr = Helper.getTagArrayFromTaggedSentence(taggedString);
		Set<String> nounPhrasesSet = Helper.getNounPhrases(textArray, tagsArr);
		
		//create tagMap
		Map<String,Set<String>> ngramToTagSet = Helper.getNgramToTagSetMapSentence(textArray, tagsArr, nounPhrasesSet, max_ngram_length);

		return ngramToTagSet;
	}
}

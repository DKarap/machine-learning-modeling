package com.machine_learning_modeling.core.analysis.parser;

import java.util.Map;
import java.util.Set;

public interface ParserPCFG {

	
	/**
	 * 
	 * @param textArray - array of a sentence's ordered unigrams
	 * @param max_ngram_length - max ngram of tagged ngrams to return
	 * @return a map for each ngram to its corresponding pos tag set
	 */
	public Map<String,Set<String>> parseTokenizedSentence(String[] textArray, int max_ngram_length) ;

}

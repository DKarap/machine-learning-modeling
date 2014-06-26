package com.machine_learning_modeling.core.analysis.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.machine_learning.exception.NoLanguageSupportException;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

public class ParserPCFG_stanford_implementation implements ParserPCFG{

	private final String language;
	private final LexicalizedParser lexicalizedParser;

	public ParserPCFG_stanford_implementation(String language,String grammar_filepath) throws NoLanguageSupportException {
		super();
		this.language = language;
		this.lexicalizedParser = LexicalizedParser.loadModel(grammar_filepath);
	}
	
	

	public String getLanguage() {
		return language;
	}



	/**
	 * 
	 * @param textArray a list of words to parse - CASE SENSITIVE
	 * @param max_ngram_length that we return
	 * @return map of ngrams together with their POS tags  
	 */
	@Override
	public Map<String,Set<String>> parseTokenizedSentence(String[] textArray, int max_ngram_length) {
		Map<String,Set<String>> ngramToTagSet = new HashMap<String,Set<String>>();
		
		List<CoreLabel> rawWords = Sentence.toCoreLabelList(textArray);
		Tree parse = this.lexicalizedParser.apply(rawWords);
		
//		parse.pennPrint();
//		ArrayList<TaggedWord> taggedWordList = parse.taggedYield();
//		for(TaggedWord taggedWord:taggedWordList)
//			System.out.println(taggedWord.tag()+"\t"+taggedWord.value()+"\t"+taggedWord.word());
//		System.out.println(parse.taggedYield());
		
		
		for (Tree child : parse.subTrees()) {
			String tag = child.value();
			final StringBuilder buf = new StringBuilder();
			List<Tree> leaves = child.getLeaves(); // leaves correspond to the tokens
			
			for (Tree leaf : leaves) {
				List<Word> words = leaf.yieldWords();
				for (Word word : words)
					buf.append(word.word()+" ");
			}
			
			//filter: ROOT value and records with identical value tag(word-word)
			String ngram = buf.toString().toLowerCase().trim();
			if(!tag.equals("ROOT") && !tag.equals("S") && !tag.equalsIgnoreCase(ngram) && ngram.split("\\s").length <= max_ngram_length){
//				System.out.println("TAG:" + tag + "\tWORD:" + ngram);
				//update map
				Set<String> TagsSet = ngramToTagSet.get(ngram);
				if(TagsSet != null){
					TagsSet.add(tag);
				}
				else{
					ngramToTagSet.put(ngram, new HashSet<String>(Arrays.asList(tag)));
				}
			}
		}
		return ngramToTagSet;
	}
	
//	public static Map<String,Set<String>> getNounPhrasesFromTaggedSentence(String posTaggedSentence){
//		
//	}
}
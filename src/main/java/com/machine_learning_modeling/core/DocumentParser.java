package com.machine_learning_modeling.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cybozu.labs.langdetect.LangDetectException;
import com.machine_learning.core.analysis.ExtractMainContentOfWebPage;
import com.machine_learning.core.analysis.LanguageDetection;
import com.machine_learning.core.analysis.SentenceDetector;
import com.machine_learning.core.analysis.tokenizer.Tokenizer;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning.exception.NoSemanticFieldSupportException;

import de.l3s.boilerpipe.BoilerpipeProcessingException;

/**
 * #Description:
 * 	
 *		Document parser is responsible for:
 *		 	1. extracting the features together with their stats from a document's given fields 
 *			2. Detect and save the given semantics to the document; 
 * 				Possible semantics are: 
 * 					*'detail_page_main_content' from the 'detail_page_text' 
 * 					*'language' from the 'detail_page_main_content'
 * 
 * 
 * @author mimis
 *
 */
public class DocumentParser {
	private final LanguageDetection languageDetection;

	
	
	
	public DocumentParser(String lang_profiles_filepath) throws LangDetectException {
		super();
		this.languageDetection = new LanguageDetection(lang_profiles_filepath);
	}

	public void closeDocumentParser(){
		this.languageDetection.closeLanguageProfiles();
	}
	
	/**
	 * parse given document and extract its features from the given fields -  requires the document's language!!!!! 
	 * @param document
	 * @param tokenizer
	 * @param sentenceDetector
	 * @param parserPCFG_stanford
	 * @return list of features with their corresponding characteristics(tf and df per field,pos)
	 * @throws IOException
	 * @throws FieldDoesntExistInDocumentException 
	 * @throws NoLanguageSupportException 
	 */
	public List<Feature> getDocumentFeatures(Document document, Collection<String> fieldToParseList, Tokenizer tokenizer,SentenceDetector sentenceDetector,  PartOfSpeechTagger partOfSpeechTagger, int min_ngram_length, int max_ngram_length, int min_token_length, int max_token_length, boolean extract_pos_tags,boolean lowerCase, boolean ignorePunctuation, boolean ignoreDigits) throws IOException, NoLanguageSupportException{
		List<Feature> featureList = new ArrayList<Feature>();
		String language = document.getLanguage();
		if(language == null){
			throw new NoLanguageSupportException("In order to extract the documents features we need to know its language beforehand;given lang is null here..");
		}
		
		boolean update_doc_freq = false; //dont update doc frequencies since we keep stats only for the given single document
		boolean update_source_freq = false; // ''
		
		for(String field : fieldToParseList){
			String text = document.getFieldToTextMap().get(field);
			
			if(text != null){

				//1. Sentence Detector
				List<String> sentences = sentenceDetector.sentenceDetector(text);

				//for each sentence
				for(String sentence : sentences){
					//System.out.println(sentence);
					
					//2. Tokenization -
					List<String> allTokensLowerCase = tokenizer.simpleTokenize(sentence, min_ngram_length, max_ngram_length, min_token_length, max_token_length, lowerCase, ignorePunctuation, ignoreDigits);//all ngrams lower case
					List<String> unigramsTokens = tokenizer.simpleTokenize(sentence, 1, 1,  min_token_length,  max_token_length, false, true, true); //case sensitive only unigrams for parsing
					
					//3. Get ngrams annotated - ONLY UNIGRAMS and CASE SENSITIVE as input!!!! LOWER CASE output!
					Map<String,Set<String>> tokenToTagsSetMap = new HashMap<String,Set<String>>();
					if(extract_pos_tags)
						tokenToTagsSetMap = partOfSpeechTagger.parseTokenizedSentence(unigramsTokens.toArray(new String[unigramsTokens.size()]), max_ngram_length);
					//System.out.println(tokenToTagsSetMap.toString());

					//updates per Token the field and POS frequencies
					addFeaturesToFeatureList(featureList, language, field, allTokensLowerCase, tokenToTagsSetMap, update_doc_freq,update_source_freq);
				}
	
			}
		}	
		return featureList;
	}

	
	/**
	 * Detect and save the given semantics to the document; 
	 * 	Possible semantics are: 
	 * 		*detail_page_main_content from the detail_page_text 
	 * 		*language from the detail_page_main_content   
	 * 	
	 * @param document with fieldName To Value Map for each field that we want to process or not..
	 * @param semanticsList
	 * @throws BoilerpipeProcessingException
	 * @throws LangDetectException
	 * @throws NoSemanticFieldSupportException 
	 */
	public void semanticParser(Document document, List<String> semanticsList) throws BoilerpipeProcessingException, LangDetectException, NoSemanticFieldSupportException{
		/*
		 * ORDER OF PROCESS IS IMPORTANT!!!!
		 */
    	//extract main content of detail page    	
    	if(semanticsList.contains("detail_page_main_content"))
    		detectDetailPageMainContent(document);
    	
   		//detect language based only the detail_page_main_content
    	if(semanticsList.contains("language"))
    		detectLanguage(document);
    	
    	
    	if(semanticsList.size()>2){
    		for(String semantic:semanticsList)
    			if(!semantic.equals("detail_page_main_content") && !semantic.equals("language"))
    				throw new NoSemanticFieldSupportException("No Semantic support for field:"+semantic);
    	}
	}
	
	
	
	
	
	/**
	 * Extract the main content of the 'detail_page_text' field and save it to 'detail_page_main_content' document's field, otherwise sane 'NULL' value 
	 * @param document with fieldTo Value Map including the 'detail_page_text' field
	 * @throws BoilerpipeProcessingException
	 */
	private void detectDetailPageMainContent(Document document) throws BoilerpipeProcessingException{
		String detailPageText = document.getFieldToTextMap().get("detail_page_text");
		if(detailPageText!=null){
			String mainContentOfDetailPageText = ExtractMainContentOfWebPage.getMainContentFromWebPage(detailPageText);
			if(mainContentOfDetailPageText !=null && !mainContentOfDetailPageText.isEmpty())
				document.getFieldToTextMap().put("detail_page_main_content", mainContentOfDetailPageText);
			else
				document.getFieldToTextMap().put("detail_page_main_content", null);

		}
	}
	
	
	
	
	/**
	 * 	Detect Language form the 'detail_page_main_content' field and save it into the 'language' doc's field, otherwise save 'unknown'
	 * @param document with fieldTo Value Map including the 'detail_page_main_content' field
	 * @throws LangDetectException
	 */
	private void detectLanguage(Document document) throws LangDetectException{
    	String detail_page_main_content = document.getFieldToTextMap().get("detail_page_main_content");
    	String lang = null;
    	if(detail_page_main_content != null && !detail_page_main_content.isEmpty()){
    		lang = this.languageDetection.detectLang(detail_page_main_content);
    	}
    	
    	if(lang != null)
    		document.getFieldToTextMap().put("language", lang);
    	else
    		document.getFieldToTextMap().put("language", "unknown");
	}
	
	
	/**
	 * 
	 * @param featureList
	 * @param language
	 * @param field
	 * @param tokens
	 * @param wordToTagsSetMap
	 * @param update_doc_freq
	 */
	private void addFeaturesToFeatureList(List<Feature> featureList, String language, String field, List<String> tokens,	Map<String, Set<String>> wordToTagsSetMap, boolean update_doc_freq, boolean update_source_freq) {
		for(String token : tokens){
			Feature newFeature = new Feature(token,language,field, wordToTagsSetMap);
			
			
			int indexOfFeature = featureList.indexOf(newFeature);
			//first time seen feature - assign initial frequencies to 1
			if(indexOfFeature == -1){
				featureList.add(newFeature);
			}
			//update only attribute total frequencies
			else{
				Feature featureToUpdate = featureList.get(indexOfFeature);
				featureToUpdate.updateFrequenciesByFeature(newFeature, update_doc_freq,  update_source_freq);
			}
		}
	}
}

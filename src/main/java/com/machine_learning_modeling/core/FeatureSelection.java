package com.machine_learning_modeling.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.machine_learning_modeling.domain.Attribute;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.exception.DictionaryDuplicationException;
import com.machine_learning_modeling.exception.SmoothingException;



/**
 * 
 * @author mimis
 *
 */
public class FeatureSelection {

	
	private final int min_ngram_length;
	private final int max_ngram_length;
	private final int min_token_length; //ONLY UNIGRAMS
	private final int max_token_length; //ONLY UNIGRAMS
	private final int min_document_frequency;
	private final int min_source_frequency;
	private final boolean accept_only_nouns;
	private final List<String> accepted_fields; //ACCEPT FEATURES THAT HAVE BEEN SEEN ONLY THESE FIELDS IN OUR CORPUS, IF NOT NULL OR EMPTY
	

	public FeatureSelection(
			int min_ngram_length, int max_ngram_length, int min_token_length,
			int max_token_length, int min_document_frequency,int min_source_frequency,
			boolean accept_only_nouns, List<String> accepted_fields) {
		super();
		this.min_ngram_length = min_ngram_length;
		this.max_ngram_length = max_ngram_length;
		this.min_token_length = min_token_length;
		this.max_token_length = max_token_length;
		this.min_document_frequency = min_document_frequency;
		this.min_source_frequency = min_source_frequency;
		this.accept_only_nouns = accept_only_nouns;
		this.accepted_fields = accepted_fields != null && !accepted_fields.isEmpty() ? accepted_fields : null;
	}






	public List<Feature> parser(List<Feature> featureCandidateList, Dictionary dictionary, int top_N_features_to_return, Map<String,Double> fieldToWeigthMap) throws DictionaryDuplicationException, IOException, SmoothingException {

		List<Feature> finalFeatureList = new ArrayList<Feature>();
		
		/*
		 * Filter Features by given rules(skip_stopwords,max_ngram_length,max_token_length) without any global statistics...
		 */
		filterCandidatesBasedOnRules(featureCandidateList);

		

		/*
		* Get the corresponding features  from Dictionary
		*/
		for(Feature feature:featureCandidateList){
			finalFeatureList.add(dictionary.updateFeature(feature, false, false));
		}


		
		
		/*
		 * Filter features based on collections statistics; (Doc Freq + Part Of Speech Tagging + Field where have been seen the feature in our corpus)
		 */
		filterCandidatesBasedOnCollectionStats(finalFeatureList);
		
		

		/*
		 *Utilities Measure:  nr_of_fields_appear_in_current_doc * IDF
		 */
		for(Feature feature_in_index:finalFeatureList){
			//Get number of fields and total freq the current candidate feature appears in this document...
			Feature document_feature = featureCandidateList.get(featureCandidateList.indexOf(new Feature(feature_in_index.getValue(),feature_in_index.getLanguage())));
			//int nr_of_fields_that_appears_in_current_doc = document_feature.getFieldAttributeList().size();
			int total_freq_in_current_doc = 0;
			for(Attribute attr:document_feature.getFieldAttributeList())
				total_freq_in_current_doc += attr.getTotal_frequency();
			

			
			//if the feature in dictionary doesnt include the 
			if( feature_in_index.getIdf_doc() == 0 || feature_in_index.getIdf_source() == 0){
				throw new SmoothingException("Feature  doesnt include utility weight(idf) durring feature selection....:"+feature_in_index.getValue());
			}
			
			/*
			 * Compute feature weight
			 */
			//1.  feature_weight => nr_of_fields_that_appears_in_current_doc * IDF
//			double feature_weight  = (nr_of_fields_that_appears_in_current_doc * feature.getIdf_doc()) + (nr_of_fields_that_appears_in_current_doc * feature.getIdf_source());
			
			//2. feature_weight  =>  ((tf_idf_doc + tf_idf_source)/2)  * Σ_for_all_fields(field_weight * feature_prob_to_appear_in_field)
//			double feature_weight_based_on_probability_of_field_appearance  = compute_feature_weight_based_on_probability_of_field_appearance(feature, fieldToWeigthMap);
//			double tf_idf_doc = total_freq_in_current_doc * feature.getIdf_doc();
////			double tf_idf_source = total_freq_in_current_doc * feature.getIdf_source();
//			double feature_weight = tf_idf_doc * feature_weight_based_on_probability_of_field_appearance;
			
			
			//3. feature_weight => feature_idf_doc * Σ_for_each_field   field_weight  * feature_field_freq_current_doc * (feature_field_df_corpus / (feature_df_corpus * feature_idf_doc))
			double feature_weight = computeFeatureWeight(document_feature, feature_in_index, fieldToWeigthMap);
			feature_in_index.setWeight(feature_weight);
		}
		
		
		
		
		/**
		 * If top_N_features_to_return != -1 then return the top n based on their weight.. 
		 */
		if(top_N_features_to_return != -1 && top_N_features_to_return < finalFeatureList.size() ){
			Collections.sort(finalFeatureList, Feature.FeatureComparatorWeight);
			return finalFeatureList.subList(0, top_N_features_to_return);
		}
		return finalFeatureList;
	}
	

	
	private double computeFeatureWeight(Feature document_feature,Feature feature_in_index,Map<String,Double> fieldToWeigthMap){
		double feature_weight = 0.0;
		//for each field that have been seen this feature in the whole corpus
		for(Attribute fieldAttrInIndex:feature_in_index.getFieldAttributeList()){
			double field_weight = fieldToWeigthMap.get(fieldAttrInIndex.getName());
			
			//TODO: use the probability to find the feature in the whole corpus in order to give prefer more common terms; currently we favor very rare terms..
			double field_probability_to_appear_in_current_field_corpus = (double) fieldAttrInIndex.getDoc_frequency() / feature_in_index.getDoc_frequency();					
			//field_probability_to_appear_in_current_field_corpus *=  (double) feature_in_index.getDoc_frequency() / dictionary.
			//check if the current feature appears in the document;s field
			Attribute fieldAttrInCurrentDoc = document_feature.getFieldAttribute(fieldAttrInIndex.getName());
			
			
			
			if(fieldAttrInCurrentDoc!=null){
				feature_weight +=  field_weight * fieldAttrInCurrentDoc.getTotal_frequency() * field_probability_to_appear_in_current_field_corpus * feature_in_index.getIdf_doc();
			}
			else{
				feature_weight +=  field_weight * field_probability_to_appear_in_current_field_corpus * feature_in_index.getIdf_doc();
			}
		}
		return feature_weight;
	}
	

	


	
	private void filterCandidatesBasedOnCollectionStats(List<Feature> featureCandidateList){
		Iterator<Feature> iterator = featureCandidateList.iterator();
		while(iterator.hasNext()){
			Feature feature = iterator.next();
			
			if(
					(feature.getDoc_frequency() < this.min_document_frequency)
										||
					(feature.getSource_frequency() < this.min_source_frequency)
										||
					(this.accept_only_nouns && !feature.isNoun())
										||
					(this.accepted_fields != null && !feature.seenInFieldBefore2(this.accepted_fields))

					
				){
							iterator.remove();
			}

		}
	}

	
	
	private void filterCandidatesBasedOnRules(List<Feature> featureCandidateList){
		Iterator<Feature> iterator = featureCandidateList.iterator();
		while(iterator.hasNext()){
			Feature feature = iterator.next();
			
			if(
				    (this.max_ngram_length < feature.getNgram_length() || feature.getNgram_length() < this.min_ngram_length)
				    					||
				    (!tokensLengthIsValid(feature.getValue()))
				){
							iterator.remove();
			}

		}
	}
	
	
	private boolean tokensLengthIsValid(String ngram){
		 for(String word :ngram.split("\\s")){
			 if(this.max_token_length < word.length() || word.length() < this.min_token_length)
				 return false;
		 }
		 return true;
	}
	
	
	
	







	public int getMin_ngram_length() {
		return min_ngram_length;
	}






	public int getMax_ngram_length() {
		return max_ngram_length;
	}






	public int getMin_token_length() {
		return min_token_length;
	}






	public int getMax_token_length() {
		return max_token_length;
	}






	public int getMin_document_frequency() {
		return min_document_frequency;
	}






	public int getMin_source_frequency() {
		return min_source_frequency;
	}






	public boolean isAccept_only_nouns() {
		return accept_only_nouns;
	}






	public List<String> getAccepted_fields() {
		return accepted_fields;
	}

	
	
}

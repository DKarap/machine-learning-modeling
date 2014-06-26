package com.machine_learning_modeling.core;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;

import com.machine_learning_modeling.domain.Attribute;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.exception.DictionaryDuplicationException;
import com.machine_learning_modeling.utils.ElasticSearch;
import com.machine_learning_modeling.utils.Helper;




/**
 * @author mimis
 *
 */
public class Dictionary {
//	private final static Logger LOGGER = Logger.getLogger(Dictionary.class.getName()); 

	private Node node;	
	private Client client;
	private String clusterName; 
	private String indexName; 
	private String document_type;
	private Map<String,Feature> featureMapRAM;
	private int bulk_size;
	private long max_nr_of_features_in_memory;
	private long number_docs; //from how many docs we extracted features
	private long number_sources; //from how many sources we extracted features
	
	
	private final boolean read_only_mode;

	/**
	 * Read and Write mode
	 * @param indexName
	 * @param document_type
	 * @param max_nr_of_features_in_memory
	 * @throws SecurityException
	 * @throws IOException
	 * @throws DictionaryDuplicationException
	 */
	public Dictionary(String indexName, String document_type,long max_nr_of_features_in_memory,int bulk_size) throws SecurityException, IOException, DictionaryDuplicationException {
		super();
		this.read_only_mode = false;
        this.node = nodeBuilder()
        .client(true)
        .node();
        
      	this.client = node.client();
		this.indexName = indexName;
		this.max_nr_of_features_in_memory = max_nr_of_features_in_memory;
		this.bulk_size = bulk_size;
		this.document_type = document_type;
		this.featureMapRAM = new HashMap<String,Feature>();
		this.number_docs = getIndexStat("index_stats","total_docs");
		this.number_sources = getIndexStat("index_stats","total_sources");
	}

	
	/**
	 * Read and Write mode for a specific cluster
	 * @param indexName
	 * @param document_type
	 * @param max_nr_of_features_in_memory
	 * @throws SecurityException
	 * @throws IOException
	 * @throws DictionaryDuplicationException
	 */
	public Dictionary(String clusterName, String indexName, String document_type, long max_nr_of_features_in_memory,int bulk_size ) throws SecurityException, IOException, DictionaryDuplicationException {
		super();
		this.read_only_mode = false;
        this.node = nodeBuilder()
        .clusterName(clusterName)
        .client(true)
        .node();
        this.client = node.client();
        this.clusterName = clusterName;
        this.max_nr_of_features_in_memory = max_nr_of_features_in_memory;
        this.bulk_size = bulk_size;
		this.indexName = indexName;
		this.document_type = document_type;
		this.featureMapRAM = new HashMap<String,Feature>();
		this.number_docs = getIndexStat("index_stats","total_docs");
		this.number_sources = getIndexStat("index_stats","total_sources");
	}

	
	
	/**
	 * READ ONLY mode..make use of the ram dictionary where we keep the most N frequent freatures there...this is to speed up the GetFeature Function...
	 * @param indexName
	 * @param document_type
	 * @throws SecurityException
	 * @throws IOException
	 * @throws DictionaryDuplicationException
	 */
	public Dictionary(String indexName, String document_type, FeatureSelection featureSelection,int size_of_ram_dictionary) throws DictionaryDuplicationException  {
		super();			
		this.read_only_mode = true;
		
        this.node = nodeBuilder()
        .client(true)
        .node();
        
      	this.client = node.client();
		this.indexName = indexName;
		this.document_type = document_type;
		this.featureMapRAM = new HashMap<String,Feature>();
		this.number_docs = getIndexStat("index_stats","total_docs");
		this.number_sources = getIndexStat("index_stats","total_sources");
		
		/*
		 * Initialize memory dictionary with the n most frequent features
		 */
		createReadOnlyMemoryDictionary(featureSelection,size_of_ram_dictionary);
	}
	
	private void createReadOnlyMemoryDictionary(FeatureSelection featureSelection, int size_of_ram_dictionary){
		//Min-Max NGRAM LENGTH
		QueryBuilder qb_min_max_ngram_length = QueryBuilders
        .rangeQuery("ngram_length")
        .from(featureSelection.getMin_ngram_length())
        .to(featureSelection.getMax_ngram_length())
        .includeLower(true)
        .includeUpper(true);
		
		//Min Document frequency
		QueryBuilder qb_doc_freq = QueryBuilders
        .rangeQuery("doc_frequency")
        .from(featureSelection.getMin_document_frequency())
        .includeLower(true);
		
		//Min Source frequency
		QueryBuilder qb_source_freq = QueryBuilders
        .rangeQuery("source_frequency")
        .from(featureSelection.getMin_source_frequency())
        .includeLower(true);

		
		//construct the boolean query
		QueryBuilder qb = QueryBuilders
        .boolQuery()
        .must(qb_min_max_ngram_length)
        .must(qb_doc_freq)
        .must(qb_source_freq);

		
		//Execute query...via the SCAN api..	
		SearchResponse searchScrollResponse = client.prepareSearch(this.indexName)
		.setSearchType(SearchType.SCAN)
		.setTypes(this.document_type)
		.setQuery(qb)
		.setFrom(0)
		.setSize(100)
		.addSort("doc_frequency", SortOrder.DESC)
		.setScroll(TimeValue.timeValueMinutes(20))
		.execute()
		.actionGet();		
		
		//create memory dictionary
		System.out.print("#Start creating the readonly ram dictionary...");
		int counterFeaturesInDictionary=0;
		while (true) {
			searchScrollResponse = scroll(searchScrollResponse, 5);
		    for (SearchHit hit : searchScrollResponse.getHits()) {
				Map<String,Object> sourceMap = hit.getSource();			
				sourceMap.put("_id", hit.getId());
				Feature feature =  new Feature(sourceMap);
				this.featureMapRAM.put(feature.getValue(), feature);
				counterFeaturesInDictionary++;
		    }
		    //Break condition: No hits are returned
		    if (searchScrollResponse.getHits().getHits().length == 0 || counterFeaturesInDictionary > size_of_ram_dictionary) {
		        break;
		    }
		}
		System.out.println("[done for " + counterFeaturesInDictionary+"("+this.featureMapRAM.size()+") features]...");
	}
	
	
	
	public void eraseDictionary(){
		this.client.prepareDeleteByQuery(this.indexName).
        setQuery(QueryBuilders.matchAllQuery()).
        setTypes(this.document_type,"index_stats").  //delete both features and index_stats  types....
        execute().actionGet();
		this.number_docs = 0;
		this.number_sources = 0;
		this.featureMapRAM = new HashMap<String,Feature>();
	}
	
	
	/**
	 * 
	 * @return total number of documents the given cluster name got
	 */
	public long getNumberOfFeaturesInDictionary(){
		CountResponse response = client.prepareCount(this.indexName)
        .setQuery(QueryBuilders.matchAllQuery())
   		.setTypes(this.document_type)
        .execute()
        .actionGet();
		return response.getCount();	
	}
	
	
	public int getNumberOfFeaturesInMemory(){
		return this.featureMapRAM.size();
	}

	
	public long getNumberOfParsedSources() {
		return number_sources;
	}
	
	public long getNumberOfParsedDocuments() {
		return number_docs;
	}

	public void increaseNumberOfParsedDocuments(long doc_freq){
		this.number_docs += doc_freq;
	}

	public void increaseNumberOfParsedSources(long source_freq){
		this.number_sources += source_freq;
	}

	

	public String getClusterName() {
		return clusterName;
	}




	/**
	 * close Dictionary at the end
	 * @throws IOException 
	 * @throws DictionaryDuplicationException 
	 */
	public void closeDictionary() throws DictionaryDuplicationException, IOException{
	    /**
	     * If we are on write mode then flush last features in memory into index if exist!! 
	     */
	    if(!this.read_only_mode && getNumberOfFeaturesInMemory() != 0){
			flushMemoryFeaturesIntoIndex();
	    }

	    
		this.client.close();
		this.node.close();
	}
	
	
		
	
	private XContentBuilder createJsonFromFeature(Feature feature) throws IOException{
		XContentBuilder builder = jsonBuilder()
			    .startObject()
			        .field("word", feature.getValue())
   			        .field("language", feature.getLanguage())
			        .field("ngram_length", feature.getNgram_length())
			        .field("name_entity", feature.getName_entity())
   			        .field("doc_frequency", feature.getDoc_frequency())
   			        .field("source_frequency", feature.getSource_frequency())
			        .startArray("field");
			        
						appendAttributeObject(builder, feature.getFieldAttributeList());
				
					builder.endArray()
					
					.startArray("part_of_speech");

					appendAttributeObject(builder, feature.getPart_of_speechAttributeList());
					
					builder.endArray()
			    .endObject();
		return builder;
			        
	}

	private void appendAttributeObject(XContentBuilder builder, List<Attribute> attributeList) throws IOException{
		
		for(Attribute attribute : attributeList){
			builder.startObject()
				.field("name", attribute.getName())
				.field("doc_frequency", attribute.getDoc_frequency())
				.field("total_frequency", attribute.getTotal_frequency())
			.endObject();
		}
	}

	

	public void addFeature(Feature featureToAdd, boolean update_doc_freq, boolean update_source_freq) throws DictionaryDuplicationException, IOException{

    	if(getNumberOfFeaturesInMemory() > this.max_nr_of_features_in_memory){
    		flushFeaturesInMemoryIntoDiskVersion(update_doc_freq,update_source_freq,false,bulk_size);
    	}
    	else{
    		addFeatureRAM(featureToAdd, update_doc_freq,update_source_freq);	
    	}
	}

	public void flushMemoryFeaturesIntoIndex() throws DictionaryDuplicationException, IOException{
		flushFeaturesInMemoryIntoDiskVersion(true,true,true,bulk_size);
	}

	private void addFeatureRAM(Feature featureToAdd, boolean update_doc_freq, boolean update_source_freq) throws DictionaryDuplicationException, IOException{

		Feature featureInDictionary = this.featureMapRAM.get(featureToAdd.getValue());
		
		//insert feature
		if(featureInDictionary == null){
			this.featureMapRAM.put(featureToAdd.getValue(),featureToAdd);
		}
		//update frequencies of feature
		else{
			featureInDictionary.updateFrequenciesByFeature(featureToAdd, update_doc_freq, update_source_freq); //update Nested fields frequencies(feild, part_of_speech)
		}
	}
	
	

	/**
	 * Get from Index a given feature by its feature value..This is the most time consuming process..
	 * @param fieldToSearchName
	 * @param fieldToSearchNameValue
	 * @return
	 * @throws DictionaryDuplicationException
	 */
	public Feature getFeature(String fieldToSearchName,String fieldToSearchNameValue) throws DictionaryDuplicationException{
		//check if exist in memory dict
		if(this.read_only_mode){
			if(this.featureMapRAM.containsKey(fieldToSearchNameValue)){
				return this.featureMapRAM.get(fieldToSearchNameValue);
			}
		}
		
		//else retrieve it form disk.. 
		SearchResponse searchResponse = this.client.prepareSearch(this.indexName)
				.setTypes(this.document_type)
				.setQuery(QueryBuilders.matchAllQuery())
				.setPostFilter(FilterBuilders.termFilter(fieldToSearchName, fieldToSearchNameValue))
				.execute()
				.actionGet();
		
		SearchHits hits = searchResponse.getHits();
		
		//throw exception if we found more than one features
		if(hits.getTotalHits() > 1){
			throw new DictionaryDuplicationException("WE FOUND MORE THAN ONE FEATURES:"+searchResponse.toString());
		}
		else if(hits.getTotalHits() == 1){
			SearchHit hit = hits.getHits()[0];
			Map<String,Object> sourceMap = hit.getSource();
			sourceMap.put("_id", hit.getId());
			return new Feature(sourceMap);
		}
		else{
			return null;
		}
	}

	
	
	public long getIndexStat(String document_type, String statistic_name) throws DictionaryDuplicationException{
		SearchResponse searchResponse = this.client.prepareSearch(this.indexName)
				.setTypes(document_type)
				.setQuery(QueryBuilders.matchAllQuery())
				.execute()
				.actionGet();
		
		SearchHits hits = searchResponse.getHits();
		
		//throw exception if we found more than one features
		if(hits.getTotalHits() > 1){
			throw new DictionaryDuplicationException("WE FOUND MORE THAN ONE index stat  :"+searchResponse.toString());
		}
		else if(hits.getTotalHits() == 1){
			SearchHit hit = hits.getHits()[0];
			return Long.parseLong(hit.getSource().get(statistic_name).toString());
		}
		else{
			return 0;
		}
	}
	
	
	public void setIndexStats() throws DictionaryDuplicationException, IOException{
		XContentBuilder builder = jsonBuilder().startObject()
									.field("total_docs", this.number_docs)
									.field("total_sources", this.number_sources)
									.endObject();
		//Get the id of index stats doc;there is only one.. 
		SearchResponse searchResponse = this.client.prepareSearch(this.indexName)
		.setTypes("index_stats")
		.setQuery(QueryBuilders.matchAllQuery())
		.execute()
		.actionGet();

		SearchHits hits = searchResponse.getHits();
		

		//throw exception if we found more than one features
		if(hits.getTotalHits() > 1){
			throw new DictionaryDuplicationException("WE FOUND MORE THAN ONE index_stats :"+searchResponse.toString());
		}
		//update counter 
		else if(hits.getTotalHits() == 1){
			SearchHit hit = hits.getHits()[0];
			ElasticSearch.indexDoc(this.client, this.indexName, "index_stats", hit.getId(), builder);
		}
		//insert counter
		else{
			ElasticSearch.indexDoc(this.client, this.indexName, "index_stats", null, builder);
		}
	}
	

	
	public void refreshIndex(){
		client.admin().indices().refresh(new RefreshRequest(this.indexName)).actionGet();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	

	
	
	
	
	
	
	
	
	
	
	/**
	 * FIND THE GIVEN FEATURE IN THE INDEX, UPDATE IT AND RETURN IT
	 * @param featureToAdd
	 * @param update_doc_freq
	 * @return
	 * @throws DictionaryDuplicationException
	 * @throws IOException
	 */
	public Feature updateFeature(Feature featureToAdd,  boolean update_doc_freq, boolean update_source_freq) throws DictionaryDuplicationException, IOException{
		//search dictionary for the current feature via the feature's value
		Feature featureInDictionary = getFeature("word",featureToAdd.getValue());
		
		if(featureInDictionary == null){
			return featureToAdd;
		}
		//update frequencies of feature
		else{
			featureInDictionary.updateFrequenciesByFeature(featureToAdd, update_doc_freq, update_source_freq); //update Nested fields frequencies(feild, part_of_speech)
			return featureInDictionary;
		}
	}

	
		
	private void bulkIndexing(List<Feature> featuresToInstertList) throws IOException{
		BulkRequestBuilder bulkRequest = client.prepareBulk();

		
		for(Feature featureToInsert:featuresToInstertList){
			XContentBuilder builder = createJsonFromFeature(featureToInsert);
			
			if(featureToInsert.get_id() != null)
				bulkRequest.add(client.prepareIndex(this.indexName,this.document_type,featureToInsert.get_id()).setSource(builder).request());
			else
				bulkRequest.add(client.prepareIndex(this.indexName,this.document_type).setSource(builder).request());
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
		    System.out.println("#Bulk Indexing got failures....");
		}
	}
	
	
	
	/***
	 * ############################   Version 2.2: GET ALL DOC'S FEATURES IN MEMORY AND UPDATE THEM using bulk_searching and THEN SAVE ALL IN DISK USING BULK_INDEXING
	 * 
	 * 				#PROBLEM WITH BULKSEARCHING: RESPONCE IS SOMETIMES NULL
	 */

	
	/**
	 * First Update the features by retrieving their stats in ES index - make use of BulkIndexing and BulkSearching
	 * Second insert the updated features
	 * Last refresh ES index in order to get the changes
	 *  
	 * @param update_doc_freq
	 * @throws DictionaryDuplicationException
	 * @throws IOException
	 */
	private void flushFeaturesInMemoryIntoDiskVersion( boolean update_doc_freq, boolean update_source_freq, boolean flushAll,int bulk_size) throws DictionaryDuplicationException, IOException{
        
        /*
         * Update number of DOCUMENTS and SOURCES that we parsed 
         */
		setIndexStats();
		
		
        /*
		 * update and save the memory features into the ES index the BOTTOM HALF frequent features (based on doc freq)
		 */
		List<Feature> featureListRAM = new ArrayList<Feature>(this.featureMapRAM.values());
        Collections.sort(featureListRAM, Feature.FeatureComparatorDocFreq);
        
        //get the half LAST features(the most less frequent) and flush them every N features into the ES ndex
        int indexToSplitDictionary = 0;
        if(!flushAll)
        	indexToSplitDictionary = featureListRAM.size() / 2;
        List<Feature> featureListToFlush = featureListRAM.subList(indexToSplitDictionary, featureListRAM.size());
        for(int i=0;i<featureListToFlush.size();i+=bulk_size){
        	int last_element_index = i+bulk_size < featureListToFlush.size() ? i+bulk_size : featureListToFlush.size();
            flushFeaturesIntoESindex(featureListToFlush.subList(i, last_element_index),update_doc_freq, update_source_freq);
        }
		
		
		/*
		 * refresh ES index
		 */		
		refreshIndex();
        this.featureMapRAM = new HashMap<String,Feature>();
        for(int i=0;i<indexToSplitDictionary;i++){
        	Feature featureToAdd = featureListRAM.get(i);
        	this.featureMapRAM.put(featureToAdd.getValue(), featureToAdd);
        }
    }

	
	
	private void flushFeaturesIntoESindex(List<Feature> featureToFlushList, boolean update_doc_freq,boolean update_source_freq) throws DictionaryDuplicationException, IOException{
		/*
		 * update memory features 
		 */
//				long start = System.currentTimeMillis();
		MultiSearchResponse multiResponce = bulkSearching(featureToFlushList);
//		LOGGER.info("[bulkSearching "+ featureToFlushList.size() +" features took:"+Helper.measureExecutionTime(start)+" secs]");

//		start = System.currentTimeMillis();
		List<Feature> featuresToInstertList = updateFeatures(multiResponce, featureToFlushList, update_doc_freq, update_source_freq);
//		LOGGER.info("[update "+ featureToFlushList.size() +" features took:"+Helper.measureExecutionTime(start)+" secs]");


		/*
		 * Insert them into ES index
		 */
//		start = System.currentTimeMillis();
		bulkIndexing(featuresToInstertList);
//		System.out.println("[insert "+ featureToFlushList.size() +" features took:"+Helper.measureExecutionTime(start)+" ms]");

	}
	
	
	
	private MultiSearchResponse bulkSearching(List<Feature> featuresToSearchList){
		//add search requests
		MultiSearchRequestBuilder builder = this.client.prepareMultiSearch();
		for(Feature feature:featuresToSearchList){
			SearchRequestBuilder sr = this.client.prepareSearch(this.indexName)
			.setTypes(this.document_type)
			.setQuery(QueryBuilders.matchAllQuery())
			.setPostFilter(FilterBuilders.termFilter("word", feature.getValue()));
			builder.add(sr);
		}
		//execute multi searchs	
		MultiSearchResponse multiResponce =  builder.execute().actionGet();
		return multiResponce;
	}
	
	
	
	private List<Feature> updateFeatures(MultiSearchResponse multiResponce, List<Feature> featuresToUpdateList, boolean update_doc_freq, boolean update_source_freq) throws DictionaryDuplicationException{
		List<Feature> updatedFeaturesList = new ArrayList<Feature>();

		Item[] multiResponceItemArr = multiResponce.getResponses();
		for (int i=0;i<multiResponceItemArr.length;i++) {
			Item item = multiResponceItemArr[i];
			
			if(item.isFailure()){
				System.out.println("#Fail:"+item.getFailureMessage());
				continue;
			}

			SearchResponse response = item.getResponse();
			SearchHits hits = response.getHits();
			long totalHits = hits.getTotalHits();
			
			//throw exception if we found more than one features
			if(totalHits > 1){
				throw new DictionaryDuplicationException("WE FOUND MORE THAN ONE FEATURES in DICTIONARY:"+featuresToUpdateList.get(i).getValue()+"\t"+response.toString());
			}
			//update feature
			else if(totalHits == 1){
				SearchHit hit = hits.getHits()[0];
				Map<String,Object> sourceMap = hit.getSource();
				sourceMap.put("_id", hit.getId());
				Feature featureInDictionary = new Feature(sourceMap);
				
				featureInDictionary.updateFrequenciesByFeature(featuresToUpdateList.get(i), update_doc_freq, update_source_freq); //update Nested fields frequencies(feild, part_of_speech)
				updatedFeaturesList.add(featureInDictionary);
			}
			//add feature as it is and increaae doc freq
			else if(totalHits == 0){
				updatedFeaturesList.add(featuresToUpdateList.get(i));
			}

		}
		return updatedFeaturesList;
	}
	
	
	
	
	/**
	 * Iterate all the feature sin the index and compute their IDF values..
	 * @throws Exception 
	 * @throws ElasticsearchException 
	 * @throws IOException 
	 * @throws DictionaryDuplicationException 
	 */
	public void computeUtilityMeasures() throws ElasticsearchException, IOException, DictionaryDuplicationException{
		
	    /**
	     * If we are on write mode then flush last features in memory into index if exist!! 
	     */
	    if(!this.read_only_mode && getNumberOfFeaturesInMemory() != 0){
			flushMemoryFeaturesIntoIndex();
	    }

		
		
		
		SearchResponse scrollResp = getAllFeatures(120);
		
		//Scroll until no hits are returned
		while (true) {
		    scrollResp = scroll(scrollResp, 5);
		    for (SearchHit hit : scrollResp.getHits()) {
				Map<String,Object> sourceMap = hit.getSource();
				sourceMap.put("_id", hit.getId());
				Feature featureInDictionary = new Feature(sourceMap);
				featureInDictionary.setIdf_doc(computeIDF(this.number_docs, featureInDictionary.getDoc_frequency()));
				featureInDictionary.setIdf_source(computeIDF(this.number_sources, featureInDictionary.getSource_frequency()));
				
				XContentBuilder builder = jsonBuilder().startObject()
				.field("idf_doc", featureInDictionary.getIdf_doc())
				.field("idf_source", featureInDictionary.getIdf_source())
				.endObject();

				ElasticSearch.updateDoc(this.client, this.indexName, this.document_type, hit.getId(), builder);
		    }
		    //Break condition: No hits are returned
		    if (scrollResp.getHits().getHits().length == 0) {
		        break;
		    }
		}
		refreshIndex();
	}
	
	private SearchResponse getAllFeatures(int scroll_time_in_minute){
		SearchResponse searchResponse = client.prepareSearch(this.indexName)
			.setSearchType(SearchType.SCAN)
			.setTypes(this.document_type)
	        .setQuery(QueryBuilders.matchAllQuery())
	        .setSize(100)
	        .setScroll(TimeValue.timeValueMinutes(scroll_time_in_minute))
	        .execute().actionGet();
 
		return searchResponse;
	}

	private SearchResponse scroll(SearchResponse searchResponse,int scroll_time_in_minute){
		searchResponse = client.prepareSearchScroll(searchResponse.getScrollId())
        .setScroll(TimeValue.timeValueMinutes(scroll_time_in_minute))
        .execute().actionGet();
		return searchResponse;
	}
	
	
	private double computeIDF(long N, long frequency){
		return Helper.log2((double)N/frequency);
	}
	
}



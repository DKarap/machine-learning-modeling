package com.machine_learning_modeling.utils;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class ElasticSearch {

	public static void updateDoc(Client client, String collection,String type, String doc_id, XContentBuilder builder) throws ElasticsearchException{
		client.prepareUpdate(collection,type,doc_id)
	      .setDoc(builder)
	      .execute()
	      .actionGet();
	}

	public static void indexDoc(Client client, String collection,String type, String doc_id, XContentBuilder builder){
		if(doc_id != null){
			client.prepareIndex(collection,type,doc_id)
		      .setSource(builder)
		      .execute()
		      .actionGet();
		}else{
			client.prepareIndex(collection,type)
		      .setSource(builder)
		      .execute()
		      .actionGet();
		}
	}

	public static void indexDoc(Client client, String collection,String type,String doc_id, String builder){
		if(doc_id != null){
			client.prepareIndex(collection,type,doc_id)
		      .setSource(builder)
		      .execute()
		      .actionGet();
		}else{
			client.prepareIndex(collection,type)
		      .setSource(builder)
		      .execute()
		      .actionGet();
		}
	}
	
	public static void eraseIndex(Client client,String collection,String type ){
		client.prepareDeleteByQuery(collection)
		.setQuery(QueryBuilders.matchAllQuery()).setTypes(type)
		.execute().actionGet();
	}

}

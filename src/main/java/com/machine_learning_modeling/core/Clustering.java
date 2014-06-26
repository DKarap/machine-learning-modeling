package com.machine_learning_modeling.core;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.domain.clustering.Cluster;
import com.machine_learning_modeling.domain.clustering.DocumentsPairSimilarity;
import com.machine_learning_modeling.utils.Helper;

public class Clustering {

	private double similarity_threshold;
	private double merging_threshold;
	private String similarity_matrix_file;
	private boolean use_old_similarity_matrix;
	private Map<String,Double> old_similarity_matrix_map;
	private List<Document> document_list = new ArrayList<Document>();
	private List<DocumentsPairSimilarity> documentsPairSimilaritiesList = new ArrayList<DocumentsPairSimilarity>();
	private int number_of_clusters;
	private int KNN;//k for the KNN algo..
	
	private int top_features_to_select_for_cluster_similarity;
	private double cluster_similarity_threshold;
	private double knn_threshold;
	
	
	
	public Clustering(double similarity_threshold, double merging_threshold,
			String similarity_matrix_file, Boolean use_old_similarity_matrix,int number_of_clusters,int KNN,int top_features_to_select_for_cluster_similarity,double cluster_similarity_threshold, double knn_threshold) {
		super();
		this.similarity_threshold = similarity_threshold;
		this.merging_threshold = merging_threshold;
		this.similarity_matrix_file = similarity_matrix_file;
		this.use_old_similarity_matrix = use_old_similarity_matrix;
		this.number_of_clusters = number_of_clusters;
		this.KNN = KNN;
		this.top_features_to_select_for_cluster_similarity = top_features_to_select_for_cluster_similarity;
		this.cluster_similarity_threshold = cluster_similarity_threshold;
		this.knn_threshold = knn_threshold;
	}




	public Set<Cluster> clustering(List<Integer> job_id_list, Connection connection, String database_table, List<String> fieldsToSelectList, String language) throws IOException, SQLException{
    	/**
    	 * ####1. Get previous Similarity Matrix in order to skip computing the similarities two times...
    	 * Matrix is ordered by doc_id_1... 
    	 * format: "doc_id_1,doc_id_2	\t	similarity"
    	 */
		System.out.println("Load Similarity Matrix from disk..");
    	this.old_similarity_matrix_map = getSimilarityMatrixFromFile(similarity_matrix_file, use_old_similarity_matrix);
    	
    	
    	/**
    	 * ####2. Create Similarity Matrix
    	 */
    	System.out.println("Create Similarity Matrix..");
    	createSimilarityMatrixFromMySQL(job_id_list, connection, database_table, fieldsToSelectList, language);


    	/**
    	 * ####3. Clustering Step - 1 => Initial Soft Clusters utilizing  similarity threshold - (one doc can belong in this face in more than one cluster) 
    	 * 		For each document get the most similar documents with similarity greater than a given threshold..and assign them to the same cluster..
    	 * 		if none document is similar then assign the current job to a singleton cluster..
    	 */
    	System.out.println("Initial Soft Clustering..");
    	Set<Cluster> clusterSet = initialSoftClustering(job_id_list);
    	
    	

    	/**
    	 * ####4. Clustering Step - 2 => Merge clusters by measuring their Intersection against their total number of items - 
    	 * 									this is mainly to 
    	 * 		Merging_measure =>  (Intersection / TOTAL_ITEMS_Cluster_1)  +  (Intersection / TOTAL_ITEMS_Cluster_2)
    	 */
    	System.out.println("Merge Clusters That got lot common job titles..");
    	while(true){
    		int initial_nr_of_clusters = clusterSet.size();
    		clusterSet = mergeClustersBasedOnItemIntersection(clusterSet);    		
    		if(clusterSet.size() == initial_nr_of_clusters){
    			break;
    		}
    	}
    	
    	
    	
    	/**
    	 * ####5. Clustering Step - 3 => create the final N clusters based on KNN
    	 */
    	System.out.println("KNN Clustering..");
    	clusterSet = knnClustering(clusterSet);
    	
    	
    	
    	/**
    	 * ####5. Clustering Step - 3 => Merge Singleton Clusters(Others label)
    	 */
//    	System.out.println("Merge Singleton Clusters..(OTHERS)");
//    	clusterSet = mergeSingletonClusters(clusterSet);
    	
    	
    	
    	
    	
    	
    	/**
    	 * ####6. Save Similarity Matrix into disk
    	 */
    	System.out.println("Save Similarity Matrix("+documentsPairSimilaritiesList.size()+" size) to disk..");
    	saveSimilarityMatrixToDisk(similarity_matrix_file, documentsPairSimilaritiesList);
    	
    	
    	
    	return clusterSet;
	}
	
	private Set<Cluster> knnClustering(Set<Cluster> clusterSet){
		Set<Cluster> finalClustersSet = new HashSet<Cluster>();
		
		//1. sort clusters by size
		List<Cluster> clusterList = new ArrayList<Cluster>(clusterSet);
		Collections.sort(clusterList, Cluster.ClusterComparatorSize);

		//2. initialization by assign the first cluster as SEED cluster
		Cluster initialSeedCluster = clusterList.get(0);
		computeClusterFeatureVector(initialSeedCluster);
		finalClustersSet.add(initialSeedCluster);
		int clusterCounters = 1;
		
		//3. choose the rest SEEDS clusters
		for(int i=1;i<clusterList.size();i++){
			Cluster currentSeedCandidateCluster = clusterList.get(i);
			computeClusterFeatureVector(currentSeedCandidateCluster);
			
			//4. Is current cluster suitable for SEED cluster? Compare it with the previous selected SEED clusters
			//   and if is not similar add it as SEED cluster
			if(isSuitableForSeedCluster(currentSeedCandidateCluster,finalClustersSet)){
				finalClustersSet.add(currentSeedCandidateCluster);
				if(++clusterCounters == this.number_of_clusters)
					break;
			}else{
				List<Feature> seedClusterFeatureVector = currentSeedCandidateCluster.getFeature_vector().subList(0, currentSeedCandidateCluster.getFeature_vector().size() > top_features_to_select_for_cluster_similarity ? top_features_to_select_for_cluster_similarity : currentSeedCandidateCluster.getFeature_vector().size());
				System.out.println("Cluster is not suitable for seed:"+seedClusterFeatureVector.toString());
			}
		}
		
		
		//5. KNN
		Cluster other_cluster = null;
		
		while(true){
			int initial_seeds_docs_size = getTotalNumberOfDocumentsInClusterList(finalClustersSet);
			//5.1 get all the docs that are not assigned in a cluster yet
			Set<Integer> docIdsNotAssignedToAclusterYet = getDocumentIdsNotAssignToAclusterYet(finalClustersSet, clusterSet);
			//5.2 for each doc get the K most similar docs from the similarity matrix
			for(Integer docIdToGroup:docIdsNotAssignedToAclusterYet){
	    		List<DocumentsPairSimilarity> currentDocpairSimilaritiesList = getDocumentPairSimilaritiesById(documentsPairSimilaritiesList, docIdToGroup, knn_threshold);
	    		Collections.sort(currentDocpairSimilaritiesList, DocumentsPairSimilarity.DocumentSimilarityComparator);
	    		currentDocpairSimilaritiesList = currentDocpairSimilaritiesList.subList(0, currentDocpairSimilaritiesList.size() > this.KNN ? this.KNN : currentDocpairSimilaritiesList.size());
	    		//5.3 get cluster id where the majority KNN docs exist in //NO that=>or -1 if the majority doesnt exist in a specific cluster
	    		Set<Integer> knnDocIdsSet = getKNNdocsIdsFromDocPairSim(currentDocpairSimilaritiesList, docIdToGroup);
	    		int knn = getKNNclusterId(knnDocIdsSet, finalClustersSet);
	    		//5.4 assign doc to the nearest cluster from 5.3
	    		if(knn != -1){
	    			for(Cluster c:finalClustersSet){
	    				if(c.getId() == knn){
	    					c.addDoc_id_to_cluster(docIdToGroup);
	    					break;
	    				}
	    			}
	    		}
//	    		else{
//	    			if(other_cluster == null){
//	    				other_cluster = new Cluster(docIdToGroup);
//	    				other_cluster.setLabel("OTHER");    				
//	    			}else
//	    				other_cluster.addDoc_id_to_cluster(docIdToGroup);
//	    		}
			}
			
			System.out.println("initial_seeds_docs_size:"+initial_seeds_docs_size + " \t after"+getTotalNumberOfDocumentsInClusterList(finalClustersSet));
			if(initial_seeds_docs_size == getTotalNumberOfDocumentsInClusterList(finalClustersSet))
				break;			
		}
		
		System.out.println("Create Other cluster...");
		List<Integer> docIdsNotAssignedToAclusterYet = new ArrayList<Integer>(getDocumentIdsNotAssignToAclusterYet(finalClustersSet, clusterSet));
		if(!docIdsNotAssignedToAclusterYet.isEmpty()){
			other_cluster = new Cluster(docIdsNotAssignedToAclusterYet.remove(0));
			other_cluster.setLabel("OTHER");
			other_cluster.addDocIdsToCluster(docIdsNotAssignedToAclusterYet);
			finalClustersSet.add(other_cluster);
		}
		
		
		//6. compute again their feature vectors
		for(Cluster c:finalClustersSet)
			computeClusterFeatureVector(c);
		
		return finalClustersSet;
	}
	
	
	private int getKNNclusterId(Set<Integer> knnDocIdsSet, Set<Cluster> seedClustersSet){
		int knn = -1;
		Map<Integer,Integer> clusterIdToFreq = new HashMap<Integer,Integer>(); 
		for(Integer docId : knnDocIdsSet){
			boolean currentDocIdExistInSeedCluster = false;
			for(Cluster c:seedClustersSet){
				if(c.getDoc_ids_set().contains(docId)){
					Integer freq = clusterIdToFreq.get(c.getId());
					if(freq!=null)
						clusterIdToFreq.put(c.getId(), freq+1);
					else
						clusterIdToFreq.put(c.getId(), 1);
					currentDocIdExistInSeedCluster = true;
				}
			}
			//OTHER CLUSTER - DONT KEEP FREQ ABOUT THE OTHER CLUSTER
//			if(!currentDocIdExistInSeedCluster){
//				Integer freq = clusterIdToFreq.get(-1);
//				if(freq!=null)
//					clusterIdToFreq.put(-1, freq+1);
//				else
//					clusterIdToFreq.put(-1, 1);
//			}
		}		
		clusterIdToFreq = Helper.sortByComparator(clusterIdToFreq);
		for(Map.Entry<Integer,Integer> entry:clusterIdToFreq.entrySet()){
			knn = entry.getKey();
			break;
		}
		return knn;
	}
	
	
	private Set<Integer> getKNNdocsIdsFromDocPairSim(List<DocumentsPairSimilarity> currentDocpairSimilaritiesList, Integer docId){
		Set<Integer> knnDocIdsSet = new HashSet<Integer>();
		for(DocumentsPairSimilarity pairSim : currentDocpairSimilaritiesList){
			knnDocIdsSet.add(pairSim.getDoc_id_1());
			knnDocIdsSet.add(pairSim.getDoc_id_2());
		}
		knnDocIdsSet.remove(docId);
		return knnDocIdsSet;
	}

	
	private Set<Integer> getDocumentIdsNotAssignToAclusterYet(Set<Cluster> seedClustersSet, Set<Cluster> allClusterSet){
//		Set<Integer> docIdsNotAssignedToAclusterYet = new HashSet<Integer>();
//		for(Cluster cluster : allClusterSet){
//			if(!seedClustersSet.contains(cluster)){
//				docIdsNotAssignedToAclusterYet.addAll(cluster.getDoc_ids_set());
//			}
//		}
//		System.out.println("docIdsNotAssignedToAclusterYet:"+docIdsNotAssignedToAclusterYet.size()+"\tSeeds:"+getTotalNumberOfDocumentsInClusterList(seedClustersSet));
//
//		return docIdsNotAssignedToAclusterYet;
		
		
		Set<Integer> seedDocIdsSet = new HashSet<Integer>();
		for(Cluster cluster : seedClustersSet)
			seedDocIdsSet.addAll(cluster.getDoc_ids_set());
		Set<Integer> allDocIdsSet = new HashSet<Integer>();
		for(Cluster cluster : allClusterSet)
			allDocIdsSet.addAll(cluster.getDoc_ids_set());
		
		allDocIdsSet.removeAll(seedDocIdsSet);
		return allDocIdsSet;
	}
	
	/**
	 * test if the current seed candidate cluster is too similar with an already selected SEED cluster
	 * @param seedCandidateCluster
	 * @param alreadySelectedSeedClusters
	 * @return
	 */
	private boolean isSuitableForSeedCluster(Cluster seedCandidateCluster, Set<Cluster> alreadySelectedSeedClusters){
		
		List<Feature> candidateSeedClusterFeatureVector = seedCandidateCluster.getFeature_vector();
		Collections.sort(candidateSeedClusterFeatureVector, Feature.FeatureComparatorDocFreq);
		candidateSeedClusterFeatureVector = candidateSeedClusterFeatureVector.subList(0, candidateSeedClusterFeatureVector.size() > top_features_to_select_for_cluster_similarity ? top_features_to_select_for_cluster_similarity : candidateSeedClusterFeatureVector.size());

		for(Cluster seed_cluster:alreadySelectedSeedClusters){
			List<Feature> seedClusterFeatureVector = seed_cluster.getFeature_vector();
			Collections.sort(seedClusterFeatureVector, Feature.FeatureComparatorDocFreq);
			seedClusterFeatureVector = seedClusterFeatureVector.subList(0, seedClusterFeatureVector.size() > top_features_to_select_for_cluster_similarity ? top_features_to_select_for_cluster_similarity : seedClusterFeatureVector.size());
			List<Feature> seedClusterFeatureVectorCopy = new ArrayList<Feature>(seedClusterFeatureVector);
			//compare feature vectors.
			seedClusterFeatureVectorCopy.retainAll(candidateSeedClusterFeatureVector);
			double similarity = (double) seedClusterFeatureVectorCopy.size() / candidateSeedClusterFeatureVector.size();

			//check if is too common with an already selected cluster
			if(similarity > this.cluster_similarity_threshold){
				return false;
			}	
		}
		return true;
	}
	
	public void computeClusterFeatureVector(Cluster cluster){
		List<Feature> clusterFeatureList = new ArrayList<Feature>();
		for(int id : cluster.getDoc_ids_set()){
			Document doc1 = getDocumentFromListById(document_list, id);
			Map<String,Double> doc_vec_map_1 = Helper.getMapFromDocumentVector(doc1.getFieldValueByKey("document_vector"));
			for(Map.Entry<String,Double> entry : doc_vec_map_1.entrySet()){
				Feature newFeature = new Feature(entry.getKey(), doc1.getLanguage());
				int indexOfFeature = clusterFeatureList.indexOf(newFeature);
				if(indexOfFeature != -1){
					Feature featureToUpdate = clusterFeatureList.get(indexOfFeature);
					featureToUpdate.increaseGeneralDocFreq();
				}
				else{
					newFeature.setWeight(entry.getValue());
					clusterFeatureList.add(newFeature);
				}
			}	
		}
		cluster.setFeature_vector(clusterFeatureList);
	}
	
	public  Document getDocumentFromListById(List<Document> document_list, int doc_id){
		return document_list.get(document_list.indexOf(new Document(doc_id, null, null)));
	}

	
	/**
	 * #### Clustering Step - 3 => Merge Singleton Clusters(Others label)
	 */
	private Set<Cluster> mergeSingletonClusters(Set<Cluster> clusterSet){
		Set<Cluster> finalClustersSet = new HashSet<Cluster>();
		Cluster merged_singletons_cluster = null;
		for(Cluster cluster:clusterSet){
			//current cluster is singleton...
			if(cluster.getNumberOfDocsInCluster() == 1){
				if(merged_singletons_cluster == null){
					merged_singletons_cluster = new Cluster(cluster.getId());
					merged_singletons_cluster.setLabel("OTHER");
				}
				else
					merged_singletons_cluster.addDoc_id_to_cluster(cluster.getId());
			}
			//current cluster is non singleton...just add it to the finalClusterList 
			else
				finalClustersSet.add(cluster);
		}
		finalClustersSet.add(merged_singletons_cluster);
		return finalClustersSet;
	}

	
	
	/**
	 * #### Clustering Step - 2 => Merge clusters by measuring their Intersection against their total number of items
	 * 		Merging_measure =>  (Intersection / TOTAL_ITEMS_Cluster_1)  +  (Intersection / TOTAL_ITEMS_Cluster_2)
	 * Algorithm:  1. Initial starter Cluster / Start with with the bigger clusters first
	 * 			   2. Get set of clusters that are not merged yet with other cluster and got intersection higher than given threshold with the initial cluster
	 * 			   3. Merge the clusters from 2. and add them to the set with the clusters
	 * 			   4. got to step 1.        
	 */
	private Set<Cluster>  mergeClustersBasedOnItemIntersection(Set<Cluster> clusterSet){
		Set<Cluster> mergedClustersSet = new HashSet<Cluster>();
		//sort clusters by number of items that include
		List<Cluster> clusterList = new ArrayList<Cluster>(clusterSet);
		Collections.sort(clusterList,Cluster.ClusterComparatorSize);
		//keep track which clusters are merged with others
		Set<Integer> alreadyMergedClusterIdsSet = new HashSet<Integer>();
		for(int i=0; i< clusterList.size(); i++){
			Cluster cluster_1 = clusterList.get(i);
			List<Integer> cluster_ids_to_be_merged_list = new ArrayList<Integer>();
			if(alreadyMergedClusterIdsSet.contains(cluster_1.getId()))
				continue;

			for(int y=i+1; y< clusterList.size(); y++){
				Cluster cluster_2 = clusterList.get(y);
				//if current cluster is not already merged then check if should be merged with the current initial cluster
				if(!alreadyMergedClusterIdsSet.contains(cluster_2.getId())){
	    			double mergingMeasure = computeMergingMeasure(cluster_1, cluster_2);
	    			if(mergingMeasure >= merging_threshold ){
	    	    		cluster_ids_to_be_merged_list.add(cluster_2.getId());
	    	    		alreadyMergedClusterIdsSet.add(cluster_2.getId());
	    			}	
				}
			}
			cluster_1.addDocIdsToCluster(cluster_ids_to_be_merged_list);
			mergedClustersSet.add(cluster_1);
		}
		return mergedClustersSet;
	}
	
	
	/**
	 * #### Clustering Step - 1 => Initial Soft Clusters utilizing  similarity threshold - (one doc can belong in this face in more than one cluster) 
	 * 		For each document get the most similar documents with similarity greater than a given threshold..and assign them to the same cluster..
	 * 		if none document is similar then assign the current job to a singleton cluster..
	 */
	private Set<Cluster> initialSoftClustering(List<Integer> job_id_list){
    	Set<Cluster> clusterSet = new HashSet<Cluster>();
    	for(int i=0; i< job_id_list.size(); i++){
    		int doc_id_1 = job_id_list.get(i);//this is the seed doc id..that we built upon this cluster
    		Cluster currentCluster = new Cluster(doc_id_1);
    		//get the most similar docs to current document
    		List<DocumentsPairSimilarity> currentDocpairSimilaritiesList = getDocumentPairSimilaritiesById(documentsPairSimilaritiesList, doc_id_1,similarity_threshold);
    		if(!currentDocpairSimilaritiesList.isEmpty()){
        		for(DocumentsPairSimilarity pairSim : currentDocpairSimilaritiesList){
        			currentCluster.addDoc_id_to_cluster(pairSim.getDoc_id_1());
        			currentCluster.addDoc_id_to_cluster(pairSim.getDoc_id_2());
        		}
    		}
    		clusterSet.add(currentCluster);
    	}
    	return clusterSet;
	}	
	
	/**
	 * TODO needs speed improvement..
	 * @param allDocumentsPairSimilaritiesList
	 * @param id
	 * @param threshold
	 * @return
	 */
	private List<DocumentsPairSimilarity> getDocumentPairSimilaritiesById(List<DocumentsPairSimilarity> allDocumentsPairSimilaritiesList, int id, double threshold){
		List<DocumentsPairSimilarity> documentPairSimilaritiesList = new ArrayList<DocumentsPairSimilarity>();
		for(DocumentsPairSimilarity documentsPairSimilarity:allDocumentsPairSimilaritiesList){
			if(documentsPairSimilarity.containsDoc(id) && documentsPairSimilarity.getSimilarity() >= threshold){
				documentPairSimilaritiesList.add(documentsPairSimilarity);
			}
		}
		return documentPairSimilaritiesList;
	}

	
	/**
	 * #### Create Similarity Matrix
	 */
	private void createSimilarityMatrixFromMySQL(List<Integer> job_id_list, Connection connection, String database_table, List<String> fieldsToSelectList, String language) throws SQLException{
    	int count = 0;
    	for(int i=0; i< job_id_list.size(); i++){
    		int doc_id_1 = job_id_list.get(i);
    		Document doc_1 = Helper.getDocumentById(connection, database_table, doc_id_1, fieldsToSelectList, language);
    		document_list.add(doc_1);

    		if(count++ % 100 == 0)
    			System.out.println("#NrDocsParsed for SimMatrix:"+count);
    			
    		for(int y=i+1; y< job_id_list.size(); y++){
        		int doc_id_2 = job_id_list.get(y);
        		double current_cosine_similarity = 0.0;

        		//check if the similarity exist in the previous SimilarityMatrix
        		if(old_similarity_matrix_map.containsKey(doc_id_1 + "," + doc_id_2)){
            		current_cosine_similarity = old_similarity_matrix_map.get(doc_id_1 + "," + doc_id_2);
        		}
        		//otherwise retrieve form mysql
        		else{
            		Document doc_2 = Helper.getDocumentById(connection, database_table, doc_id_2, fieldsToSelectList, language);
            		current_cosine_similarity = computeDocumentsCosineSimilarity(doc_1, doc_2);
        		}
        		documentsPairSimilaritiesList.add(new DocumentsPairSimilarity(doc_id_1, doc_id_2, current_cosine_similarity));
    		}
    	}
	}
	
	
	

	/**
	 *  Save Similarity Matrix into disk
	 * @param similarity_matrix_file
	 * @param documentsPairSimilaritiesList
	 * @throws IOException
	 */
	private void saveSimilarityMatrixToDisk(String similarity_matrix_file, List<DocumentsPairSimilarity> documentsPairSimilaritiesList) throws IOException{
    	Helper.writeToFile(similarity_matrix_file, "", false);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(similarity_matrix_file,true),"UTF8"));
    	for(DocumentsPairSimilarity doc_pair_similarity : documentsPairSimilaritiesList){
    		String record = doc_pair_similarity.getDoc_id_1() + "," +doc_pair_similarity.getDoc_id_2() + "\t" + doc_pair_similarity.getSimilarity() + "\n"; 
    		out.write(record);
    	}
		out.close();
	}
	
	
	/**
	 * Get previous Similarity Matrix in order to skip computing the similarities two times...
	 * Matrix is ordered by doc_id_1... 
	 * format: "doc_id_1,doc_id_2	\t	similarity"
	 */
	private Map<String,Double> getSimilarityMatrixFromFile(String similarity_matrix_file, boolean use_old_similarity_matrix) throws IOException{
    	Map<String,Double> old_similarity_matrix_map = new HashMap<String,Double>();
    	if(use_old_similarity_matrix){
	    	List<String> similarities_records_list = Helper.getFileContentLineByLine(similarity_matrix_file);
	    	for(String similarity_record : similarities_records_list){
	    		String[]similaritiesArr = similarity_record.split("\t");
	    		old_similarity_matrix_map.put(similaritiesArr[0], Double.parseDouble(similaritiesArr[1]));
	    	}
    	}
    	return old_similarity_matrix_map;
	}
	
	
	/**
	 * 
	 * @param cluster_1
	 * @param cluster_2
	 * @return Merging_measure =>  (Intersection / TOTAL_ITEMS_Cluster_1)  +  (Intersection / TOTAL_ITEMS_Cluster_2) 
	 */
	private double computeMergingMeasure(Cluster cluster_1, Cluster cluster_2){
		int cluster_1_size = cluster_1.getNumberOfDocsInCluster();
		int cluster_2_size = cluster_2.getNumberOfDocsInCluster();
		int intersection = computeIntersectionOfClusters(cluster_1, cluster_2);
		
//		int denominator = cluster_1_size > cluster_2_size ? cluster_2_size : cluster_1_size;
		double merging_measure_1 = (double)intersection/cluster_1_size;
		double merging_measure_2 = (double)intersection/cluster_2_size;
		return (merging_measure_1 + merging_measure_2);
	}
	private int computeIntersectionOfClusters(Cluster cluster_1, Cluster cluster_2){
		Set<Integer> cluster_doc_ids_set_1 = cluster_1.getDoc_ids_set();
		Set<Integer> cluster_doc_ids_set_2 = cluster_2.getDoc_ids_set();
		SetView<Integer> intesection = Sets.intersection(cluster_doc_ids_set_1, cluster_doc_ids_set_2);
		return intesection.size();
	}
	
	
	/**
	 * @param doc_1
	 * @param doc_2
	 * @return cosine similarity between two documents
	 */
	private double computeDocumentsCosineSimilarity(Document doc_1, Document doc_2){
		Map<String,Double> doc_vec_map_1 = Helper.getMapFromDocumentVector(doc_1.getFieldValueByKey("document_vector"));
		Map<String,Double> doc_vec_map_2 = Helper.getMapFromDocumentVector(doc_2.getFieldValueByKey("document_vector"));
		
		double dot_product = getDotProduct(doc_vec_map_1, doc_vec_map_2);
		double norm_vector_1 = normalizeVector(doc_vec_map_1);
		double norm_vector_2 = normalizeVector(doc_vec_map_2);
		
		return dot_product / (norm_vector_1 * norm_vector_2);
	}
	
	private double getDotProduct(Map<String,Double> doc_vec_map_1,Map<String,Double> doc_vec_map_2){
		double dot_product = 0.0;
		for(Map.Entry<String, Double> entry:doc_vec_map_1.entrySet()){
			if(doc_vec_map_2.containsKey(entry.getKey()))
				dot_product += entry.getValue() * doc_vec_map_2.get(entry.getKey());
		}
		return dot_product;
	}
	
	private double normalizeVector(Map<String,Double> doc_vec_map){
		double normalized_vector = 0.0;
		for(Map.Entry<String, Double> entry : doc_vec_map.entrySet())
			normalized_vector += Math.pow(entry.getValue(),2);
		return Math.sqrt(normalized_vector);
	}




	public List<Document> getDocument_list() {
		return document_list;
	}




	public void setDocument_list(List<Document> document_list) {
		this.document_list = document_list;
	}
	
	public  int getTotalNumberOfDocumentsInClusterList(Collection<Cluster> clusterList){
		Set<Integer> total_number_docsId = new HashSet<Integer>();
		for(Cluster cluster : clusterList)
			total_number_docsId.addAll(cluster.getDoc_ids_set());
		return total_number_docsId.size();
	}
}

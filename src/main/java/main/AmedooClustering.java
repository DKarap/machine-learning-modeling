package main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.cybozu.labs.langdetect.LangDetectException;
import com.machine_learning_modeling.core.Clustering;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.domain.clustering.Cluster;
import com.machine_learning_modeling.exception.DictionaryDuplicationException;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning.utils.Helper;
import com.machine_learning.utils.MySql;



public class AmedooClustering {

	/**
	 * 
	 * 
	 * ####Description:
	 * 			CLUSTER VALID JOBS (OF THE SAME LANGUAGE??) 
	 * 
	 * 
	 * 			
	 * ####Pipeline:
	 * 		[MySQL] -> [List of valid Document Ids] -> CREATE SIMILARITY MATRIX -> CLUSTERING ->  
	 * 
	 * 
	 * 
	 * ####Usage:
	 * 			IN EVERY ITERATION 
	 * 			java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooClustering Production root salle20mimis job_post "id,title,document_vector" " document_vector is not null and valid = 1 and LANGUAGE = 'en' " 4000 en 0.5 0.7 true 20 9 20 0.2 0.05 &
	 * @author mimis
	 *
	 */
	private final static Logger LOGGER = Logger.getLogger(AmedooDictionary.class.getName()); 


	public static void main(String[] args) throws IOException, NoLanguageSupportException, LangDetectException, ClassNotFoundException, SQLException, DictionaryDuplicationException  {

		/*
		 * LOGGER
		 */
		FileHandler fileHandler = new FileHandler("./logs/amedoo.log");
		LOGGER.addHandler(fileHandler);
		fileHandler.setFormatter(new SimpleFormatter());
		
		
		
		
		//################## INPUT ######################

		final String database_name = args[0];
		final String database_usr = args[1];
		final String database_psw = args[2];
		final String database_table = args[3];
		final List<String> fieldsToSelectList = Arrays.asList(args[4].split(",")); //fields to select from db
		final String select_condition = args[5]; 
		final int limit = Integer.parseInt(args[6]);
		final String language = args[7];
		
		final double similarity_threshold = Double.parseDouble(args[8]);  //utilized during Similarity Matrix
		final double merging_threshold = Double.parseDouble(args[9]);   //merging threshold
		
		
    	final boolean use_old_similarity_matrix = Boolean.parseBoolean(args[10]);
		final int number_of_clusters = Integer.parseInt(args[11]);
		final int KNN = Integer.parseInt(args[12]);//k for the KNN algo..

		//these two parameters are about the choice of the initial SEED clusters..
    	final int top_features_to_select_for_cluster_similarity = Integer.parseInt(args[13]);//how many top features we will consider for cluster comparison - use it for choosing good seed clusters
    	final double cluster_similarity_threshold = Double.parseDouble(args[14]); // the similarity measure([0..1]) that a candidate seed cluster and the previously selected seed cluster should not exceed in order the candidate to be assign as seed cluster
		
    	//this is the minimum similarity score that the K nearest neighbor must have during knn merging..
    	final double knn_threshold = Double.parseDouble(args[15]);
    	
		//similarity matrix folder+file
		final String similarity_matrix_folder = "./data/similarity_matrix/";
    	final String similarity_matrix_file = similarity_matrix_folder + language;


		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//################## MAIN ######################
    	long startTime = System.currentTimeMillis();
		Connection connection = MySql.getMySqlConnection(database_name, database_usr, database_psw);
		Clustering clustering = new Clustering(similarity_threshold, merging_threshold, similarity_matrix_file,use_old_similarity_matrix,number_of_clusters,KNN,top_features_to_select_for_cluster_similarity,cluster_similarity_threshold,knn_threshold);

		
		
		
    	
		/**
		 * ####1. Get sorted job ids for clustering => LANGUAGE = 'X' and VALID = 1
		 */
		String query = MySql.createSelectQuery(database_table, Arrays.asList("id"), select_condition + " order by id ", limit);
		Statement stmt  = connection.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
    	List<Integer> job_id_list = getJobIdsSorted(rs);
    	
    	
    	
    	
    	
    	
    	/**
    	 * #cluster documents
    	 */
    	Set<Cluster> finalClustersSet = clustering.clustering(job_id_list, connection, database_table, fieldsToSelectList, language);
    	
    	
    	

    	
    	
    	
    	
    	
    	/**
    	 * ####X. Display Clusters
    	 */
    	List<Cluster>clusterList = new ArrayList<Cluster>(finalClustersSet);
    	Collections.sort(clusterList,Cluster.ClusterComparatorSize);
    	displayClusters(clustering,clusterList,clustering.getDocument_list(),20);
    	
    	
    	
    	
    	
    	
    	
    	
    	
		/**
		 * ####6. Close mysql result set and connection
		 */
	    LOGGER.info("#Total Time:"+Helper.measureExecutionTime(startTime) + "ms)");
        rs.close();
        stmt.close();
        MySql.closeMySqlConnection(connection);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	public static void computeClusterVector(Clustering clustering,Cluster cluster,List<Document> document_list){
		clustering.computeClusterFeatureVector(cluster);
		List<Feature> clusterFeatureList = cluster.getFeature_vector();
		Collections.sort(clusterFeatureList, Feature.FeatureComparatorWeight);
		System.out.println("\n\t#Cluster top features...");
		clusterFeatureList = clusterFeatureList.subList(0, clusterFeatureList.size() > 20 ? 20 : clusterFeatureList.size());
		for(Feature f:clusterFeatureList)
			System.out.println("\t"+f.getValue()+"\t"+f.getDoc_frequency()+"\t"+f.getWeight());
	}
	
	
	
	
	public static void displayClusters(Clustering clustering,Collection<Cluster> clusterList, List<Document> document_list, int topNfeaturesToDisplay){
    	System.out.println("#Total Number of clusters:"+clusterList.size());
    	System.out.println("#Total Number of docs in all clusters:"+clustering.getTotalNumberOfDocumentsInClusterList(clusterList));

    	for(Cluster cluster:clusterList){
    		System.out.println("\n#Cluster ID:"+cluster.getId()+"\t#Docs:"+cluster.getNumberOfDocsInCluster()+"\t#Label:"+cluster.getLabel());
    		int c=0;
    		for(int id : cluster.getDoc_ids_set()){
        		Document doc1 = clustering.getDocumentFromListById(document_list, id);
        		System.out.println("\t#"+doc1.getFieldValueByKey("title"));
        		if(c++ > topNfeaturesToDisplay)
        			break;
    		}
    		computeClusterVector(clustering,cluster, document_list);
    	}
	}
	
	
	
	
	
	

	
//	private void displayCluster(Cluster cluster, List<Document> document_list){
//		System.out.println("\n#Cluster:"+cluster.getId());
//		for(int id : cluster.getDoc_ids_set()){
//			Document doc1 = getDocumentFromListById(document_list, id);
//			System.out.println("\t"+doc1.getFieldValueByKey("title"));
//		}
//	}

	
	
	
	public static List<Integer> getJobIdsSorted(ResultSet rs) throws SQLException{
		List<Integer> job_ids_list = new ArrayList<Integer>();
	    while (rs.next ()){
	    	job_ids_list.add(rs.getInt("id"));
	    }
		return job_ids_list;
	}
}

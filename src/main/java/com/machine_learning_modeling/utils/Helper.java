package com.machine_learning_modeling.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.machine_learning_modeling.domain.Document;


public class Helper {

	public static long measureExecutionTime(long startTime){
		return System.currentTimeMillis() - startTime;	
	}

	/**
	 * 
	 * @param taggedString - "word1_tag1 word2_tag2 .. wordN_tagN"
	 * @return an array with only the tags of the given tagged sentence - ["tag1","tag2",..,"tagN"]
	 */
	public static String[] getTagArrayFromTaggedSentence(String taggedString){
		String[] wordToTagArr = taggedString.split("\\s");
		StringBuilder buf = new StringBuilder();
		for(String wordToTag : wordToTagArr){
			String tag = wordToTag.split("_")[1];
			buf.append(tag+" ");
		}
		return buf.toString().trim().split("\\s");
	}

	/**
	 * 
	 * @param textArr
	 * @param tagsArr
	 * @return return a set of ngrams that are noun phrases(are continius Nouns)
	 */
	public static Set<String> getNounPhrases(String[] textArr, String[] tagsArr){
		Set<String> nounSet = new HashSet<String>();
		for(int i=0;i<tagsArr.length;i++){
			StringBuilder buf = new StringBuilder(textArr[i]+" ");
			if(tagsArr[i].startsWith("N")){
				for(int y=i+1;y<tagsArr.length;y++){
					if(tagsArr[y].startsWith("N")){
						buf.append(textArr[y]+" ");
						nounSet.add(buf.toString().trim());
					}
					else
						break;
				}
			}
		}
		return nounSet;
	}
	

	/**
	 * 
	 * @param textArray
	 * @param tagsArr
	 * @param nounPhrasesSet
	 * @param max_ngram_length
	 * @return create a map with unigrams and noun phrases to corresponding tags -lower case all ngrams
	 */
	public static Map<String,Set<String>> getNgramToTagSetMapSentence(String[] textArray, String[] tagsArr, Set<String> nounPhrasesSet, int max_ngram_length){
		Map<String,Set<String>> ngramToTagSetMap = new HashMap<String,Set<String>>();
		
		for(int i=0;i<tagsArr.length;i++){
			String current_ngram = textArray[i].toLowerCase();
			
			Set<String> tagSet = ngramToTagSetMap.get(current_ngram);
			if(tagSet!=null)
				tagSet.add(tagsArr[i]);
			else
				ngramToTagSetMap.put(current_ngram, new HashSet<String>(Arrays.asList(tagsArr[i])));
		}
		
		
		//add noun phrases
		for(String nounPhrase:nounPhrasesSet){
			if(nounPhrase.split("\\s").length <= max_ngram_length)
				ngramToTagSetMap.put(nounPhrase.toLowerCase(), new HashSet<String>(Arrays.asList("NP")));
		}
		return ngramToTagSetMap;
	}
	

	
	public static List<String> getFileContentLineByLine(String filePath) {
		List<String> linesSet = new ArrayList<String>();
		try{
			File file = new File(filePath);
			if(file.exists() && !file.isDirectory()) { 
				BufferedReader br = new BufferedReader(new FileReader(filePath));
				String line;
				while ((line = br.readLine()) != null) {
					linesSet.add(line);
				}
				br.close();
			}
		}catch(Exception e){
			return null;
		}
		return linesSet;
	}

	
	public static void writeToFile(String filename, String text, boolean append) throws IOException{
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,append),"UTF8"));
		out.write(text);
		out.close();
	}

	public static Map<String,Double> getMapFromDocumentVector(String document_vector){
		String[] featureAndWeightList = document_vector.split(",");
		Map<String,Double> featureToWeightMap = new HashMap<String,Double>();
		for(String featureAndWeight : featureAndWeightList){
			String[] featureAndWeightArr = featureAndWeight.split("-");
			featureToWeightMap.put(featureAndWeightArr[0], Double.parseDouble(featureAndWeightArr[1]));
		}
		return featureToWeightMap;
	}

	public static Map sortByComparator(Map unsortMap) {

		List list = new LinkedList(unsortMap.entrySet());
 
		// sort list based on comparator
		Collections.sort(list, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
                                       .compareTo(((Map.Entry) (o1)).getValue());
			}
		});
 
		// put sorted list into map again
                //LinkedHashMap make sure order in which keys were inserted
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public static double log2(double value){
		return (Math.log(value) / Math.log(2));

	}

	/**
	 * TODO this must be DELETED after Fix #49 
	 *     use MySql.getSingleRecord();
	 */
	public static Document getDocumentById(Connection connection, String database_table, int id, List<String> fieldsToSelectList, String language) throws SQLException{
		String query = MySql.createSelectQuery(database_table, fieldsToSelectList, " id = " + id , 1);
		Statement stmt  = connection.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
    	Document document = null;
	    while (rs.next ()){
	    	document = Helper.createDocumentFromMySQlresultSet(rs, fieldsToSelectList, language);
	    }
    	rs.close();
        stmt.close();
    	return document;
	}

	
	public static Document createDocumentFromElasticSearchresultSet(
			Map<String, Object> source_map, List<String> fieldsToSelectList,
			String language) throws SQLException {
		HashMap<String, String> rowResult = new HashMap<String, String>();
		for (String fieldName : fieldsToSelectList) {
			if (!fieldName.equals("id"))
				rowResult.put(fieldName, (String) source_map.get(fieldName));
		}
		Document document = new Document((Integer) source_map.get("id"),
				rowResult, language);
		return document;
	}

	
	public static Document createDocumentFromMySQlresultSet(ResultSet rs,
			List<String> fieldsToSelectList, String language)
			throws SQLException {
		HashMap<String, String> rowResult = new HashMap<String, String>();
		for (String fieldName : fieldsToSelectList)
			rowResult.put(fieldName, rs.getString(fieldName));
		Document document = new Document(Long.parseLong(rowResult.get("id")),
				rowResult, language);
		return document;
	}
	

}

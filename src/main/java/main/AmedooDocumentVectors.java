package main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.elasticsearch.ElasticsearchException;

import com.cybozu.labs.langdetect.LangDetectException;
import com.machine_learning_modeling.core.Dictionary;
import com.machine_learning_modeling.core.DocumentParser;
import com.machine_learning_modeling.core.FeatureSelection;
import com.machine_learning.core.analysis.SentenceDetector;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger_opennlp_implementation;
import com.machine_learning.core.analysis.tokenizer.Tokenizer;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.exception.DictionaryDuplicationException;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning_modeling.exception.SmoothingException;
import com.machine_learning_modeling.utils.Helper;
import com.machine_learning.utils.MySql;


/**
 * 
 * ####Description:
 * 		CREATE FOR EACH DOCUMENT ITS DOCUMENT VECTOR REPRESENTATION BASED ON THE GIVEN FEATURE SELECTION SETTING
 *
 *
 *
 * ####Pipeline:
 * 		[MySQL] -> [List of Documents] -> {for each doc :=> [DOCUMENT PARSER] -> [DOC FEATURES] -> [FEATURE SELECTION] -> [FINAL FEATURES] -> [DOCUMENT VECTOR - MYSQL] }
 * 
 * 
 * 
 * ####Usage: 
 * 		java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooDocumentVectors Production root salle20mimis job_post "id,title,recordText,detail_page_main_content,detail_page_url" " valid = 1 and  language = 'en' " 40000 "title:0.7,recordText:0.1,detail_page_main_content:0.1,detail_page_url:0.1" dictionary_jobs_en feature en false 1 1 3 25 1 1 true ""  -1 false true true true false document_vector &    
 * 
 * 
 */
public class AmedooDocumentVectors {
	
	private final static Logger LOGGER = Logger.getLogger(AmedooDocumentVectors.class.getName()); 

	public static void main(String[] args) throws ElasticsearchException, SecurityException, IOException, DictionaryDuplicationException, LangDetectException, NoLanguageSupportException, ClassNotFoundException, SQLException, InterruptedException {

		/*
		 * LOGGER
		 */
		FileHandler fileHandler = new FileHandler("./logs/amedoo.log");
		LOGGER.addHandler(fileHandler);
		fileHandler.setFormatter(new SimpleFormatter());
	
		
		
		
		
		
		
		
		
		
		
		
		//################## INPUT ######################


		
		//MySQL API
		final String database_name = args[0];
		final String database_usr = args[1];
		final String database_psw = args[2];
		final String database_table = args[3];
		final List<String> fieldsToSelectList = Arrays.asList(args[4].split(",")); //fields to select from db
		final String select_condition = args[5]; // should be based on the LANGUAGE
		final int limit = Integer.parseInt(args[6]);		 

		
		//Document fields to parse and their weights in order to create the document's vector: 
		final Map<String,Double> fieldToWeigthMap = getFieldsToWeightMap(args[7]);
		final Set<String> fieldsToParseSet = fieldToWeigthMap.keySet(); //fields to process in order to create the document vector representation
		
		
		
		//Dictionary API
		final String dictionary_name = args[8];
		final String document_type = args[9];
		final String language = args[10];
		

		//Feature Selection API
		final boolean accept_only_noun_phrases = Boolean.parseBoolean(args[11]);
		final int min_ngram_length = Integer.parseInt(args[12]);
		final int max_ngram_length = Integer.parseInt(args[13]);
		final int min_token_length = Integer.parseInt(args[14]);
		final int max_token_length = Integer.parseInt(args[15]);
		final int min_document_frequency = Integer.parseInt(args[16]);
		final int min_source_frequency = Integer.parseInt(args[17]);
		final boolean skip_stopwords = Boolean.parseBoolean(args[18]);
		List<String> accepted_fields = null; //fields in where the accepted features must have been seen before in our corpus, otherwise let empty ""
		if(!args[19].equalsIgnoreCase("null") && !args[19].isEmpty())
			accepted_fields = Arrays.asList(args[19].split(","));
		final int top_N_features_to_return = Integer.parseInt(args[20]);// how many features to keep from each document..; if -1 then keep all.. 
		final boolean use_pos_tagger = Boolean.parseBoolean(args[21]);
		final boolean lowerCase = Boolean.parseBoolean(args[22]);
		final boolean ignorePunctuation = Boolean.parseBoolean(args[23]);
		final boolean ignoreDigits = Boolean.parseBoolean(args[24]);
		final boolean debugMode = Boolean.parseBoolean(args[25]);
		//Database Field where to save the extracted document vectors
		final List<String> database_field_to_save_doc_vector= new ArrayList<String>(Arrays.asList(args[26]));

		
		final List<String> stopwords;
		if(language.equals("en"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/en/stopwords.txt");
		else if(language.equals("nl"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/nl/stopwords.txt");
		else
			stopwords=null;
		
		String pos_maxent_opennlp_filepath = null;
		if(language.equals("en"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/en-pos-maxent.bin";
		else if(language.equals("nl"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/nl-pos-maxent.bin";
		else if(language.equals("de"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/de-pos-maxent.bin";
		else
			throw new NoLanguageSupportException(language + " is not suported for PartOfSpeechTagger_opennlp_implementation..");

		String sentence_model_filepath = null;
		if(language.equals("en"))
			sentence_model_filepath = "./data/sentence_detector_openNlp/en-sent.bin";
		else if(language.equals("nl"))
			sentence_model_filepath = "./data/sentence_detector_openNlp/nl-sent.bin";

		
		
		
		
		final int size_of_ram_dictionary = 1000000;//how many features will keep in the readonly memory dictionary
		final String lang_profiles_filepath = "./data/language_profiles";		

		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//################## MAIN ######################
		FeatureSelection featureSelection = new FeatureSelection( min_ngram_length, max_ngram_length,  min_token_length, max_token_length, min_document_frequency,min_source_frequency,accept_only_noun_phrases,accepted_fields);
		DocumentParser documentParser = new DocumentParser(lang_profiles_filepath);
		Tokenizer tokenizer = new Tokenizer(skip_stopwords,stopwords);
		SentenceDetector sentenceDetector = new SentenceDetector(language,sentence_model_filepath);
		PartOfSpeechTagger partOfSpeechTagger = new PartOfSpeechTagger_opennlp_implementation(language,pos_maxent_opennlp_filepath);

//		Dictionary dictionary = new Dictionary(dictionary_name, document_type); //dictionary doesnt include the test features
		Dictionary dictionary = new Dictionary(dictionary_name, document_type, featureSelection,size_of_ram_dictionary); //dictionary doesnt include the test features

		Connection connection = MySql.getMySqlConnection(database_name, database_usr, database_psw);
    	PreparedStatement preparedUpdateStatement = MySql.createPrepareUpdateByIdStatement(connection, database_table, database_field_to_save_doc_vector);


    	
    	
    	
    	
    	
    	
		/**
		 * ####1. Select job records for processing
		 */
		System.out.print("#Select job records for processing...");
		String query = MySql.createSelectQuery(database_table, fieldsToSelectList, select_condition, limit);
		Statement stmt  = connection.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
		System.out.print("[done]...");
    	//Get results
    	int parsedDocumentcounter = 1;
		long startTime = System.currentTimeMillis();
		
	    while (rs.next ()){
	    	
	    	/**
	    	 * ####2. Create current document
	    	 */
	    	Document document = Helper.createDocumentFromMySQlresultSet(rs, fieldsToSelectList, language);
	    	
	    	
	    	
	    	
	    	
	    	/**
	    	 * ####3. Get list of features from current document  
	    	 */
    		List<Feature> document_feature_list = documentParser.getDocumentFeatures(document, fieldsToParseSet, tokenizer, sentenceDetector, partOfSpeechTagger,min_ngram_length, max_ngram_length, min_token_length, max_token_length,use_pos_tagger, lowerCase, ignorePunctuation, ignoreDigits);
    		

    		
    		
    		
    		
    		/**
    		 * ####4. Feature Selection from current document features: from all the candidates keep only the most informative
    		 */
    		List<Feature> document_selected_feature_list = null;
			try {
				document_selected_feature_list = featureSelection.parser(document_feature_list, dictionary, top_N_features_to_return, fieldToWeigthMap);
			} catch (SmoothingException e) {
				LOGGER.info("FATAL ERROR:"+e.getMessage());
				break;
			}
    		
			
			/**
			 * ####. Debugging: display the features sorted by weight fro each document 
			 */
			if(debugMode){
				System.out.println("\n\nTitle:"+document.getFieldValueByKey("title"));
				Collections.sort(document_selected_feature_list, Feature.FeatureComparatorWeight);
				for(Feature feature:document_selected_feature_list)
					System.out.println(feature.getValue()+"\t"+feature.getWeight());
			}
			
			

			
			
			
    		/**
    		 * ####5. Create Document Vector from the final selected features  and save it to current document
     		 * ####6. Save document Vector into MYSQL 
    		 */
    		String document_vector = createDocumentVectorFromFeatureList(document_selected_feature_list);
    		document.addField(database_field_to_save_doc_vector.get(0), document_vector);
    		MySql.updateById(preparedUpdateStatement, database_field_to_save_doc_vector, document.getFieldToTextMap());
	    	
	    	
	    	
	    	
	    	
	    	//Display info for every N docs
	    	if(parsedDocumentcounter++ % 100 == 0)
	    		LOGGER.info("#Jobs proccesed so far:"+parsedDocumentcounter);
	    }
	    
	    
	    
	    
		/**
		 * ####6. Close documentParser, dictionary and mysql result set and connection
		 */
		LOGGER.info("#Total Time:"+Helper.measureExecutionTime(startTime)+"ms (Avg.Per.Doc:"+(double)Helper.measureExecutionTime(startTime) / parsedDocumentcounter +"ms)");
	    documentParser.closeDocumentParser();
	    dictionary.closeDictionary();
        rs.close();
        stmt.close();
        MySql.closeMySqlConnection(connection);
	}
	
	
	
	
	/**
	 * 
	 * @param featureList
	 * @return comma separated string with the given feature's values
	 */
	public static String createDocumentVectorFromFeatureList(List<Feature> featureList){
		StringBuilder buf = new StringBuilder();
		for(Feature feature:featureList)
			buf.append(feature.getValue() + " - " + feature.getWeight() +",");
		return buf.toString().replaceAll(",$", "");
	}



	/**
	 * 
	 * @param fieldsToParseAndTheirWeightsMap - "field_name:field_weight,field_name2:field_weight2"
	 * @return Map<String,Double> 
	 */
	public static Map<String,Double> getFieldsToWeightMap(String fieldsToParseAndTheirWeightsMap){
		Map<String,Double> fieldToWeigthMap = new HashMap<String,Double>();
		String[] fieldsNameAndWeigthsArr = fieldsToParseAndTheirWeightsMap.split(",");
		for(String fieldsNameAndWeigth: fieldsNameAndWeigthsArr){
			String[] fieldsNameAndWeigthArr = fieldsNameAndWeigth.split(":");
			fieldToWeigthMap.put(fieldsNameAndWeigthArr[0], Double.parseDouble(fieldsNameAndWeigthArr[1]));
		}
		return fieldToWeigthMap;
	}
}

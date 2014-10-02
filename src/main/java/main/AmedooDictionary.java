package main;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.cybozu.labs.langdetect.LangDetectException;
import com.machine_learning_modeling.core.Dictionary;
import com.machine_learning_modeling.core.DocumentParser;
import com.machine_learning.core.analysis.SentenceDetector;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger;
import com.machine_learning_modeling.core.analysis.parser.PartOfSpeechTagger_opennlp_implementation;
import com.machine_learning.core.analysis.tokenizer.Tokenizer;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning_modeling.domain.Feature;
import com.machine_learning_modeling.exception.DictionaryDuplicationException;
import com.machine_learning.exception.NoLanguageSupportException;
import com.machine_learning_modeling.utils.Helper;
import com.machine_learning.utils.MySql;


/**
 * 
 * 
 * ####Description:
 * 			CREATE/UPDATE A DICTIONARY FOR A GIVEN LANGUAGE  WITH ALL THE FEATURES THAT EXIST IN THE GIVEN DOC FIELDS
 * 
 * 
 * 			
 * ####Pipeline:
 * 		[MySQL] -> [List of Documents] ->{ [DOCUMENT PARSER] -> [DOC FEATURES] -> [UPDATE FEATURES/INDEX STATISTICS] } -> [ELASTICSEARCH INDEX] 
 * 
 * 
 * 
 * ####Usage:
 * 			java -Xmx2G -Xms2G -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.AmedooDictionary dictionary_jobs_en feature en 1 2 3 25 true true true Production root salle20mimis job_post_backup1 "id,navigation_id,title,recordText,detail_page_main_content,detail_page_url" "title,recordText,detail_page_main_content,detail_page_url" " LANGUAGE = 'en' " 400000 1500000 100 true true &
 * 
 * 
			
 * @author mimis
 *
 */
public class AmedooDictionary {
	
	
	private final static Logger LOGGER = Logger.getLogger(AmedooDictionary.class.getName()); 


	public static void main(String[] args) throws IOException, NoLanguageSupportException, LangDetectException, ClassNotFoundException, SQLException, DictionaryDuplicationException  {

		/*
		 * LOGGER
		 */
		FileHandler fileHandler = new FileHandler("./logs/amedoo.log");
		LOGGER.addHandler(fileHandler);
		fileHandler.setFormatter(new SimpleFormatter());
		
		
		
		
		//################## INPUT ######################
		final String dictionary_name = args[0];
		final String document_type = args[1];
		final String language = args[2];
		final int min_ngram_length = Integer.parseInt(args[3]);
		final int max_ngram_length = Integer.parseInt(args[4]);
		final int min_token_length = Integer.parseInt(args[5]);
		final int max_token_length = Integer.parseInt(args[6]);

		final boolean lowerCase = Boolean.parseBoolean(args[7]);
		final boolean ignorePunctuation = Boolean.parseBoolean(args[8]);
		final boolean ignoreDigits = Boolean.parseBoolean(args[9]);
		
		
		final String database_name = args[10];
		final String database_usr = args[11];
		final String database_psw = args[12];
		final String database_table = args[13];
		final List<String> fieldsToSelectList = Arrays.asList(args[14].split(",")); //fields to select from db
		final List<String> fieldsToParseList = Arrays.asList(args[15].split(",")); //fields to process in order to create the dictionary
		final String select_condition = args[16]; // should be based on the LANGUAGE
		final int limit = Integer.parseInt(args[17]);
		final int max_nr_of_features_in_memory = Integer.parseInt(args[18]); //how many features on memory to keep?
		final int bulk_size =  Integer.parseInt(args[19]); //how many features to flush per time into the Es index via th ebulk api
		final boolean erase_dictionary_initialy = Boolean.parseBoolean(args[20]);
		final boolean skip_stopwords = Boolean.parseBoolean(args[21]);

		
		final List<String> stopwords;
		if(language.equals("en"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/en/stopwords.txt");
		else if(language.equals("nl"))
			stopwords = Helper.getFileContentLineByLine("./data/stop_words/nl/stopwords.txt");
		else
			stopwords=null;
		
		final String lang_profiles_filepath = "./data/language_profiles";		
		
		
		String pos_maxent_opennlp_filepath = null;
		if(language.equals("en"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/en-pos-maxent.bin";
		else if(language.equals("nl"))
			pos_maxent_opennlp_filepath = "./data/part_of_speech_opennlp/models/nl-pos-maxent.bin";
		else
			throw new NoLanguageSupportException(language + " is not suported for PartOfSpeechTagger_opennlp_implementation..");

		String sentence_model_filepath = null;
		if(language.equals("en"))
			sentence_model_filepath = "./data/sentence_detector_openNlp/en-sent.bin";
		else if(language.equals("nl"))
			sentence_model_filepath = "./data/sentence_detector_openNlp/nl-sent.bin";
		
		
		
		
		//update doc frequency since we get a unique list of features to add from each document;
		final boolean update_doc_freq = true; 
		boolean update_source_freq;
		boolean use_pos_tagger = true;
		
		

		
		
		
		
		
		
		
		
		
		//################## MAIN ######################
		Dictionary dictionary = new Dictionary(dictionary_name, document_type,max_nr_of_features_in_memory,bulk_size);
		DocumentParser documentParser = new DocumentParser(lang_profiles_filepath);
		Tokenizer tokenizer = new Tokenizer(skip_stopwords,stopwords);
		SentenceDetector sentenceDetector = new SentenceDetector(language,sentence_model_filepath);
		PartOfSpeechTagger partOfSpeechTagger = new PartOfSpeechTagger_opennlp_implementation(language,pos_maxent_opennlp_filepath);
		Connection connection = MySql.getMySqlConnection(database_name, database_usr, database_psw);

		/*
		 * erase dictionary initialy
		 */
		if(erase_dictionary_initialy)
			dictionary.eraseDictionary();
		
		
		
    	
		/**
		 * ####1. Select job records for processing
		 */
		System.out.print("#Select job records for processing...");
		Set<Integer> navigation_id_set = new HashSet<Integer>();
		String query = MySql.createSelectQuery(database_table, fieldsToSelectList, select_condition, limit);
		Statement stmt  = connection.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
		System.out.print("[done]...");
    	//Get results
		long startTime = System.currentTimeMillis();
    	int parsedDocumentcounter = 1;
	    while (rs.next ()){
	    	/*
	    	 * Check if the current job is from a new source
	    	 */
	    	if(navigation_id_set.contains(rs.getInt("navigation_id")))
	    		update_source_freq = false;
	    	else{
	    		update_source_freq = true;
	    		navigation_id_set.add(rs.getInt("navigation_id"));
	    	}
	    	
	    	
	    	/**
	    	 * ####0. Create current document
	    	 */
	    	Document document = Helper.createDocumentFromMySQlresultSet(rs, fieldsToSelectList, language);
	    	
	    	
	    	
    		
    		
	    	
	    	
	    	
	    	/**
	    	 * ####1. Get list of features from current document(Doc.Freq.==1)  
	    	 */
    		List<Feature> document_feature_list = documentParser.getDocumentFeatures(document, fieldsToParseList, tokenizer, sentenceDetector, partOfSpeechTagger,min_ngram_length, max_ngram_length, min_token_length, max_token_length, use_pos_tagger, lowerCase, ignorePunctuation, ignoreDigits);

	    	
    		
    		
    		
	    	/**
    		 * ####2. Increase the number of parsed documents and sources in Ram...
    		 */
    		dictionary.increaseNumberOfParsedDocuments(1L);
	    	if(update_source_freq)
	    		dictionary.increaseNumberOfParsedSources(1L);
    		
    		
			/**
			 * ####3. update document's features into RAM dictionary 
			 * 		1.general doc freq
			 * 		2.pos + field => total+doc freq
			 */
    		long start = System.currentTimeMillis();
	    	for(Feature feature : document_feature_list){
	    		dictionary.addFeature(feature, update_doc_freq,update_source_freq);
	    	}
	    	
	    	
	    	
	    		    	
	    	
	    	/*
	    	 * Just debuging info msg
	    	 */
	    	if(parsedDocumentcounter++ % 1000 == 0){
		    	LOGGER.info("#Documents proccesed so far:"+parsedDocumentcounter + "\tFeatures in Memory:"+dictionary.getNumberOfFeaturesInMemory() +"\t#Document'S Features:"+document_feature_list.size()	 +"\t#Add feature in Ram time:"+Helper.measureExecutionTime(start));
	    	}	    	
	    }
	    
	    
//	    /**
//	     * 5. flush last features in memory onto index - - Update Index Stats (Document and Source Parsed counters) into Disk
//	     */
//	    if(dictionary.getNumberOfFeaturesInMemory() != 0){
//			dictionary.flushMemoryFeaturesIntoIndex();
//	    }
	    
	    
	    
	    

    	/**
    	 * 6. Compute Utility measures here..there is a problem with the refresh durring create doc vectors so do it now
    	 * 		  COMPUTE UTLILITY MEASURE(IDF on Doc_Freq and Source_Freq) FOR EACH FEATURE, IF THE INPUT IS SET SO..
    	 */
   		System.out.print("Compute features utility measures...");
   		dictionary.computeUtilityMeasures();
   		System.out.println("...[done..]");
		

	    
	    
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
}

package main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import com.cybozu.labs.langdetect.LangDetectException;
import com.machine_learning_modeling.core.DocumentParser;
import com.machine_learning_modeling.domain.Document;
import com.machine_learning.exception.NoSemanticFieldSupportException;
import com.machine_learning_modeling.utils.Helper;
import com.machine_learning.utils.MySql;

import de.l3s.boilerpipe.BoilerpipeProcessingException;


public class AmedooDocumentSemanticParser {

	/**
	 * ####Description:
	 * 			DETECT given semantics and save them into the database
	 * 
	 * 
	 * 
	 * 
	 * ####Pipeline:
	 * 		[MySQL] -> [List of Documents] ->{ [DOCUMENT SEMANTIC PARSER] -> [MYSQL] }
	 * 
	 * 
	 * 
	 * 
	 * ####Run:
	 * 			java -cp target/amedoo-1.0-SNAPSHOT.jar com.amedoo.machine_learning.DocumentSemanticParser    Production username pasword job_post_backup1 "id,title,recordText,detail_page_text" "detail_page_main_content,language,location" " detail_page_main_content is null "  10
	 * 
	 * 
	 * 
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws BoilerpipeProcessingException 
	 * @throws LangDetectException 
	 * @throws NoSemanticFieldSupportException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, BoilerpipeProcessingException, LangDetectException, NoSemanticFieldSupportException {
		
		//################## INPUT ######################
		final String database_name = args[0];
		final String database_usr = args[1];
		final String database_psw = args[2];
		final String database_table = args[3];
		final List<String> fieldsToSelectList = Arrays.asList(args[4].split(","));
		final List<String> semanticList = Arrays.asList(args[5].split(",")); //fields for semantic processing
		final String select_condition = args[6];
		final int limit = Integer.parseInt(args[7]);
		final String lang_profiles_filepath = "./data/language_profiles";		

		
		
		
		
		
		  

		
		
		
		
		//################## MAIN ######################
		DocumentParser documentParser = new DocumentParser(lang_profiles_filepath);
		
		Connection connection = MySql.getMySqlConnection(database_name, database_usr, database_psw);
    	PreparedStatement preparedUpdateStatement = MySql.createPrepareUpdateByIdStatement(connection, database_table, semanticList);

		
    	
    	
		/**
		 * ####1. Select job records for processing
		 */
		String query = MySql.createSelectQuery(database_table, fieldsToSelectList, select_condition, limit);
		Statement stmt  = connection.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
    	//Get results
    	int counter = 1;
	    while (rs.next ()){
	    	/*
	    	 * Create current document
	    	 */
	    	Document document = Helper.createDocumentFromMySQlresultSet(rs, fieldsToSelectList, null);

	    	
	    	
	    	
	    	
	    	
	    	
	    	/**
	    	 * ####2. Process job record  
	    	 */
    		documentParser.semanticParser(document, semanticList);
	    	
	    	
	    	
    		
    		
    		
			/**
			 * ####3. update job record's LANGUAGE and DETAIL PAGE TEXT
			 */
    		MySql.updateById(preparedUpdateStatement, semanticList, document.getFieldToTextMap());
	    	
	    	
	    	
	    	
	    	
	    	//Display info for every N docs
	    	if(counter++ % 1 == 0)
	    		System.out.println("#Jobs proccesed so far:"+counter+"\tcurrentDoc:"+document.getFieldValueByKey("id")+"\t"+document.getFieldValueByKey("title")+"\t"+document.getFieldValueByKey("language")+"\t"+(document.getFieldValueByKey("detail_page_main_content")==null?"null":"ok"));
	    }

	    
	    
		/*
		 * ####4. Close documentParser and mysql result set and connection
		 */
	    documentParser.closeDocumentParser();
	    preparedUpdateStatement.close();
        rs.close();
        stmt.close();
        MySql.closeMySqlConnection(connection);	
	}
}

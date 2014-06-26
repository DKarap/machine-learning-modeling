package com.machine_learning_modeling.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;



public class MySql {
	

	
	public static Connection getMySqlConnection(String database, String usr, String psw) throws ClassNotFoundException, SQLException{
		String myDriver = "org.gjt.mm.mysql.Driver";
		String myUrl = "jdbc:mysql://localhost/"+database;
		myUrl += "?zeroDateTimeBehavior=convertToNull";
		Class.forName(myDriver);
		Connection connection = DriverManager.getConnection(myUrl, usr, psw);
		
		return connection;
	}
	
	/**
	 * Close given connection from MySql
	 * @return
	 * @throws SQLException 
	 */
	public static void closeMySqlConnection(Connection connection) throws SQLException{
		connection.close();
	}	

	
	public static Map<Object,Object> getSingleRecord(Connection connection, String query, List<String> fieldsToSelectList) throws SQLException{
		Statement stmt  = connection.createStatement();
    	ResultSet rs = stmt.executeQuery(query);
		HashMap<Object,Object> currentDocFieldsMap = new HashMap<Object,Object>();

    	
	    while (rs.next ()){
			for (String fieldName : fieldsToSelectList)
				currentDocFieldsMap.put(fieldName, rs.getString(fieldName));
	    }
	    rs.close();
	    stmt.close();
	    return currentDocFieldsMap;
}
	
	
	/**
	 * 
	 * @param database_table
	 * @param fieldsList
	 * @param condition
	 * @param limit
	 * @return select query based on thte given input
	 */
	public static String createSelectQuery(String database_table, List<String> fieldsList, String condition, int limit){
			String query = "SELECT "+ StringUtils.join(fieldsList, ",") 
						 + " FROM " + database_table;
			if(condition != null)
				query += " WHERE " + condition;
			if(limit != -1)
				query += " LIMIT " + limit;
			return query;
	}

	
	
	
	/**
	 * 
	 * @param connection
	 * @param database_table
	 * @param fieldsToUpdateList
	 * @return Prepared update Statement for given fields and select based on ID 
	 * @throws SQLException
	 */
	public static PreparedStatement createPrepareUpdateByIdStatement(Connection connection, String database_table, List<String> fieldsToUpdateList) throws SQLException{
		String sql = "UPDATE " + database_table + " SET ";
		for(String fieldToUpdate:fieldsToUpdateList){
			sql += fieldToUpdate + " = ?,";
		}
		sql = sql.replaceAll(",$", "");
		sql += " WHERE id=?";
		return connection.prepareStatement(sql);
	}
	
	/**
	 * Update corresponding record of given document(by id) 
	 * @param preparedStatement
	 * @param fieldsToUpdateList with String value
	 * @param Map<String,String> fieldToTextMap
	 * @return number of rows
	 * @throws SQLException
	 */
	public static int updateById(PreparedStatement preparedStatement, List<String> fieldsToUpdateList, Map<String,String> fieldToTextMap) throws SQLException{
		int index=1;
		for(String fieldToUpdate:fieldsToUpdateList){
			preparedStatement.setString(index++, fieldToTextMap.get(fieldToUpdate));
		}
		//add id
		preparedStatement.setLong(fieldsToUpdateList.size() + 1, Long.parseLong(fieldToTextMap.get("id")));
		return preparedStatement.executeUpdate();
	}
	
	
}

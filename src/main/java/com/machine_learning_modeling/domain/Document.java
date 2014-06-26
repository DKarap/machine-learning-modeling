package com.machine_learning_modeling.domain;

import java.util.Map;

public class Document {

	private long id;
	private Map<String,String> fieldToTextMap;
	private String language;
	
	public Document(long id, Map<String, String> fieldToTextMap, String language) {
		super();
		this.id = id;
		this.fieldToTextMap = fieldToTextMap;
		this.language = language;
	}

	
	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public Map<String, String> getFieldToTextMap() {
		return fieldToTextMap;
	}

	public void setFieldToTextMap(Map<String, String> fieldToTextMap) {
		this.fieldToTextMap = fieldToTextMap;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
	
	
	public String getFieldValueByKey(String key) {
		return this.fieldToTextMap.get(key);
	}

	public void addField(String key,String value) {
		this.fieldToTextMap.put(key, value);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Document other = (Document) obj;
		if (id != other.id)
			return false;
		return true;
	}

	
	
}

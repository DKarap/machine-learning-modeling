package com.machine_learning_modeling.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.machine_learning_modeling.exception.FieldDontHaveAttributesException;


public class Feature {
	
	private String _id;
	private String value;
	private long doc_frequency; //general doc freq
	private long source_frequency; //in how many websites/sources this feature appears
	private double idf_doc; //idf for the document frequency
	private double idf_source; //idf for the source frequency
	private String language;
	private int ngram_length;
	private String name_entity;
	private List<Attribute> fieldAttributeList;
	private List<Attribute> part_of_speechAttributeList;
	private double weight;
	
	public Feature(String value, String language) {
		super();
		this.value = value;
		this.language = language;
		this.ngram_length = value.split("\\s").length;
		this.doc_frequency = 1;
		this.source_frequency = 1;
		this.idf_doc = 0.0; //idf for the document frequency
		this.idf_source  = 0.0; //idf for the source frequency
		this.fieldAttributeList = new ArrayList<Attribute>();
		this.part_of_speechAttributeList = new ArrayList<Attribute>();
		this.weight = 0.0;
	}

	@SuppressWarnings({ "unchecked" })
	public Feature(Map<String,Object> elasticSearchSourceMap) {
		super();
		this._id = elasticSearchSourceMap.get("_id").toString();
		this.value = elasticSearchSourceMap.get("word").toString();
		this.language = elasticSearchSourceMap.get("language").toString();
		this.ngram_length = this.value.split("\\s").length;
		this.doc_frequency = Long.parseLong(elasticSearchSourceMap.get("doc_frequency").toString()); 
		this.source_frequency = Long.parseLong(elasticSearchSourceMap.get("source_frequency").toString()); 
		this.idf_doc = elasticSearchSourceMap.get("idf_doc") != null ? Double.parseDouble(elasticSearchSourceMap.get("idf_doc").toString()) : 0.0;
		this.idf_source = elasticSearchSourceMap.get("idf_source") != null ? Double.parseDouble(elasticSearchSourceMap.get("idf_source").toString()) : 0.0;
		this.fieldAttributeList = createAttributeList( (List<Map<String, Object>>) elasticSearchSourceMap.get("field"));
		this.part_of_speechAttributeList = createAttributeList( (List<Map<String, Object>>) elasticSearchSourceMap.get("part_of_speech"));
		this.weight = 0.0;
	}
	
	
	public Feature(String token, String language, String field, Map<String, Set<String>> wordToTagsSetMap){
		super();
		this.value = token;
		this.language = language;
		this.ngram_length = value.split("\\s").length;
		this.doc_frequency = 1; 
		this.source_frequency = 1;
		this.idf_doc = 0.0; //idf for the document frequency
		this.idf_source  = 0.0; //idf for the source frequency
		this.fieldAttributeList = new ArrayList<Attribute>();
		this.part_of_speechAttributeList = new ArrayList<Attribute>();
		this.weight = 0.0;
		
		Attribute fieldAttr = new Attribute(field);
		addFieldAttr(fieldAttr);

		if(wordToTagsSetMap != null && !wordToTagsSetMap.isEmpty()){
			List<Attribute> tagAttrList = new ArrayList<Attribute>(); //can be more than one Part of Speech Tag
			Set<String> tokenTagsSet = wordToTagsSetMap.get(token);
			if(tokenTagsSet != null){
				for(String tag:tokenTagsSet){
					tagAttrList.add(new Attribute(tag));
				}
			}
			addPartOfSpeechAttrList(tagAttrList);
		}
	}
	
	
	
	public Attribute getAttribute(String field,String attr_name) throws FieldDontHaveAttributesException{
		if(field.equals("field")){
			return getAttributeFromGivenAttrList(fieldAttributeList, new Attribute(attr_name));
		}
		else if(field.equals("part_of_speech")){
			return getAttributeFromGivenAttrList(part_of_speechAttributeList, new Attribute(attr_name));			
		}
		else{
			throw new FieldDontHaveAttributesException("Given field '"+field+"' doesnt have attributes"); 
		}
	}
	
	private Attribute getAttributeFromGivenAttrList(List<Attribute> attributeList, Attribute attribute){
		int index = attributeList.indexOf(attribute);
		if(index!=-1)
			return attributeList.get(index);
		else
			return null;
	}
	
	private List<Attribute> createAttributeList(List<Map<String, Object>> fieldList){
		List<Attribute> fieldAttributeList = new ArrayList<Attribute>();

		for(int i=0;i<fieldList.size();i++){
			Map<String, Object> fieldMap = fieldList.get(i);
			fieldAttributeList.add(new Attribute(fieldMap.get("name").toString(), Long.parseLong(fieldMap.get("total_frequency").toString()), Long.parseLong(fieldMap.get("doc_frequency").toString())));
		}
		
		return fieldAttributeList;
	}
	
	public void increaseGeneralDocFreq(){
		this.doc_frequency += 1;
	}
	public void increaseGeneralDocFreq(long doc_freq){
		this.doc_frequency += doc_freq;
	}

	public void increaseSourceFreq(){
		this.source_frequency += 1;
	}
	public void increaseSourceFreq(long doc_freq){
		this.source_frequency += doc_freq;
	}

	
	public String get_id() {
		return _id;
	}



	public void set_id(String _id) {
		this._id = _id;
	}



	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	private void addAttribute(List<Attribute> attributeList, Attribute attributeToAdd, boolean update_doc_freq ){
		int index = attributeList.indexOf(attributeToAdd);
		if(index != -1){
			Attribute attributeToUpdate = attributeList.get(index);
		
			attributeToUpdate.increaseTotal_frequency(attributeToAdd.getTotal_frequency());
			if(update_doc_freq){
				attributeToUpdate.increaseDoc_frequency(attributeToAdd.getDoc_frequency());
			}
		}
		else{
			attributeList.add(attributeToAdd);
		}
	}

	
	
	//update_total_freq always increase by one
	public void updateFrequenciesByFeature(Feature featureToAdd,  boolean update_doc_freq, boolean update_source_freq ) {
		if(update_doc_freq){
			increaseGeneralDocFreq(featureToAdd.getDoc_frequency());
		}

		if(update_source_freq){
			increaseSourceFreq(featureToAdd.getSource_frequency());
		}

		
		for(Attribute attr: featureToAdd.getFieldAttributeList()){
			addAttribute(this.fieldAttributeList, attr, update_doc_freq);
		}

		for(Attribute attr: featureToAdd.getPart_of_speechAttributeList()){
			addAttribute(this.part_of_speechAttributeList, attr, update_doc_freq);
		}

	}

	
	
	public boolean isNoun(){
		for(Attribute attr:part_of_speechAttributeList){
			if(attr.getName().startsWith("N"))
				return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param fieldNameList
	 * @return true if the feature have been seen in our corpus in at least one of the given fields names
	 */
	public boolean seenInFieldBefore(List<String> fieldNameList){
		for(Attribute attr : this.fieldAttributeList){
			if(fieldNameList.contains(attr.getName()))
				return true;
		}
		return false;
	}
	public boolean seenInFieldBefore2(List<String> fieldNameList){
		for(String fieldToAppear : fieldNameList){
			if(this.fieldAttributeList.contains(new Attribute(fieldToAppear)))
				return true;
		}
		return false;
	}
	
	
	
	
	
	public void addFieldAttr(Attribute attr) {
		this.fieldAttributeList.add(attr);
	}

	public void addPartOfSpeechAttr(Attribute attr) {
		this.part_of_speechAttributeList.add(attr);
	}
	public void addPartOfSpeechAttrList(List<Attribute> attrList) {
		this.part_of_speechAttributeList.addAll(attrList);
	}
	
	public long getDoc_frequency() {
		return doc_frequency;
	}


	public void setDoc_frequency(long doc_frequency) {
		this.doc_frequency = doc_frequency;
	}


	public long getSource_frequency() {
		return source_frequency;
	}

	public void setSource_frequency(long source_frequency) {
		this.source_frequency = source_frequency;
	}

	public List<Attribute> getFieldAttributeList() {
		return fieldAttributeList;
	}

	public Attribute getFieldAttribute(String name) {
		int index = fieldAttributeList.indexOf(new Attribute(name));
		if(index==-1)
			return null;
		else
			return fieldAttributeList.get(index);
	}

	public void setFieldAttributeList(List<Attribute> fieldAttributeList) {
		this.fieldAttributeList = fieldAttributeList;
	}


	public List<Attribute> getPart_of_speechAttributeList() {
		return part_of_speechAttributeList;
	}


	public void setPart_of_speechAttributeList(
			List<Attribute> part_of_speechAttributeList) {
		this.part_of_speechAttributeList = part_of_speechAttributeList;
	}


	public String getLanguage() {
		return language;
	}


	public void setLanguage(String language) {
		this.language = language;
	}





	public double getIdf_doc() {
		return idf_doc;
	}

	public void setIdf_doc(double idf_doc) {
		this.idf_doc = idf_doc;
	}

	public double getIdf_source() {
		return idf_source;
	}

	public void setIdf_source(double idf_source) {
		this.idf_source = idf_source;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getNgram_length() {
		return ngram_length;
	}

	public void setNgram_length(int ngram_length) {
		this.ngram_length = ngram_length;
	}

	public String getName_entity() {
		return name_entity;
	}

	public void setName_entity(String name_entity) {
		this.name_entity = name_entity;
	}




//	@Override
	public String toString2() {
		return "Feature [value=" + value  
				+ ", doc_frequency=" + doc_frequency + ", source_frequency=" + source_frequency + ", language=" + language + ", ngram_length=" + ngram_length	+ ", name_entity=" + name_entity
				+ ", idf_source = " + this.idf_source + ", idf_doc = " + this.idf_doc
				+ ", fieldAttributeList="
				+ fieldAttributeList 
				+ ", part_of_speechAttributeList="
				+ part_of_speechAttributeList + "]";
	}
	
	@Override
	public String toString() {
		return "Feature [value=" + value + "]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Feature other = (Feature) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	public static Comparator<Feature> FeatureComparatorDocFreq   = new Comparator<Feature>() {

		@Override
		public int compare(Feature Feature1, Feature Feature2) {
			if( Feature1.doc_frequency > Feature2.doc_frequency)
				return -1;
			else if( Feature1.doc_frequency < Feature2.doc_frequency)
				return 1;
			else
				return 0;
		}
	};
	
	public static Comparator<Feature> FeatureComparatorWeight   = new Comparator<Feature>() {

		@Override
		public int compare(Feature Feature1, Feature Feature2) {
			if( Feature1.weight > Feature2.weight)
				return -1;
			else if( Feature1.weight < Feature2.weight)
				return 1;
			else
				return 0;
		}
	};

}

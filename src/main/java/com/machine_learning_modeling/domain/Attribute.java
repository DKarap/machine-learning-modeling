package com.machine_learning_modeling.domain;

public class Attribute {

	private final String name;//title,content or noun, noun phrase
	public long total_frequency;
	public long doc_frequency;
	
	
	
	
	public Attribute(String name, long total_frequency, long doc_frequency) {
		super();
		this.name = name;
		this.total_frequency = total_frequency;
		this.doc_frequency = doc_frequency;
	}


	public Attribute(String name) {
		super();
		this.name = name;
		this.doc_frequency = 1;
		this.total_frequency = 1;
	}
	
	
	public String getName() {
		return name;
	}
	public long getTotal_frequency() {
		return total_frequency;
	}
	public void setTotal_frequency(long total_frequency) {
		this.total_frequency = total_frequency;
	}
	public long getDoc_frequency() {
		return doc_frequency;
	}
	public void setDoc_frequency(long doc_frequency) {
		this.doc_frequency = doc_frequency;
	}
	public void increaseTotal_frequency(long increaseValue) {
		this.total_frequency = this.total_frequency + increaseValue;
	}	
	public void increaseDoc_frequency(long increaseValue) {
		this.doc_frequency = this.doc_frequency + increaseValue;
	}

	

	@Override
	public String toString() {
		return "Attribute [name=" + name + ", total_frequency="
				+ total_frequency + ", doc_frequency=" + doc_frequency + "]";
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Attribute other = (Attribute) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}	
	
	
	
	
}

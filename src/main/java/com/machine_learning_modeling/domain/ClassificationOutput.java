package com.machine_learning_modeling.domain;


public class ClassificationOutput {

	
	final String classOfLine;
	final Double score;
	
	public ClassificationOutput(String classOfLine, Double score) {
		super();
		this.classOfLine = classOfLine;
		this.score = score;
	}

	public String getClassification() {
		return classOfLine;
	}

	public Double getScore() {
		return score;
	}

	@Override
	public String toString() {
		return "ClassificationOutput [classOfLine=" + classOfLine + ", score="
				+ score + "]";
	}
	
	
}

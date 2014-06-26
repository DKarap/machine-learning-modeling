package com.machine_learning_modeling.domain.clustering;

import java.util.Comparator;

/**
 * This object is used as SIMILARITY MATRIX
 * @author mimis
 *
 */
public class DocumentsPairSimilarity {

	private final int doc_id_1;
	private final int doc_id_2;
	private final double similarity;
	


	public DocumentsPairSimilarity(int doc_id_1, int doc_id_2, double similarity) {
		super();
		this.doc_id_1 = doc_id_1;
		this.doc_id_2 = doc_id_2;
		this.similarity = similarity;
	}
	
	
	public int getDoc_id_1() {
		return doc_id_1;
	}
	
	public int getDoc_id_2() {
		return doc_id_2;
	}
	
	public double getSimilarity() {
		return similarity;
	}
	
	public boolean containsDoc(int doc_id){
		if(doc_id_1 == doc_id || doc_id_2 == doc_id)
			return true;
		else
			return false;
	}
	
	public static Comparator<DocumentsPairSimilarity> DocumentSimilarityComparator   = new Comparator<DocumentsPairSimilarity>() {

		@Override
		public int compare(DocumentsPairSimilarity documentsPairSimilarity1, DocumentsPairSimilarity documentsPairSimilarity2) {
			if( documentsPairSimilarity1.getSimilarity() > documentsPairSimilarity2.getSimilarity())
				return -1;
			else if( documentsPairSimilarity1.getSimilarity() < documentsPairSimilarity2.getSimilarity())
				return 1;
			else
				return 0;
		}
	};



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + doc_id_1;
		result = prime * result + doc_id_2;
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
		DocumentsPairSimilarity other = (DocumentsPairSimilarity) obj;
		if (doc_id_1 != other.doc_id_1)
			return false;
		if (doc_id_2 != other.doc_id_2)
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DocumentsPairSimilarity [doc_id_1=" + doc_id_1 + ", doc_id_2="
				+ doc_id_2 + ", similarity=" + similarity + "]";
	}
	
	
	
	
}

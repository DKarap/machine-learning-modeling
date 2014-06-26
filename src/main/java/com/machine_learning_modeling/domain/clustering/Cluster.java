package com.machine_learning_modeling.domain.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.machine_learning_modeling.domain.Feature;

public class Cluster {

	private final int id; //this is the seed doc id that this cluster is build on..
	private Set<Integer> doc_ids_set;
	private String label;
	private List<Feature> feature_vector;
	
	public Cluster(int id) {
		super();
		this.id = id;
		this.doc_ids_set = new HashSet<Integer>();
		this.doc_ids_set.add(id);
		this.feature_vector = new ArrayList<Feature>();
	}

	
	
	
	public List<Feature> getFeature_vector() {
		return feature_vector;
	}




	public void setFeature_vector(List<Feature> feature_vector) {
		this.feature_vector = feature_vector;
	}




	public String getLabel() {
		return label;
	}



	public void setLabel(String label) {
		this.label = label;
	}



	public int getNumberOfDocsInCluster(){
		return this.doc_ids_set.size();
	}
	
	public int getId() {
		return id;
	}


	public void addDocIdsToCluster(Collection<Integer> document_ids_collection){
		this.doc_ids_set.addAll(document_ids_collection);
	}

	public Set<Integer> getDoc_ids_set() {
		return doc_ids_set;
	}

	public void setDoc_ids_set(Set<Integer> doc_ids_set) {
		this.doc_ids_set = doc_ids_set;
	}
	
	public void addDoc_id_to_cluster(Integer doc_id) {
		this.doc_ids_set.add(doc_id);
	}

	@Override
	public String toString() {
		return "Cluster [doc_ids_set=" + doc_ids_set + "]";
	}
	
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((doc_ids_set == null) ? 0 : doc_ids_set.hashCode());
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
		Cluster other = (Cluster) obj;
		if (doc_ids_set == null) {
			if (other.doc_ids_set != null)
				return false;
		} else if (!doc_ids_set.equals(other.doc_ids_set))
			return false;
		return true;
	}
	
	
	public static Comparator<Cluster> ClusterComparatorSize   = new Comparator<Cluster>() {

		@Override
		public int compare(Cluster Cluster1, Cluster Cluster2) {
			if( Cluster1.getNumberOfDocsInCluster() > Cluster2.getNumberOfDocsInCluster())
				return -1;
			else if( Cluster1.getNumberOfDocsInCluster() < Cluster2.getNumberOfDocsInCluster())
				return 1;
			else
				return 0;
		}
	};

	
	
}

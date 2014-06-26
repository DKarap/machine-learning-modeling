package com.machine_learning_modeling.core;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.machine_learning_modeling.domain.clustering.Cluster;

/**
 * Unit test for simple App.
 */
public class ClusteringTest extends TestCase {
	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ClusteringTest(String testName) {
		super(testName);
	}

	
	
	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ClusteringTest.class);
	}

	
	
	/**
	 */
	public void testClustering()  {

		/*
		 * test duplication of clusters
		 */
    	Set<Cluster> clusterList = new HashSet<Cluster>();
		Cluster currentCluster = new Cluster(1);
		currentCluster.addDoc_id_to_cluster(1);
		currentCluster.addDoc_id_to_cluster(2);
		clusterList.add(currentCluster);
		
		Cluster currentCluster2 = new Cluster(2);
		currentCluster2.addDoc_id_to_cluster(2);
		currentCluster2.addDoc_id_to_cluster(1);
		clusterList.add(currentCluster2);
		
		assertEquals("Duplicate clusters...", 1, clusterList.size());
	}

}

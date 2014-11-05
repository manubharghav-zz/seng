import java.util.ArrayList;
import java.util.Collections;



/**
 * All query operators return QryResult objects. QryResult objects encapsulate
 * the inverted lists (InvList) produced by QryopIl query operators and the
 * score lists (ScoreList) produced by QryopSl query operators. QryopIl query
 * operators populate the invertedList and and leave the docScores empty.
 * QryopSl query operators leave the invertedList empty and populate the
 * docScores. Encapsulating the two types of Qryop results in a single class
 * makes it easy to build structured queries with nested query operators.
 * 
 * Copyright (c) 2014, Carnegie Mellon University. All Rights Reserved.
 */

public class QryResult {

	// Store the results of different types of query operators.

	ScoreList docScores = new ScoreList();
	InvList invertedList = new InvList();

	class Item implements Comparable<Item> {
		
		// this is the external doc id from the index
		private String docid;
		private double score;

		private Item(String docid, double score) {
			this.docid = docid;
			this.score = score;
		}

		@Override
		public int compareTo(Item e) {
			int i = Double.compare(e.score, this.score);
			if (i != 0) {
				return i;
			} else {
				try {
					return this.docid.compareTo(e.docid);
				} catch (Exception exception) {
					System.err
							.println("Error while obtaining the external DocId from the index in ScoreList CompareTo method"
									+ exception);
				}
				return i;
			}

		}
		
		public String getDocid(){
	    	return this.docid;
	    }
	    
	    public double getScore(){
	    	return this.score;
	    }
	}

	/*
	 * Sort the matching documents by their scores, in descending order. The
	 * external document id should be a secondary sort key (i.e., for breaking
	 * ties). Smaller ids should be ranked higher (i.e. ascending order).
	 */
	public ArrayList<Item> sortResults() {
		long time = System.currentTimeMillis();
		
//		System.out.println("results size " + docScores.scores.size());
		ArrayList<Item> scoreEntries = new ArrayList<Item>();
		for (ScoreList.ScoreListEntry entry : docScores.scores) {
			try{
			scoreEntries.add(new Item(
					QryEval.getExternalDocid(entry.getDocid()), entry
							.getScore()));
			}
			catch(Exception e){
				System.err.println("Error while fetching docId for " + entry.getDocid() + "  in sortResults in queryResult" );
			}
		}
		Collections.sort(scoreEntries);
		
		System.out.println("results_sorting " + (System.currentTimeMillis() - time) );
		return scoreEntries;
	}

}

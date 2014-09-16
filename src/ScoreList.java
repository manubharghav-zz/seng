/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;

public class ScoreList {

  //  A little utilty class to create a <docid, score> object.

  protected class ScoreListEntry implements Comparable<ScoreListEntry> {
    private int docid;
    private double score;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
    
    
    	/*
		 * The scorelist entry class impements the comparable interface so that
		 * it becomes easy on later to sort the result set.
		 */
	@Override
	public int compareTo(ScoreListEntry entry) {
		// TODO verify the sort order for this compareTo method in the case
		// of comparing scores.Already verified for names but not for scores
		int i = Double.compare(entry.score,this.score);
		if(i!=0){
			return i;
		}
		else{
			try{
				return QryEval.getExternalDocid(this.docid).compareTo(QryEval.getExternalDocid(entry.docid));
			}
			catch(Exception e){
				System.out.println("Error while obtaining the external DocId from the index in ScoreList CompareTo method" + e);
			}
			return i;
		}
		
	}
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

}

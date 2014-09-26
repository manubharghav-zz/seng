/**
 *  This class implements the SUM operator for all BM25 models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlSum extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlSum(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   *  @throws IOException
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluates the query operator, including any child operators and
   *  returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelBM25){
      return (evaluateBM25(r));
    }
    return null;
  }

  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
	public QryResult evaluateBM25(RetrievalModel r) throws IOException {

		// Initialization

		allocDaaTPtrs(r);

		QryResult result = new QryResult();
		if(this.daatPtrs.size()==0){
			return result;
		}
		int crtDocId = Integer.MAX_VALUE;
		double score=0.0;
		Set<Integer> currentIDListIndex  = new HashSet<Integer>();// list of all those docpts which contain the minimum id.
		Set<Integer> completedListIndex = new HashSet<Integer>(); // list of all those docptrs which have been exhausted(completely parsed).
		
		
		EVALUATEDOCUMENTS:
		while(true){
			// identify the minimum current id. and keep track of the all those
			// docptr which have the minimum id in their lists. So that you
			// could just update all those lists in one go.
			for(int i=0;i<this.daatPtrs.size();i++){
				
				DaaTPtr ptrj = this.daatPtrs.get(i);
				if (completedListIndex.contains(i)  || ptrj.nextDoc >= ptrj.scoreList.scores.size()){
					completedListIndex.add(i);
					if(completedListIndex.size()==this.daatPtrs.size()){
						break EVALUATEDOCUMENTS;
					}
					continue;
				}
				if(ptrj.scoreList.getDocid(ptrj.nextDoc)<crtDocId){
					
					crtDocId = ptrj.scoreList.getDocid(ptrj.nextDoc);
//					System.out.println(QryEval.getExternalDocid(crtDocId));
					currentIDListIndex.clear();
					currentIDListIndex.add(i);
					score = ptrj.scoreList.getDocidScore(ptrj.nextDoc);
				}
				else if(ptrj.scoreList.getDocid(ptrj.nextDoc)==crtDocId){
					currentIDListIndex.add(i);
					score = score + ptrj.scoreList.getDocidScore(ptrj.nextDoc);
					
				}
			}
			result.docScores.add(crtDocId, score);
			for(Integer j:currentIDListIndex){
				this.daatPtrs.get(j).nextDoc++;
			}
			currentIDListIndex.clear();
			crtDocId = Integer.MAX_VALUE;
			
		}
		freeDaaTPtrs();
		return result;
	}

  /*
   *  Calculate the default score for the specified document if it
   *  does not match the query operator.  This score is 0 for many
   *  retrieval models, but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#OR( " + result + ")");
  }
}

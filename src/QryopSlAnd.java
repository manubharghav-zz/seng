/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;





public class QryopSlAnd extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlAnd(Qryop... q) {
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

	  if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean){
	      return (evaluateBoolean (r));
	  }
	  else if(r instanceof RetrievalModelIndri){
		  return (evaluateIndri(r));
	  }
	    return null;
  }
  
  /**
   *  Evaluates the query operator for indri retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public int getSmallestCurrentDocid() {

	  int nextDocid = Integer.MAX_VALUE;

	  for (int i = 0; i < this.daatPtrs.size(); i++) {
		  DaaTPtr ptri = this.daatPtrs.get(i);
		  if (nextDocid > ptri.invList.getDocid(ptri.nextDoc))
			  nextDocid = ptri.invList.getDocid(ptri.nextDoc);
	  }

	  return (nextDocid);
  }
  private QryResult evaluateIndri(RetrievalModel r) throws IOException {

	  try {
		  // Initialization

		  allocDaaTPtrs(r);

		  QryResult result = new QryResult();
		  if (this.daatPtrs.size() == 0) {
			  return result;
		  }
		  int crtDocId = Integer.MAX_VALUE;
		  double score = 0.0;
		  Set<Integer> currentIDListIndex = new HashSet<Integer>();// list of
		  // all
		  // those
		  // docpts
		  // which
		  // contain
		  // the
		  // minimum
		  // id.
		  Set<Integer> completedListIndex = new HashSet<Integer>(); // list of
		  // all
		  // those
		  // docptrs
		  // which
		  // have
		  // been
		  // exhausted(completely
		  // parsed).
		  
		  EVALUATEDOCUMENTS: while (true) {
			  // identify the minimum current id. and keep track of the all
			  // those
			  // docptr which have the minimum id in their lists. So that you
			  // could just update all those lists in one go.
			  for (int i = 0; i < this.daatPtrs.size(); i++) {

				  DaaTPtr ptrj = this.daatPtrs.get(i);
				  if (completedListIndex.contains(i)
						  || ptrj.nextDoc >= ptrj.scoreList.scores.size()) {
					  completedListIndex.add(i);
					  if (completedListIndex.size() == this.daatPtrs.size()) {
						  break EVALUATEDOCUMENTS;
					  }
					  continue;
				  }
				  if (ptrj.scoreList.getDocid(ptrj.nextDoc) < crtDocId) {

					  crtDocId = ptrj.scoreList.getDocid(ptrj.nextDoc);
					  currentIDListIndex.clear();
					  currentIDListIndex.add(i);
				  } else if (ptrj.scoreList.getDocid(ptrj.nextDoc) == crtDocId) {
					  currentIDListIndex.add(i);
				  }
			  }
//			  int minDocid = getSmallestCurrentDocid();
			  if(QryEval.getExternalDocid(crtDocId).equals("clueweb09-enwp03-57-00556")){
				  System.out.println("manu");
			  }
			  score = 1.0;
			  for (int i = 0; i < this.daatPtrs.size(); i++) {
				  if (currentIDListIndex.contains(i)) {

					  score = score
							  * Math.pow(
									  this.daatPtrs.get(i).scoreList
									  .getDocidScore(this.daatPtrs
											  .get(i).nextDoc),
											  1.0 / args.size());
					  this.daatPtrs.get(i).nextDoc++;

				  } else {
					  QryopSl opSL = (QryopSl) this.args.get(i);
					  score = score
							  * Math.pow(opSL.getDefaultScore(r, crtDocId),
									  1.0 / args.size());
				  }
			  }

			  result.docScores.add(crtDocId, score);
			  currentIDListIndex.clear();
			  crtDocId = Integer.MAX_VALUE;

		  }
		  freeDaaTPtrs();
		  return result;
	  } catch (Exception e) {
		  System.out.println("Exception in Indir" + e);
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
  public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

    //  Initialization

    allocDaaTPtrs (r);
    QryResult result = new QryResult ();
    
    // a null check to terminate execution.
	if(this.daatPtrs.size()==0){
		return result;
	}

    //  Sort the arguments so that the shortest lists are first.  This
    //  improves the efficiency of exact-match AND without changing
    //  the result.

    for (int i=0; i<(this.daatPtrs.size()-1); i++) {
      for (int j=i+1; j<this.daatPtrs.size(); j++) {
	if (this.daatPtrs.get(i).scoreList.scores.size() >
	    this.daatPtrs.get(j).scoreList.scores.size()) {
	    ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
	    this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
	    this.daatPtrs.get(j).scoreList = tmpScoreList;
	}
      }
    }

    //  Exact-match AND requires that ALL scoreLists contain a
    //  document id.  Use the first (shortest) list to control the
    //  search for matches.

    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.

    DaaTPtr ptr0 = this.daatPtrs.get(0);

    EVALUATEDOCUMENTS:
    for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

      int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
      double docScore = ptr0.scoreList.getDocidScore(ptr0.nextDoc);

      for (int j=1; j<this.daatPtrs.size(); j++) {

	DaaTPtr ptrj = this.daatPtrs.get(j);

	while (true) {
	  if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
	    break EVALUATEDOCUMENTS;		// No more docs can match
	  else
	    if (ptrj.scoreList.getDocid (ptrj.nextDoc) > ptr0Docid)
	      continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
	  else
	    if (ptrj.scoreList.getDocid (ptrj.nextDoc) < ptr0Docid)
	      ptrj.nextDoc ++;			// Not yet at the right doc.
	  else {
		  // this ensures that the and score computation happens independant of the choice of the retrieval model.
		  docScore = Math.min(docScore, ptrj.scoreList.getDocidScore(ptrj.nextDoc));
	      break;// ptrj matches ptr0Docid
	  }
	}
      }

      //  The ptr0Docid matched all query arguments, so save it.

      result.docScores.add (ptr0Docid, docScore);
    }

    freeDaaTPtrs ();

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

	  double score = 1.0;
		if (r instanceof RetrievalModelIndri){
			for (Qryop arg : args) {
				if (arg instanceof QryopSl) {
					QryopSl SlOp = (QryopSl) arg;
					score = score*SlOp.getDefaultScore(r, docid);
				}
			}
		}
		
		

		return Math.pow(score, 1.0/args.size());
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#AND( " + result + ")");
  }
}

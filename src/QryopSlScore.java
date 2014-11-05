/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	
	
	// storing the fields to id the computation of the default scores for documents in the indri retreival models.
	private int ctf;
	private String field;
	private DocLengthStore s;
	private long N;
	private double pMle;

	/**
	 * Construct a new SCORE operator. The SCORE operator accepts just one
	 * argument.
	 * 
	 * @param q
	 *            The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Construct a new SCORE operator. Allow a SCORE operator to be created with
	 * no arguments. This simplifies the design of some query parsing
	 * architectures.
	 * 
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() {
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param q
	 *            The query argument to append.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (evaluateBoolean(r));
		else if (r instanceof RetrievalModelRankedBoolean) {
			return (evaluateRankedBoolean(r));
		} else if (r instanceof RetrievalModelBM25) {
			return evaluateBM25(r);
		}
		else if(r instanceof RetrievalModelIndri){
			return evaluateIndri(r);
		}
		return null;
	}
	
	
	public QryResult evaluateIndri(RetrievalModel r) throws IOException{
		RetrievalModelIndri model = (RetrievalModelIndri) r;
		QryResult result = args.get(0).evaluate(model);
		if(s==null){
			this.s = new DocLengthStore(QryEval.READER);
		}
		if (result.docScores.scores.size() == result.invertedList.df) {
			result.docScores.scores.clear();
		}
		
		
		//store the relevant information in the score query operator class.
		this.ctf = result.invertedList.ctf;
		this.field = result.invertedList.field;
		this.N = QryEval.READER.getSumTotalTermFreq(this.field);

		
		
//		long N = QryEval.READER.getSumTotalTermFreq(this.field);
		double pMleTerm = ((double)this.ctf)/(N);
		
		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Indri. All matching documents get a score of 1.0.
			double tf_td = (float) result.invertedList.getTf(i);
			double score =  ((1-model.lambda)*pMleTerm)  + (model.lambda * ((tf_td +  model.mu * ( pMleTerm)) / (s.getDocLength(
							result.invertedList.field,
							result.invertedList.getDocid(i)) + model.mu)) ) ;
//			System.out.println("em " + QryEval.getExternalDocid(result.invertedList.postings.get(i).docid));
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) score);
			
		}
		
		
		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
		
	}

	/**
	 * Evaluate the query operator for okapi bm25 models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */

	private QryResult evaluateBM25(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		RetrievalModelBM25 model = (RetrievalModelBM25) r;
		QryResult result = args.get(0).evaluate(r);
		DocLengthStore s = new DocLengthStore(QryEval.READER);
		if (result.docScores.scores.size() == result.invertedList.df) {
			result.docScores.scores.clear();
		}

		double tf_td;
		double tf_q = 1.0;
		double tf_weight;
		double user_weight;
		double RSF;
		double N = (double) QryEval.READER.numDocs();

		// parameters for the bm25 model.
		double k_1 = model.k_1;
		double k_3 = model.k_3;
		double b = model.b;
		double avg_doclen = QryEval.READER
				.getSumTotalTermFreq(result.invertedList.field)
				/ (float) QryEval.READER.getDocCount(result.invertedList.field);
		double score;

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.
		System.out.println(result.invertedList.df);
		RSF = Math.log((0.5 + N - result.invertedList.df)
				/ ((result.invertedList.df) + 0.5));
		user_weight = ((k_3 + 1.0) * tf_q) / (k_3 + tf_q);
//		System.out.println(user_weight);
		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			if(QryEval.getExternalDocid(result.invertedList.getDocid(i)).equals("clueweb09-en0001-18-32681")){
				System.out.println("manu");
			}
			tf_td = (double) result.invertedList.getTf(i);
			// System.out.println("ter_frequency" +tf_td);

			tf_weight = (tf_td)
					/ (tf_td + (k_1 * ((1.0 - b) + (b * (s.getDocLength(
							result.invertedList.field,
							result.invertedList.getDocid(i)) / avg_doclen)))));

			score = (RSF) * (tf_weight) * (user_weight);
			// System.out.println(score);
			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) score);

		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for ranked boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */

	private QryResult evaluateRankedBoolean(RetrievalModel r)
			throws IOException {
		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		if (result.docScores.scores.size() == result.invertedList.df) {
			result.docScores.scores.clear();
		}

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			// if(!QryEval.getExternalDocid(result.invertedList.postings.get(i).docid).equals("clueweb09-en0009-25-22037")){
			//
			// continue;
			// }
			if (r instanceof RetrievalModelRankedBoolean) {
				result.docScores.add(result.invertedList.postings.get(i).docid,
						(float) result.invertedList.postings.get(i).tf);
			}

		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		if (result.docScores.scores.size() == result.invertedList.df) {
			result.docScores.scores.clear();
		}

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.
			if (r instanceof RetrievalModelUnrankedBoolean) {
				result.docScores.add(result.invertedList.postings.get(i).docid,
						(float) 1.0);
			}

		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/*
	 * Calculate the default score for a document that does not match the query
	 * argument. This score is 0 for many retrieval models, but not all
	 * retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO enquire whether the default score for ranked boolean score is
		// also 0.0
		if (r instanceof RetrievalModelIndri) {
			RetrievalModelIndri model = (RetrievalModelIndri) r;
//			long N = QryEval.READER.getSumTotalTermFreq(this.field);
			double pMleTerm = ((double)this.ctf)/(N);
//			double term1 = (1 - model.lambda) * pMleTerm;
//			double term2 = (model.lambda * model.mu * pMleTerm);
//			double term3 = (this.s.getDocLength(this.field,(int) docid) + model.mu);
//			
//			double score = term1 + (term2/term3);
			
			double score = ((1 - model.lambda) * pMleTerm)
					+ ((model.lambda * model.mu * pMleTerm) / (this.s.getDocLength(this.field,(int) docid) + model.mu));
//			
			return score;
		}

		return 0.0;
	}

	/**
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}
}

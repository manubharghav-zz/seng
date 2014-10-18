/**
 *  This class implements the SYN operator for all retrieval models.
 *  The synonym operator creates a new inverted list that is the union
 *  of its constituents.  Typically it is used for morphological or
 *  conceptual variants, e.g., #SYN (cat cats) or #SYN (cat kitty) or
 *  #SYN (astronaut cosmonaut).
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopIlWindow extends QryopIl {

	// this variable stores the n parameter for the near operator
	private int n = 1;

	// Constructer to set the n parameter.
	public QryopIlWindow(int n) {
		this.n = n;
	}

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	 */
	public QryopIlWindow(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param {q} q The query argument (query operator) to append.
	 * @return void
	 * @throws IOException
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/*
	 * Takes in 2 position lists and returns the entries from list 2 which are
	 * less than n away from the corresponding terms in list 1
	 */
	private Vector<Integer> tmp(ArrayList<Vector<Integer>> documents) {
		
		Vector<Integer> result = new Vector<Integer>();
		ArrayList<ListIterator<Integer> >  iters = new ArrayList<ListIterator<Integer>>(documents.size());
		for(int i=0;i<documents.size();i++){
			iters.add(documents.get(i).listIterator());
		}
		MANU:while(true){
			//get the minimum.  and the mximum 
			
			int min_pos=Integer.MAX_VALUE;
			int max_pos = Integer.MIN_VALUE;
			int min_doc_index=0;
			int max_doc_index=0;
			
			for(int i=0;i<iters.size();i++){
				ListIterator<Integer> it = iters.get(i);
				int element = it.next();
				it.previous();
				if(element<min_pos){
					min_pos = element;
					min_doc_index=i;
				}
				
				if(element>max_pos){
					max_pos  = element;
					max_doc_index= i;
				}
			}
			
			if(1+max_pos - min_pos <= this.n){
				result.add(max_pos);
				for(ListIterator<Integer> it:iters){
					it.next();
					if(!it.hasNext()){
						break MANU;
					}
				}
			}
			else{
				iters.get(min_doc_index).next();
				if(!iters.get(min_doc_index).hasNext()){
					break MANU;
				}
			}
			
			
			// check and update
			
			
			
		}		
		return result;		
	}

	/**
	 * Evaluates the query operator, including any child operators and returns
	 * the result.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		// Initialization

		allocDaaTPtrs(r);
		syntaxCheckArgResults(this.daatPtrs);

		QryResult result = new QryResult();
		if (this.daatPtrs.size() == 0) {
			return result;
		}
		result.invertedList.field = new String(
				this.daatPtrs.get(0).invList.field);

		// Each pass of the loop adds 1 document to result until all of
		// the inverted lists are depleted. When a list is depleted, it
		// is removed from daatPtrs, so this loop runs until daatPtrs is empty.

		// This implementation is intended to be clear. A more efficient
		// implementation would combine loops and use merge-sort.

		// doing a null check.
		Iterator<DaaTPtr> iter = this.daatPtrs.iterator();
		while (iter.hasNext()) {
			if (iter.next().invList.df == 0) {
				iter.remove();
			}
		}

		DaaTPtr ptr0 = this.daatPtrs.get(0);
		ArrayList<Vector<Integer>> documents = new ArrayList<Vector<Integer>>();

		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {
			documents.clear();
			int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);
//			System.out.println("processing doc: " + QryEval.getExternalDocid(ptr0Docid));
			// get positions.
			Vector<Integer> posting = ptr0.invList.postings.get(ptr0.nextDoc).positions;
			documents.add(posting);

			for (int j = 1; j < this.daatPtrs.size(); j++) {

				DaaTPtr ptrj = this.daatPtrs.get(j);

				while (true) {
					if (ptrj.nextDoc >= ptrj.invList.postings.size()) {
						break EVALUATEDOCUMENTS; // No more docs can match
					} else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
					// match.
					else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						Vector<Integer> postingForPtrJ = ptrj.invList.postings
								.get(ptrj.nextDoc).positions;
						documents.add(postingForPtrJ);
						break;

					}
				}
			}
			
			if(documents.size() == this.daatPtrs.size()){
				// only if all the inverted lists containing a a specific document.
				Vector<Integer> out = tmp(documents);
//				documents.clear();
				if(out.size()!=0){
					result.invertedList.appendPosting(ptr0Docid, out);
				}
				
//				System.out.println("doc id  in window : " + result.invertedList.df   +   "   " + QryEval.getExternalDocid(ptr0Docid));
				if (r instanceof RetrievalModelRankedBoolean) {
					result.docScores.add(ptr0Docid, posting.size());
				} else if (r instanceof RetrievalModelUnrankedBoolean) {
					result.docScores.add(ptr0Docid, (float) 1.0);
				}
				
				
			}
//			else{
//				documents.clear();
//			}
			// The ptr0Docid matched all query arguments, so save it.
//			result.invertedList.appendPosting(ptr0Docid, posting);
//			if (r instanceof RetrievalModelRankedBoolean) {
//				result.docScores.add(ptr0Docid, posting.size());
//			} else if (r instanceof RetrievalModelUnrankedBoolean) {
//				result.docScores.add(ptr0Docid, (float) 1.0);
//			}

		}

		freeDaaTPtrs();

		return result;
	}

	/**
	 * Return the smallest unexamined docid from the DaaTPtrs.
	 * 
	 * @return The smallest internal document id.
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

	/**
	 * syntaxCheckArgResults does syntax checking that can only be done after
	 * query arguments are evaluated.
	 * 
	 * @param ptrs
	 *            A list of DaaTPtrs for this query operator.
	 * @return True if the syntax is valid, false otherwise.
	 */
	public Boolean syntaxCheckArgResults(List<DaaTPtr> ptrs) {

		for (int i = 0; i < this.args.size(); i++) {

			if (!(this.args.get(i) instanceof QryopIl))
				QryEval.fatalError("Error:  Invalid argument in "
						+ this.toString());
			else if ((i > 0)
					&& (!ptrs.get(i).invList.field
							.equals(ptrs.get(0).invList.field)))
				QryEval.fatalError("Error:  Arguments must be in the same field:  "
						+ this.toString());
		}

		return true;
	}

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#Window( " + result + ")");
	}
}

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

public class QryopIlNear extends QryopIl {

	// this variable stores the n parameter for the near operator
	private int n = 1;

	// Constructer to set the n parameter.
	public QryopIlNear(int n) {
		this.n = n;
	}

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	 */
	public QryopIlNear(Qryop... q) {
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
	private Vector<Integer> tmp(Vector<Integer> l1, Vector<Integer> l2) {
		Vector<Integer> result = new Vector<Integer>();
		if (l1.size() == 0 || l2.size() == 0) {
			return new Vector<Integer>(result);
		}

		int index_l1 = 0;
		int index_l2 = 0;
		while (index_l2 < l2.size() && index_l1 < l1.size()) {
			if (l2.get(index_l2) == 3101) {
				System.out.println();
			}
			if (l2.get(index_l2) < l1.get(index_l1)) {
				index_l2++;
			} else if (l2.get(index_l2) - l1.get(index_l1) <= this.n) {
				result.add(l2.get(index_l2));
				index_l2++;
				index_l1++;
			} else if ((l2.get(index_l2) - l1.get(index_l1) > n)) {
				index_l1++;
			}

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

		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);
			// get positions.
			Vector<Integer> posting = ptr0.invList.postings.get(ptr0.nextDoc).positions;

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
						posting = tmp(posting, postingForPtrJ);
						if (posting.size() == 0) {
							continue EVALUATEDOCUMENTS;
						}
						break;

					}
				}
			}

			// The ptr0Docid matched all query arguments, so save it.
			result.invertedList.appendPosting(ptr0Docid, posting);
			if (r instanceof RetrievalModelRankedBoolean) {
				result.docScores.add(ptr0Docid, posting.size());
			} else if (r instanceof RetrievalModelUnrankedBoolean) {
				result.docScores.add(ptr0Docid, (float) 1.0);
			}

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

		return ("#SYN( " + result + ")");
	}
}

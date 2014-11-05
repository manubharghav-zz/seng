import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

class NoQueryExpansionForThisModel extends Exception {
	public NoQueryExpansionForThisModel(String message) {
		super(message);
	}
}

public class IndriQueryExpansion {
	long time = 0;

	RetrievalModelIndri model;
	Map<Integer, ScoreList> map = new HashMap<Integer, ScoreList>();
	DocLengthStore docStore;
	long N;

	public RetrievalModelIndri getModel() {
		return model;
	}

	public void setModel(RetrievalModel model)
			throws NoQueryExpansionForThisModel, IOException {
		this.N = QryEval.READER.getSumTotalTermFreq("body");
		this.docStore = new DocLengthStore(QryEval.READER);
		if (model instanceof RetrievalModelIndri) {
			this.model = (RetrievalModelIndri) model;
		} else {
			System.out
					.println("Wrong model. Query expansion not developed for this model");
			throw new NoQueryExpansionForThisModel(
					"Error here mate. Please look into it");
		}
	}
	
	private static int binarySearchForTerms(String[] a,String key) {
		int low = 1;
		int high = a.length-1;

		while (low <= high && low> 0) {
			int mid = (low + high) >>> 1;
			String midVal = a[mid];
			int cmp = midVal.compareTo(key);

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	public QryResult evaluateQuery(Qryop query, int QueryId,
			BufferedWriter expandedQueryWriter) throws Exception {
		ScoreList initialResult;

		if (model.fbInitialRankingFile == null) {
			initialResult = new ScoreList();
			ArrayList<QryResult.Item> list = query.evaluate(model)
					.sortResults();
			for (int i = 0; i < model.fbDocs; i++) {
				initialResult.add(QryEval.getInternalDocid(list.get(i)
						.getDocid()), list.get(i).getScore());
			}

		} else {
			if (map.size() == 0) {
				System.out.println("loading from file");
				loadResultsFromFile();
			}
			initialResult = new ScoreList();
			ScoreList tmp = map.get(QueryId);
			for (int i = 0; i < model.fbDocs; i++) {
				initialResult.add(tmp.getDocid(i), tmp.getDocidScore(i));
			}

		}

		// we have our initial result now.
		/*
		 * 1)Extract the relevant terms.2)Compute the scores3)Take the top k
		 * terms.
		 */

		// 1,2

		Map<String, Double> termScores = new HashMap<String, Double>();
		for (ScoreList.ScoreListEntry entry : initialResult.scores) {
			int docId = entry.getDocid();
			//System.out.println(docId  + "  " + entry.getScore());
			TermVector tv = new TermVector(docId, "body");
			for (String stem : tv.stems) {
				if (stem == null || stem.contains(".") || stem.contains(",")) {
					continue;
				} else {
					termScores.put(stem, 0.0);
				}
			}
		}
		for (ScoreList.ScoreListEntry entry : initialResult.scores) {
			int docId = entry.getDocid();
			System.out.println("doc Id: " + docId);
			TermVector tv = new TermVector(docId, "body");
//			Set<String> termList = new HashSet<String>();
			
			for (String term : termScores.keySet()) {
//				System.out.println(term);
//				if(term.equals("2")){
//					System.out.println();
//				}
				int index = binarySearchForTerms(tv.stems, term);
//				System.out.println(index);
				if(index<0){
					long ctf = QryEval.READER.totalTermFreq(new Term("body", term));
					// calculate the deafult score and update it.
					Double term2 = Math.log((((double) this.N) / (double) ctf));
					// System.out.println(term + "    " + term2 + "   " + ctf);
					Double tmp = ((model.getFbMu() * ctf) / (this.N))
							/ (model.getFbMu() + this.docStore.getDocLength("body",
									docId));
					termScores.put(term, termScores.get(term)
							+ (term2 * tmp * entry.getScore()));
				}
				else{
					String termString = tv.stemString(index); // get the string for the
					
					double term2 = Math.log((((double) this.N) / (double) tv
							.totalStemFreq(index)));

					Double tmp = (tv.stemFreq(index) + (model.getFbMu()
							* ((double) QryEval.READER.totalTermFreq(new Term(
									"body", termString))) / (this.N)))
							/ (model.getFbMu() + this.docStore.getDocLength("body",
									docId));
					Double tmp2 = ((double) tv.stemFreq(index) / this.docStore
							.getDocLength("body", docId));
					termScores.put(termString, termScores.get(termString)
							+ (term2 * tmp * entry.getScore()));
					
				}
				
			}
			
			


		}

		// 3)

		ArrayList<Map.Entry<String, Double>> sortedSet = new ArrayList<Map.Entry<String, Double>>();
		sortedSet.addAll(termScores.entrySet());
		// Comparator<Map.Entry<String, Double>> NAME = (Map.Entry<String,
		// Double> o1, Map.Entry<String, Double> o2) ->
		// o1.getValue().compareTo(o2.getValue());
		Collections.sort(sortedSet,
				new Comparator<Map.Entry<String, Double>>() {
					public int compare(Map.Entry<String, Double> a,
							Map.Entry<String, Double> b) {
						return b.getValue().compareTo(a.getValue());
					}
				});

		QryopSlWand wandOperator = new QryopSlWand();

		for (int i = 0; i < model.fbTerms; i++) {
			// System.out.println(model.fbTerms);
			wandOperator.addWeights(sortedSet.get(i).getValue());
			wandOperator.add(new QryopIlTerm(sortedSet.get(i).getKey()));
		}

		expandedQueryWriter.write(QueryId + ": " + wandOperator.getQuery()
				+ "\n");
		QryopSlWand wandCombined = new QryopSlWand();

		wandCombined.addWeights(model.getFbOrigWeight());
		wandCombined.addWeights(1 - model.getFbOrigWeight());
		wandCombined.add(query);
		wandCombined.add(wandOperator);
		long time_start = System.currentTimeMillis();
		QryResult result = wandCombined.evaluate(model);
		System.out.println(System.currentTimeMillis() - time_start);
		return result;

	}

	private void loadResultsFromFile() throws NumberFormatException, Exception {
		BufferedReader input = new BufferedReader(new FileReader(
				model.getFbInitialRankingFile()));
		String s;
		while ((s = input.readLine()) != null) {
			String[] splits = s.split("\t");
			if (splits.length == 1) {
				splits = s.split(" ");
			}
			int qryId = Integer.parseInt(splits[0]);
			if (map.containsKey(qryId)) {
				map.get(qryId).add(QryEval.getInternalDocid(splits[2]),
						Double.parseDouble(splits[4]));
			} else {
				ScoreList scorelist = new ScoreList();
				scorelist.add(QryEval.getInternalDocid(splits[2]),
						Double.parseDouble(splits[4]));
				map.put(qryId, scorelist);
			}
		}

	}

}

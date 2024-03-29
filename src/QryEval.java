/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";

  //  The index file reader is accessible via a global variable. This
  //  isn't great programming style, but the alternative is for every
  //  query operator to store or pass this value, which creates its
  //  own headaches.

  public static IndexReader READER;


  //  Create and configure an English analyzer that will be used for
  //  query parsing.

  public static EnglishAnalyzerConfigurable analyzer =
      new EnglishAnalyzerConfigurable (Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  /**
   *  @param args The only argument is the path to the parameter file.
   *  @throws Exception
   */
  public static void main(String[] args) throws Exception {
    
	  

    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }
    BufferedWriter writer;
    BufferedWriter ExpandedQueryWriter=null;
    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();
    
    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }
    
    writer  = new BufferedWriter(new FileWriter(new File(params.get("trecEvalOutputPath"))));

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));
    RetrievalModel model = null;
    String defaultOp = null;
    String retrievalAlgorithm  = params.get("retrievalAlgorithm").toLowerCase();
    if(retrievalAlgorithm.equals("unrankedboolean")){
    	System.out.println("unranked Boolean");
    	defaultOp = "or";
    	model = new RetrievalModelUnrankedBoolean();
    }
    else if(retrievalAlgorithm.equals("rankedboolean")) {
    	defaultOp = "or";
    	System.out.println("ranked boolean");
    	model = new RetrievalModelRankedBoolean();
    }
    else if(retrievalAlgorithm.equals("bm25")) {
    	defaultOp = "sum";
    	System.out.println("bm25");
    	model = new RetrievalModelBM25();
    	model.setParameter("k_1", params.get("BM25:k_1"));
    	model.setParameter("b", params.get("BM25:b"));
    	model.setParameter("k_3", params.get("BM25:k_3"));
    }
    else if(retrievalAlgorithm.equals("indri")){
    	defaultOp = "and";
    	System.out.println("indri");
    	model = new RetrievalModelIndri();
    	RetrievalModelIndri indriModel = (RetrievalModelIndri) model;
    	model.setParameter("mu", params.get("Indri:mu"));
    	model.setParameter("lambda", params.get("Indri:lambda"));
    	//start reading fb paramenters.
    	if(params.containsKey("fb")){
    		indriModel.setFb(Boolean.parseBoolean(params.get("fb")));    		
    	}
    	if(params.containsKey("fbDocs")){
    		indriModel.setFbDocs(Integer.parseInt(params.get("fbDocs")));
    	}
    	if(params.containsKey("fbTerms")){
    		indriModel.setFbTerms(Integer.parseInt(params.get("fbTerms")));
    	}
    	if(params.containsKey("fbMu")){
    		indriModel.setFbMu(Double.parseDouble(params.get("fbMu")));
    	}
    	if(params.containsKey("fbOrigWeight")){
    		indriModel.setFbOrigWeight(Double.parseDouble(params.get("fbOrigWeight")));
    	}
    	if(params.containsKey("fbInitialRankingFile")){
    		indriModel.setFbInitialRankingFile(params.get("fbInitialRankingFile"));
    	}
    	if(params.containsKey("fbExpansionQueryFile")){
    		indriModel.setFbExpansionQueryFile(params.get("fbExpansionQueryFile"));
    		ExpandedQueryWriter = new BufferedWriter(new FileWriter(new File(params.get("fbExpansionQueryFile"))));
    	}
    }
    
    String QueryFilePath = params.get("queryFilePath");
    
    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    DocLengthStore s = new DocLengthStore(READER);
    

    BufferedReader input = new BufferedReader(new FileReader(QueryFilePath));
    String line2="";
    long time = System.currentTimeMillis();
	while ((line2 = input.readLine()) != null) {

		String[] splits = line2.split(":");
		String query_num = splits[0];
		String query = splits[1];
		System.out.println("Processing query: " + query_num);
		try {
			Qryop qTree;
			qTree = parseQuery(defaultOp, query);
			long time2 = System.currentTimeMillis();
			if (model instanceof RetrievalModelIndri
					&& params.containsKey("fb")
					&& params.get("fb").equals("true")) {
				IndriQueryExpansion expander = new IndriQueryExpansion();
				expander.setModel(model);
				printResultstoFile(query_num, expander.evaluateQuery(qTree, Integer.parseInt(query_num), ExpandedQueryWriter), writer);
			}
			else{
				printResultstoFile(query_num, qTree.evaluate(model), writer);
			}
			System.out.println("outer state " + (System.currentTimeMillis() - time2));
		} catch (Exception e) {
			System.out.println(e + " occured while processing " + line2);
		}

	}
    System.out.println("Time taken: " + (System.currentTimeMillis() - time));
    try{
    	input.close();
    	writer.close();
    	if(ExpandedQueryWriter!=null){
    		ExpandedQueryWriter.close();
    	}
    }
 catch (Exception e) {
			// hmm weird case. cant do anything now. If null pointer occurs the
			// writer could have been closed earlier somewhere or even worse
			// initialized.
		}

    // Later HW assignments will use more RAM, so you want to be aware
    // of how much memory your program uses.
    printMemoryUsage(true);
    }



  /**
   *  Write an error message and exit.  This can be done in other
   *  ways, but I wanted something that takes just one statement so
   *  that it is easy to insert checks without cluttering the code.
   *  @param message The error message to write before exiting.
   *  @return void
   */
  static void fatalError (String message) {
    System.err.println (message);
    System.exit(1);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id. If the internal id doesn't exists, returns null.
   *  
   * @param iid The internal document id of the document.
   * @throws IOException 
   */
	static String getExternalDocid (int iid) throws IOException {
    Document d = QryEval.READER.document (iid);
    String eid = d.get ("externalId");
    return eid;
  }

  /**
   *  Finds the internal document id for a document specified by its
   *  external id, e.g. clueweb09-enwp00-88-09710.  If no such
   *  document exists, it throws an exception. 
   * 
   * @param externalId The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid (String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));
    
    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1,false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    
    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }
  
  
public static Qryop getDefaultQueryOperator(String defaultOp){
	Qryop currentOp = null;
	if(defaultOp.equals("or")){
		currentOp = new QryopSlOr();
	}
	else if(defaultOp.equals("sum")){
		currentOp = new QryopSlSum();
	}
	else if(defaultOp.equals("and")){
		currentOp = new QryopSlAnd();
	}
	return currentOp;
}

  /**
   * parseQuery converts a query string into a query tree.
 * @param model 
   * 
   * @param qString
   *          A string containing a query.
   * @param qTree
   *          A query tree
   * @throws IOException
   */
  static Qryop parseQuery(String defaultOp, String qString) throws IOException {
	  

	
    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.

    qString = qString.trim();

    if (qString.charAt(0) != '#') {
    	qString = "#" + defaultOp +"(" + qString + ")";
    	
    }

    // Tokenize the query.

    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;

    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.

		while (tokens.hasMoreTokens()) {
			

			token = tokens.nextToken();
//			System.out.println(token);
			if (token.matches("[/ ,(\t\n\r]")) {
				// Ignore most delimiters.
			} else if (token.equalsIgnoreCase("#and")) {
				currentOp = new QryopSlAnd();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#wand")) {
				currentOp = new QryopSlWand();
				stack.push(currentOp);
				currentOp.hasWeights = true;
				currentOp.readweight=false;
			} else if (token.equalsIgnoreCase("#wsum")) {
				currentOp = new QryopSlWsum();
				stack.push(currentOp);
				currentOp.hasWeights = true;
				currentOp.readweight=false;
			} else if (token.equalsIgnoreCase("#or")) {
				currentOp = new QryopSlOr();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				currentOp = new QryopSlSum();
				stack.push(currentOp);
			} else if (token.toLowerCase().contains("#near")) {
				String[] splits = token.split("/");

				currentOp = new QryopIlNear(Integer.parseInt(splits[1]));
				stack.push(currentOp);
			} else if (token.toLowerCase().contains("#window")) {
				String[] splits = token.split("/");

				currentOp = new QryopIlWindow(Integer.parseInt(splits[1]));
				stack.push(currentOp);
			}
			else if (token.equalsIgnoreCase("#syn")) {
				currentOp = new QryopIlSyn();
				stack.push(currentOp);
			} else if (token.startsWith(")")) { // Finish current query
												// operator.
				// If the current query operator is not an argument to
				// another query operator (i.e., the stack is empty when it
				// is removed), we're done (assuming correct syntax - see
				// below). Otherwise, add the current operator as an
				// argument to the higher-level operator, and shift
				// processing back to the higher-level operator.
//				System.out.println(stack.size());
				stack.pop();

				if (stack.empty()){
					if(tokens.hasMoreTokens()){
						Qryop arg = currentOp;
						currentOp = getDefaultQueryOperator(defaultOp);
						currentOp.add(arg);
					}
					else{
						if (currentOp instanceof QryopIlNear
								|| currentOp instanceof QryopIlSyn) {
							Qryop arg = currentOp;
							currentOp = getDefaultQueryOperator(defaultOp);
							currentOp.add(arg);
							break;
						}
					}
				}
				else {

					Qryop arg = currentOp;
					currentOp = stack.peek();
					if(arg.args.size()>0){
						currentOp.add(arg);
					}
					else{
						if(currentOp.hasWeights){
							currentOp.weights.remove(currentOp.weights.size()-1);
						}
					}
					
					if(currentOp.hasWeights){
						currentOp.readweight = false;
					}
				}
			} else {

				// NOTE: You should do lexical processing of the token before
				// creating the query term, and you should check to see whether
				// the token specifies a particular field (e.g., apple.title).
				if (currentOp.hasWeights && !currentOp.readweight) {
					float weight = Float.parseFloat(token);
					currentOp.addWeights(weight);
					currentOp.readweight=true;
					continue;
				}
				
				StringTokenizer fields = new StringTokenizer(token, ".", false);
				String word = fields.nextToken();
				String[] tokenizedWord = tokenizeQuery(word);
				if (tokenizedWord.length > 0) {

					if (fields.hasMoreTokens()) {
						currentOp.add(new QryopIlTerm((tokenizedWord[0]),
								fields.nextToken()));
					} else {
						currentOp.add(new QryopIlTerm((tokenizedWord[0])));
					}
					
				}
				else{
					if(currentOp.hasWeights){
						currentOp.weights.remove(currentOp.weights.size()-1);
					}
				}
				if(currentOp.hasWeights){
					currentOp.readweight=false;
				}
				

			}
		}

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.

    if (tokens.hasMoreTokens()) {
    	System.err.println("Error while parsing query. Please loook");
    }
    
    if(currentOp instanceof QryopIlWindow || currentOp instanceof QryopIlSyn || currentOp instanceof QryopIlNear){
    	Qryop arg = currentOp;
    	currentOp = getDefaultQueryOperator(defaultOp);
		currentOp.add(arg);
    }

    return currentOp;
  }

  /**
   *  Print a message indicating the amount of memory used.  The
   *  caller can indicate whether garbage collection should be
   *  performed, which slows the program but reduces memory usage.
   *  @param gc If true, run the garbage collector before reporting.
   *  @return void
   */
  public static void printMemoryUsage (boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println ("Memory used:  " +
			((runtime.totalMemory() - runtime.freeMemory()) /
			 (1024L * 1024L)) + " MB");
  }
  
  /**
   * Print the query results. 
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT.  YOU MUST CHANGE THIS
   * METHOD SO THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK
   * PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException 
   */
  static void printResults(String queryName, QryResult result) throws IOException {
	 
    System.out.println(queryName + ":  ");
    result.sortResults();
    if (result.docScores.scores.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < Math.min(100, result.docScores.scores.size()); i++) {
        System.out.println("\t" + i + ":  "
			   + getExternalDocid (result.docScores.getDocid(i))
			   + ", "
			   + result.docScores.getDocidScore(i));
      }
    }
  }
  
  /**
   * Print the query results using the writer object. 
   * 
   * The format is 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName Original query.
   * @param result Result object generated by {@link Qryop#evaluate()}.
   * @param writer The output file writer.
   * @throws IOException 
   */
	static void printResultstoFile(String queryName, QryResult result, Writer writer) throws IOException {
		ArrayList<QryResult.Item> list = result.sortResults();

		if (result.docScores.scores.size() < 1) {
			writer.write((queryName + "\t"+"Q0" + "\t" + "dummy"+"\t"+"1"+"\t"+"0"+"\t"+"run-1"+"\n"));
		} else {
			for (int i = 0; i < Math.min(100, result.docScores.scores.size()); i++) {
				writer.write(queryName  +"\t"+"Q0"+"\t"
						+ list.get(i).getDocid() +"\t"+String.valueOf(i+1)+"\t"
						+ list.get(i).getScore() +"\t"+"run-1"+"\n");
			}
		}

	}

  /**
   *  Given a query string, returns the terms one at a time with stopwords
   *  removed and the terms stemmed using the Krovetz stemmer. 
   * 
   *  Use this method to process raw query terms. 
   * 
   *  @param query String containing query
   *  @return Array of query tokens
   *  @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
}

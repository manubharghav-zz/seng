import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;




public class QryopSlWand extends QryopSl {

	float totalWeights = 0;
	
	
	public void computeTotalWeights(){
		totalWeights=0;
		for(float f:this.weights){
			totalWeights+=f;
		}
	}
	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		
		if(r instanceof RetrievalModelIndri){
			double score = 1.0;
			if(args.size()==0){
				  return 1.0;
			  }
			if (r instanceof RetrievalModelIndri){
				for(int i=0;i<args.size();i++){
//				for (Qryop arg : args) {
					Qryop arg = args.get(i);
					if (arg instanceof QryopSl) {
						QryopSl SlOp = (QryopSl) arg;
						score = score*Math.pow(SlOp.getDefaultScore(r, docid),(weights.get(i)/totalWeights));
					}
				}
			}
			return score;
//			return Math.pow(score, 1.0/args.size());
		}
		return 0;
	}
	
	public QryopSlWand(Qryop... q) {
	    for (int i = 0; i < q.length; i++)
	      this.args.add(q[i]);
	  }

	@Override
	public void add(Qryop q) throws IOException {
		this.args.add(q);

	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		computeTotalWeights();
		Iterator<Float> i1 = weights.iterator();
		Iterator<Qryop> i2 = this.args.iterator();
		
		while(i1.hasNext()){
			float weightAtI = i1.next();
			Qryop argATI = i2.next();
			if(weightAtI<0.0001){
				i1.remove();
				i2.remove();
				System.out.println("removing from wand");
			}
		}
		if(!(r instanceof RetrievalModelIndri)){
			return null;
		}

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
			  // all those docpts  which contain minimum id.
			  Set<Integer> completedListIndex = new HashSet<Integer>(); // list of
			  // all those docptrs which have been exhausted(completely parsed).

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
				  score = 1.0;
				  for (int i = 0; i < this.daatPtrs.size(); i++) {
					  if (currentIDListIndex.contains(i)) {

						  score = score
								  * Math.pow(
										  this.daatPtrs.get(i).scoreList
										  .getDocidScore(this.daatPtrs
												  .get(i).nextDoc),(weights.get(i)/this.totalWeights));
						  this.daatPtrs.get(i).nextDoc++;
//						  System.out.println(weights.get(i)/this.totalWeights);

					  } else {
//						  System.out.println("fucked");
						  QryopSl opSL = (QryopSl) this.args.get(i);
						  score = score
								  * Math.pow(opSL.getDefaultScore(r, crtDocId),(weights.get(i)/this.totalWeights));
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
	
	@Override
	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.weights.get(i) + " " +this.args.get(i).toString() + " ";

		return ("#WAND( " + result + ")");

	}
	

	public String getQuery() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++){
			String tmp = this.args.get(i).toString();
			result += this.weights.get(i) + " " +(tmp.split("\\.")[0]) + " ";
		}
		return ("#WAND( " + result + ")");

	}

}

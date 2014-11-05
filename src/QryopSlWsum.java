import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;




public class QryopSlWsum extends QryopSl {

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
			double score = 0.0;
			if(args.size()==0){
				  return 1.0;
			  }
			if (r instanceof RetrievalModelIndri){
				for(int i=0;i<args.size();i++){
//				for (Qryop arg : args) {
					Qryop arg = args.get(i);
					if (arg instanceof QryopSl) {
						QryopSl SlOp = (QryopSl) arg;
						score = score + (weights.get(i)/totalWeights)* SlOp.getDefaultScore(r, docid);
					}
				}
			}
			return score;
//			return Math.pow(score, 1.0/args.size());
		}
		return 0;
	}
	
	public QryopSlWsum(Qryop... q) {
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
				  score = 0.0;
				  for (int i = 0; i < this.daatPtrs.size(); i++) {
					  if (currentIDListIndex.contains(i)) {

						  score = score +( (weights.get(i)/this.totalWeights)* (this.daatPtrs.get(i).scoreList.getDocidScore(this.daatPtrs.get(i).nextDoc)));
						  this.daatPtrs.get(i).nextDoc++;

					  } else {
						  QryopSl opSL = (QryopSl) this.args.get(i);
						  score = score + ((weights.get(i)/this.totalWeights) * (opSL.getDefaultScore(r, crtDocId)));
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
			result += this.args.get(i).toString() + " ";

		return ("#WSum( " + result + ")");

	}

}

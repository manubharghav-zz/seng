/*
 * The Indri retrieval model.
 */

public class RetrievalModelIndri  extends RetrievalModel{
	// parameters.

	int mu;
	double lambda;
	boolean fb;
	int fbDocs;
	int fbTerms;
	double fbMu;
	double fbOrigWeight;
	String fbInitialRankingFile;
	String fbExpansionQueryFile;

	/**
	 * Set a retrieval model parameter.
	 * 
	 * @param parameterName
	 * @param parametervalue
	 * @return boolean value indicating success or failure.
	 */
	public boolean setParameter(String parameterName, double value) {
		if (parameterName.equals("mu")) {
			this.mu = (int) value;
		} else if (parameterName.equals("lambda")) {
			this.lambda = value;
		}
		return true;
	}

	public int getMu() {
		return mu;
	}

	public void setMu(int mu) {
		this.mu = mu;
	}

	public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	public boolean isFb() {
		return fb;
	}

	public void setFb(boolean fb) {
		this.fb = fb;
	}

	public int getFbDocs() {
		return fbDocs;
	}

	public void setFbDocs(int fbDocs) {
		this.fbDocs = fbDocs;
	}

	public int getFbTerms() {
		return fbTerms;
	}

	public void setFbTerms(int fbTerms) {
		this.fbTerms = fbTerms;
	}

	public double getFbMu() {
		return fbMu;
	}

	public void setFbMu(double fbMu) {
		this.fbMu = fbMu;
	}

	public double getFbOrigWeight() {
		return fbOrigWeight;
	}

	public void setFbOrigWeight(double fbOrigWeight) {
		this.fbOrigWeight = fbOrigWeight;
	}

	public String getFbInitialRankingFile() {
		return fbInitialRankingFile;
	}

	public void setFbInitialRankingFile(String fbInitialRankingFile) {
		this.fbInitialRankingFile = fbInitialRankingFile;
	}

	public String getFbExpansionQueryFile() {
		return fbExpansionQueryFile;
	}

	public void setFbExpansionQueryFile(String fbExpansionQueryFile) {
		this.fbExpansionQueryFile = fbExpansionQueryFile;
	}

	/**
	 * Set a retrieval model parameter.
	 * 
	 * @param parameterName
	 * @param parametervalue
	 * @return boolean value.
	 */
	public boolean setParameter(String parameterName, String value) {

		if (parameterName.equals("mu")) {
			this.mu = Integer.valueOf(value);
		} else if (parameterName.equals("lambda")) {
			this.lambda = Double.valueOf(value);
		}
		return true;
	}
}

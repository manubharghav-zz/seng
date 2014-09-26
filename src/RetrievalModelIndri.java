/*
 * The Indri retrieval model.
 */

public class RetrievalModelIndri  extends RetrievalModel{
	// parameters.

	int mu;
	double lambda;

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

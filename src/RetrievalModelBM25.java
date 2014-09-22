/**
 * The BM25 retrieval model.
 * 
 * Copyright (c) 2014, Carnegie Mellon University. All Rights Reserved.
 */

public class RetrievalModelBM25 extends RetrievalModel {

	// parameters.
	double k_1;
	double b;
	double k_3;

	/**
	 * Set a retrieval model parameter.
	 * 
	 * @param parameterName
	 * @param parametervalue
	 * @return Always false because this retrieval model has no parameters.
	 */
	public boolean setParameter(String parameterName, double value) {
		if (parameterName.equals("k_1")) {
			this.k_1 = value;
		} else if (parameterName.equals("b")) {
			this.b = value;
		} else if (parameterName.equals("k_3")) {
			this.k_3 = value;
		}
		return false;
	}

	/**
	 * Set a retrieval model parameter.
	 * 
	 * @param parameterName
	 * @param parametervalue
	 * @return Always false because this retrieval model has no parameters.
	 */
	public boolean setParameter(String parameterName, String value) {
		try {
			if (parameterName.equals("k_1")) {
				this.k_1 = Double.valueOf(value);
			} else if (parameterName.equals("b")) {
				this.b = Double.valueOf(value);
			} else if (parameterName.equals("k_3")) {
				this.k_3 = Double.valueOf(value);
			}
			return true;
		} catch (Exception e) {

			System.err
					.println("Error setting the parameters for the bm25 model");
			return false;
		}

	}
}

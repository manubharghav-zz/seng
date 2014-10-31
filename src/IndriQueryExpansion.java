

class NoQueryExpansionForThisModel extends Exception {
    public NoQueryExpansionForThisModel(String message) {
        super(message);
    }
}
public class IndriQueryExpansion {
	
	RetrievalModelIndri model;
	
	public RetrievalModelIndri getModel() {
		return model;
	}


	public void setModel(RetrievalModel model) throws NoQueryExpansionForThisModel {
		if (model instanceof RetrievalModelIndri) {
			this.model = (RetrievalModelIndri) model;
		}
		else{
			System.out.println("Wrong model. Query expansion not developed for this model");
			throw new NoQueryExpansionForThisModel("Error here mate. Please look into it");
		}
	}
	
	
	public QryResult evaluateQuery(Qryop query){
		
		
		
		
		
		
		return null;
	}
	
}

package ptdb.common.dto;

/**
 * 
 * A task request to fetch a model from the database.
 * 
 * <p>It is used as a data transfer object and hold the model name 
 * with its getter and setter method.</p>
 * 
 * @author Yousef Alsaeed
 * @version $Id$
 * @since Ptolemy II 8.1
 * @Pt.ProposedRating Red (yalsaeed)
 * @Pt.AcceptedRating Red (yalsaeed)
 *
 *
 */
public class GetModelsTask extends Task {
    
    /**
     * Default nullary constructor.
     * <p>Should be removed once the code using it
     * is fixed to use the constructor with the model name parameter.</p>
     */
    public GetModelsTask() {
        //FIXME: Remove this method as soon as the code pointing to it is modified.
    }
    
    /**
     * Construct an instance of the object and set the model name to be fetched
     * from the database.
     * 
     * @param modelName the model name to be fetched from the database.
     */
    public GetModelsTask(String modelName) { 
        _modelName = modelName;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////
    
    

    /**
     * Return the model name that is needed to be fetched from the database.
     *
     * @return The model name.
     * 
     * @see #setModelName
     */
    public String getModelName() {
        return _modelName;

    }

    /**
     * Set the model name to be fetched from the database.
     *
     * @param modelName the name of the model to be fetched from the database.
     * 
     * @see #getModelName
     */
    public void setModelName(String modelName) {
        _modelName = modelName;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    /** The model name. */
    private String _modelName;

}

package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreviewLabel {

    
//    @JsonProperty("_id")
//    private String id;
//    
//    @JsonProperty("_rev")
//    private String rev;
    
    @JsonProperty("libelle")
    private String label;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("objectId")
    private String objectId;

//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getType(){
        return type;
    }
    
    public void setType(String type){
        this.type=type;
    }
    
    public String getObjectId(){
        return objectId;
    }
    
    public void setObjectid(String objectId){
        this.objectId = objectId;
    }

    @Override
    public String toString() {
        return "PreviewLabel [label=" + label + "]";
    }
}

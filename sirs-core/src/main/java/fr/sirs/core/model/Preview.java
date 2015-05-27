package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Preview implements AvecLibelle {
    
    @JsonProperty("docId")
    private String docId;
    
    @JsonProperty("docClass")
    private String docClass;
    
    @JsonProperty("elementId")
    private String elementId;
    
    @JsonProperty("elementClass")
    private String elementClass;
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("valid")
    private boolean valid;
    
    @JsonProperty("designation")
    private String designation;
    
    private final SimpleStringProperty libelleProperty = new SimpleStringProperty();

    /**
     * @return the ID of the current element if its a document, or the Id of its
     * top-level container if target element is a contained element.
     */
    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getDocClass() {
        return docClass;
    }

    public void setDocClass(String docClass) {
        this.docClass = docClass;
    }

    /**
     * @return the ID of the target element.
     */
    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getElementClass() {
        return elementClass;
    }

    public void setElementClass(String elementClass) {
        this.elementClass = elementClass;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean getValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    @Override
    public String toString() {
        return "Preview{" + "docId=" + docId + ", docClass=" + docClass + ", elementId=" + elementId + ", elementClass=" + elementClass + ", author=" + author + ", valid=" + valid + ", designation=" + designation + ", label=" + libelleProperty.get() + '}';
    }

    @Override
    public StringProperty libelleProperty() {
        return libelleProperty;
    }

    @Override
    public String getLibelle() {
        return libelleProperty.get();
    }

    @JsonProperty("libelle")
    @Override
    public void setLibelle(String libelle) {
        libelleProperty.set(libelle);
    }
    
}
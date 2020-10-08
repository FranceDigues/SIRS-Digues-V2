

package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.sirs.core.authentication.PasswordSerializer;
import fr.sirs.core.authentication.SafePasswordDeserializer;
import fr.sirs.util.property.Internal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.ektorp.Attachment;
import org.ektorp.support.Revisions;
import org.ektorp.util.Assert;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@SuppressWarnings("serial")
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BookMark
    implements Element    {
    /**
    * @deprecated Please avoid to use this constructor from application because
    * of validity/author control. If the element you want to create is a
    * couchDB document, you can use the create() method from the corresponding
    * repo. On the contrary, use the T createElement(Class clazz) method
    * of the ElementCreator available in the Session.
    */
    @Deprecated public BookMark() {super();}

    //
    // BEGIN-DUP This code is duplicated from org.ektorp.support.CouchDbDocument
    //
    public static final String ATTACHMENTS_NAME = "_attachments";

    private String id;
    private String revision;
    private Map<String, Attachment> attachments;
    private List<String> conflicts;
    private Revisions revisions;

    @Internal
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    public void setId(String s) {
        Assert.hasText(s, "id must have a value");
        if (id != null && id.equals(s)) {
            return;
        }
        if (id != null) {
            throw new IllegalStateException("cannot set id, id already set");
        }
        id = s;
    }

    @Internal
    @JsonProperty("_rev")
    public String getRevision() {
        return revision;
    }

    @JsonProperty("_rev")
    public void setRevision(String s) {
        // no empty strings thanks
        if (s != null && s.length() == 0) {
            return;
        }
        this.revision = s;
    }

    @Internal
    @JsonIgnore
    public boolean isNew() {
        return revision == null;
    }

    @Internal
    @JsonProperty(ATTACHMENTS_NAME)
    public Map<String, Attachment> getAttachments() {
        return attachments;
    }

    @JsonProperty(ATTACHMENTS_NAME)
    void setAttachments(Map<String, Attachment> attachments) {
        this.attachments = attachments;
    }

    @JsonProperty("_conflicts")
    void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }

    @JsonProperty("_revisions")
    void setRevisions(Revisions r) {
        this.revisions = r;
    }

    /**
     * Note: Will only be populated if this document has been loaded with the revisions option = true.
     * @return Known revisions.
     */
    @Internal
    @JsonIgnore
    public Revisions getRevisions() {
        return revisions;
    }

    /**
     *
     * @return a list of conflicting revisions. Note: Will only be populated if this document has been loaded through the CouchDbConnector.getWithConflicts method.
     */
    @Internal
    @JsonIgnore
    public List<String> getConflicts() {
        return conflicts;
    }
    /**
     *
     * @return true if this document has a conflict. Note: Will only give a correct value if this document has been loaded through the CouchDbConnector.getWithConflicts method.
     */
    public boolean hasConflict() {
        return conflicts != null && !conflicts.isEmpty();
    }

    protected void removeAttachment(String id) {
        Assert.hasText(id, "id may not be null or emtpy");
        if (attachments != null) {
            attachments.remove(id);
        }
    }

    protected void addInlineAttachment(Attachment a) {
        Assert.notNull(a, "attachment may not be null");
        Assert.hasText(a.getDataBase64(), "attachment must have data base64-encoded");
        if (attachments == null) {
            attachments = new HashMap<>();
        }
        attachments.put(a.getId(), a);
    }

    //
    // END-DUP
    //

    /**
     * @return the parent {@link Element} of the current object, or itself if its a CouchDB document root node.
     * Can be null for newly created objects which has not been saved in database yet.
     */
    @Override
    @Internal
    @JsonIgnore
    public Element getCouchDBDocument() {
        return this;
    }

    @Override
    @Internal
    @JsonIgnore
    public String getDocumentId() {
        return getId();
    }

    @Override
    @JsonIgnore
    public void setParent(Element parent){
        //
        // NOP
        //
    }

    /**
     * @return the parent {@link Element} of the current element, or null if no parent is set.
     * Note that no CouchDB document has a parent property. Only contained elements have one.
     */
    @Override
    public ObjectProperty<Element> parentProperty() {
        return null;
    }

    /**
     * @return the parent {@link Element} of the current element, or null if no parent is set.
     * Note that no CouchDB document has a parent property. Only contained elements have one.
     */
    @Override
    public Element getParent(){
       return null;
    }

    /**
    * JavaFX property for description.
    */
    private final StringProperty  description = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for description.
    */
    public StringProperty descriptionProperty() {
       return description;
    }
    /**
    * JavaFX property for titre.
    */
    private final StringProperty  titre = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for titre.
    */
    public StringProperty titreProperty() {
       return titre;
    }
    /**
    * JavaFX property for parametres.
    */
    private final StringProperty  parametres = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for parametres.
    */
    public StringProperty parametresProperty() {
       return parametres;
    }
    /**
    * JavaFX property for identifiant.
    */
    private final StringProperty  identifiant = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for identifiant.
    */
    public StringProperty identifiantProperty() {
       return identifiant;
    }
    /**
    * JavaFX property for motDePasse.
    */
    private final StringProperty  motDePasse = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for motDePasse.
    */
    public StringProperty motDePasseProperty() {
       return motDePasse;
    }
    /**
    * JavaFX property for designation.
    */
    private final StringProperty  designation = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for designation.
    */
    public StringProperty designationProperty() {
       return designation;
    }
    /**
    * JavaFX property for author.
    */
    private final StringProperty  author = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for author.
    */
    public StringProperty authorProperty() {
       return author;
    }
    /**
    * JavaFX property for valid.
    */
    private final BooleanProperty  valid = new SimpleBooleanProperty();
    /**
    * @return JavaFX observable property for valid.
    */
    public BooleanProperty validProperty() {
       return valid;
    }
    /**
    * JavaFX property for typeService.
    */
    private final StringProperty  typeService = new SimpleStringProperty();
    /**
    * @return JavaFX observable property for typeService.
    */
    public StringProperty typeServiceProperty() {
       return typeService;
    }

    public String getDescription(){
        return this.description.get();
    }


    public void setDescription(String description){
        this.description.set(description);
    }

    public String getTitre(){
        return this.titre.get();
    }


    public void setTitre(String titre){
        this.titre.set(titre);
    }

    public String getParametres(){
        return this.parametres.get();
    }


    public void setParametres(String parametres){
        this.parametres.set(parametres);
    }

    public String getIdentifiant(){
        return this.identifiant.get();
    }


    public void setIdentifiant(String identifiant){
        this.identifiant.set(identifiant);
    }

    @JsonDeserialize(using=SafePasswordDeserializer.class)
    public String getMotDePasse(){
        return this.motDePasse.get();
    }

    @JsonSerialize(using=PasswordSerializer.class)
    public void setMotDePasse(String motDePasse){
        this.motDePasse.set(motDePasse);
    }

    public String getDesignation(){
        return this.designation.get();
    }


    public void setDesignation(String designation){
        this.designation.set(designation);
    }

    public String getAuthor(){
        return this.author.get();
    }


    public void setAuthor(String author){
        this.author.set(author);
    }

    public boolean getValid(){
        return this.valid.get();
    }


    public void setValid(boolean valid){
        this.valid.set(valid);
    }

    public String getTypeService(){
        return this.typeService.get();
    }


    public void setTypeService(String typeService){
        this.typeService.set(typeService);
    }

    @Override
    public BookMark copy() {

        BookMark bookMark = new BookMark();

        bookMark.setDescription(getDescription());
        bookMark.setTitre(getTitre());
        bookMark.setParametres(getParametres());
        bookMark.setIdentifiant(getIdentifiant());
        bookMark.setMotDePasse(getMotDePasse());
        bookMark.setDesignation(getDesignation());
        bookMark.setAuthor(getAuthor());
        bookMark.setValid(getValid());
        bookMark.setTypeService(getTypeService());

        return bookMark;
    }

    @Override
    public int hashCode() {
        return (id == null)? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BookMark other = (BookMark) obj;
        if (id != null) {
            return id.equals(other.id); // TODO : check revision ?
        } else {
            return contentBasedEquals(other);
        }
    }
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder("[BookMark ");
        builder.append("description: ");
        builder.append(getDescription());
        builder.append(", ");
        builder.append("titre: ");
        builder.append(getTitre());
        builder.append(", ");
        builder.append("parametres: ");
        builder.append(getParametres());
        builder.append(", ");
        builder.append("identifiant: ");
        builder.append(getIdentifiant());
        builder.append(", ");
        builder.append("motDePasse: ");
        builder.append(getMotDePasse());
        builder.append(", ");
        builder.append("designation: ");
        builder.append(getDesignation());
        builder.append(", ");
        builder.append("author: ");
        builder.append(getAuthor());
        builder.append(", ");
        builder.append("valid: ");
        builder.append(getValid());
        builder.append(", ");
        builder.append("typeService: ");
        builder.append(getTypeService());
        return builder.toString();
    }

    @Override
    public boolean contentBasedEquals(Element element) {
        if(element instanceof BookMark) {

            final BookMark other = (BookMark) element;
            if (getId() == null) {
                if (other.getId() != null) return false;
            } else if (!getId().equals(other.getId())) return false;
            if ((this.getDescription()==null ^ other.getDescription()==null) || ( (this.getDescription()!=null && other.getDescription()!=null) && !this.getDescription().equals(other.getDescription()))) return false;
            if ((this.getTitre()==null ^ other.getTitre()==null) || ( (this.getTitre()!=null && other.getTitre()!=null) && !this.getTitre().equals(other.getTitre()))) return false;
            if ((this.getParametres()==null ^ other.getParametres()==null) || ( (this.getParametres()!=null && other.getParametres()!=null) && !this.getParametres().equals(other.getParametres()))) return false;
            if ((this.getIdentifiant()==null ^ other.getIdentifiant()==null) || ( (this.getIdentifiant()!=null && other.getIdentifiant()!=null) && !this.getIdentifiant().equals(other.getIdentifiant()))) return false;
            if ((this.getMotDePasse()==null ^ other.getMotDePasse()==null) || ( (this.getMotDePasse()!=null && other.getMotDePasse()!=null) && !this.getMotDePasse().equals(other.getMotDePasse()))) return false;
            if ((this.getDesignation()==null ^ other.getDesignation()==null) || ( (this.getDesignation()!=null && other.getDesignation()!=null) && !this.getDesignation().equals(other.getDesignation()))) return false;
            if ((this.getAuthor()==null ^ other.getAuthor()==null) || ( (this.getAuthor()!=null && other.getAuthor()!=null) && !this.getAuthor().equals(other.getAuthor()))) return false;
            if (this.getValid() != other.getValid()) return false;
            if ((this.getTypeService()==null ^ other.getTypeService()==null) || ( (this.getTypeService()!=null && other.getTypeService()!=null) && !this.getTypeService().equals(other.getTypeService()))) return false;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeChild(final Element toRemove) {
        if (toRemove == null) return false;
        return false;
    }

    @Override
    public boolean addChild(final Element toAdd) {
        if (toAdd == null) return false;
        return false;
    }

    @Override
    public Element getChildById(final String toSearch) {
        if (toSearch == null) return null;
        if (getId() != null && getId().equals(toSearch)) return this;
        Element result = null;
        return result;
    }

}


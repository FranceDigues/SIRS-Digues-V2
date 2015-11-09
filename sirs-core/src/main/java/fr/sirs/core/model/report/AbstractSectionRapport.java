

package fr.sirs.core.model.report;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.AvecLibelle;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.SQLQuery;
import fr.sirs.util.FilteredIterable;
import fr.sirs.util.property.Internal;
import fr.sirs.util.property.Reference;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.beans.property.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.db.JDBCFeatureStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.NamesExt;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.text.Paragraph;
import org.opengis.util.GenericName;

@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@SuppressWarnings("serial")
public abstract class AbstractSectionRapport implements Element , AvecLibelle {

    private String id;

    public final void print(final TextDocument target, final Iterable<Element> sourceData) throws Exception {
        ArgumentChecks.ensureNonNull("Target document", target);
        ArgumentChecks.ensureNonNull("Source data collection", sourceData);

        // TODO : put title ?
        // TODO : apply filter now
        final Paragraph sectionStart = target.addParagraph(libelle.get());
        final Paragraph sectionEnd = target.insertParagraph(sectionStart, sectionStart, false);

        sectionStart.applyHeading(true, 2);

        final PrintContext ctx = new PrintContext(target, sectionStart, sectionEnd, sourceData);
        printSection(ctx);
    }

    /**
     * Execute printing of the current section according to given context data.
     *
     * Note : elements / properties given by input context has already been filtered
     * user {@link #getRequeteId() }.
     *
     * @param context Printing context, contains all data needed to print section.
     * @throws Exception
     */
    protected abstract void printSection(PrintContext context) throws Exception;

    @Override
    @Internal
    @JsonProperty("id")
    public String getId(){
        if(this.id==null)
          this.id = UUID.randomUUID().toString();
        return id;
    }

    @JsonProperty("id")
    public void setId(String id){
        this.id = id;
    }

    @Override
    @Internal
    @JsonIgnore
    public String getDocumentId(){
        if(documentId != null)
            return documentId;
        if(parent == null )
            return null;
        if(parent.get()==null)
            return null;
        return parent.get().getDocumentId();
    }

    /**
     * @return the parent {@link Element} of the current object, or itself if its a CouchDB document root node.
     * Can be null for newly created objects which has not been saved in database yet.
     */
    @Override
    @Internal
    @JsonIgnore
    public Element getCouchDBDocument(){
        if(parent == null )
            return null;
        if(parent.get()==null)
            return null;
        return parent.get().getCouchDBDocument();
    }

    private String documentId;

    @JsonProperty(required=false)
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    protected final ObjectProperty<Element> parent =  new SimpleObjectProperty<>();

    /**
     * @return the parent {@link Element} of the current element, or null if no parent is set.
     * Note that no CouchDB document has a parent property. Only contained elements have one.
     */
    @Override
    public ObjectProperty<Element> parentProperty() {
        return parent;
    }

    /**
     * @return the parent {@link Element} of the current element, or null if no parent is set.
     * Note that no CouchDB document has a parent property. Only contained elements have one.
     */
    @Override
    public Element getParent(){
       return parent.get();
    }

    @Override
    @JsonBackReference("parent")
    public void setParent(Element parent){
       this.parent.set(parent);
    }

    private final StringProperty  author = new SimpleStringProperty();
    @Override
    public StringProperty authorProperty() {
       return author;
    }

    private final BooleanProperty  valid = new SimpleBooleanProperty();
    @Override
    public BooleanProperty validProperty() {
       return valid;
    }

    private final StringProperty  designation = new SimpleStringProperty();
    @Override
    public StringProperty designationProperty() {
       return designation;
    }

    private final StringProperty  libelle = new SimpleStringProperty();
    @Override
    public StringProperty libelleProperty() {
       return libelle;
    }


    private final StringProperty  requeteId = new SimpleStringProperty();
    public StringProperty requeteIdProperty() {
       return requeteId;
    }

    @Override
    public String getAuthor(){
        return this.author.get();
    }


    @Override
    public void setAuthor(String author){
        this.author.set(author);
    }

    @Override
    public boolean getValid(){
        return this.valid.get();
    }


    @Override
    public void setValid(boolean valid){
        this.valid.set(valid);
    }

    @Override
    public String getDesignation(){
        return this.designation.get();
    }


    @Override
    public void setDesignation(String designation){
        this.designation.set(designation);
    }

    @Override
    public String getLibelle(){
        return this.libelle.get();
    }


    @Override
    public void setLibelle(String libelle){
        this.libelle.set(libelle);
    }

    @Reference(ref=fr.sirs.core.model.SQLQuery.class)
    public String getRequeteId(){
        return this.requeteId.get();
    }


    public void setRequeteId(String requeteId){
        this.requeteId.set(requeteId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractSectionRapport other = (AbstractSectionRapport) obj;
        if (id != null) {
            return id.equals(other.id); // TODO : check revision ?
        } else {
            return contentBasedEquals(other);
        }
    }
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder("[RapportSectionObligationReglementaire ");
        builder.append("author: ");
        builder.append(getAuthor());
        builder.append(", ");
        builder.append("valid: ");
        builder.append(getValid());
        builder.append(", ");
        builder.append("designation: ");
        builder.append(getDesignation());
        builder.append(", ");
        builder.append("libelle: ");
        builder.append(getLibelle());
        builder.append(", ");
        builder.append("requeteId: ");
        builder.append(getRequeteId());
        return builder.toString();
    }

    @Override
    public boolean contentBasedEquals(Element element) {
        if(element instanceof AbstractSectionRapport) {
            final AbstractSectionRapport other = (AbstractSectionRapport) element;
            if (getId() == null) {
                if (other.getId() != null) return false;
            } else if (!getId().equals(other.getId())) return false;
            if ((this.getAuthor()==null ^ other.getAuthor()==null) || ( (this.getAuthor()!=null && other.getAuthor()!=null) && !this.getAuthor().equals(other.getAuthor()))) return false;
            if (this.getValid() != other.getValid()) return false;
            if ((this.getDesignation()==null ^ other.getDesignation()==null) || ( (this.getDesignation()!=null && other.getDesignation()!=null) && !this.getDesignation().equals(other.getDesignation()))) return false;
            if ((this.getLibelle()==null ^ other.getLibelle()==null) || ( (this.getLibelle()!=null && other.getLibelle()!=null) && !this.getLibelle().equals(other.getLibelle()))) return false;
            if ((this.getRequeteId()==null ^ other.getRequeteId()==null) || ( (this.getRequeteId()!=null && other.getRequeteId()!=null) && !this.getRequeteId().equals(other.getRequeteId()))) return false;
            return true;
        }
        return false;
    }

    /**
     * Put current object attributes in given one.
     * @param target The object to set attribute values.
     */
    protected void copy(final AbstractSectionRapport target) {
        target.setAuthor(getAuthor());
        target.setValid(getValid());
        target.setDesignation(getDesignation());
        target.setLibelle(getLibelle());
        target.setRequeteId(getRequeteId());
    }

    /**
     * Contains all informations needed for current section printing :
     * - target document
     * - A start paragraph which determine beginning of the section
     * - An end paragraph to tells where section must ends
     * - Names of all the properties returned by this section filter
     * - List of the elements to print (already filtered using this section query).
     */
    protected class PrintContext {

        /**
         * Doccument to insert content into
         */
        public final TextDocument target;
        /**
         * Sections must print their data after this paragraph.
         */
        public final Paragraph startParagraph;
        /**
         * Sections must print their data before this paragraph.
         */
        public final Paragraph endParagraph;
        /**
         * Names of the properties which should be used to print input elements.
         */
        protected final LinkedHashSet<String> propertyNames;
        /**
         * Filtered list of elements which should be printed.
         */
        public final Iterable<Element> elements;

        public PrintContext(TextDocument target, Paragraph startParagraph, Paragraph endParagraph, Iterable<Element> elements) throws SQLException, DataStoreException, InterruptedException, ExecutionException {
            ArgumentChecks.ensureNonNull("Target document", target);
            ArgumentChecks.ensureNonNull("Start paragraph", startParagraph);
            ArgumentChecks.ensureNonNull("End paragraph", endParagraph);
            ArgumentChecks.ensureNonNull("Elements to print", elements);
            this.target = target;
            this.startParagraph = startParagraph;
            this.endParagraph = endParagraph;

            if (getRequeteId() == null) {
                propertyNames = null;
                this.elements = elements;
            } else {
                final SessionCore session = InjectorCore.getBean(SessionCore.class);
                final Query query = QueryBuilder.language(JDBCFeatureStore.CUSTOM_SQL,
                        session.getRepositoryForClass(SQLQuery.class).get(getRequeteId()).getSql(),
                        NamesExt.create("query")
                );

                propertyNames = new LinkedHashSet<>();
                for (final GenericName name : query.getPropertyNames()) {
                    propertyNames.add(name.tip().toString());
                }

                final FeatureStore h2Store = session.getH2Helper().getStore().get();
                final Predicate<Element> predicate;
                // Analyze input filter to determine if we only need ID comparison, or if we must perform a full scan.
                if (propertyNames.contains(SirsCore.ID_FIELD)) {
                    final HashSet<String> ids = new HashSet<>();
                    try (FeatureReader reader = h2Store.getFeatureReader(query)) {
                        while (reader.hasNext()) {
                            final Object pValue = reader.next().getPropertyValue(SirsCore.ID_FIELD);
                            if (pValue instanceof String) {
                                ids.add((String) pValue);
                            }
                        }
                    }

                    predicate = input -> ids.contains(input.getId());

                } else {
                    final HashMap<String, HashMap<String, PropertyDescriptor>> classProperties = new HashMap<>();
                    final FeatureCollection filterValues = h2Store.createSession(false).getFeatureCollection(query);
                    predicate = input -> {
                        // Get list of input element properties.
                        final String className = input.getClass().getCanonicalName();
                        HashMap<String, PropertyDescriptor> properties = classProperties.get(className);
                        if (properties == null) {
                            final PropertyDescriptor[] descriptors;
                            try {
                                descriptors = Introspector.getBeanInfo(input.getClass()).getPropertyDescriptors();
                            } catch (IntrospectionException ex) {
                                SirsCore.LOGGER.log(Level.WARNING, "Invalid class : " + className, ex);
                                return false;
                            }
                            properties = new HashMap<>(descriptors.length);
                            for (final PropertyDescriptor desc : descriptors) {
                                if (desc.getReadMethod() != null) {
                                    desc.getReadMethod().setAccessible(true);
                                    properties.put(desc.getName(), desc);
                                }
                            }
                            classProperties.put(className, properties);
                        }

                        /* Now we can compare our element to filtered data. 2 steps :
                         * - Ensure our input element has at least all the properties of the filtered features
                         * - Ensure equality of all these properties with one of our filtered features.
                         */
                        final HashMap<String, Object> values = new HashMap<>(propertyNames.size());
                        for (final String pName : propertyNames) {
                            final PropertyDescriptor desc = properties.get(pName);
                            if (desc == null) {
                                return false;
                            } else {
                                try {
                                    values.put(pName, desc.getReadMethod().invoke(input));
                                } catch (Exception ex) {
                                    throw new SirsCoreRuntimeException(ex);
                                }
                            }
                        }

                        boolean isEqual;
                        try (final FeatureIterator reader = filterValues.iterator()) {
                            while (reader.hasNext()) {
                                isEqual = true;
                                final Feature next = reader.next();
                                for (final org.geotoolkit.feature.Property p : next.getProperties()) {
                                    if (!Objects.equals(p.getValue(), values.get(p.getName().tip().toString()))) {
                                        isEqual = false;
                                        break;
                                    }
                                }
                                if (isEqual)
                                    return true;
                            }
                        }

                        return false;
                    };
                }

                this.elements = new FilteredIterable<>(elements, predicate);

            }
        }

        /**
         * Tells if a given property should be ignored when building report.
         * @param propertyName Name of the property to test.
         * @return True if given property should be ignored, false otherwise
         */
        public boolean ignoreProperty(final String propertyName) {
            return (propertyNames != null && propertyNames.contains(propertyName));
        }
    }
}


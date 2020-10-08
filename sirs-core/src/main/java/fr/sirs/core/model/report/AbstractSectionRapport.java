/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.core.model.report;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import fr.sirs.core.model.SQLQueries;
import fr.sirs.core.model.SQLQuery;
import fr.sirs.util.odt.ODTUtils;
import fr.sirs.util.property.Internal;
import fr.sirs.util.property.Reference;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.db.JDBCFeatureStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.Property;
import org.geotoolkit.util.NamesExt;
import org.odftoolkit.simple.TextDocument;
import org.opengis.feature.FeatureType;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(Include.NON_EMPTY)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@SuppressWarnings("serial")
public abstract class AbstractSectionRapport implements Element , AvecLibelle {

    private String id;

    public final void print(final TextDocument target, final Stream<? extends Element> sourceData) throws Exception {
        ArgumentChecks.ensureNonNull("Target document", target);

        //Write section in a temporary document to ensure it will be inserted entirely or not at all in real target.
        try (final TextDocument tmpDoc = TextDocument.newTextDocument()) {
            tmpDoc.addParagraph(libelle.get()).applyHeading(true, 2);
            final PrintContext ctx = new PrintContext(tmpDoc, sourceData);
            printSection(ctx);

            ODTUtils.append(target, tmpDoc);
        }
    }

    /**
     * Execute printing of the current section according to given context data.
     *
     * Note : elements / properties given by input context has already been filtered
     * user {@link #getRequeteId() }.
     *
     * @param context Printing context, contains all data needed to print section.
     * @throws Exception If an error occurs while printing this section.
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
     * - target document : All content will be appended at its end
     * - Names of all the properties returned by this section filter
     * - List of the elements to print (already filtered using this section query).
     *
     * If no element is provided (null value), It's the responsability of the aimed
     * section implementation to decide if it can print content or just return empty
     * document.
     */
    protected class PrintContext {

        /**
         * Doccument to insert content into
         */
        public final TextDocument target;

        /**
         * Names of the properties which should be used to print input elements.
         */
        protected final List<String> propertyNames;
        
        /**
         * Filtered list of elements which should be printed. Can be null.
         */
        public final Stream<? extends Element> elements;

        /**
         * Résultat de requête sur la base SQL utilisé pour le filtrage des éléments. Can be null.
         */
        protected final FeatureCollection queryResult;

        /**
         * 
         * @param target
         * @param elements
         * @throws SQLException
         * @throws DataStoreException
         * @throws InterruptedException
         * @throws ExecutionException
         * @throws IOException 
         */
        public PrintContext(final TextDocument target, final Stream<? extends Element> elements) 
                throws SQLException, DataStoreException, InterruptedException, ExecutionException, IOException {
            ArgumentChecks.ensureNonNull("Target document", target);
            this.target = target;

            final Optional<SQLQuery> sqlQuery = SQLQueries.findQuery(getRequeteId());
            
            if (!sqlQuery.isPresent()) {
                propertyNames = null;
                this.elements = elements;
                queryResult = null;

            } 
            
            // si une requête est présente
            else {

                // A- récupération des résultats de la requête
                //============================================
                final FeatureStore h2Store = InjectorCore.getBean(SessionCore.class).getH2Helper().getStore().get();
                queryResult = h2Store.createSession(false).getFeatureCollection(
                        QueryBuilder.language(JDBCFeatureStore.CUSTOM_SQL,
                        sqlQuery.get().getSql(),
                        NamesExt.create("query")
                ));

                // B- détermination des noms de colonnes
                //======================================
                try (final FeatureIterator reader = queryResult.iterator()) {
                    if (reader.hasNext()) {
                        propertyNames = propertyNames(reader.next().getType());
                    }
                    else {
                        // SYM-1741 : en l'absence de résultat de la requête, on ne souhaite pas lancer d'exception mais poursuivre l'impression 
                        propertyNames = null;
                    }
                }

                // C- filtrage des éléments
                //=========================
                if (elements == null) {
                    // S'il n'y a aucun élément, il n'y a pas de filtrage à faire
                    this.elements = null;
                } 
                
                // à ce stade les noms de propriété sont null si et seulement si la requête ne retourne aucun résultat
                // SYM-1741 : en l'absence de résultat de la requête, on ne souhaite pas lancer d'exception mais poursuivre l'impression 
                else if(propertyNames==null){
                    this.elements = Stream.empty();
                }
                else {
                    
                    // sinon, il faut déterminer le prédicat à utiliser pour filtrer les éléments
                    final Predicate<Element> predicate;
                    
                    // Analyze input filter to determine if we only need ID comparison, or if we must perform a full scan.
                    if (propertyNames.contains(SirsCore.ID_FIELD)) {
                        
                        /*
                        CAS 1 :
                        S'il y a un identifiant dans les propriétés de la requête, le prédicat de filtrage utilisé
                        consistera à tester si l'identifiant de chaque élément est présent dans comme valeur d'identifiant
                        d'un des résultats de la requête.
                        */
                        
                        // 1- recherche des valeurs des identifiants des résultats de la requête 
                        final Set<String> queryResultIds = new HashSet<>();
                        try (final FeatureIterator reader = queryResult.iterator()) {
                            while (reader.hasNext()) {
                                final Object queryEntryId = reader.next().getPropertyValue(SirsCore.ID_FIELD);
                                if (queryEntryId instanceof String) {
                                    queryResultIds.add((String) queryEntryId);
                                }
                            }
                        }

                        // 2- définition du prédicat
                        predicate = element -> queryResultIds.contains(element.getId());

                    } 
                    else {
                        
                        /*
                        CAS 2 :
                        S'il n'y a pas d'identifiant dans les propriétés de la requête
                        Dans ce cas, on ne veut garder que les éléments pour lesquels il existe un résultat de requête 
                        ayant les mêmes propriétés.
                        
                        L'élément est éliminé, en particulier, si :
                        - si un problème est rencontré lors de l'introspection des propriétés de sa classe
                        - si l'élément ne dispose pas de toutes les propriétés existant pour les résultats de recherche
                        - s'il n'existe aucun tuple du résultat de requête ayant toutes ses champs comparables aux champs de l'élément
                        du point de vue du nom et de la valeur.
                        */
                        
                        // index des accesseurs par classe et par nom de propritété de la feature correspondante
                        // cet index est défini hors du prédicat pour éviter les répétitions de calculs pour une même classe d'éléments
                        final Map<Class, Map<String, Method>> classProperties = new HashMap<>();
                        predicate = element -> {
                            
                            // 1- recherche des accesseurs aux propriétés de l'élément
                            final Class className = element.getClass();
                            Map<String, Method> eltProperties = classProperties.get(className);
                            
                            if (eltProperties == null) {
                                final PropertyDescriptor[] descriptors;
                                try {
                                    descriptors = Introspector.getBeanInfo(element.getClass()).getPropertyDescriptors();
                                } catch (IntrospectionException ex) {
                                    SirsCore.LOGGER.log(Level.WARNING, "Invalid class : " + className.getCanonicalName(), ex);
                                    return false;
                                }
                                
                                eltProperties = new HashMap<>(descriptors.length);
                                for (final PropertyDescriptor desc : descriptors) {
                                    final Method readMethod = desc.getReadMethod();
                                    if (readMethod != null) {
                                        readMethod.setAccessible(true);
                                        eltProperties.put(desc.getName(), readMethod);
                                    }
                                }
                                classProperties.put(className, eltProperties);
                            }

                            /* Now we can compare our element to filtered data. 2 steps :
                             * - Ensure our input element has at least all the properties of the filtered features
                             * - Ensure equality of all these properties with one of our filtered features.
                             */
                            
                            // 2- récupération des valeurs des propriétés de l'élément
                            final Map<String, Object> values = new HashMap<>(propertyNames.size());
                            for (final String pName : propertyNames) {
                                
                                /*
                                On recherche un accesseur à une propritété qui porte le même nom qu'un champ de la requête
                                - s'il y a au moins une propriété pour laquelle on n'en trouve pas, le prédicat ne retient pas l'élément
                                - si au contraire toutes les propriétés des résultats de requêtes sont retrouvées dans l'élément, 
                                on lit les valeurs et on les indexe par le nom de la propritété
                                */
                                final Method readMethod = eltProperties.get(pName);
                                if (readMethod == null) {
                                    return false;
                                } else {
                                    try {
                                        values.put(pName, readMethod.invoke(element));
                                    } catch (Exception ex) {
                                        throw new SirsCoreRuntimeException(ex);
                                    }
                                }
                            }

                            // 3- rejet des éléments pour lesquels il n'existe pas de résultat de requête ayant les mêmes
                            // valeurs aux propriétés correspondantes
                            boolean isEqual;
                            try (final FeatureIterator reader = queryResult.iterator()) {
                                
                                // parcours des résultats de la requête
                                while (reader.hasNext()) {
                                    isEqual = true;
                                    
                                    // parcours des champs du tuple courant
                                    final Feature next = reader.next();
                                    for (final Property p : next.getProperties()) {
                                        // dès qu'on trouve une propriété dont la valeur n'est pas égale à la valeur correspondante de l'élément, on rejette l'élément
                                        if (!propertiesEqual(p.getValue(), values.get(p.getName().tip().toString()))) {
                                            isEqual = false;
                                            break;
                                        }
                                    }
                                    if (isEqual) {
                                        return true;
                                    }
                                }
                            }

                            return false;
                        };
                    }

                    this.elements = elements.filter(predicate);
                }
            }
        }
    }

    /**
     * Test equality of input objects. It has been designed to manage numeric
     * properties with different precision.
     * @param p1 The first object to test
     * @param p2 The second object to test
     * @return True if input properties are equal, false otherwise.
     */
    private static boolean propertiesEqual(final Object p1, final Object p2) {
        if (p1 instanceof Double && p2 instanceof Float
                || p1 instanceof Float && p2 instanceof Double) {
            return ((Number)p1).floatValue() == ((Number)p2).floatValue();
        } else {
            return Objects.equals(p1, p2);
        }
    }
    
    public static List<String> propertyNames(FeatureType featureType){
        return featureType.getProperties(true).stream()
                .map(p -> p.getName().tip().toString())
                .collect(Collectors.toList());
    }
}


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
package fr.sirs.core.model;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.SQLQueryRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.DocumentNotFoundException;

/**
 * Utility class to load/save {@link SQLQuery} into property file.
 *
 * @author Alexis Manin
 * @author Johann Sorel
 */
public class SQLQueries {

    private static final char SEPARATOR = '§';
    
    public static final Comparator<SQLQuery> QUERY_COMPARATOR = (SQLQuery o1, SQLQuery o2) -> {
            if(o1.getLibelle()!=null && o2.getLibelle()!=null){
                return o1.getLibelle().compareTo(o2.getLibelle());
            }
            else return 0;
        };

    /**
     * Open a file containing SQL queries, and put them in memory.
     *
     * Note : no syntax check is done on loaded requests, so every query even if
     * it's malformed, is loaded.
     * 
     * Les requêtes sont triées par ordre alphabétique du libellé.
     *
     * @param queryFile The property file containing wanted requests.
     * @return A list of all queries listed in input file. Never null, but can
     * be empty.
     * @throws IOException If we cannot read into input file.
     */
    public static List<SQLQuery> openQueryFile(final Path queryFile) throws IOException {
        ArgumentChecks.ensureNonNull("Path to read queries from.", queryFile);
        final Properties props = new Properties();
        if (Files.isRegularFile(queryFile)) {
            try (InputStream in = Files.newInputStream(queryFile)) {
                props.load(in);
            }
        }
        return propertiesToQueries(props);
    }
    
    /**
     * Retourne les requêtes enregistrées en base, triées par ordre alphabétique du libellé.
     * 
     * @return une liste des requêtes enregistrées en base, triées par ordre alphabétique du libellé.
     */
    public static List<SQLQuery> dbQueries(){
        final SQLQueryRepository queryRepo = (SQLQueryRepository) InjectorCore.getBean(SessionCore.class).getRepositoryForClass(SQLQuery.class);
        final ObservableList<SQLQuery> dbQueries = FXCollections.observableArrayList(queryRepo.getAll());
        dbQueries.sort(SQLQueries.QUERY_COMPARATOR);
        return dbQueries;
    }

    /**
     * Lecture d'une propriété de fichier de requête : transformation d'une
     * propriété en objet de type SQLQuery (libellé, requête, description).
     * @param props Propriétés à lire.
     * @return Une liste de requêtes SQL lues depuis les propriétés données.
     */
    private static List<SQLQuery> propertiesToQueries(final Properties props){

        final List<SQLQuery> queries = new ArrayList<>();
        for (final Entry entry : props.entrySet()) {
            final SQLQuery query = new SQLQuery();
            query.setLibelle((String) entry.getKey());

            final String value = (String) entry.getValue();
            final int index = value.indexOf(SEPARATOR);
            if(index<=0){
                query.setSql(value);
                query.setDescription("");
            }else{
                query.setSql(value.substring(0, index));
                query.setDescription(value.substring(index+1));
            }
            queries.add(query);
        }
        queries.sort(QUERY_COMPARATOR);
        return queries;

    }
    
    // clef indiquant le libellé d'une requête préprogrammée
    private static final String PRE_QUERY_TITLE = "title";
    // clef indiquant la description d'une requête préprogrammée
    private static final String PRE_QUERY_DESCRIPTION = "description";
    // clef indiquant le corps d'une requête préprogrammée
    private static final String PRE_QUERY_QUERY = "query";
    
    /**
     * Interprétationd des properties des requêtes préprogrammées.
     * 
     * Chaque requête préprogrammée est représentée par trois propriétés dont la sépantique en fonction des suffixes
     * est la suivante :
     * [propertyPrefix].title : libellé de la requête
     * [propertyPrefix].query : corps de la requête en SQL
     * [propertyPrefix].description : descriptif de la requête
     * 
     * Les requêtes sont triées par ordre alphabétique des libellés.
     * 
     * @param props contenu du fichier .properties des requêtes préprogrammées.
     * @return 
     */
    private static List<SQLQuery> propertiesToPreprogrammedQueries(final Properties props){

        final Map<String, SQLQuery> queries = new HashMap<>();
        for (final Entry entry : props.entrySet()) {
            
            // Récupération de la clef properties
            final String key=(String)entry.getKey();
            final int idx = key.lastIndexOf('.');
            
            // Récupération de la partie identifiant de la requête : jusqu'au dernier point
            final String queryId = key.substring(0, idx);
            // Récupération de la partie descriptive de la requête : titre, description ou corps de la requête
            final String entryType = key.substring(idx+1);
            
            if(!queries.containsKey(queryId)){
                queries.put(queryId, new SQLQuery());
            }
            
            final SQLQuery query = queries.get(queryId);
            switch(entryType){
                case PRE_QUERY_TITLE: query.setLibelle((String)entry.getValue()); break;
                case PRE_QUERY_DESCRIPTION: query.setDescription((String)entry.getValue()); break;
                case PRE_QUERY_QUERY: query.setSql((String)entry.getValue()); break;
                default:
                    SirsCore.LOGGER.warning("type d'entrée inconnu dans le fichier de requêtes préprogrammées.");
            }
        }
        final List<SQLQuery> queryList = new ArrayList<>(queries.values());
        queryList.sort(QUERY_COMPARATOR);
        return queryList;
    }

    /**
     * Chargement des requêtes préprogrammées.
     * 
     * Les requêtes sont triées par ordre alphabétique des libellés.
     *
     * @return La liste des requêtes par défaut dans l'application.
     * @throws IOException Si les requêtes ne peuvent être lues depuis les ressources.
     */
    public static List<SQLQuery> preprogrammedQueries() throws IOException {
        final File localPreprogrammedFile = SirsCore.PREPROGRAMMED_QUERIES_PATH.toFile();
        if(localPreprogrammedFile.exists() && Files.isRegularFile(SirsCore.PREPROGRAMMED_QUERIES_PATH)){
            final Properties props = new Properties();
            try (final InputStream in = new FileInputStream(localPreprogrammedFile)) {
                props.load(in);
            }
            return propertiesToPreprogrammedQueries(props);
        }
        throw new IllegalStateException("le fichier des requêtes préprogrammées ne semble pas exister ou être dans un état inattendu.");
    }

    private static String getValueString(final SQLQuery query){
        return query.getSql()+SEPARATOR+query.getDescription();
    }

    /**
     * Load locally saved queries into memory.
     *
     * @return Previously locally saved queries. Can be empty, never null.
     * @throws IOException If we failed to read in system property file.
     */
    public static List<SQLQuery> getLocalQueries() throws IOException {
        return openQueryFile(SirsCore.LOCAL_QUERIES_PATH);
    }

    /**
     * Save queries in system local property file.
     *
     * Note : System file is overriden, so if it contained queries which are not
     * in input list, they're lost.
     *
     * @param queries The list of queries to save.
     * @throws IOException If an error occurred at writing.
     */
    public static void saveQueriesLocally(List<SQLQuery> queries) throws IOException {
        saveQueriesInFile(queries, SirsCore.LOCAL_QUERIES_PATH);
    }

    /**
     * Save queries in specified property file.
     *
     * Note : The file is overriden, so if it contained queries which are not in
     * input list, they're lost.
     *
     * @param queries The list of queries to save.
     * @param outputFile File to write queries into.
     * @throws IOException If an error occurred at writing.
     */
    public static void saveQueriesInFile(final List<SQLQuery> queries, final Path outputFile) throws IOException {
        ArgumentChecks.ensureNonNull("Queries to save.", queries);
        ArgumentChecks.ensureNonNull("File to save queries into.", outputFile);

        if (Files.isDirectory(outputFile)) {
            throw new IllegalArgumentException("Cannot save queries into a directory.");
        }
        final Properties props = new Properties();
        for (SQLQuery query : queries) {
            props.put(query.getLibelle(), getValueString(query));
        }
        try (OutputStream out = Files.newOutputStream(outputFile)) {
            props.store(out, "");
        }
    }

    /**
     * Search in database for a query with the given ID. If we cannot find any,
     * we'll search for a query in local default queries whose {@link SQLQuery#getLibelle() }
     * is equal to given string.
     * @param queryId Id (if stored in database) or label (if stored in default queries)
     * of the wanted query.
     * @return
     * @throws IOException
     */
    public static Optional<SQLQuery> findQuery(final String queryId) throws IOException {
        if (queryId == null || queryId.isEmpty())
            return Optional.empty();
        try {
            return Optional.of(InjectorCore.getBean(SessionCore.class).getRepositoryForClass(SQLQuery.class).get(queryId));
        } catch (DocumentNotFoundException e) {
            return SQLQueries.preprogrammedQueries().stream().filter(q -> q.getLibelle().equals(queryId)).findFirst();
        }
    }

    /**
     * CellFactory pour ListViews de SQLQueries.
     */
    public static class QueryListCellFactory implements Callback<ListView<SQLQuery>, ListCell<SQLQuery>> {

        @Override
        public ListCell<SQLQuery> call(ListView<SQLQuery> param) {
            return new SQLQueryListCell();
        }
    }

    /**
     * ListCell pour ListViews de SQLQueries.
     *
     * Affiche le libellé de la SQLQuery.
     */
    public static class SQLQueryListCell extends ListCell<SQLQuery> {

        @Override
        protected void updateItem(SQLQuery item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty && item != null) {
                this.textProperty().bind(item.libelleProperty());
            } else {
                this.textProperty().unbind();
                this.setText(null);
            }
        }
    }
}

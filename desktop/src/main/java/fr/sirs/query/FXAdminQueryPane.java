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
package fr.sirs.query;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.SQLQueryRepository;
import fr.sirs.core.model.Role;
import fr.sirs.core.model.SQLQueries;
import fr.sirs.core.model.SQLQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;

/**
 * A panel which allows an administrator to send locally saved queries into CouchDB,
 * or edit and remove queries.
 * 
 * /!\ WARNING : All modifications will be saved only if {@link #saveQueries() }
 * method is called.
 * 
 * TODO : Add methods to find doublons in each list, or common to lists.
 * TODO : add form at panel bottom to allow query edition.
 * 
 * @author Alexis Manin (Geomatys)
 */
public class FXAdminQueryPane extends BorderPane {
    
    @FXML
    private Button uiToDatabaseBtn;

    @FXML
    private ListView<SQLQuery> uiDBList;

    @FXML
    private Button uiToLocalBtn;

    @FXML
    private ListView<SQLQuery> uiLocalList;

    @FXML
    private Button uiDeleteDBBtn;

    @FXML
    private Button uiDeleteLocalBtn;
    
    @FXML
    private BorderPane uiBottomPane;
    
    private FXQueryPane queryEditor;
    
    // Notification lists. Contains element to add or remove at save.
    private final HashSet<SQLQuery> toAddInDB = new HashSet<>();
    private final HashMap<String, SQLQuery> toRemoveFromDB = new HashMap<>();
    private final HashSet<SQLQuery> toUpdate = new HashSet<>();
    
    /** 
     * A copy of the last edited query. So we will submit edited query for update 
     * only if its not equal to its copy.
     */
    private SQLQuery initialState;
    
    public FXAdminQueryPane() throws IOException {
        super();
        if (!Role.ADMIN.equals(Injector.getSession().getRole())) {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Ce panneau est reservé aux administrateurs !", ButtonType.OK);
            alert.setResizable(true);
            alert.show();
            setCenter(new Label("Seuls les administrateurs peuvent utiliser ce panneau."));
            return;
        }
        
        SIRS.loadFXML(this);
        
        // TOOLTIP DEFINITION
        uiToDatabaseBtn.setTooltip(new Tooltip("Déplacer la sélection en base de données."));
        uiToLocalBtn.setTooltip(new Tooltip("Déplacer la sélection vers le stockage local."));
        uiDeleteDBBtn.setTooltip(new Tooltip("Supprimer la sélection de la base de données."));
        uiDeleteLocalBtn.setTooltip(new Tooltip("Supprimer la sélection du système local."));
        
        uiLocalList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiDBList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiLocalList.setCellFactory(new SQLQueries.QueryListCellFactory());
        uiDBList.setCellFactory(new SQLQueries.QueryListCellFactory());
        
        // Fill query lists
        uiLocalList.setItems(FXCollections.observableArrayList(SQLQueries.getLocalQueries()));

        uiDBList.setItems(FXCollections.observableArrayList(SQLQueries.dbQueries()));
        
        // Listen on database list to know which elements we must update.
        uiDBList.getItems().addListener((ListChangeListener.Change<? extends SQLQuery> c) -> {
            while(c.next()) {
                if (c.wasAdded()) {
                    toAddInDB.addAll(c.getAddedSubList());
                }
                
                if (c.wasRemoved()) {
                    for (final SQLQuery query : c.getRemoved()) {
                        // Notify that we must delete query. If the query has no ID, 
                        // It's a query which has already switched of list since last update.
                        if (query.getId() == null) {
                            toAddInDB.remove(query);
                        } else {
                            toRemoveFromDB.put(query.getId(), query);                            
                        }
                    }
                }
            }
        });
        
        // delete button actions
        uiDeleteLocalBtn.setOnAction((ActionEvent e)-> deleteSelection(uiLocalList));
        uiDeleteDBBtn.setOnAction((ActionEvent e)-> deleteSelection(uiDBList));
        
        queryEditor = new FXQueryPane();
        
        // As focused item is not cleared when a list lose focus, we're forced to make update ourself on click.
//        uiLocalList.getFocusModel().focusedItemProperty().addListener(this::updateEditor);
//        uiDBList.getFocusModel().focusedItemProperty().addListener(this::updateEditor);
        uiLocalList.setOnMouseClicked((MouseEvent e)-> updateEditor(null, null, uiLocalList.getFocusModel().getFocusedItem()));
        uiDBList.setOnMouseClicked((MouseEvent e)-> updateEditor(null, null, uiDBList.getFocusModel().getFocusedItem()));
        
        uiBottomPane.setCenter(queryEditor);        
    }

    /**
     * Update query editor content. This method has been designed to serve as 
     * changeListener on focused item of {@link #uiDBList} and {@link #uiLocalList}.
     * @param observable The ListView on which the focus has been requested.
     * @param oldValue The previously focused element.
     * @param newValue The current focused element.
     */
    private void updateEditor(ObservableValue<? extends SQLQuery> observable, SQLQuery oldValue, SQLQuery newValue) {
        if (newValue != null) {
            /* First, we check if last edited query has been modified. Check is needed
             * only for queries already inserted in database, because all local queries
             * will be updated at save, and queries moved to db will be added anyway.
             * Also, if the query is already triggered for deletion, there's no need 
             * for update.
             */
            final SQLQuery previouslyEdited = queryEditor.getSQLQuery();
            if (previouslyEdited != null && previouslyEdited.getId() != null) {
                if (!previouslyEdited.equals(initialState) && !toRemoveFromDB.containsKey(previouslyEdited.getId())) {
                    toUpdate.add(previouslyEdited);
                }
            }
            initialState = newValue.copy();
            queryEditor.setSQLQuery(newValue);
        }
    }
    
    /**
     * Save queries (re)moved using the panel. It means insertions / deletion in
     * couchDB and local system properties.
     */
    void saveQueries() {
        final Task t = new Task() {

            @Override
            protected Object call() throws Exception {
                updateTitle("Sauvegarde de requêtes SQL");
                
                updateMessage("Sauvegarde des requêtes locales.");
                SQLQueries.saveQueriesLocally(uiLocalList.getItems());

                updateMessage("Mise à jour des requêtes dans la base de données.");
                // Update database. For each element updated, we can remove it from notification lists.
                final SQLQueryRepository queryRepo = (SQLQueryRepository)Injector.getSession().getRepositoryForClass(SQLQuery.class);
                Iterator<SQLQuery> addIt = toAddInDB.iterator();
                while (addIt.hasNext()) {
                    queryRepo.add(addIt.next());
                    addIt.remove();
                }
                
                Iterator<SQLQuery> updateIt = toUpdate.iterator();
                while (updateIt.hasNext()) {
                    queryRepo.update(updateIt.next());
                    updateIt.remove();
                }
                
                Iterator<SQLQuery> removeIt = toRemoveFromDB.values().iterator();
                while (removeIt.hasNext()) {
                    queryRepo.remove(removeIt.next());
                    removeIt.remove();
                }
                return null;
            }
        };
        
        TaskManager.INSTANCE.submit(t);
        
    }

    @FXML
    void localToDatabase(ActionEvent event) {
        transferFromListToList(uiLocalList, uiDBList);
    }

    @FXML
    void databaseToLocal(ActionEvent event) {
        transferFromListToList(uiDBList, uiLocalList);
    }
    
    boolean deleteSelection(final ListView source) {
        ObservableList<SQLQuery> selectedItems = source.getSelectionModel().getSelectedItems();
        return source.getItems().removeAll(selectedItems);
    }
    
    /**
     * Move (not copy) selected items of one list to another.
     * 
     * @param source The list to get and cut selection from.
     * @param destination The destination to put selection into.
     */
    private void transferFromListToList(final ListView source, final ListView destination) {
        // Get list selection
        final ObservableList<SQLQuery> selectedItems = source.getSelectionModel().getSelectedItems();

        // On copie la liste pour éviter toute interaction malencontreuse lors du retrait de la liste d'origine (puisque sa liste d'items sélectionnés va évoluer).
        final List<SQLQuery> copy = new ArrayList<>(selectedItems);

        // add it in destination 
        destination.getItems().addAll(copy);

        // remove from source list
//        source.getItems().removeAll(copy); // Ne fonctionne pas car de la copie local -> BD, l'identifiant peut valoir null or c'est avec l'id que sont construites hashCode() et equals()
        // On vérifie donc dans la liste la présence des éléments au sens de la référence.
        source.getItems().removeIf(query -> {
            for(final SQLQuery q : copy){
                if(q==query) return true;
            }
            return false;
        });
    }
    
    /**
     * Show a dialog for query transfer. Return only if user has saved or canceled changes.
     */
    public static void showAndWait() {
        final Stage dialog = new Stage();
        dialog.getIcons().add(SIRS.ICON);
        dialog.setTitle("Administration des requêtes");
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(Injector.getSession().getFrame().getScene().getWindow());

        final FXAdminQueryPane adminPanel;
        try {
            adminPanel = new FXAdminQueryPane();
        } catch (IOException ex) {
            SIRS.LOGGER.log(Level.WARNING, null, ex);
            GeotkFX.newExceptionDialog("Une erreur est survenue lors de la construction des listes de requêtes.", ex).show();
            return;
        }

        final Button cancelBtn = new Button("Annuler");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction((ActionEvent e) -> dialog.close());

        final Button saveBtn = new Button("Sauvegarder");
        saveBtn.setOnAction((ActionEvent e) -> {
            adminPanel.saveQueries();
            dialog.close();
        });

        final BorderPane dialogPane = new BorderPane(adminPanel);
        final ButtonBar hBox = new ButtonBar();
        hBox.setPadding(new Insets(5));
        hBox.getButtons().addAll(cancelBtn, saveBtn);
        dialogPane.setBottom(hBox);

        dialog.setScene(new Scene(dialogPane));
        dialog.showAndWait();
    }
}

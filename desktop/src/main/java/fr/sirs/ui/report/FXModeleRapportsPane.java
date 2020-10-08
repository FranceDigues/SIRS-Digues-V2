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
package fr.sirs.ui.report;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.report.ModeleRapport;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.ui.Growl;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;

/**
 * Displays list of available models for edition. The {@link #editor} displays
 * currently selected model, but it is not added into the scene by default. The
 * aim is to let user get the editor and put it wherever he wants. For example,
 * you could display it just next to the model list by calling {@link #setRight(javafx.scene.Node) }
 * with {@link #editor} as parameter.
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXModeleRapportsPane extends BorderPane {
    
    @FXML
    private ListView<Preview> uiReportList;
    
    @FXML
    private Button uiAdd;
    
    @FXML
    private Button uiDelete;

    /**
     * Editor for currently selected model
     */
    public final AbstractFXElementPane<ModeleRapport> editor;

    private final AbstractSIRSRepository<ModeleRapport> repo;

    public FXModeleRapportsPane() {
        super();
        SIRS.loadFXML(this);
        
        
        uiAdd.disableProperty().bind(Injector.getSession().adminOrUserOrExtern().not());
        uiDelete.disableProperty().bind(Injector.getSession().adminOrUserOrExtern().not());

        final Session session = Injector.getSession();
        repo = session.getRepositoryForClass(ModeleRapport.class);
        if (repo == null) {
            throw new IllegalStateException("No repository available for type "+ModeleRapport.class.getCanonicalName());
        }

        uiReportList.getSelectionModel().selectedItemProperty().addListener(this::selectionChanged);
        uiReportList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        

        uiReportList.setItems(SIRS.observableList(session.getPreviews().getByClass(ModeleRapport.class)).sorted());
        uiReportList.setCellFactory((ListView<Preview> param) -> {
            return new ListCell<Preview>() {

                @Override
                protected void updateItem(Preview item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        final String libelle = item.getLibelle();
                        setText((libelle == null || libelle.isEmpty())? "Sans nom" : libelle);
                    }
                    
                    if (item != null && !item.getValid()) {
                        getStyleClass().add("invalidRow");
                    } else {
                        getStyleClass().removeAll("invalidRow");
                    }
                }
            };
        });

        editor = SIRS.generateEditionPane(null);
        editor.visibleProperty().bind(editor.elementProperty().isNotNull());
        editor.managedProperty().bind(editor.visibleProperty());
        editor.setMinSize(10, 10);
        editor.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
    }

    public ReadOnlyObjectProperty<ModeleRapport> selectedModelProperty() {
        return editor.elementProperty();
    }

    @FXML
    void addReport(ActionEvent event) {
        final ModeleRapport newModele = repo.create();
        newModele.setLibelle("Nouveau modèle");
        repo.add(newModele);

        final Preview p = new Preview();
        p.setElementClass(ModeleRapport.class.getCanonicalName());
        p.setDocClass(p.getElementClass());
        p.setElementId(newModele.getId());
        p.setDocId(newModele.getId());
    }
    
    @FXML
    void deleteReport(ActionEvent event) {
        final ObservableList<Preview> selectedItems = uiReportList.getSelectionModel().getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        final Alert alert = new Alert(
                Alert.AlertType.WARNING,
                "Vous allez supprimer définitivement les modèles séléctionnés. Êtes-vous sûr ?",
                ButtonType.NO, ButtonType.YES
        );

        alert.setResizable(true);
        if (ButtonType.YES.equals(alert.showAndWait().orElse(ButtonType.NO))) {
            
            // 1- on commence par récupérer les identifiants à partir des éléments sélectionnés
            final String[] ids = new String[selectedItems.size()];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = selectedItems.get(i).getElementId();
            }
            
            // 2- on récupère les modèles de rapport en base.
            final List<ModeleRapport> modeles = repo.get(ids);
            
            // 3- on vérifie pour chaque rapport s'il peut être supprimé par l'utilisateur connecté
            final Iterator<ModeleRapport> it = modeles.iterator();
            boolean authorized = true;
            while(it.hasNext()){
                final ModeleRapport modele = it.next();
                
                // On vérifie les droits de l'utilisateur courant à supprimer le modèle
                if(!Injector.getSession().editionAuthorized(modele)){
                    it.remove();// On enlève le modèle de la liste à supprimer.
                    authorized=false;
                }
            } 
            
            // 4- si on a détecté des rapports qui ne peuvent être supprimés, on avertit l'utilisateur
            if (!authorized) {
                new Growl(Growl.Type.WARNING, "Certains éléments n'ont pas été supprimés car vous n'avez pas les droits nécessaires.").showAndFade();
            }
            
            // 5- on applique la suppression en base
            repo.executeBulkDelete(modeles);
        }
    }

    private void selectionChanged(final ObservableValue<? extends Preview> obs, Preview oldValue, Preview newValue) {
        if (oldValue != null) {
            oldValue.libelleProperty().unbind();
        }

        if (newValue != null) {
            final ModeleRapport rapport = repo.get(newValue.getElementId());
            newValue.libelleProperty().bind(rapport.libelleProperty());
            editor.setElement(rapport);
        } else {
            editor.setElement(null);
        }
    }
}

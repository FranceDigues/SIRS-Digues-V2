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
package fr.sirs.other;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.ICON_EXCLAMATION_CIRCLE;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.ReferenceUsageRepository;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.core.model.ReferenceUsage;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import org.ektorp.DbAccessException;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.gui.javafx.util.TaskManager;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @author Aleis Manin   (Geomatys)
 */
public class FXReferenceAnalysePane extends BorderPane {

    private final ReferenceType reference;
    private final Label uiCount = new Label();
    
    public FXReferenceAnalysePane(final ReferenceType reference) {
        
        this.reference = reference;
        
        // 1- Mise en forme du titre avec le nom de la référence, le nombre d'utilisations et un bouton de suppression
        //============================================================================================================
        final LabelMapper mapper = LabelMapper.get(reference.getClass());
        final Label uiTitle = new Label(new StringBuilder("Usages dans la base de la référence \"")
                .append(reference.getLibelle())
                .append("\" (").append(mapper.mapClassName()).append(")").toString());
        uiTitle.getStyleClass().add("pojotable-header");
        uiTitle.setAlignment(Pos.CENTER);
        uiTitle.setPadding(new Insets(5));
        uiTitle.setPrefWidth(USE_COMPUTED_SIZE);
        
        // libellé du décompte du nombre d'utilisations
        uiCount.getStyleClass().add("pojotable-header");
        uiCount.setAlignment(Pos.CENTER);
        uiCount.setPadding(new Insets(5));
        uiCount.setPrefWidth(USE_COMPUTED_SIZE);
        // bouton de suppression de la référence
        final Button uiDeleteReference = new Button("Supprimer la référence", new ImageView(ICON_EXCLAMATION_CIRCLE));
        
        
        // 3- Action de suppression de la référence
        //=========================================
        uiDeleteReference.setOnAction((ActionEvent event) -> {
            
            // On recalcule l'utilisation de la référence
            final List<ReferenceUsage> usage = getUsage(reference);
            
            // On vérifie que l'identifiant de la référence n'est utilisé que par elle-même.
            if(usage.size()==1 && reference.getId().equals(usage.get(0).getObjectId())){
                
                // on supprime la référence de la base
                ((AbstractSIRSRepository) Injector.getSession().getRepositoryForClass(reference.getClass())).remove(reference);
                
                new Alert(Alert.AlertType.CONFIRMATION, "La référence a été supprimée de la base locale.", ButtonType.CLOSE).showAndWait();
                
                // On met à jour le contenu du tableau
                Injector.getSession().getTaskManager().submit(newRefreshTask());
            }
            else if (usage.size()==0){
                new Alert(Alert.AlertType.INFORMATION, "La référence a déjà été supprimée de la base locale.", ButtonType.OK).showAndWait();
            }
            else {
                // Sinon, on refuse la suppression de la référence
                new Alert(Alert.AlertType.ERROR, "Pour des raisons de cohérence des données,\n "
                        + "il est impossible de supprimer des références utilisées par des documents depuis l'application.\n "
                        + "Si vous souhaitez vraiment supprimer cette référence,\n veuillez supprimer directement le document"
                        + " "+reference.getId()+" dans la base de données.", 
                        ButtonType.CLOSE).showAndWait();
            }
        });
        
        // 4- Mise en page des éléments graphiques
        //========================================
        final HBox hBox = new HBox(uiTitle, uiCount, uiDeleteReference);
        hBox.setSpacing(20);
        setTop(hBox);

        // 5- Calcul initial de l'utilisation de la référence
        //===================================================
        Injector.getSession().getTaskManager().submit(newRefreshTask());
    }
    
    /**
     * Tâche de calcul de l'utilisation de la référence avec raffraîchissement de l'UI.
     */
    private Task<List<ReferenceUsage>> newRefreshTask(){
        final Task<List<ReferenceUsage>> task = new TaskManager.MockTask(
                "Recherche de l'utilisation d'une référence.",
                () -> getUsage(reference));
        
        // À l'issue du calcul, on met à jour la liste des utilisations de la référence ainsi que le décompte.
        task.setOnSucceeded(evt -> SIRS.fxRun(false, () -> {
            final List<ReferenceUsage> value = task.getValue();
            displayReferences(value);
            uiCount.setText(String.valueOf(value.size()));
                }));
        
        return task;
    }

    /**
     * Display given reference usages in a table view.
     * @param referenceUsages Objects to display.
     */
    private void displayReferences(final List<ReferenceUsage> referenceUsages) {
        if (referenceUsages.isEmpty()) {
            setCenter(null);
        } else {
            final LabelMapper mapper = LabelMapper.get(ReferenceUsage.class);

            final TableView<ReferenceUsage> usages = new TableView<>(SIRS.observableList(referenceUsages));
            usages.setEditable(false);

            final TableColumn<ReferenceUsage, String> typeColumn = new TableColumn<>(mapper.mapPropertyName("type"));
            typeColumn.setCellValueFactory((TableColumn.CellDataFeatures<ReferenceUsage, String> param) -> {
                String type = param.getValue().getType();
                try {
                    LabelMapper targetMapper = LabelMapper.get(Class.forName(type, false, Thread.currentThread().getContextClassLoader()));
                    if (targetMapper != null) {
                        type = targetMapper.mapClassName();
                    }
                } catch (ClassNotFoundException e) {
                    SIRS.LOGGER.log(Level.FINE, "Cannot find title for type ".concat(type), e);
                }

                return new SimpleObjectProperty(type);
            });

            final TableColumn<ReferenceUsage, String> labelColumn = new TableColumn<>(mapper.mapPropertyName("label"));
            labelColumn.setCellValueFactory((TableColumn.CellDataFeatures<ReferenceUsage, String> param) -> new SimpleStringProperty(param.getValue().getLabel()));

            usages.getColumns().addAll(new EditColumn(), typeColumn, labelColumn);
            setCenter(usages);
        }
    }

    /**
     * Find the element pointed by input {@link ReferenceUsage#getObjectId() }
     * property, and display an editor for it when user click on the cell.
     */
    public static class EditColumn extends TableColumn<ReferenceUsage, String> {

        public EditColumn() {
            super("Edition");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(SIRS.ICON_EDIT_BLACK));

            final Tooltip tooltip = new Tooltip("Ouvrir la fiche de l'élément");

            final Predicate<String> enabled = param -> param != null && !param.isEmpty();
            final Function<String, Boolean> openEditor = param -> {
                Injector.getSession().showEditionTab(param);
                return true;
            };

            setCellValueFactory((param) -> new SimpleStringProperty(param.getValue().getObjectId()));

            setCellFactory((TableColumn<ReferenceUsage, String> param) -> {
                final ButtonTableCell button = new ButtonTableCell(false, new ImageView(SIRS.ICON_EDIT_BLACK), o -> o != null, openEditor);
                button.setTooltip(tooltip);
                return button;
            });
        }
    }

    /**
     * Find usages of a given reference in database.
     *
     * Note : as view can be long to build, we loop over timeout errors, until a
     * result or another error is thrown.
     * @param ref The reference to find usage for in database.
     * @return List of found references.
     */
    private List<ReferenceUsage> getUsage(final ReferenceType ref) {
        final ReferenceUsageRepository repo = Injector.getSession().getReferenceUsageRepository();
        Throwable e = new TimeoutException();
        while ((e instanceof TimeoutException || e instanceof InterruptedIOException) && !Thread.currentThread().isInterrupted()) {
            try {
                return repo.getReferenceUsages(ref.getId());
            } catch (DbAccessException newException) {
                final Throwable tmp = newException.getCause() == null? newException : newException.getCause();
                tmp.addSuppressed(e);
                e = tmp;
            }
        }

        throw new DbAccessException(e);
    }
}

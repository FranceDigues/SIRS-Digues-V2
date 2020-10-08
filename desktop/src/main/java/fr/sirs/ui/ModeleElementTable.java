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
package fr.sirs.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.report.ModeleElement;
import fr.sirs.theme.ui.AbstractFXElementPane;
import fr.sirs.theme.ui.PojoTable;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/**
 * Table displaying list of available models for element printing. Special behavior :
 * Model editor is opened next to the table.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class ModeleElementTable extends PojoTable {

    private AbstractFXElementPane editor;

    public ModeleElementTable() {
        super(Injector.getSession().getRepositoryForClass(ModeleElement.class), "Modèles .odt enregistrés en base", (ObjectProperty<Element>) null);
        editableProperty().set(false);
        detaillableProperty().set(false);
        fichableProperty().set(false);
        importPointProperty().set(false);
        commentAndPhotoProperty().set(false);
        searchVisibleProperty().set(false);
        exportVisibleProperty().set(false);
        ficheModeVisibleProperty().set(false);
        filterVisibleProperty().set(false);

        /*
        On autorise l'ajout uniquement pour les roles "administrateur, "utilisateur"
        et "externe" (pas pour les invités).
        On désactive donc les boutons d'ajout si l'utilisateur courant n'est pas dans un de ces rôles.
        */
        uiAdd.disableProperty().unbind();
        uiAdd.disableProperty().bind(Injector.getSession().adminOrUserOrExtern().not());

        uiDelete.disableProperty().unbind();
        uiDelete.disableProperty().bind(Injector.getSession().adminOrUserOrExtern().not());

        for(TableColumn col : getColumns()) {
            if("Désignation".equalsIgnoreCase(col.getText())){
                col.setVisible(false);
            }
        }

        getColumns().add((TableColumn) new StateColumn());

        getTable().setMaxWidth(USE_COMPUTED_SIZE);
        getTable().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        final TableView.TableViewSelectionModel<Element> selectionModel = getTable().getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener((obs, oldValue, newValue) -> editPojo(newValue));

        setRight(new BorderPane());
    }


    @Override
    protected Object editPojo(Object pojo) {
        if (editor == null) {
            editor = SIRS.generateEditionPane((Element)pojo);
            setRight(editor);
            editor.visibleProperty().bind(editor.elementProperty().isNotNull());
        } else {
            editor.setElement((Element)pojo);
        }

        return pojo;
    }

    private static class StateColumn extends TableColumn<ModeleElement, byte[]>{
        public StateColumn(){
            super("État du modèle");
            setCellValueFactory((CellDataFeatures<ModeleElement, byte[]> param) -> param.getValue().odtProperty());
            setCellFactory((TableColumn<ModeleElement, byte[]> param) ->  new StateCell());
        }
    }

    private static class StateCell extends TableCell<ModeleElement, byte[]>{
        public StateCell(){
            super();
            setEditable(false);
        }

        @Override
        public void updateItem(final byte[] item, final boolean empty){

            setGraphic(null);
            if(empty){
                setStyle(null);
                setText(null);
            }
            else if(item==null || item.length==0){
                setStyle("-fx-background-color: red");
                setText("Aucun modèle défini");
            }
            else{
                setStyle("-fx-background-color: green");
                setText("Modèle présent");
            }
        }
    }
}

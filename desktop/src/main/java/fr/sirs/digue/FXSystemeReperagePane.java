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
package fr.sirs.digue;

import fr.sirs.Injector;
import fr.sirs.Session;
import fr.sirs.SIRS;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXSystemeReperagePane extends BorderPane {

    @FXML private TextField uiNom;
    @FXML private TextArea uiComment;
    @FXML private DatePicker uiDate;

    private final ObjectProperty<SystemeReperage> srProperty = new SimpleObjectProperty<>();
    private final BorneTable borneTable = new BorneTable();
    private final BooleanProperty editableProperty = new SimpleBooleanProperty(true);

    public FXSystemeReperagePane(){
        SIRS.loadFXML(this);

        setCenter(borneTable);

        srProperty.addListener(this::updateFields);

        this.visibleProperty().bind(srProperty.isNotNull());

        uiNom.editableProperty().bind(editableProperty);
        uiComment.disableProperty().bind(editableProperty.not());
        uiDate.setDisable(true);
        // Client query...
        borneTable.editableProperty().set(false);
    }

    public BooleanProperty editableProperty(){
        return editableProperty;
    }

    public ObjectProperty<SystemeReperage> getSystemeReperageProperty() {
        return srProperty;
    }

    private void updateFields(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue) {
        if (oldValue != null) {
            uiNom.textProperty().unbindBidirectional(oldValue.libelleProperty());
            uiComment.textProperty().unbindBidirectional(oldValue.commentaireProperty());
            uiDate.valueProperty().unbindBidirectional(oldValue.dateMajProperty());
            borneTable.getUiTable().setItems(FXCollections.emptyObservableList());
        }

        if(newValue==null) return;
        uiNom.textProperty().bindBidirectional(newValue.libelleProperty());
        uiDate.valueProperty().bindBidirectional(newValue.dateMajProperty());
        uiComment.textProperty().bindBidirectional(newValue.commentaireProperty());

        borneTable.getUiTable().setItems(newValue.systemeReperageBornes);
    }

    public void save(){
        final SystemeReperage sr = srProperty.get();
        if(sr==null) return;

        final Session session = Injector.getBean(Session.class);
        final SystemeReperageRepository repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);

        final String tcId = sr.getLinearId();
        if (tcId == null || tcId.isEmpty()) {
            throw new IllegalArgumentException("Aucun tronçon n'est associé au SR. Sauvegarde impossible.");
        }
        final TronconDigue troncon = session.getRepositoryForClass(TronconDigue.class).get(tcId);
        repo.update(sr, troncon);
    }

    private class BorneTable extends PojoTable {

        public BorneTable() {
            super(SystemeReperageBorne.class, "Liste des bornes", (ObjectProperty<Element>) null);
            getColumns().remove((TableColumn) editCol);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiDelete.setVisible(false);
            uiFicheMode.setVisible(false);
        }

        @Override
        protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
            //on ne sauvegarde pas, le formulaire conteneur s'en charge
        }
    }

}

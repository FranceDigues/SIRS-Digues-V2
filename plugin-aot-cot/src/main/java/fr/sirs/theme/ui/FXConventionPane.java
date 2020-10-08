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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.core.component.AbstractPositionDocumentRepository;
import fr.sirs.core.model.Convention;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXConventionPane extends FXConventionPaneStub {

    // Surcharge du tableau des positions
    final PositionConventionPojoTable positionDocumentTable;

    /**
     * Constructor. Initialize part of the UI which will not require update when element edited change.
     */
    private FXConventionPane() {

        super();
        positionDocumentTable = new PositionConventionPojoTable(null, elementProperty());
        positionDocumentTable.editableProperty().bind(disableFieldsProperty().not());
        ui_positionDocument.setContent(positionDocumentTable);
        ui_positionDocument.setClosable(false);
    }

    public FXConventionPane(final Convention convention){
        this();
        this.elementProperty().set(convention);
//
//        organismeSignataireIdsTable.setObservableListToListen(elementProperty.get().getOrganismeSignataireIds());
//        contactSignataireIdsTable.setObservableListToListen(elementProperty.get().getContactSignataireIds());
    }


    /**
     * Initialize fields at element setting.
     * @param observableElement
     * @param oldElement
     * @param newElement
     */
    @Override
    protected void initFields(ObservableValue<? extends Convention > observableElement, Convention oldElement, Convention newElement) {

        super.initFields(observableElement, oldElement, newElement);
        positionDocumentTable.setPropertyToListen("sirsdocumentProperty", elementProperty().get().getId());
        positionDocumentTable.setTableItems(()-> (ObservableList) AbstractPositionDocumentRepository.getPositionDocumentByDocumentId(elementProperty().get().getId(), Injector.getSession()));
    }

}

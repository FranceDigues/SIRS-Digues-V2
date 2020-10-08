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
package fr.sirs.theme.ui.pojotable;

import fr.sirs.core.model.PointXYZ;
import fr.sirs.theme.ui.FXAbstractImportPointLeve;
import fr.sirs.theme.ui.FXImportDZ;
import fr.sirs.theme.ui.FXImportXYZ;
import fr.sirs.theme.ui.PojoTable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

/**
 * Comportement par défaut des pojotables à l'action sur le bouton d'import.
 * 
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class ImportAction implements EventHandler<ActionEvent> {
    
    private final Class<?> pojoClass;
    private final PojoTable pojoTable;

    public ImportAction(Class<?> pojoClass, PojoTable pojoTable) {
        this.pojoClass = pojoClass;
        this.pojoTable = pojoTable;
    }
    
    @Override
    public void handle(ActionEvent event) {
        final FXAbstractImportPointLeve importCoord;
        if(PointXYZ.class.isAssignableFrom(pojoClass)) importCoord = new FXImportXYZ(pojoTable);
        else importCoord = new FXImportDZ(pojoTable);

        final Dialog dialog = new Dialog();
        final DialogPane pane = new DialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setContent(importCoord);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Import de points");
        dialog.setOnCloseRequest(event1 -> dialog.hide());
        dialog.show();
    }
    
}

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

import fr.sirs.util.SirsStringConverter;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T> The type of retrieved element
 * @param <C> The type of ComboBox items
 */
public abstract class PojoTableComboBoxChoiceStage<T, C> extends PojoTableChoiceStage<T> {

    protected final ComboBox<C> comboBox = new ComboBox<>();

    protected final Button cancel = new Button("Annuler");
    protected final Button add= new Button("ajouter");

    protected PojoTableComboBoxChoiceStage() {
        comboBox.setConverter(new SirsStringConverter());

        cancel.setOnAction((ActionEvent event) -> {
            retrievedElement.unbind();
            retrievedElement.set(null);
            hide();
        });
        add.setOnAction((ActionEvent event) -> {
            hide();
        });

        final HBox hBox = new HBox(cancel, add);
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(20);
        hBox.setPadding(new Insets(20));

        final VBox vBox = new VBox(comboBox, hBox);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(20));
        setScene(new Scene(vBox));
    }
    
    protected PojoTableComboBoxChoiceStage(String okButtonTitle){
        this();        
        add.setText(okButtonTitle);

    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.theme;

import fr.sirs.Injector;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.GlobalPrestation;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class GlobalPrestationTheme extends Theme {

    private static final String GLOBAL_PRESTATION_THEME_TITLE = "Prestation Globale";

    public GlobalPrestationTheme() {
        super(GLOBAL_PRESTATION_THEME_TITLE, Type.UNLOCALIZED);
    }

    @Override
    public Parent createPane() {

        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        PojoTable pojoTable = new PojoTable(Injector.getSession().getRepositoryForClass(GlobalPrestation.class), getName(), (ObjectProperty<? extends Element>) null);
        pojoTable.editableProperty().bind(editMode.editionState());
        return new BorderPane(pojoTable, topPane, null, null, null);

    }

}

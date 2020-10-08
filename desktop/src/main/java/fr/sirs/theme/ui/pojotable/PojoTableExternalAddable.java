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

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.AbstractObservation;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Role;
import fr.sirs.theme.ui.PojoTable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class PojoTableExternalAddable<T extends AbstractObservation> extends PojoTable {

    /**
     * Contrôle de l'éditabilité du tableau spécifique, même lorsqu'on n'est pas
     * en mode éditable en général. Faux a priori, doit être mis à vrai pour
     * être activé.
     */
    private final BooleanProperty externEditableProperty = new SimpleBooleanProperty(false);
    protected final Button uiExternAdd = new Button(null, new ImageView(SIRS.ICON_ADD_WHITE));// bouton d'ajout spécifique aux utilisateurs externes
    protected final Button uiExternDelete = new Button(null, new ImageView(SIRS.ICON_TRASH_WHITE));// bouton de suppression spécifique aux utilisateurs externes

    public PojoTableExternalAddable(Class<? extends AbstractObservation> type, ObjectProperty<? extends Element> container) {
        super(type, null, container);

        externEditableProperty.bind(Injector.getSession().roleBinding().isEqualTo(Role.EXTERN));

        // A- ajout
        //==============================================================================================================
        uiAdd.visibleProperty().bind(externEditableProperty.not().or(editableProperty));
        uiExternAdd.managedProperty().bind(uiExternAdd.visibleProperty());
        uiExternAdd.visibleProperty().bind(externEditableProperty.and(uiAdd.disabledProperty()));

        uiExternAdd.getStyleClass().add(BUTTON_STYLE);
        uiExternAdd.setOnAction(uiAdd.getOnAction());// même action que le bouton d'ajout classique

        uiExternAdd.setTooltip(new Tooltip(createNewProperty.get() ? "Créer un nouvel élément (externe)" : "Ajouter un élément existant (externe)"));
        createNewProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                uiExternAdd.setTooltip(new Tooltip("Créer un nouvel élément (externe)"));
            } else {
                uiExternAdd.setTooltip(new Tooltip("Ajouter un élément existant (externe)"));
            }
        });

        // B- retrait
        //==============================================================================================================
        uiDelete.visibleProperty().bind(externEditableProperty.not().or(editableProperty));
        uiExternDelete.managedProperty().bind(uiExternDelete.visibleProperty());
        uiExternDelete.visibleProperty().bind(externEditableProperty.and(uiDelete.disabledProperty()));

        uiExternDelete.getStyleClass().add(BUTTON_STYLE);
        uiExternDelete.setOnAction(uiDelete.getOnAction());// même action que le bouton de suppression classique

        uiExternDelete.setTooltip(new Tooltip("Supprimer les éléments sélectionnés (externe)"));

        // C- insertion dans la barre d'outils
        //==============================================================================================================
        final ObservableList<Node> toolbarButtons = searchEditionToolbar.getChildren();
        toolbarButtons.add(toolbarButtons.indexOf(uiAdd) + 1, uiExternAdd); // On insère le bouton d'ajout
        toolbarButtons.add(toolbarButtons.indexOf(uiDelete) + 1, uiExternDelete); // On insère le bouton d'ajout
    }
}

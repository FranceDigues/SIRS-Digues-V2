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
package fr.sirs.plugin.dependance;

import fr.sirs.Injector;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Panneau regroupant les désordres pour les dépendances.
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class DesordresDependanceTheme extends AbstractPluginsButtonTheme {
    private static final Image BUTTON_IMAGE = new Image(
            DesordresDependanceTheme.class.getResourceAsStream("images/desordre.png"));

    public DesordresDependanceTheme() {
        super(LabelMapper.get(DesordreDependance.class).mapClassName(), "Gestion des désordres sur dépendances.", BUTTON_IMAGE);
    }

    /**
     * Création du panneau principal de ce thème qui regroupera tous les éléments.
     *
     * @return Le panneau généré pour ce thème.
     */
    @Override
    public Parent createPane() {
        // Gestion du bouton consultation / édition pour la pojo table
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final PojoTable dependancesTable = new PojoTable(Injector.getSession().getRepositoryForClass(DesordreDependance.class), "Liste des désordres", (ObjectProperty<? extends Element>) null);
        dependancesTable.editableProperty().bind(editMode.editionState());
        return new BorderPane(dependancesTable, topPane, null, null, null);
    }
}

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
import fr.sirs.core.model.Element;
import fr.sirs.map.FXMapTab;
import java.util.function.Predicate;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;

/**
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class ShowOnMapColumn extends TableColumn<Element,Element> {

    public ShowOnMapColumn(Callback cellValueFactory, Image icon, Predicate visiblePredicate) {
        super("Afficher sur la carte");
        setSortable(false);
        setResizable(false);
        setPrefWidth(24);
        setMinWidth(24);
        setMaxWidth(24);
        setGraphic(new ImageView(icon));

        final Tooltip tooltip = new Tooltip("Voir l'élément sur la carte");
        setCellValueFactory(cellValueFactory);
        setCellFactory((TableColumn<Element, Element> param) -> {
            final ButtonTableCell<Element, Element> button = new ButtonTableCell<>(
                    false,
                    new ImageView(icon),
                    visiblePredicate,
                    ShowOnMapColumn::showOnMap);
            button.setTooltip(tooltip);
            return button;
        });
    }

    private static Element showOnMap(final Element e) {
        final FXMapTab tab = Injector.getSession().getFrame().getMapTab();
        tab.getMap().focusOnElement(e);
        tab.show();
        return e;
    }
}

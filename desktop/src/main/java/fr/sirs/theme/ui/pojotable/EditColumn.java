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

import fr.sirs.SIRS;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;

/**
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class EditColumn extends TableColumn {

    public EditColumn(Callback cellValueFactory, Function editFct, Predicate visiblePredicate) {
        super("Edition");
        setSortable(false);
        setResizable(false);
        setPrefWidth(24);
        setMinWidth(24);
        setMaxWidth(24);
        setGraphic(new ImageView(SIRS.ICON_EDIT_BLACK));

        final Tooltip tooltip = new Tooltip("Ouvrir la fiche de l'élément");

        setCellValueFactory(cellValueFactory);

        setCellFactory(new Callback<TableColumn, TableCell>() {

            @Override
            public TableCell call(TableColumn param) {
                ButtonTableCell button = new ButtonTableCell(
                        false, new ImageView(SIRS.ICON_EDIT_BLACK), visiblePredicate, editFct);
                button.setTooltip(tooltip);
                return button;
            }
        });
    }
}

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

import fr.sirs.core.model.Element;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.internal.GeotkFX;

/**
 * A column allowing to delete the {@link Element} of a row. Two modes possible :
 * - Concrete deletion, which remove the element from database
 * - unlink mode, which dereference element from current list and parent element.
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class DeleteColumn  extends TableColumn<Element,Element>{

        public DeleteColumn(final BooleanProperty createNewProperty, final Consumer<Element> deletePojos, Callback cellValueFactory, Predicate visiblePredicate) {
            super("Suppression");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DELETE));

            final Tooltip deleteTooltip = new Tooltip("Supprimer l'élement");
            final Tooltip unlinkTooltip = new Tooltip("Dissocier l'élement");
            setCellValueFactory(cellValueFactory);
            setCellFactory((TableColumn<Element, Element> param) -> {
                final boolean realDelete = createNewProperty.get();
                final ButtonTableCell<Element, Element> button = new ButtonTableCell<>(false,
                        realDelete ? new ImageView(GeotkFX.ICON_DELETE) : new ImageView(GeotkFX.ICON_UNLINK),
                        visiblePredicate,
                        (Element t) -> {
                            final Alert confirm;
                            if (realDelete) {
                                confirm = new Alert(Alert.AlertType.WARNING, "Vous allez supprimer DEFINITIVEMENT l'entrée de la base de données. Êtes-vous sûr ?", ButtonType.NO, ButtonType.YES);
                            } else {
                                confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le lien ?", ButtonType.NO, ButtonType.YES);
                            }
                            confirm.setResizable(true);
                            final Optional<ButtonType> res = confirm.showAndWait();
                            if (res.isPresent() && ButtonType.YES.equals(res.get())) {
                                deletePojos.accept(t);
                                return null;
                            } else {
                                return t;
                            }
                        });

                if (realDelete) {
                    button.setTooltip(deleteTooltip);
                } else {
                    button.setTooltip(unlinkTooltip);
                }

                return button;
            });
        }
    
}
